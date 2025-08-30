package kr.hhplus.be.server.core.coupon.service

import kr.hhplus.be.server.core.coupon.domain.UserCoupon

/**
 * 사용자 쿠폰 서비스 인터페이스
 */
interface UserCouponServiceInterface {
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
}
