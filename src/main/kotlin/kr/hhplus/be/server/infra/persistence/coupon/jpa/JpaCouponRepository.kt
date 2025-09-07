package kr.hhplus.be.server.infra.persistence.coupon.jpa

import kr.hhplus.be.server.core.coupon.domain.Coupon
import org.springframework.data.jpa.repository.JpaRepository

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
}
