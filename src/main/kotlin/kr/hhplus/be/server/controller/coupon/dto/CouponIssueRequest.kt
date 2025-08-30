package kr.hhplus.be.server.controller.coupon.dto

/**
 * 쿠폰 발급 요청 DTO
 */
data class CouponIssueRequest(
    val userId: Long,
    val couponId: Long,
)