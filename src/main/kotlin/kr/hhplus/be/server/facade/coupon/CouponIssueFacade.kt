package kr.hhplus.be.server.facade.coupon

import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest
import kr.hhplus.be.server.core.coupon.domain.UserCoupon
import kr.hhplus.be.server.core.coupon.service.CouponServiceInterface
import kr.hhplus.be.server.core.coupon.service.UserCouponServiceInterface
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 쿠폰 발급 처리 파사드
 *
 * 설명:
 * 스케줄러에서 분리된 순수한 쿠폰 발급 비즈니스 로직을 담당합니다.
 * 기존 CouponFacade의 동기 처리 로직과 동일한 검증 및 발급 과정을 수행합니다.
 *
 * 특징:
 * - 중복 발급 방지 검증
 * - 쿠폰 재고 차감
 * - 사용자 쿠폰 생성
 * - 트랜잭션 단위로 안전한 처리
 * - 스케줄러와 비즈니스 로직의 관심사 분리
 */
@Service
class CouponIssueFacade(
    private val couponService: CouponServiceInterface,
    private val userCouponService: UserCouponServiceInterface,
) {
    /**
     * 쿠폰 발급 요청 처리
     *
     * 처리 과정:
     * 1. 중복 발급 검증 (UserCouponService에서 처리)
     * 2. 쿠폰 재고 차감 (CouponService에서 처리)
     * 3. 사용자 쿠폰 생성 (UserCouponService에서 처리)
     *
     * 주의사항:
     * - 사용자 검증은 요청 시점에 이미 완료되었다고 가정
     * - 모든 처리는 트랜잭션 단위로 수행
     * - 기존 CouponFacade.issueCoupon()과 동일한 로직
     *
     * @param request 쿠폰 발급 요청
     * @return 발급된 UserCoupon
     * @throws IllegalStateException 중복 발급 시
     * @throws IllegalArgumentException 쿠폰 재고 부족 시
     */
    @Transactional
    fun processIssueRequest(request: CouponIssueRequest): UserCoupon {
        try {
            // 1. 쿠폰 재고 차감 (중복 발급 검증은 UserCouponService에서 수행)
            couponService.issueCoupon(request.couponId)

            // 2. 사용자 쿠폰 생성 (내부에서 중복 발급 검증 수행)
            val userCoupon = userCouponService.createUserCoupon(request.userId, request.couponId)

            return userCoupon
        } catch (e: Exception) {
            // 예외를 다시 던져서 스케줄러에서 적절히 처리할 수 있도록 함
            throw e
        }
    }

    /**
     * 쿠폰 발급 요청 검증
     *
     * @param request 쿠폰 발급 요청
     * @return 검증 성공 여부
     */
    fun validateRequest(request: CouponIssueRequest): Boolean {
        return try {
            require(request.userId > 0) { "사용자 ID가 유효하지 않습니다: ${request.userId}" }
            require(request.couponId > 0) { "쿠폰 ID가 유효하지 않습니다: ${request.couponId}" }
            require(request.requestId.isNotBlank()) { "요청 ID가 유효하지 않습니다: ${request.requestId}" }
            require(request.timestamp > 0) { "타임스탬프가 유효하지 않습니다: ${request.timestamp}" }
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
