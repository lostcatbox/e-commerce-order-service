package kr.hhplus.be.server.core.coupon.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 쿠폰 발급 요청 도메인 모델
 * Redis 대기열에서 관리되는 쿠폰 발급 요청 정보
 */
data class CouponIssueRequest
    @JsonCreator
    constructor(
        @JsonProperty("userId") val userId: Long,
        @JsonProperty("couponId") val couponId: Long,
        @JsonProperty("requestId") val requestId: String,
        @JsonProperty("timestamp") val timestamp: Long,
    ) {
        companion object {
            /**
             * 쿠폰 발급 요청 생성
             * @param userId 사용자 ID
             * @param couponId 쿠폰 ID
             * @return 생성된 CouponIssueRequest
             */
            fun create(
                userId: Long,
                couponId: Long,
            ): CouponIssueRequest {
                require(userId > 0) { "사용자 ID는 0보다 커야 합니다. 입력된 ID: $userId" }
                require(couponId > 0) { "쿠폰 ID는 0보다 커야 합니다. 입력된 ID: $couponId" }

                val timestamp = System.currentTimeMillis()
                val requestId = generateRequestId(userId, couponId, timestamp)

                return CouponIssueRequest(
                    userId = userId,
                    couponId = couponId,
                    requestId = requestId,
                    timestamp = timestamp,
                )
            }

            /**
             * 요청 ID 생성
             * @param userId 사용자 ID
             * @param couponId 쿠폰 ID
             * @param timestamp 타임스탬프
             * @return 생성된 요청 ID
             */
            private fun generateRequestId(
                userId: Long,
                couponId: Long,
                timestamp: Long,
            ): String = "coupon-issue-$userId-$couponId-$timestamp"
        }
    }
