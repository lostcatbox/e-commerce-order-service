package kr.hhplus.be.server.core.point.service

import kr.hhplus.be.server.core.point.domain.UserPoint

/**
 * 포인트 서비스 인터페이스
 */
interface PointServiceInterface {
    /**
     * 사용자 포인트 잔액 조회
     * @param userId 사용자 ID
     * @return UserPoint
     */
    fun getPointBalance(userId: Long): UserPoint

    /**
     * 사용자 포인트 충전
     * @param userId 사용자 ID
     * @param amount 충전할 금액
     * @return 충전된 UserPoint
     */
    fun chargePoint(
        userId: Long,
        amount: Long,
    ): UserPoint

    /**
     * 사용자 포인트 사용
     * @param userId 사용자 ID
     * @param amount 사용할 금액
     * @return 사용된 UserPoint
     */
    fun usePoint(
        userId: Long,
        amount: Long,
    ): UserPoint
}
