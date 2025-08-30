package kr.hhplus.be.server.controller.coupon

import kr.hhplus.be.server.controller.coupon.dto.CouponInfoResponse
import kr.hhplus.be.server.controller.coupon.dto.CouponIssueRequest
import kr.hhplus.be.server.controller.coupon.dto.UserCouponsResponse
import kr.hhplus.be.server.core.coupon.service.CouponServiceInterface
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
     * 선착순 쿠폰 발급
     * @param request 쿠폰 발급 요청
     * @return 발급된 유저 쿠폰 정보
     */
    @PostMapping("/issue")
    fun issueCoupon(
        @RequestBody request: CouponIssueRequest,
    ): UserCouponsResponse.UserCouponInfo {
        val userCoupon = couponService.issueCoupon(request.userId, request.couponId)
        return UserCouponsResponse.UserCouponInfo(
            userId = userCoupon.userId,
            couponId = userCoupon.couponId,
            status = userCoupon.getStatus(),
            issuedAt = userCoupon.issuedAt,
            usedAt = userCoupon.getUsedAt(),
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
