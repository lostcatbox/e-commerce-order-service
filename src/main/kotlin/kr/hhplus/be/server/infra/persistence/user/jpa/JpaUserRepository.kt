package kr.hhplus.be.server.infra.persistence.user.jpa

import kr.hhplus.be.server.core.user.domain.User
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 사용자 JPA Repository 인터페이스
 */
interface JpaUserRepository : JpaRepository<User, Long> {
    /**
     * 사용자 ID로 사용자 조회
     * @param userId 사용자 ID
     * @return User 또는 null
     */
    fun findByUserId(userId: Long): User?
}
