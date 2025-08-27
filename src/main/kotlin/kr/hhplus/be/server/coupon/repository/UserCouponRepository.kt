package kr.hhplus.be.server.coupon.repository

import kr.hhplus.be.server.coupon.domain.UserCoupon

/**
 * 유저 쿠폰 Repository 인터페이스
 */
interface UserCouponRepository {

    /**
     * 사용자의 쿠폰 목록 조회
     * @param userId 사용자 ID
     * @return 사용자의 쿠폰 목록
     */
    fun findByUserId(userId: Long): List<UserCoupon>

    /**
     * 사용자의 특정 쿠폰 조회
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return UserCoupon 또는 null (쿠폰이 존재하지 않는 경우)
     */
    fun findByUserIdAndCouponId(userId: Long, couponId: Long): UserCoupon?

    /**
     * 유저 쿠폰 저장/업데이트
     * @param userCoupon 저장할 유저 쿠폰
     * @return 저장된 UserCoupon
     */
    fun save(userCoupon: UserCoupon): UserCoupon

    /**
     * 사용자의 사용 가능한 쿠폰 목록 조회
     * @param userId 사용자 ID
     * @return 사용 가능한 쿠폰 목록
     */
    fun findUsableCouponsByUserId(userId: Long): List<UserCoupon>
}
