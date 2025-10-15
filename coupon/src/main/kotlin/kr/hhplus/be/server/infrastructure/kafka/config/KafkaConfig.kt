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
 *
 * 특징:
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
     */
    @Bean
    fun producerFactory(): ProducerFactory<String, String> {
        val configProps =
            mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            )
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> = KafkaTemplate(producerFactory())

    /**
     * Kafka Consumer 설정
     * - 수동 커밋으로 정확한 처리 보장
     */
    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> {
        val configProps =
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG to groupId,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                // 수동 커밋으로 정확한 처리 보장
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            )
        return DefaultKafkaConsumerFactory(configProps)
    }

    /**
     * Kafka Listener Container Factory 설정
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
        return factory
    }
}
