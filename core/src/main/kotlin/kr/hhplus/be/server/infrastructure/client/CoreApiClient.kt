package kr.hhplus.be.server.infrastructure.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping

/**
 * Core 서버에서 Coupon 서버로의 API 호출을 위한 Feign Client
 */
@FeignClient(
    name = "coupon-service",
    url = "\${coupon.service.url:http://localhost:8081}",
)
interface CoreApiClient {
    /**
     * 사용자 쿠폰 조회
     * @param userCouponId 사용자 쿠폰 ID
     * @return 사용자 쿠폰 정보
     */
    @GetMapping("/api/v1/coupons/user-coupons/{userCouponId}")
    fun getUserCoupon(
        @PathVariable userCouponId: Long,
    ): UserCouponResponse

    /**
     * 쿠폰 정보 조회
     * @param couponId 쿠폰 ID
     * @return 쿠폰 정보
     */
    @GetMapping("/api/v1/coupons/{couponId}")
    fun getCouponInfo(
        @PathVariable couponId: Long,
    ): CouponResponse

    /**
     * 쿠폰 사용
     * @param userCouponId 사용자 쿠폰 ID
     * @return 사용된 사용자 쿠폰 정보
     */
    @PostMapping("/api/v1/coupons/user-coupons/{userCouponId}/use")
    fun useCoupon(
        @PathVariable userCouponId: Long,
    ): UserCouponResponse
}

/**
 * 사용자 쿠폰 응답 DTO
 */
data class UserCouponResponse(
    val userCouponId: Long,
    val userId: Long,
    val couponId: Long,
    val status: String,
)

/**
 * 쿠폰 정보 응답 DTO
 */
data class CouponResponse(
    val couponId: Long,
    val description: String,
    val discountAmount: Long,
    val stock: Int,
    val status: String,
)
