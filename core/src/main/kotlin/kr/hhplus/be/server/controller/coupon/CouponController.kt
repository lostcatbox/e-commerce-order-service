package kr.hhplus.be.server.controller.coupon

import kr.hhplus.be.server.controller.coupon.dto.CouponInfoResponse
import kr.hhplus.be.server.controller.coupon.dto.UserCouponsResponse
import kr.hhplus.be.server.core.coupon.domain.CouponIssueResponse
import kr.hhplus.be.server.core.coupon.service.CouponServiceInterface
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 쿠폰 컨트롤러
 */
@RestController
@RequestMapping("/api/coupons")
class CouponController(
    private val couponService: CouponServiceInterface,
) {
    /**
     * 쿠폰 정보 조회
     * @param couponId 쿠폰 ID
     * @return 쿠폰 정보
     */
    @GetMapping("/{couponId}")
    fun getCouponInfo(
        @PathVariable couponId: Long,
    ): CouponInfoResponse {
        val coupon = couponService.getCouponInfo(couponId)
        return CouponInfoResponse.from(coupon)
    }

    /**
     * 쿠폰 발급 요청 (비동기)
     *
     * 요청 즉시 성공/실패 응답을 받으며,
     * 실제 쿠폰 발급은 백그라운드에서 비동기로 처리됩니다.
     *
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 발급 요청 응답
     */
    @PostMapping("/{couponId}/issue")
    fun requestCouponIssue(
        @RequestParam userId: Long,
        @PathVariable couponId: Long,
    ): ResponseEntity<CouponIssueResponse> {
        val response = couponService.requestCouponIssueAsync(userId, couponId)

        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    /**
     * 쿠폰 발급 대기열 크기 조회
     *
     * @param couponId 쿠폰 ID
     * @return 대기열 크기
     */
    @GetMapping("/{couponId}/queue-size")
    fun getQueueSize(
        @PathVariable couponId: Long,
    ): ResponseEntity<Map<String, Any>> {
        val queueSize = couponService.getQueueSize(couponId)

        return ResponseEntity.ok(
            mapOf(
                "couponId" to couponId,
                "queueSize" to queueSize,
                "message" to "현재 $queueSize 명이 대기 중입니다.",
            ),
        )
    }

    /**
     * 사용자의 쿠폰 목록 조회
     * @param userId 사용자 ID
     * @return 사용자의 쿠폰 목록
     */
    @GetMapping("/users/{userId}")
    fun getUserCoupons(
        @PathVariable userId: Long,
    ): UserCouponsResponse {
        val userCoupons = couponService.getUserCoupons(userId)
        return UserCouponsResponse.from(userCoupons)
    }
}
