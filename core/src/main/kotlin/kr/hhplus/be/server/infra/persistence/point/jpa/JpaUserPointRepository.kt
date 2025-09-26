package kr.hhplus.be.server.infra.persistence.point.jpa

import jakarta.persistence.LockModeType
import kr.hhplus.be.server.core.point.domain.UserPoint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

/**
 * 사용자 포인트 JPA Repository 인터페이스
 */
interface JpaUserPointRepository : JpaRepository<UserPoint, Long> {
    /**
     * 사용자 ID로 포인트 조회
     * @param userId 사용자 ID
     * @return UserPoint 또는 null
     */
    fun findByUserId(userId: Long): UserPoint?

    @Lock(LockModeType.OPTIMISTIC)
    fun findWithOptimisticLockByUserId(userId: Long): UserPoint?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithPessimisticLockByUserId(userId: Long): UserPoint?
}
