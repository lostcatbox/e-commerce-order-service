package kr.hhplus.be.server.config

import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Configuration

/**
 * Feign Client 설정
 */
@Configuration
@EnableFeignClients(basePackages = ["kr.hhplus.be.server.infrastructure.client"])
class FeignConfig
