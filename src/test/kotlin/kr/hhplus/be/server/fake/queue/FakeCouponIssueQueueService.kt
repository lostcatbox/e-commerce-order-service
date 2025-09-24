package kr.hhplus.be.server.fake.queue

import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest
import kr.hhplus.be.server.core.coupon.service.CouponIssueQueueServiceInterface
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

/**
 * 테스트용 가짜 쿠폰 발급 대기열 서비스
 * 
 * 설명:
 * - Redis 의존성 없이 메모리 기반으로 동작하는 대기열 구현
 * - 실제 Redis 동작과 동일한 FIFO 방식으로 구현
 * - Thread-safe한 ConcurrentHashMap과 LinkedBlockingQueue 사용
 * - 테스트 시 외부 인프라 의존성 제거
 */
class FakeCouponIssueQueueService : CouponIssueQueueServiceInterface {
    
    // 쿠폰별 대기열을 관리하는 Map
    private val queues: ConcurrentHashMap<Long, LinkedBlockingQueue<CouponIssueRequest>> = ConcurrentHashMap()
    
    override fun addCouponIssueRequest(userId: Long, couponId: Long): String {
        val request = CouponIssueRequest.create(userId, couponId)
        
        // 쿠폰별 대기열이 없으면 생성
        val queue = queues.computeIfAbsent(couponId) { LinkedBlockingQueue() }
        
        // 대기열에 요청 추가 (FIFO)
        queue.offer(request)
        
        return request.requestId
    }

    override fun getNextCouponIssueRequest(): CouponIssueRequest? {
        // 모든 대기열을 순회하면서 요청 조회
        for (queue in queues.values) {
            val request = queue.poll() // null-safe poll
            if (request != null) {
                return request
            }
        }
        return null
    }

    override fun getQueueSize(couponId: Long): Long {
        return queues[couponId]?.size?.toLong() ?: 0L
    }
    
    /**
     * 테스트 전용: 모든 대기열 초기화
     */
    fun clear() {
        queues.clear()
    }
    
    /**
     * 테스트 전용: 전체 대기열 개수 조회
     */
    fun getTotalQueueCount(): Int {
        return queues.values.sumOf { it.size }
    }
}
