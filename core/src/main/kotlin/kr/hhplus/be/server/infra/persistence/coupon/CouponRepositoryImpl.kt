package kr.hhplus.be.server.infra.persistence.coupon

import kr.hhplus.be.server.core.coupon.domain.Coupon
import kr.hhplus.be.server.core.coupon.repository.CouponRepository
import kr.hhplus.be.server.infra.persistence.coupon.jpa.JpaCouponRepository
import org.springframework.stereotype.Repository

/**
 * 쿠폰 Repository 구현체
 */
@Repository
class CouponRepositoryImpl(
    private val jpaCouponRepository: JpaCouponRepository,
) : CouponRepository {
    override fun findByCouponId(couponId: Long): Coupon? = jpaCouponRepository.findByCouponId(couponId)

    override fun findByCouponIdWithPessimisticLock(couponId: Long): Coupon? =
        jpaCouponRepository.findWithPerssimisticLockByCouponId(couponId)

    override fun save(coupon: Coupon): Coupon = jpaCouponRepository.save(coupon)
}
