package kr.hhplus.be.server.infra.persistence.product.redis

import kr.hhplus.be.server.core.product.domain.ProductSale
import kr.hhplus.be.server.core.product.service.dto.ProductPeriodSaleDto
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Redis 기반 상품 판매량 Repository 구현체
 * Sorted Set을 활용하여 판매량 기록 및 조회
 */
@Repository
class RedisProductSaleRepository(
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val RANK_KEY_PREFIX = "rank:date:"
        private const val UNION_KEY_PREFIX = "rank:union:"
        private val log = LoggerFactory.getLogger(RedisProductSaleRepository::class.java)
    }

    fun findPopularProductsInfo(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<ProductPeriodSaleDto> =
        try {
            getPopularProductsFromRedis(startDate, endDate)
        } catch (exception: Exception) {
            log.error("Redis에서 인기 상품 조회 실패", exception)
            emptyList()
        }

    fun save(productSale: ProductSale): ProductSale {
        try {
            val dateKey = "$RANK_KEY_PREFIX${productSale.saleDate}"

            // Redis Sorted Set에 판매량 증가 (ZINCRBY)
            redisTemplate.opsForZSet().incrementScore(
                dateKey,
                productSale.productId.toString(),
                productSale.totalQuantity.toDouble(),
            )

            // TTL 설정 (5일)
            redisTemplate.expire(dateKey, Duration.ofDays(5))

            log.debug(
                "Redis에 판매량 기록 완료: productId={}, quantity={}, date={}",
                productSale.productId,
                productSale.totalQuantity,
                productSale.saleDate,
            )

            return productSale
        } catch (exception: Exception) {
            log.error("Redis 판매량 저장 실패", exception)
            throw exception
        }
    }

    fun findByProductIdAndSaleDate(
        productId: Long,
        saleDate: Long,
    ): ProductSale? =
        try {
            val dateKey = "$RANK_KEY_PREFIX$saleDate"
            val score = redisTemplate.opsForZSet().score(dateKey, productId.toString())

            score?.let {
                ProductSale.Companion.createNewSale(
                    productId = productId,
                    saleDate = saleDate,
                    quantity = it.toInt(),
                )
            }
        } catch (exception: Exception) {
            log.error("Redis에서 판매 데이터 조회 실패", exception)
            null
        }

    /**
     * Redis Sorted Set에서 인기 상품 조회
     */
    private fun getPopularProductsFromRedis(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<ProductPeriodSaleDto> {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val keys =
            generateSequence(startDate) { it.plusDays(1) }
                .takeWhile { !it.isAfter(endDate) }
                .map { "$RANK_KEY_PREFIX${it.format(formatter)}" }
                .toList()

        if (keys.isEmpty()) {
            log.warn("조회할 날짜 키가 없습니다. startDate={}, endDate={}", startDate, endDate)
            return emptyList()
        }

        // ZUNIONSTORE로 임시 키 생성
        val tempKey = "$UNION_KEY_PREFIX${UUID.randomUUID()}"

        try {
            if (keys.size == 1) {
                // 키가 하나만 있으면 바로 조회
                return getTopProductsFromKey(keys[0])
            } else {
                // 여러 키를 합산하여 임시 키에 저장
                redisTemplate.opsForZSet().unionAndStore(keys[0], keys.drop(1), tempKey)
                return getTopProductsFromKey(tempKey)
            }
        } finally {
            // 임시 키 삭제
            redisTemplate.delete(tempKey)
        }
    }

    /**
     * 특정 키에서 상위 5개 상품 조회
     */
    private fun getTopProductsFromKey(key: String): List<ProductPeriodSaleDto> =
        redisTemplate
            .opsForZSet()
            .reverseRangeWithScores(key, 0, 4)
            ?.mapNotNull { tuple ->
                tuple.value?.toString()?.toLongOrNull()?.let { productId ->
                    ProductPeriodSaleDto(
                        productId = productId,
                        totalSales = tuple.score?.toLong() ?: 0L,
                    )
                }
            } ?: emptyList()
}
