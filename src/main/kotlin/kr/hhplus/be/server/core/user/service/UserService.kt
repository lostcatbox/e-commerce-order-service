package kr.hhplus.be.server.core.user.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 사용자 서비스 구현체
 */
@Service
@Transactional(readOnly = true)
class UserService : UserServiceInterface {
    /**
     * 활성 사용자 확인
     *
     * 현재는 단순히 사용자 ID가 0보다 큰지 검증하는 로직만 포함
     */
    override fun checkActiveUser(userId: Long) {
        validateUserId(userId)
    }

    private fun validateUserId(userId: Long) {
        require(userId > 0) { "사용자 ID는 0보다 커야 합니다. 입력된 ID: $userId" }
    }
}
