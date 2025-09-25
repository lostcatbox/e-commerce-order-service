package kr.hhplus.be.server.infrastructure.kafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.core.coupon.domain.CouponIssueEvent
import kr.hhplus.be.server.core.coupon.service.CouponIssueProcessor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

/**
 * 쿠폰 발급 이벤트 Kafka Consumer (Kafka 큐 전용)
 *
 * 설명:
 * - Kafka를 사용할 때만 활성화되는 Consumer
 * - Redis 큐를 사용하는 경우에는 스케줄러가 처리하므로 비활성화
 * - CouponIssueProcessor를 통한 공통 처리 로직 사용
 *
 * 특징:
 * - Kafka 큐 전용 (app.queue.type=kafka일 때만 활성화)
 * - 수동 커밋으로 정확한 처리 보장
 * - 에러 처리 및 재시도 로직
 * - 여러 인스턴스로 수평 확장 가능
 * - 파티션별 순서 보장 및 백프레셔 제어
 */
@Service
@ConditionalOnProperty(name = ["app.queue.type"], havingValue = "kafka")
class CouponIssueEventConsumer(
    private val couponIssueProcessor: CouponIssueProcessor,
    private val objectMapper: ObjectMapper
) {

    /**
     * 쿠폰 발급 이벤트 처리
     *
     * @param record Kafka 레코드
     * @param payload 메시지 내용
     * @param partition 파티션 번호
     * @param offset 오프셋
     * @param acknowledgment 수동 커밋용 객체
     */
    @KafkaListener(
        topics = ["\${app.kafka.topic.coupon-issue}"],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleCouponIssueEvent(
        record: ConsumerRecord<String, String>,
        @Payload payload: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        println("쿠폰 발급 이벤트 수신 - Partition: $partition, Offset: $offset")

        try {
            // JSON 역직렬화
            val event = deserializeEvent(payload)
            if (event == null) {
                println("쿠폰 발급 이벤트 역직렬화 실패 - Payload: $payload")
                acknowledgment.acknowledge() // 잘못된 메시지는 건너뛰기
                return
            }

            // CouponIssueProcessor를 통한 이벤트 처리
            val request = event.toCouponIssueRequest()
            val result = couponIssueProcessor.processRequest(request)

            // 처리 결과에 따른 커밋 처리
            if (result.isRetryable()) {
                // 재시도 가능한 에러 (시스템 에러)는 커밋하지 않음
                println("재시도 가능한 에러 발생 - EventId: ${event.eventId}, Error: ${result.errorMessage}")
                throw RuntimeException("재시도 필요: ${result.errorMessage}")
            } else {
                // 성공 또는 재시도 불가능한 에러는 커밋
                acknowledgment.acknowledge()

                if (result.isSuccess()) {
                    println("쿠폰 발급 이벤트 처리 완료 - EventId: ${event.eventId}, " +
                            "UserId: ${event.userId}, CouponId: ${event.couponId}, " +
                            "UserCouponId: ${result.userCoupon?.userCouponId}")
                } else {
                    println("쿠폰 발급 이벤트 처리 실패 (재시도 안함) - EventId: ${event.eventId}, " +
                            "Status: ${result.status}, Error: ${result.errorMessage}")
                }
            }

        } catch (e: Exception) {
            // 예상치 못한 시스템 에러
            handleSystemError(e, payload, partition, offset)
            throw e // 컨테이너가 재시도 처리
        }
    }

    /**
     * JSON 문자열을 CouponIssueEvent로 역직렬화
     */
    private fun deserializeEvent(payload: String): CouponIssueEvent? {
        return try {
            val event = objectMapper.readValue(payload, CouponIssueEvent::class.java)
            if (event.isValid()) {
                event
            } else {
                println("유효하지 않은 쿠폰 발급 이벤트: $event")
                null
            }
        } catch (e: Exception) {
            println("쿠폰 발급 이벤트 JSON 파싱 실패: $payload, Error: ${e.message}")
            null
        }
    }


    /**
     * 시스템 에러 처리
     * (DB 연결 실패, 네트워크 오류 등)
     */
    private fun handleSystemError(
        error: Exception,
        payload: String,
        partition: Int,
        offset: Long
    ) {
        println("시스템 에러 발생 - 재처리 예정 - Partition: $partition, Offset: $offset, Error: ${error.message}")
        error.printStackTrace()

        // TODO: 필요시 알림 발송, 모니터링 메트릭 증가 등
    }
}
