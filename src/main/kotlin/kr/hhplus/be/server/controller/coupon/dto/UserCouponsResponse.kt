package kr.hhplus.be.server.controller.coupon.dto

import kr.hhplus.be.server.coupon.domain.UserCoupon
import kr.hhplus.be.server.coupon.domain.UserCouponStatus

/**
 * 사용자 쿠폰 목록 조회 응답 DTO
 */
data class UserCouponsResponse(
    val userCoupons: List<UserCouponInfo>
) {
    data class UserCouponInfo(
        val userId: Long,
        val couponId: Long,
        val status: UserCouponStatus,
        val issuedAt: Long,
        val usedAt: Long?
    )

    companion object {
        fun from(userCoupons: List<UserCoupon>): UserCouponsResponse {
            val userCouponInfos = userCoupons.map { userCoupon ->
                UserCouponInfo(
                    userId = userCoupon.userId,
                    couponId = userCoupon.couponId,
                    status = userCoupon.getStatus(),
                    issuedAt = userCoupon.issuedAt,
                    usedAt = userCoupon.getUsedAt()
                )
            }
            return UserCouponsResponse(userCouponInfos)
        }
    }
}