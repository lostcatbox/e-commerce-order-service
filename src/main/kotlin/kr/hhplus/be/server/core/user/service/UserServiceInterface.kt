package kr.hhplus.be.server.core.user.service

import kr.hhplus.be.server.core.user.domain.User

/**
 * 사용자 서비스 인터페이스
 */
interface UserServiceInterface {
    /**
     * 활성 사용자 확인
     * @param userId 사용자 ID
     */
    fun checkActiveUser(userId: Long)
}
