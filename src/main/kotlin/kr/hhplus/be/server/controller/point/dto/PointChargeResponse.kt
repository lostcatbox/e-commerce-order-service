package kr.hhplus.be.server.controller.point.dto

/**
 * 포인트 충전 응답
 */
data class PointChargeResponse(
    val userId: Long,
    val chargedAmount: Long,
    val previousBalance: Long,
    val currentBalance: Long,
    val chargedAt: Long
)
