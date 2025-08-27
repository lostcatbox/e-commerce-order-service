package kr.hhplus.be.server.coupon.repository

import kr.hhplus.be.server.coupon.domain.Coupon

/**
 * 쿠폰 Repository 인터페이스
 */
interface CouponRepository {

    /**
     * 쿠폰 ID로 쿠폰 조회
     * @param couponId 쿠폰 ID
     * @return Coupon 또는 null (쿠폰이 존재하지 않는 경우)
     */
    fun findByCouponId(couponId: Long): Coupon?

    /**
     * 쿠폰 저장/업데이트
     * @param coupon 저장할 쿠폰
     * @return 저장된 Coupon
     */
    fun save(coupon: Coupon): Coupon

    /**
     * 모든 쿠폰 조회
     * @return 쿠폰 목록
     */
    fun findAll(): List<Coupon>
}
