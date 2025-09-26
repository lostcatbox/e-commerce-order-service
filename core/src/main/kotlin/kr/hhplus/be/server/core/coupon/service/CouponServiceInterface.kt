package kr.hhplus.be.server.core.coupon.service

import kr.hhplus.be.server.core.coupon.domain.Coupon
import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest
import kr.hhplus.be.server.core.coupon.domain.CouponIssueResponse
import kr.hhplus.be.server.core.coupon.domain.UserCoupon

/**
 * 쿠폰 서비스 인터페이스 - 쿠폰 도메인의 단일 진입점
 *
 * 설명:
 * - 기존 Facade 패턴을 제거하고 모든 쿠폰 관련 기능을 통합
 * - 외부(Controller, Scheduler 등)에서는 이 인터페이스만 사용
 * - 내부 서비스들(UserCouponService, CouponIssueQueueService)은 구현 세부사항으로 캡슐화
 */
interface CouponServiceInterface {
    /**
     * 쿠폰 정보 조회
     * @param couponId 쿠폰 ID
     * @return Coupon
     */
    fun getCouponInfo(couponId: Long): Coupon

    /**
     * 쿠폰 발급 요청 (비동기)
     *
     * 처리 과정:
     * 1. 사용자 검증
     * 2. 쿠폰 재고 확인 (RDB에서 조회만)
     * 3. 재고가 있으면 대기열에 추가 후 즉시 응답
     * 4. 실제 발급은 스케줄러가 비동기로 처리
     *
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 발급 요청 응답 (즉시 응답)
     */
    fun requestCouponIssueAsync(
        userId: Long,
        couponId: Long,
    ): CouponIssueResponse

    /**
     * 특정 쿠폰의 대기열 크기 조회
     * @param couponId 쿠폰 ID
     * @return 대기열 크기
     */
    fun getQueueSize(couponId: Long): Long

    // ===== 쿠폰 발급 처리 기능 (기존 CouponIssueFacade) =====

    /**
     * 쿠폰 발급 처리 (스케줄러에서 사용)
     *
     * 완전한 쿠폰 발급 프로세스:
     * 1. 쿠폰 재고 차감 (분산락 + 비관적 락으로 동시성 제어)
     * 2. 사용자 쿠폰 생성 (중복 발급 검증 포함)
     *
     * 단일 트랜잭션으로 처리하여 데이터 정합성 보장
     *
     * @param request 쿠폰 발급 요청
     * @return 발급된 UserCoupon
     * @throws IllegalStateException 중복 발급 시
     * @throws IllegalArgumentException 쿠폰 재고 부족 시
     */
    fun issueCoupon(request: CouponIssueRequest): UserCoupon

    /**
     * 쿠폰 발급 요청 검증
     * @param request 쿠폰 발급 요청
     * @return 검증 성공 여부
     */
    fun validateRequest(request: CouponIssueRequest): Boolean

    /**
     * 대기열에서 다음 쿠폰 발급 요청 조회 및 제거
     * @return 다음 처리할 요청, 없으면 null
     */
    fun getNextCouponIssueRequest(): CouponIssueRequest?

    /**
     * 사용자 쿠폰 발급
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 발급된 UserCoupon
     */
    fun createUserCoupon(
        userId: Long,
        couponId: Long,
    ): UserCoupon

    /**
     * 사용자의 쿠폰 목록 조회
     * @param userId 사용자 ID
     * @return 사용자의 쿠폰 목록
     */
    fun getUserCoupons(userId: Long): List<UserCoupon>

    /**
     * 사용자의 사용 가능한 쿠폰 목록 조회
     * @param userId 사용자 ID
     * @return 사용 가능한 쿠폰 목록
     */
    fun getUsableCoupons(userId: Long): List<UserCoupon>

    /**
     * 사용자 쿠폰 사용
     * @param userCouponId 사용자 쿠폰 ID
     * @return 사용된 UserCoupon
     */
    fun useCoupon(userCouponId: Long): UserCoupon

    /**
     * 사용자 쿠폰 조회
     * @param usedCouponId 사용자 쿠폰 ID
     * @return UserCoupon
     */
    fun getUserCoupon(usedCouponId: Long): UserCoupon
}
