package kr.hhplus.be.server.controller.coupon.dto

/**
 * 선착순 쿠폰 정보 조회 응답
 */
data class CouponInfoResponse(
    val couponId: Long,
    val name: String,
    val description: String,
    val discountAmount: Long,
    val totalStock: Int,
    val remainingStock: Int,
    val status: String, // OPENED, CLOSED
)
