package kr.hhplus.be.server.infrastructure.kafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest
import kr.hhplus.be.server.core.coupon.service.CouponService
import kr.hhplus.be.server.event.CouponIssueEvent
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

/**
 * 쿠폰 발급 이벤트 Kafka Consumer
 *
 * 설명:
 * - Kafka 토픽에서 쿠폰 발급 이벤트 소비
 * - 비동기로 쿠폰 발급 처리
 * - 수동 커밋으로 정확한 처리 보장
 * - 실제 쿠폰 발급 비즈니스 로직 처리
 */
@Service
class CouponIssueEventConsumer(
    private val objectMapper: ObjectMapper,
    private val couponService: CouponService,
) {
    /**
     * 쿠폰 발급 이벤트 처리
     */
    @KafkaListener(
        topics = ["\${app.kafka.topic.coupon-issue}"],
        groupId = "\${spring.kafka.consumer.group-id}",
    )
    fun handleCouponIssueEvent(
        @Payload payload: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment,
    ) {
        println("쿠폰 발급 이벤트 수신 - Partition: $partition, Offset: $offset")

        try {
            // 1. JSON 역직렬화
            val event = objectMapper.readValue(payload, CouponIssueEvent::class.java)

            // 2. 이벤트 검증
            if (!event.isValid()) {
                println("잘못된 이벤트 형식 - EventId: ${event.eventId}")
                acknowledgment.acknowledge() // 잘못된 이벤트는 재처리하지 않음
                return
            }

            println("쿠폰 발급 이벤트 처리 시작 - EventId: ${event.eventId}, UserId: ${event.userId}, CouponId: ${event.couponId}")

            // 3. 실제 쿠폰 발급 처리
            val request =
                CouponIssueRequest.create(
                    userId = event.userId,
                    couponId = event.couponId,
                )
            val userCouponId = couponService.issueCoupon(request)

            // 4. 결과 처리
            println("쿠폰 발급 성공 - EventId: ${event.eventId}, UserCouponId: $userCouponId")
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            println("쿠폰 발급 이벤트 처리 실패 - Partition: $partition, Offset: $offset, Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
