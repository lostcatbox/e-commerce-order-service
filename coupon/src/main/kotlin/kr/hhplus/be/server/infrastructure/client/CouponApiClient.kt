package kr.hhplus.be.server.infrastructure.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

/**
 * Coupon 서버에서 Core 서버로의 API 호출을 위한 Feign Client
 */
@FeignClient(
    name = "core-service",
    url = "\${core.service.url:http://localhost:8081}",
)
interface CouponApiClient {
    /**
     * 사용자 활성 상태 체크
     * @param userId 사용자 ID
     * @return 사용자 활성 상태 정보
     */
    @GetMapping("/api/v1/users/{userId}/active")
    fun checkActiveUser(
        @PathVariable userId: Long,
    ): UserActiveResponse
}

/**
 * 사용자 활성 상태 응답 DTO
 */
data class UserActiveResponse(
    val userId: Long,
    val isActive: Boolean,
)
