package kr.hhplus.be.server.infrastructure.kafka.service

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest
import kr.hhplus.be.server.core.coupon.service.CouponIssueQueueServiceInterface
import kr.hhplus.be.server.event.CouponIssueEvent
import kr.hhplus.be.server.infrastructure.kafka.producer.CouponIssueEventProducer
import org.springframework.stereotype.Component

/**
 * Kafka 기반 쿠폰 발급 대기열 서비스 구현체
 *
 * 설명:
 * - Kafka Producer를 통해 쿠폰 발급 이벤트를 토픽으로 발행
 * - Consumer가 메시지를 소비하여 비동기로 쿠폰 발급 처리
 * - 기존 스케줄러 방식을 대체하는 이벤트 기반 아키텍처
 *
 * 특징:
 * - 이벤트 기반 비동기 처리
 * - 높은 처리량과 확장성
 * - 메시지 순서 보장 (파티션 키 사용)
 */
@Component
class KafkaCouponIssueQueueService(
    private val couponIssueEventProducer: CouponIssueEventProducer,
    private val objectMapper: ObjectMapper,
) : CouponIssueQueueServiceInterface {
    companion object {
        private const val QUEUE_KEY_PREFIX = "kafka:coupon:issue:queue"
    }

    /**
     * 쿠폰 발급 요청을 Kafka로 발행
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 생성된 이벤트 ID
     */
    override fun addCouponIssueRequest(
        userId: Long,
        couponId: Long,
    ): String {
        val event = CouponIssueEvent.create(userId, couponId)

        val success = couponIssueEventProducer.publishCouponIssueEvent(event)
        if (success) {
            println("Kafka로 쿠폰 발급 이벤트 발행 성공 - EventId: ${event.eventId}, UserId: $userId, CouponId: $couponId")
            return event.eventId
        } else {
            throw RuntimeException("Kafka 이벤트 발행 실패 - UserId: $userId, CouponId: $couponId")
        }
    }

    /**
     * 다음 쿠폰 발급 요청 조회 및 제거
     *
     * 참고: Kafka 환경에서는 Consumer가 직접 메시지를 소비하므로 사용되지 않음
     * 인터페이스 호환성을 위해 null 반환
     *
     * @return 항상 null (Kafka Consumer가 직접 처리)
     */
    override fun getNextCouponIssueRequest(): CouponIssueRequest? {
        println("Kafka 환경에서는 Consumer가 직접 메시지를 처리합니다.")
        return null
    }

    /**
     * 특정 쿠폰의 대기열 크기 조회
     *
     * 참고: Kafka 환경에서는 Consumer Lag으로 대기열 크기를 측정해야 함
     * 현재는 단순화하여 0 반환 (실제 구현에서는 Kafka 메트릭 활용)
     *
     * @param couponId 쿠폰 ID
     * @return 대기열 크기 (현재는 측정 불가능으로 0 반환)
     */
    override fun getQueueSize(couponId: Long): Long {
        println("Kafka 환경에서는 Consumer Lag을 통해 대기열 크기를 측정해야 합니다.")
        return 0L
    }
}
