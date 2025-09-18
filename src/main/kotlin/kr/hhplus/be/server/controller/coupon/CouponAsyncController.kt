package kr.hhplus.be.server.controller.coupon

import kr.hhplus.be.server.core.coupon.domain.CouponIssueResponse
import kr.hhplus.be.server.facade.coupon.CouponAsyncFacade
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 비동기 쿠폰 발급 컨트롤러
 *
 * 설명:
 * 기존 동기 쿠폰 발급을 비동기로 개선한 API를 제공합니다.
 * 높은 동시성 환경에서 빠른 응답과 안정적인 처리를 보장합니다.
 */
@RestController
@RequestMapping("/api/v2/coupons")
class CouponAsyncController(
    private val couponAsyncFacade: CouponAsyncFacade,
) {
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
        val response = couponAsyncFacade.requestCouponIssue(userId, couponId)

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
        val queueSize = couponAsyncFacade.getQueueSize(couponId)

        return ResponseEntity.ok(
            mapOf(
                "couponId" to couponId,
                "queueSize" to queueSize,
                "message" to "현재 $queueSize 명이 대기 중입니다."
            )
        )
    }
}
