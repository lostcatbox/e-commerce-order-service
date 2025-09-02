package kr.hhplus.be.server.core.user.service

import kr.hhplus.be.server.core.user.domain.User
import kr.hhplus.be.server.core.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 사용자 서비스 구현체
 */
@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
) : UserServiceInterface {
    /**
     * 활성 사용자 확인
     */
    override fun checkActiveUser(userId: Long) {
        userRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("존재하지 않는 사용자입니다. 사용자 ID: $userId")
    }
}
