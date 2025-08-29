package kr.hhplus.be.server.user.service

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
     * TODO: 실제 구현에서는 사용자 저장소에서 사용자 상태를 확인해야 함
     */
    override fun checkActiveUser(userId: Long) {
        validateUserId(userId)
        
        // TODO: 실제 사용자 데이터베이스에서 사용자 존재 여부 및 활성 상태 확인
        // 현재는 단순히 ID 유효성만 검증
        // 예: userRepository.findByUserId(userId)?.let { user ->
        //     require(user.isActive) { "비활성 사용자입니다. 사용자 ID: $userId" }
        // } ?: throw IllegalArgumentException("존재하지 않는 사용자입니다. 사용자 ID: $userId")
    }

    private fun validateUserId(userId: Long) {
        require(userId > 0) { "사용자 ID는 0보다 커야 합니다. 입력된 ID: $userId" }
    }
}
