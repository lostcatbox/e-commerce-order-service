package kr.hhplus.be.server.core.coupon.service

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

/**
 * Redis 기반 쿠폰 발급 대기열 서비스
 *
 * Redis List 자료구조를 활용한 FIFO 대기열 구현
 * - LPUSH: 대기열 앞쪽에 요청 추가 (선착순)
 * - RPOP: 대기열 뒤쪽에서 요청 제거 (처리)
 * - ObjectMapper를 활용한 안전한 JSON 직렬화/역직렬화
 */
@Service
class CouponIssueQueueService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private const val QUEUE_KEY_PREFIX = "coupon:issue:queue"
    }

    /**
     * 쿠폰 발급 요청을 대기열에 추가
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 생성된 요청 ID
     */
    fun addCouponIssueRequest(
        userId: Long,
        couponId: Long,
    ): String {
        val request = CouponIssueRequest.create(userId, couponId)
        val queueKey = getQueueKey(couponId)

        // ObjectMapper를 사용한 JSON 직렬화
        val jsonString = objectMapper.writeValueAsString(request)

        // Redis List 앞쪽에 요청 추가 (LPUSH로 선착순 보장)
        redisTemplate.opsForList().leftPush(queueKey, jsonString)

        return request.requestId
    }

    /**
     * 대기열에서 다음 쿠폰 발급 요청 조회 및 제거
     * @return 다음 처리할 요청, 없으면 null
     */
    fun getNextCouponIssueRequest(): CouponIssueRequest? {
        // 모든 쿠폰에 대해 대기열 키 조회
        val allQueues = getAllQueueKeys()

        for (queueKey in allQueues) {
            val jsonString = redisTemplate.opsForList().rightPop(queueKey)
            if (jsonString != null) {
                return try {
                    // ObjectMapper를 사용한 JSON 역직렬화
                    objectMapper.readValue(jsonString, CouponIssueRequest::class.java)
                } catch (e: Exception) {
                    // JSON 파싱 실패 시 로그 출력 후 다음 요청 처리
                    println("JSON 파싱 실패: $jsonString, 에러: ${e.message}")
                    continue
                }
            }
        }

        return null
    }

    /**
     * 특정 쿠폰의 대기열 크기 조회
     * @param couponId 쿠폰 ID
     * @return 대기열 크기
     */
    fun getQueueSize(couponId: Long): Long {
        val queueKey = getQueueKey(couponId)
        return redisTemplate.opsForList().size(queueKey) ?: 0L
    }

    /**
     * 쿠폰별 대기열 키 생성
     * @param couponId 쿠폰 ID
     * @return Redis 키
     */
    private fun getQueueKey(couponId: Long): String = "$QUEUE_KEY_PREFIX:$couponId"

    /**
     * 현재 존재하는 모든 대기열 키 조회
     * @return 대기열 키 목록
     */
    private fun getAllQueueKeys(): Set<String> = redisTemplate.keys("$QUEUE_KEY_PREFIX:*") ?: emptySet()
}
