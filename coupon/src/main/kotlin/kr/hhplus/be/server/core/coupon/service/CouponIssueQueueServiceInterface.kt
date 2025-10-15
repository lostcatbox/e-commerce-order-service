package kr.hhplus.be.server.core.coupon.service

import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest

/**
 * 쿠폰 발급 대기열 서비스 인터페이스
 *
 * 설명:
 * - 쿠폰 발급 요청을 대기열에 추가하고 처리하는 추상화된 인터페이스
 * - Redis, Memory, Database 등 다양한 구현체로 교체 가능
 * - 테스트 시에는 Fake 구현체로 대체하여 외부 의존성 제거
 */
interface CouponIssueQueueServiceInterface {
    /**
     * 쿠폰 발급 요청을 대기열에 추가
     *
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 생성된 요청 ID
     */
    fun addCouponIssueRequest(
        userId: Long,
        couponId: Long,
    ): String

    /**
     * 대기열에서 다음 쿠폰 발급 요청 조회 및 제거
     *
     * @return 다음 처리할 요청, 대기열이 비어있으면 null
     */
    fun getNextCouponIssueRequest(): CouponIssueRequest?

    /**
     * 특정 쿠폰의 대기열 크기 조회
     *
     * @param couponId 쿠폰 ID
     * @return 대기열 크기
     */
    fun getQueueSize(couponId: Long): Long
}
