package kr.hhplus.be.server.core.point.repository

import kr.hhplus.be.server.core.point.domain.UserPoint
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable

/**
 * 사용자 포인트 Repository 인터페이스
 */
interface UserPointRepository {
    /**
     * 사용자 포인트 조회
     * @param userId 사용자 ID
     * @return UserPoint 또는 null (사용자가 존재하지 않는 경우)
     */
    fun findByUserId(userId: Long): UserPoint?

    /**
     * 사용자 포인트 조회
     * @param userId 사용자 ID
     * @return UserPoint 또는 null (사용자가 존재하지 않는 경우)
     */
    fun findByUserIdWithOptimisticLock(userId: Long): UserPoint?

    /**
     * 사용자 포인트 조회
     * @param userId 사용자 ID
     * @return UserPoint 또는 null (사용자가 존재하지 않는 경우)
     */
    fun findByUserIdWithPessimisticLock(userId: Long): UserPoint?

    /**
     * 사용자 포인트 저장/업데이트
     * @param userPoint 저장할 사용자 포인트
     * @return 저장된 UserPoint
     */
    fun save(userPoint: UserPoint): UserPoint
}
