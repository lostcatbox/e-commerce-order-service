package kr.hhplus.be.server.controller.point

import kr.hhplus.be.server.controller.point.dto.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 포인트 유스케이스
 */
@RestController
@RequestMapping("/api/v1/points")
class PointController {

    /**
     * 사용자 포인트 잔액 조회
     * GET /api/v1/points/{userId}
     */
    @GetMapping("/{userId}")
    fun getPointBalance(
        @PathVariable userId: Long
    ): ResponseEntity<PointBalanceResponse> {
        // TODO: 포인트 서비스 호출
        val mockResponse = PointBalanceResponse(
            userId = userId,
            balance = 50000L,
            lastUpdatedAt = System.currentTimeMillis()
        )
        return ResponseEntity.ok(mockResponse)
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
        // TODO: 포인트 충전 서비스 호출
        val mockResponse = PointChargeResponse(
            userId = userId,
            chargedAmount = request.amount,
            previousBalance = 50000L,
            currentBalance = 50000L + request.amount,
            chargedAt = System.currentTimeMillis()
        )
        return ResponseEntity.ok(mockResponse)
    }
}
