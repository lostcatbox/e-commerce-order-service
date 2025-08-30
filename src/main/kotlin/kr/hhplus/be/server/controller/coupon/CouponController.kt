package kr.hhplus.be.server.controller.coupon

import kr.hhplus.be.server.controller.coupon.dto.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 선착순 쿠폰 유스케이스
 */
@RestController
@RequestMapping("/api/v1/coupons")
class CouponController {
    /**
     * 선착순 쿠폰 발급
     * POST /api/v1/coupons/{couponId}/issue
     */
    @PostMapping("/{couponId}/issue")
    fun issueCoupon(
        @PathVariable couponId: Long,
        @RequestBody request: CouponIssueRequest,
    ): ResponseEntity<Unit> {
        // TODO: 쿠폰 발급 서비스 호출
        return ResponseEntity.ok().build()
    }

    /**
     * 사용자 보유 쿠폰 목록 조회
     * GET /api/v1/coupons/users/{userId}
     */
    @GetMapping("/users/{userId}")
    fun getUserCoupons(
        @PathVariable userId: Long,
        @RequestParam(required = false, defaultValue = "ALL") status: String,
    ): ResponseEntity<UserCouponsResponse> {
        // TODO: 사용자 쿠폰 조회 서비스 호출
        val mockCoupons =
            listOf(
                UserCouponInfo(
                    userCouponId = 1L,
                    couponId = 1L,
                    couponName = "신규 가입 쿠폰",
                    discountAmount = 3000L,
                    status = "ISSUED",
                    issuedAt = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L), // 5일 전
                    expiresAt = System.currentTimeMillis() + (25 * 24 * 60 * 60 * 1000L), // 25일 후
                    usedAt = null,
                ),
                UserCouponInfo(
                    userCouponId = 2L,
                    couponId = 2L,
                    couponName = "생일 축하 쿠폰",
                    discountAmount = 10000L,
                    status = "USED",
                    issuedAt = System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L), // 10일 전
                    expiresAt = System.currentTimeMillis() + (20 * 24 * 60 * 60 * 1000L), // 20일 후
                    usedAt = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L), // 3일 전 사용
                ),
            )

        val mockResponse =
            UserCouponsResponse(
                userId = userId,
                coupons = mockCoupons,
                totalCount = mockCoupons.size,
            )
        return ResponseEntity.ok(mockResponse)
    }

    /**
     * 선착순 쿠폰 정보 조회
     * GET /api/v1/coupons/{couponId}
     */
    @GetMapping("/{couponId}")
    fun getCouponInfo(
        @PathVariable couponId: Long,
    ): ResponseEntity<CouponInfoResponse> {
        // TODO: 쿠폰 정보 조회 서비스 호출
        val mockResponse =
            CouponInfoResponse(
                couponId = couponId,
                name = "선착순 할인 쿠폰",
                description = "선착순 100명 한정 할인 쿠폰",
                discountAmount = 5000L,
                totalStock = 100,
                remainingStock = 35,
                status = "OPENED",
            )
        return ResponseEntity.ok(mockResponse)
    }
}
