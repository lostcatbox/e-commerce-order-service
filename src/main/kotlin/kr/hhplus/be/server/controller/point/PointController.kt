package kr.hhplus.be.server.controller.point

import kr.hhplus.be.server.controller.point.dto.*
import kr.hhplus.be.server.point.service.PointServiceInterface
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 포인트 유스케이스
 */
@RestController
@RequestMapping("/api/v1/points")
class PointController(
    private val pointService: PointServiceInterface
) {

    /**
     * 사용자 포인트 잔액 조회
     * GET /api/v1/points/{userId}
     */
    @GetMapping("/{userId}")
    fun getPointBalance(
        @PathVariable userId: Long
    ): ResponseEntity<PointBalanceResponse> {
        val userPoint = pointService.getPointBalance(userId)

        val response = PointBalanceResponse(
            userId = userPoint.userId,
            balance = userPoint.balance,
            lastUpdatedAt = userPoint.lastUpdatedAt
        )
        return ResponseEntity.ok(response)
    }

    /**
     * 사용자 포인트 충전
     * PATCH /api/v1/points/{userId}/charge
     */
    @PatchMapping("/{userId}/charge")
    fun chargePoint(
        @PathVariable userId: Long,
        @RequestBody request: PointChargeRequest
    ): ResponseEntity<PointChargeResponse> {
        // 충전 전 잔액 조회
        val previousUserPoint = pointService.getPointBalance(userId)

        // 포인트 충전
        val chargedUserPoint = pointService.chargePoint(userId, request.amount)

        val response = PointChargeResponse(
            userId = chargedUserPoint.userId,
            chargedAmount = request.amount,
            previousBalance = previousUserPoint.balance,
            currentBalance = chargedUserPoint.balance,
            chargedAt = chargedUserPoint.lastUpdatedAt
        )
        return ResponseEntity.ok(response)
    }
}
