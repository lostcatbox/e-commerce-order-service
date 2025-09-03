package kr.hhplus.be.server.core.user.repository

import kr.hhplus.be.server.core.user.domain.User

interface UserRepository {
    fun findByUserId(userId: Long): User?
}
