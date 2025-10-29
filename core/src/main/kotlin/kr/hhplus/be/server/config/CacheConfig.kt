package kr.hhplus.be.server.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

/**
 * 캐시 설정
 *
 * - 캐싱: Spring Data Redis 사용 (TTL 지원)
 */
@Configuration
@EnableCaching
class CacheConfig {
    /**
     * 공통 직렬화기 설정
     * - 모든 캐시에 적용 가능한 범용 직렬화기
     */
    @Bean
    fun commonCacheSerializer(): GenericJackson2JsonRedisSerializer = GenericJackson2JsonRedisSerializer()

    /**
     * Spring Data Redis 기반 캐시 매니저 설정
     * - 공통 직렬화기로 모든 캐시 타입 지원
     * - Redis key별 개별 TTL 설정 가능
     */
    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        val commonSerializer = commonCacheSerializer()

        // 기본 캐시 설정
        val defaultCacheConfig =
            RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(1)) // 기본 TTL: 1분
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(commonSerializer))
                .disableCachingNullValues()

        // 캐시별 개별 TTL 설정
        val cacheConfigurations =
            mapOf(
                "popular_products" to defaultCacheConfig.entryTtl(Duration.ofMinutes(1)), // 인기 상품: 1분
            )

        return RedisCacheManager
            .builder(redisConnectionFactory)
            .cacheDefaults(defaultCacheConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }
}
