package kr.hhplus.be.server.core.product.service

import jakarta.transaction.Transactional
import kr.hhplus.be.server.core.product.domain.ProductSale
import kr.hhplus.be.server.core.product.repository.ProductRepository
import kr.hhplus.be.server.core.product.repository.ProductSaleRepository
import kr.hhplus.be.server.core.product.service.dto.PopularProductDto
import kr.hhplus.be.server.support.utils.CommonFormatter
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ProductSaleService(
    private val productSaleRepository: ProductSaleRepository,
    private val productRepository: ProductRepository,
) : ProductSaleServiceInterface {
    companion object {
        private val log = LoggerFactory.getLogger(ProductSaleService::class.java)
    }

    /**
     * 최근 3일간 가장 많이 팔린 상위 5개 상품 조회
     *
     * @Cacheable: 캐시된 데이터가 있으면 반환, 없으면 메서드 실행 후 캐시에 저장 (Read-through 캐싱)(TTL: 1분)
     * - 캐시 키: "popular_products::top5" (고정 키 사용)
     */
    @Cacheable(
        value = ["popular_products"],
        key = "'top5'",
    )
    override fun getPopularProductsInTop5(targetDate: LocalDate): List<PopularProductDto> {
        // 최근 3일간 데이터 조회
        val twoDaysAgo = targetDate.minusDays(2)
        val popularProductsInfo = productSaleRepository.findPopularProducts(twoDaysAgo, targetDate, 5)

        // 상품 정보 조회 및 DTO 변환
        return popularProductsInfo.mapNotNull { salesInfo ->
            productRepository.findByProductId(salesInfo.productId)?.let { product ->
                PopularProductDto(
                    productId = salesInfo.productId,
                    productName = product.name,
                    productDescription = product.description,
                    productPrice = product.price,
                    totalSales = salesInfo.totalSales,
                )
            }
        }
    }

    /**
     * Write-Back 캐싱: Redis에 상품 판매량을 저장하고 즉시 응답
     * 실제 DB 저장은 스케줄러에서 배치로 처리
     */
    @Transactional
    override fun recordProductSale(
        productId: Long,
        quantity: Int,
        saleDate: LocalDate,
    ) {
        validateProductId(productId)
        validateQuantity(quantity)

        val productSale =
            ProductSale.createNewSale(
                productId = productId,
                saleDate = CommonFormatter.toLongYYYYMMDD(saleDate),
                quantity = quantity,
            )

        // Redis에 즉시 저장 (빠른 응답)
        productSaleRepository.save(productSale)

        log.debug("Redis에 상품 판매량 기록 완료: productId={}, quantity={}", productId, quantity)
    }

    /**
     * Redis → DB 동기화 (스케줄러에서 호출)
     * Redis에 저장된 모든 판매량 데이터를 DB에 배치로 저장
     */
    @Transactional
    fun syncRedisCacheToDatabase(): Boolean =
        try {
            log.info("Redis → DB 동기화 시작")

            // 1. 어제까지의 날짜 범위 계산 (오늘 제외)
            val yesterday = LocalDate.now().minusDays(1)

            val targetDateAtLong = CommonFormatter.toLongYYYYMMDD(yesterday)
            log.info(
                "판매량 데이터 동기화 대상 날짜 (Redis -> MySql): {}",
                targetDateAtLong,
            )

            val saleDataInCache = productSaleRepository.findSalesDataByDate(targetDateAtLong).toMutableList()
            val willSaveProductSales = mutableListOf<ProductSale>()
            val alreadySavedProductSalesInDB = productSaleRepository.findSaleDataFromBack(targetDateAtLong)

            // Redis와 DB에 모두 존재하는 경우, Redis의 판매량을 DB에 반영
            alreadySavedProductSalesInDB.forEach { existingSaleData ->
                saleDataInCache.find { it.productId == existingSaleData.productId }?.let { redisSaleData ->
                    existingSaleData.changeTotalQuantity(redisSaleData.getTotalQuantity())
                    saleDataInCache.remove(redisSaleData)
                    willSaveProductSales.add(existingSaleData)
                }
            }

            // Redis에만 존재하는 데이터는 DB에 새로 추가
            willSaveProductSales.addAll(saleDataInCache)

            productSaleRepository.saveAllToBack(willSaveProductSales)

            if (willSaveProductSales.size > 0) {
                log.info("Redis → DB 동기화 완료: {} 날짜, {} 건 처리", targetDateAtLong, willSaveProductSales.size)
            } else {
                log.info("동기화할 Redis 데이터가 없습니다")
            }

            true
        } catch (exception: Exception) {
            log.error("Redis → DB 동기화 실패", exception)
            false
        }

    /**
     * 상품 ID 유효성 검증
     */
    private fun validateProductId(productId: Long) {
        require(productId > 0) { "상품 ID는 0보다 커야 합니다. 입력된 ID: $productId" }
    }

    /**
     * 수량 유효성 검증
     */
    private fun validateQuantity(quantity: Int) {
        require(quantity > 0) { "판매 수량은 0보다 커야 합니다. 입력된 수량: $quantity" }
    }
}
