package kr.hhplus.be.server.infrastructure.persistance.coupon

import kr.hhplus.be.server.core.coupon.domain.UserCoupon
import kr.hhplus.be.server.core.coupon.domain.UserCouponStatus
import kr.hhplus.be.server.core.coupon.repository.UserCouponRepository
import kr.hhplus.be.server.infrastructure.persistance.coupon.jpa.JpaUserCouponRepository
import org.springframework.stereotype.Repository

/**
 * 유저 쿠폰 Repository 구현체
 */
@Repository
class UserCouponRepositoryImpl(
    private val jpaUserCouponRepository: JpaUserCouponRepository,
) : UserCouponRepository {
    override fun findByUserId(userId: Long): List<UserCoupon> = jpaUserCouponRepository.findByUserId(userId)

    override fun findByUserIdAndCouponId(
        userId: Long,
        couponId: Long,
    ): UserCoupon? = jpaUserCouponRepository.findByUserIdAndCouponId(userId, couponId)

    override fun findByUserCouponId(userCouponId: Long): UserCoupon? = jpaUserCouponRepository.findByUserCouponId(userCouponId)

    override fun save(userCoupon: UserCoupon): UserCoupon = jpaUserCouponRepository.save(userCoupon)

    override fun findUsableCouponsByUserId(userId: Long): List<UserCoupon> =
        jpaUserCouponRepository.findByUserIdAndStatus(userId, UserCouponStatus.ISSUED)
}
