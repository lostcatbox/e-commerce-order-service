package kr.hhplus.be.server.infrastructure.kafka.service

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.core.coupon.domain.CouponIssueEvent
import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest
import kr.hhplus.be.server.core.coupon.service.CouponIssueQueueServiceInterface
import kr.hhplus.be.server.infrastructure.kafka.producer.CouponIssueEventProducer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * Kafka 기반 쿠폰 발급 대기열 서비스 구현체
 *
 * 설명:
 * - CouponIssueQueueServiceInterface의 Kafka 구현체
 * - 기존 비즈니스 로직은 전혀 변경하지 않고 큐 구현체만 교체
 * - Producer는 Kafka로 이벤트 발행
 * - Consumer에서 처리된 결과는 메모리 큐로 시뮬레이션 (실제로는 DB에서 조회)
 *
 * 특징:
 * - CouponService는 수정하지 않고 그대로 사용
 * - 스케줄러도 기존 로직 그대로 사용
 * - 설정으로 Redis/Kafka 구현체 선택 가능
 */
@Component
@ConditionalOnProperty(name = ["app.queue.type"], havingValue = "kafka")
class KafkaCouponIssueQueueService(
    private val couponIssueEventProducer: CouponIssueEventProducer,
    private val objectMapper: ObjectMapper
) : CouponIssueQueueServiceInterface {

    // Kafka Consumer에서 처리된 결과를 임시로 저장하는 큐 (실제로는 DB 조회로 대체)
    private val processedRequests = ConcurrentLinkedQueue<CouponIssueRequest>()
    private val queueSizeCounter = AtomicLong(0)

    companion object {
        private const val QUEUE_KEY_PREFIX = "kafka:coupon:issue:queue"
    }

    /**
     * 쿠폰 발급 요청을 Kafka로 발행
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 생성된 이벤트 ID
     */
    override fun addCouponIssueRequest(userId: Long, couponId: Long): String {
        val event = CouponIssueEvent.create(userId, couponId)

        val success = couponIssueEventProducer.publishCouponIssueEvent(event)
        if (success) {
            queueSizeCounter.incrementAndGet()
            println("Kafka로 쿠폰 발급 이벤트 발행 성공 - EventId: ${event.eventId}, UserId: $userId, CouponId: $couponId")
            return event.eventId
        } else {
            throw RuntimeException("Kafka 이벤트 발행 실패 - UserId: $userId, CouponId: $couponId")
        }
    }

    /**
     * 다음 쿠폰 발급 요청 조회 및 제거
     *
     * 참고: 실제 Kafka 환경에서는 이 메서드가 호출되지 않음
     * - Kafka Consumer가 직접 메시지를 소비하여 처리
     * - 이 메서드는 기존 스케줄러와의 호환성을 위해 구현
     *
     * @return 다음 처리할 요청, 없으면 null
     */
    override fun getNextCouponIssueRequest(): CouponIssueRequest? {
        val request = processedRequests.poll()
        if (request != null) {
            queueSizeCounter.decrementAndGet()
            println("Kafka 큐에서 요청 조회 - RequestId: ${request.requestId}")
        }
        return request
    }

    /**
     * 특정 쿠폰의 대기열 크기 조회
     *
     * 참고: Kafka 환경에서는 정확한 큐 크기 측정이 어려움
     * - 토픽의 메시지 수는 Consumer Lag으로 측정 가능
     * - 여기서는 단순화하여 카운터 사용
     *
     * @param couponId 쿠폰 ID
     * @return 대기열 크기 (근사치)
     */
    override fun getQueueSize(couponId: Long): Long {
        return maxOf(0, queueSizeCounter.get())
    }

    /**
     * Consumer에서 처리가 완료된 요청을 큐에 추가 (시뮬레이션용)
     * 실제로는 이 메서드가 필요 없음 - Consumer가 직접 DB에 저장
     */
    fun addProcessedRequest(request: CouponIssueRequest) {
        processedRequests.offer(request)
        println("처리 완료된 요청 추가 - RequestId: ${request.requestId}")
    }

    /**
     * 큐 상태 초기화 (테스트용)
     */
    fun clearQueue() {
        processedRequests.clear()
        queueSizeCounter.set(0)
        println("Kafka 큐 상태 초기화 완료")
    }
}
