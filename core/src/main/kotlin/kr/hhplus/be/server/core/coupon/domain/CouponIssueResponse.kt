package kr.hhplus.be.server.core.coupon.domain

/**
 * 쿠폰 발급 응답 도메인 모델
 * 비동기 쿠폰 발급 요청에 대한 즉시 응답
 */
data class CouponIssueResponse(
    val success: Boolean,
    val message: String,
    val requestId: String,
) {
    companion object {
        /**
         * 성공 응답 생성
         * @param requestId 요청 ID
         * @return 성공 응답
         */
        fun success(requestId: String): CouponIssueResponse =
            CouponIssueResponse(
                success = true,
                message = "쿠폰 발급 요청이 정상적으로 접수되었습니다. 잠시 후 쿠폰이 발급됩니다.",
                requestId = requestId,
            )

        /**
         * 실패 응답 생성 (재고 부족)
         * @param couponId 쿠폰 ID
         * @return 실패 응답
         */
        fun failureOutOfStock(couponId: Long): CouponIssueResponse =
            CouponIssueResponse(
                success = false,
                message = "쿠폰 재고가 부족합니다. 쿠폰 ID: $couponId",
                requestId = "",
            )

        /**
         * 실패 응답 생성 (일반 오류)
         * @param message 오류 메시지
         * @return 실패 응답
         */
        fun failure(message: String): CouponIssueResponse =
            CouponIssueResponse(
                success = false,
                message = message,
                requestId = "",
            )
    }
}
