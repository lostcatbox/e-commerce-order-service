package kr.hhplus.be.server.infrastructure.kafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.core.coupon.domain.CouponIssueEvent
import kr.hhplus.be.server.core.coupon.service.CouponIssueProcessor
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
    private val couponIssueProcessor: CouponIssueProcessor,
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
            val request = event.toCouponIssueRequest()
            val result = couponIssueProcessor.processRequest(request)

            // 4. 결과 처리
            if (result.isSuccess()) {
                println("쿠폰 발급 성공 - EventId: ${event.eventId}, UserCouponId: ${result.userCoupon?.userCouponId}")
                acknowledgment.acknowledge()
            } else {
                println("쿠폰 발급 실패 - EventId: ${event.eventId}, Status: ${result.status}, Error: ${result.errorMessage}")

                // 재시도 가능한 에러인 경우에만 예외 발생 (시스템 에러)
                if (result.isRetryable()) {
                    throw RuntimeException("재시도 가능한 쿠폰 발급 실패: ${result.errorMessage}")
                } else {
                    // 비즈니스 에러(재고 부족, 중복 발급 등)는 재시도하지 않음
                    acknowledgment.acknowledge()
                }
            }
        } catch (e: com.fasterxml.jackson.core.JsonProcessingException) {
            println("JSON 파싱 실패 - Partition: $partition, Offset: $offset, Error: ${e.message}")
            acknowledgment.acknowledge() // JSON 오류는 재처리하지 않음
        } catch (e: Exception) {
            println("쿠폰 발급 이벤트 처리 실패 - Partition: $partition, Offset: $offset, Error: ${e.message}")
            e.printStackTrace()
            throw e // 재시도를 위해 예외 다시 던지기
        }
    }
}
