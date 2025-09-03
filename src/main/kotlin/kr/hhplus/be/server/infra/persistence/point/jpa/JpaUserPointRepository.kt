package kr.hhplus.be.server.infra.persistence.point.jpa

import kr.hhplus.be.server.core.point.domain.UserPoint
import org.springframework.data.jpa.repository.JpaRepository

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
}
