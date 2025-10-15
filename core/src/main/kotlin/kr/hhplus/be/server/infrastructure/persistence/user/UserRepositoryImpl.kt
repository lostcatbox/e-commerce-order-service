package kr.hhplus.be.server.infrastructure.persistence.user

import kr.hhplus.be.server.core.user.domain.User
import kr.hhplus.be.server.core.user.repository.UserRepository
import kr.hhplus.be.server.infrastructure.persistence.user.jpa.JpaUserRepository
import org.springframework.stereotype.Repository

/**
 * 사용자 Repository 구현체
 */
@Repository
class UserRepositoryImpl(
    private val jpaUserRepository: JpaUserRepository,
) : UserRepository {
    override fun findByUserId(userId: Long): User? = jpaUserRepository.findByUserId(userId)

    override fun save(user: User): User = jpaUserRepository.save(user)
}
