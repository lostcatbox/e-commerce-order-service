package kr.hhplus.be.server.core.coupon.service

import kr.hhplus.be.server.core.coupon.domain.Coupon

/**
 * 쿠폰 서비스 인터페이스 - 쿠폰 도메인만 담당
 */
interface CouponServiceInterface {
    /**
     * 쿠폰 정보 조회
     * @param couponId 쿠폰 ID
     * @return Coupon
     */
    fun getCouponInfo(couponId: Long): Coupon

    /**
     * 쿠폰 재고 차감 (발급 시)
     * @param couponId 쿠폰 ID
     * @return 재고 차감된 Coupon
     */
    fun issueCoupon(couponId: Long): Coupon
}
