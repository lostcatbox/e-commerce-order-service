package kr.hhplus.be.server.infrastructure.kafka.config

import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaAdmin

/**
 * Kafka 토픽 자동 생성 설정
 *
 * 설명:
 * - 애플리케이션 시작 시 필요한 토픽 자동 생성
 * - 파티션 수와 복제 팩터 설정
 * - 토픽별 설정 최적화
 */
@Configuration
class KafkaTopicConfig {

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Value("\${app.kafka.topic.coupon-issue}")
    private lateinit var couponIssueTopicName: String

    /**
     * Kafka Admin 클라이언트 설정
     */
    @Bean
    fun kafkaAdmin(): KafkaAdmin {
        val configs = mapOf(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers
        )
        return KafkaAdmin(configs)
    }

    /**
     * 쿠폰 발급 이벤트 토픽 생성
     * - 파티션 3개: 수평 확장성 확보
     * - 복제 팩터 1: 개발 환경 (운영환경에서는 3으로 설정 권장)
     * - 압축 설정: 성능 최적화
     */
    @Bean
    fun couponIssueEventsTopic(): NewTopic {
        return NewTopic(couponIssueTopicName, 3, 1.toShort())
            .configs(mapOf(
                "compression.type" to "snappy",
                "cleanup.policy" to "delete",
                "retention.ms" to "86400000", // 24시간
                "segment.ms" to "3600000"     // 1시간
            ))
    }
}
