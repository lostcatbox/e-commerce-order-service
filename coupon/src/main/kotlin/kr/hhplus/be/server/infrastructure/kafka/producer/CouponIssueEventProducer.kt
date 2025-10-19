package kr.hhplus.be.server.infrastructure.kafka.producer

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.event.CouponIssueEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

/**
 * 쿠폰 발급 이벤트 Kafka Producer
 *
 * 설명:
 * - 쿠폰 발급 이벤트를 Kafka 토픽으로 발행
 * - 파티션 키를 이용한 순서 보장
 * - 비동기 처리 및 에러 핸들링
 *
 * 특징:
 * - 쿠폰별 순서 보장 (파티션 키 사용)
 * - JSON 직렬화
 * - 전송 결과 모니터링
 * - 예외 처리 및 로깅
 */
@Service
class CouponIssueEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    @Value("\${app.kafka.topic.coupon-issue}")
    private lateinit var topicName: String

    /**
     * 쿠폰 발급 이벤트 발행
     * @param event 발행할 이벤트
     * @return 발행 성공 여부
     */
    fun publishCouponIssueEvent(event: CouponIssueEvent): Boolean {
        return try {
            // 이벤트 검증
            if (!event.isValid()) {
                println("유효하지 않은 쿠폰 발급 이벤트: $event")
                return false
            }

            // JSON 직렬화
            val messagePayload = objectMapper.writeValueAsString(event)
            val partitionKey = event.getPartitionKey()

            println("쿠폰 발급 이벤트 발행 시작 - EventId: ${event.eventId}, UserId: ${event.userId}, CouponId: ${event.couponId}")

            // Kafka로 메시지 발행 (파티션 키 포함)
            val future: CompletableFuture<SendResult<String, String>> =
                kafkaTemplate.send(topicName, partitionKey, messagePayload)

            // 성공/실패 콜백 설정
            future.whenComplete { result, exception ->
                if (exception == null) {
                    val metadata = result.recordMetadata
                    println(
                        "쿠폰 발급 이벤트 발행 성공 - EventId: ${event.eventId}, " +
                            "Topic: ${metadata.topic()}, Partition: ${metadata.partition()}, Offset: ${metadata.offset()}",
                    )
                } else {
                    println("쿠폰 발급 이벤트 발행 실패 - EventId: ${event.eventId}, Error: ${exception.message}")
                    exception.printStackTrace()
                }
            }

            true
        } catch (e: Exception) {
            println("쿠폰 발급 이벤트 발행 중 예외 발생 - EventId: ${event.eventId}, Error: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}
