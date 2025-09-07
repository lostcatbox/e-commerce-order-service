package kr.hhplus.be.server.infra.persistence.coupon.jpa

import kr.hhplus.be.server.core.coupon.domain.UserCoupon
import kr.hhplus.be.server.core.coupon.domain.UserCouponStatus
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 유저 쿠폰 JPA Repository 인터페이스
 */
interface JpaUserCouponRepository : JpaRepository<UserCoupon, Long> {
    /**
     * 사용자 ID로 쿠폰 목록 조회
     * @param userId 사용자 ID
     * @return 사용자의 쿠폰 목록
     */
    fun findByUserId(userId: Long): List<UserCoupon>

    /**
     * 사용자 ID와 쿠폰 ID로 쿠폰 조회
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return UserCoupon 또는 null
     */
    fun findByUserIdAndCouponId(userId: Long, couponId: Long): UserCoupon?

    /**
     * 사용자 쿠폰 ID로 조회
     * @param userCouponId 사용자 쿠폰 ID
     * @return UserCoupon 또는 null
     */
    fun findByUserCouponId(userCouponId: Long): UserCoupon?

    /**
     * 사용자의 사용 가능한 쿠폰 목록 조회
     * @param userId 사용자 ID
     * @param status 쿠폰 상태
     * @return 사용 가능한 쿠폰 목록
     */
    fun findByUserIdAndStatus(userId: Long, status: UserCouponStatus): List<UserCoupon>
}
