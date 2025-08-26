package kr.hhplus.be.server.controller.point.dto

/**
 * 포인트 잔액 조회 응답
 */
data class PointBalanceResponse(
    val userId: Long,
    val balance: Long,
    val lastUpdatedAt: Long
)
