package kr.hhplus.be.server.infrastructure.persistance.coupon.jpa

import jakarta.persistence.LockModeType
import kr.hhplus.be.server.core.coupon.domain.Coupon
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

/**
 * 쿠폰 JPA Repository 인터페이스
 */
interface JpaCouponRepository : JpaRepository<Coupon, Long> {
    /**
     * 쿠폰 ID로 쿠폰 조회
     * @param couponId 쿠폰 ID
     * @return Coupon 또는 null
     */
    fun findByCouponId(couponId: Long): Coupon?

    /**
     * 쿠폰 ID로 쿠폰 조회 (베타락)
     * @param couponId 쿠폰 ID
     * @return Coupon 또는 null
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithPerssimisticLockByCouponId(couponId: Long): Coupon?
}
