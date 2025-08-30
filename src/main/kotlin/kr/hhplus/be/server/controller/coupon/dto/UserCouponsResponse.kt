package kr.hhplus.be.server.controller.coupon.dto

/**
 * 사용자 보유 쿠폰 목록 조회 응답
 */
data class UserCouponsResponse(
    val userId: Long,
    val coupons: List<UserCouponInfo>,
    val totalCount: Int
)

/**
 * 사용자 쿠폰 정보
 */
data class UserCouponInfo(
    val userCouponId: Long,
    val couponId: Long,
    val couponName: String,
    val discountAmount: Long,
    val status: String, // ISSUED, USED
    val issuedAt: Long,
    val expiresAt: Long,
    val usedAt: Long?
)
