package kr.hhplus.be.server.controller.coupon.dto

import kr.hhplus.be.server.core.coupon.domain.UserCoupon

/**
 * 사용자 쿠폰 응답 DTO
 */
data class UserCouponResponse(
    val userCouponId: Long,
    val userId: Long,
    val couponId: Long,
    val status: String,
) {
    companion object {
        fun from(userCoupon: UserCoupon): UserCouponResponse =
            UserCouponResponse(
                userCouponId = userCoupon.userCouponId,
                userId = userCoupon.userId,
                couponId = userCoupon.couponId,
                status = userCoupon.getStatus().name,
            )
    }
}
