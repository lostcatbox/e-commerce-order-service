package kr.hhplus.be.server.core.coupon.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.*

/**
 * 쿠폰 발급 이벤트 도메인 객체
 *
 * 설명:
 * - Kafka 메시지로 전송되는 쿠폰 발급 이벤트
 * - JSON 직렬화/역직렬화 지원
 * - 이벤트 순서 보장을 위한 키 생성 지원
 *
 * 특징:
 * - 불변 객체 (data class)
 * - 타임스탬프 기반 순서 보장
 * - 쿠폰별 파티셔닝 지원
 * - 재처리를 위한 고유 식별자 포함
 */
data class CouponIssueEvent @JsonCreator constructor(
    @JsonProperty("eventId")
    val eventId: String,

    @JsonProperty("userId")
    val userId: Long,

    @JsonProperty("couponId")
    val couponId: Long,

    @JsonProperty("timestamp")
    val timestamp: Long,

    @JsonProperty("version")
    val version: Int = 1
) {
    companion object {
        /**
         * 새로운 쿠폰 발급 이벤트 생성
         * @param userId 사용자 ID
         * @param couponId 쿠폰 ID
         * @return 생성된 이벤트
         */
        fun create(userId: Long, couponId: Long): CouponIssueEvent {
            return CouponIssueEvent(
                eventId = UUID.randomUUID().toString(),
                userId = userId,
                couponId = couponId,
                timestamp = Instant.now().toEpochMilli()
            )
        }
    }

    /**
     * 파티션 키 생성
     * 같은 쿠폰에 대한 요청은 같은 파티션으로 라우팅하여 순서 보장
     * @return 파티션 키
     */
    fun getPartitionKey(): String = "coupon-$couponId"

    /**
     * 이벤트 검증
     * @return 유효한 이벤트인지 여부
     */
    fun isValid(): Boolean {
        return eventId.isNotBlank() &&
                userId > 0 &&
                couponId > 0 &&
                timestamp > 0 &&
                version > 0
    }

    /**
     * CouponIssueRequest로 변환
     * 기존 시스템과의 호환성을 위해 제공
     * @return 변환된 CouponIssueRequest
     */
    fun toCouponIssueRequest(): CouponIssueRequest {
        return CouponIssueRequest(
            requestId = eventId,
            userId = userId,
            couponId = couponId,
            timestamp = timestamp
        )
    }
}
