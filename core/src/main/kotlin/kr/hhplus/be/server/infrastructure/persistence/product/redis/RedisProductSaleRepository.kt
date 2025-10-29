package kr.hhplus.be.server.infrastructure.persistence.product.redis

import kr.hhplus.be.server.core.product.domain.ProductSale
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

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
        private val log = LoggerFactory.getLogger(RedisProductSaleRepository::class.java)
    }

    fun save(productSale: ProductSale): ProductSale {
        try {
            val dateKey = "$RANK_KEY_PREFIX${productSale.saleDate}"

            // Redis Sorted Set에 판매량 증가 (ZINCRBY)
            redisTemplate.opsForZSet().incrementScore(
                dateKey,
                productSale.productId.toString(),
                productSale.getTotalQuantity().toDouble(),
            )

            // TTL 설정 (5일)
            redisTemplate.expire(dateKey, Duration.ofDays(5))

            log.debug(
                "Redis에 판매량 기록 완료: productId={}, quantity={}, date={}",
                productSale.productId,
                productSale.getTotalQuantity(),
                productSale.saleDate,
            )

            return productSale
        } catch (exception: Exception) {
            log.error("Redis 판매량 저장 실패", exception)
            throw exception
        }
    }

    /**
     * 특정 날짜의 Redis 판매량 데이터 조회
     * @param saleDate 판매 날짜 (YYYYMMDD 형태)
     * @return 해당 날짜의 판매량 데이터 목록
     */
    fun findSalesDataByDate(saleDate: Long): List<ProductSale> {
        try {
            val dateKey = "$RANK_KEY_PREFIX$saleDate"
            val salesData = redisTemplate.opsForZSet().rangeWithScores(dateKey, 0, -1)

            return salesData?.mapNotNull { tuple ->
                tuple.value?.toString()?.toLongOrNull()?.let { productId ->
                    val quantity = tuple.score?.toInt() ?: 0
                    if (quantity > 0) {
                        ProductSale.createNewSale(
                            productId = productId,
                            saleDate = saleDate,
                            quantity = quantity,
                        )
                    } else {
                        null
                    }
                }
            } ?: emptyList()
        } catch (exception: Exception) {
            log.error("Redis에서 특정 날짜 판매량 데이터 조회 실패: saleDate={}", saleDate, exception)
            return emptyList()
        }
    }
}
