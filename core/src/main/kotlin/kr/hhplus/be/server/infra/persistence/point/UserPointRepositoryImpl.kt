package kr.hhplus.be.server.infra.persistence.point

import kr.hhplus.be.server.core.point.domain.UserPoint
import kr.hhplus.be.server.core.point.repository.UserPointRepository
import kr.hhplus.be.server.infra.persistence.point.jpa.JpaUserPointRepository
import org.springframework.stereotype.Repository

/**
 * 사용자 포인트 Repository 구현체
 */
@Repository
class UserPointRepositoryImpl(
    private val jpaUserPointRepository: JpaUserPointRepository,
) : UserPointRepository {
    override fun findByUserId(userId: Long): UserPoint? = jpaUserPointRepository.findByUserId(userId)

    override fun findByUserIdWithOptimisticLock(userId: Long): UserPoint? =
        jpaUserPointRepository.findWithOptimisticLockByUserId(userId) // 낙관적 락

    override fun findByUserIdWithPessimisticLock(userId: Long): UserPoint? =
        jpaUserPointRepository.findWithPessimisticLockByUserId(userId) // 비관적 락

    override fun save(userPoint: UserPoint): UserPoint = jpaUserPointRepository.save(userPoint)
}
