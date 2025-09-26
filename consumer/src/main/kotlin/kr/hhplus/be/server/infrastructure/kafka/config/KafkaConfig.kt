package kr.hhplus.be.server.infrastructure.kafka.config

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ContainerProperties

/**
 * Kafka 설정 클래스
 *
 * 설명:
 * - Kafka Producer/Consumer 설정을 관리
 * - 쿠폰 발급 이벤트 처리를 위한 최적화된 설정
 * - 순서 보장, 백프레셔, 병렬 처리 지원
 *
 * 특징:
 * - Producer: acks=all, enable.idempotence=true (정확히 한 번 전송)
 * - Consumer: 수동 커밋, 배치 처리 지원
 * - 파티션별 순서 보장 (쿠폰ID를 키로 사용)
 */
@Configuration
class KafkaConfig {

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.consumer.group-id}")
    private lateinit var groupId: String

    /**
     * Kafka Producer 설정
     * - 정확히 한 번 전송 보장 (idempotent)
     * - 모든 replica 확인 후 ack (acks=all)
     * - 배치 처리 최적화
     */
    @Bean
    fun producerFactory(): ProducerFactory<String, String> {
        val configProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,

            // 정확히 한 번 전송 보장
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to Int.MAX_VALUE,
            ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to 1,

            // 성능 최적화
            ProducerConfig.BATCH_SIZE_CONFIG to 16384,
            ProducerConfig.LINGER_MS_CONFIG to 5,
            ProducerConfig.COMPRESSION_TYPE_CONFIG to "snappy"
        )
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> {
        return KafkaTemplate(producerFactory())
    }

    /**
     * Kafka Consumer 설정
     * - 수동 커밋으로 정확한 처리 보장
     * - 배치 처리 지원
     * - 백프레셔 제어
     */
    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> {
        val configProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,

            // 수동 커밋으로 정확한 처리 보장
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,

            // 배치 처리 최적화
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 10,
            ConsumerConfig.FETCH_MIN_BYTES_CONFIG to 1024,
            ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG to 500,

            // 처리 실패 시 재처리 방지
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
        )
        return DefaultKafkaConsumerFactory(configProps)
    }

    /**
     * Kafka Listener Container Factory 설정
     * - 동시성 제어
     * - 에러 핸들링
     * - 수동 커밋 설정
     */
    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory()

        // 동시성 설정 (파티션 수와 맞춤)
        factory.setConcurrency(3)

        // 수동 커밋 설정
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE

        // 배치 리스너 비활성화 (개별 메시지 처리)
        factory.isBatchListener = false

        return factory
    }
}
