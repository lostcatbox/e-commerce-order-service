package kr.hhplus.be.server.controller.user.dto

/**
 * 사용자 활성 상태 응답 DTO
 */
data class UserActiveResponse(
    val userId: Long,
    val isActive: Boolean,
) {
    companion object {
        fun success(userId: Long): UserActiveResponse =
            UserActiveResponse(
                userId = userId,
                isActive = true,
            )
    }
}
