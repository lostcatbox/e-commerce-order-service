package kr.hhplus.be.server.coupon.service

import kr.hhplus.be.server.coupon.domain.Coupon
import kr.hhplus.be.server.coupon.domain.UserCoupon

/**
 * 쿠폰 서비스 인터페이스
 */
interface CouponServiceInterface {
    
    /**
     * 쿠폰 정보 조회
     * @param couponId 쿠폰 ID
     * @return Coupon
     */
    fun getCouponInfo(couponId: Long): Coupon
    
    /**
     * 선착순 쿠폰 발급
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 발급된 UserCoupon
     */
    fun issueCoupon(userId: Long, couponId: Long): UserCoupon
    
    /**
     * 사용자의 쿠폰 목록 조회
     * @param userId 사용자 ID
     * @return 사용자의 쿠폰 목록
     */
    fun getUserCoupons(userId: Long): List<UserCoupon>

    /**
     * 발급된 쿠폰 사용
     * @param userCouponId 사용자 쿠폰 ID
     * @return 사용된 쿠폰 정보
     */
    fun useIssuedCoupon(userCouponId: Long?): Coupon?
}
