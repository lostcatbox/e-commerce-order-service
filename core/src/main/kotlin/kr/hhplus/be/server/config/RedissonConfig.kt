package kr.hhplus.be.server.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedissonConfig(
    @Value("\${spring.data.redis.host}")
    private val redissonHost: String,
    @Value("\${spring.data.redis.port}")
    private val redissonPort: Int,
) {
    @Bean
    fun redissonClient(): RedissonClient {
        val redissonConfig = Config()
        redissonConfig.useSingleServer().setAddress("$REDISSON_HOST_PREFIX$redissonHost:$redissonPort")
        return Redisson.create(redissonConfig)
    }

    companion object {
        private const val REDISSON_HOST_PREFIX = "redis://"
    }
}
