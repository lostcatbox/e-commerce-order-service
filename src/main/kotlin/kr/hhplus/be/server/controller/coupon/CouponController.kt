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
}
