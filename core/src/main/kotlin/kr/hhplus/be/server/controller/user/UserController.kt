package kr.hhplus.be.server.controller.user

import kr.hhplus.be.server.controller.user.dto.UserActiveResponse
import kr.hhplus.be.server.core.user.service.UserServiceInterface
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 사용자 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserServiceInterface,
) {
    /**
     * 사용자 활성 상태 체크
     * @param userId 사용자 ID
     * @return 사용자 활성 상태 정보
     */
    @GetMapping("/{userId}/active")
    fun checkActiveUser(
        @PathVariable userId: Long,
    ): UserActiveResponse {
        userService.checkActiveUser(userId)
        return UserActiveResponse.success(userId)
    }
}
