package kr.hhplus.be.server.core.point.service

import kr.hhplus.be.server.core.point.domain.UserPoint
import kr.hhplus.be.server.core.point.repository.UserPointRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 서비스 구현체
 */
@Service
@Transactional
class PointService(
    private val userPointRepository: UserPointRepository,
) : PointServiceInterface {
    /**
     * 사용자 포인트 잔액 조회
     */
    @Transactional(readOnly = true)
    override fun getPointBalance(userId: Long): UserPoint {
        validateUserId(userId)

        return userPointRepository.findByUserId(userId)
            ?: UserPoint(userId = userId) // 신규 사용자인 경우 0 잔액으로 반환
    }

    /**
     * 사용자 포인트 충전
     */
    @Transactional
    override fun chargePoint(
        userId: Long,
        amount: Long,
    ): UserPoint {
        validateUserId(userId)

        // 기존 포인트 조회 (없으면 0으로 초기화)
        val currentUserPoint =
            userPointRepository.findByUserId(userId)
                ?: UserPoint(userId = userId)

        // 도메인 로직을 통한 포인트 충전
        currentUserPoint.charge(amount)

        // 충전된 포인트 저장
        return userPointRepository.save(currentUserPoint)
    }

    /**
     * 사용자 포인트 사용
     */
    @Transactional
    override fun usePoint(
        userId: Long,
        amount: Long,
    ): UserPoint {
        validateUserId(userId)

        // 기존 포인트 조회
        val currentUserPoint =
            userPointRepository.findByUserId(userId)
                ?: throw IllegalArgumentException("존재하지 않는 사용자의 포인트입니다. 사용자 ID: $userId")

        // 도메인 로직을 통한 포인트 사용
        currentUserPoint.use(amount)

        // 사용된 포인트 저장
        return userPointRepository.save(currentUserPoint)
    }

    /**
     * 사용자 ID 유효성 검증
     */
    private fun validateUserId(userId: Long) {
        require(userId >= 0) { "사용자 ID는 0 이상 이여야 합니다. 입력된 ID: $userId" }
    }
}
