package kr.hhplus.be.server.controller.coupon.dto

import kr.hhplus.be.server.coupon.domain.Coupon
import kr.hhplus.be.server.coupon.domain.CouponStatus

/**
 * 쿠폰 정보 조회 응답 DTO
 */
data class CouponInfoResponse(
    val couponId: Long,
    val description: String,
    val discountAmount: Long,
    val stock: Int,
    val couponStatus: CouponStatus,
) {
    companion object {
        fun from(coupon: Coupon): CouponInfoResponse {
            return CouponInfoResponse(
                couponId = coupon.couponId,
                description = coupon.description,
                discountAmount = coupon.discountAmount,
                stock = coupon.getStock(),
                couponStatus = coupon.getCouponStatus()
            )
        }
    }
}