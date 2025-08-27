package kr.hhplus.be.server.coupon.domain

/**
 * 유저 쿠폰 도메인 모델
 */
data class UserCoupon(
    val userId: Long,
    val couponId: Long,
    val status: UserCouponStatus,
    val issuedAt: Long = System.currentTimeMillis(),
    val usedAt: Long? = null,
) {
    companion object {
        /**
         * 쿠폰 발급
         * @param userId 사용자 ID
         * @param couponId 쿠폰 ID
         * @return 발급된 UserCoupon
         */
        fun issueCoupon(userId: Long, couponId: Long): UserCoupon {
            require(userId > 0) { "사용자 ID는 0보다 커야 합니다. 입력된 ID: $userId" }
            require(couponId > 0) { "쿠폰 ID는 0보다 커야 합니다. 입력된 ID: $couponId" }

            return UserCoupon(
                userId = userId,
                couponId = couponId,
                status = UserCouponStatus.ISSUED,
                issuedAt = System.currentTimeMillis()
            )
        }
    }

    /**
     * 쿠폰 사용
     * @return 사용된 UserCoupon
     */
    fun use(): UserCoupon {
        require(isUsable()) { "쿠폰이 사용 가능한 상태가 아닙니다. 현재 상태: $status" }

        return copy(
            status = UserCouponStatus.USED,
            usedAt = System.currentTimeMillis()
        )
    }

    /**
     * 쿠폰이 사용 가능한지 확인
     * @return 사용 가능 여부
     */
    fun isUsable(): Boolean {
        return status == UserCouponStatus.ISSUED
    }
}

/**
 * 유저 쿠폰 상태 열거형
 */
enum class UserCouponStatus {
    ISSUED, // 발급됨
    USED    // 사용됨
}
