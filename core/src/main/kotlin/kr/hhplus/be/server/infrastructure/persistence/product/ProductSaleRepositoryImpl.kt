package kr.hhplus.be.server.infrastructure.persistence.product

import kr.hhplus.be.server.core.product.domain.ProductSale
import kr.hhplus.be.server.core.product.repository.ProductSaleRepository
import kr.hhplus.be.server.core.product.service.dto.ProductPeriodSaleDto
import kr.hhplus.be.server.infrastructure.persistence.product.jpa.JpaProductSaleRepository
import kr.hhplus.be.server.infrastructure.persistence.product.redis.RedisProductSaleRepository
import kr.hhplus.be.server.support.utils.CommonFormatter
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * 상품 판매량 Repository 구현체
 */
@Repository
class ProductSaleRepositoryImpl(
    private val redisProductSaleRepository: RedisProductSaleRepository,
    private val jpaProductSaleRepository: JpaProductSaleRepository,
) : ProductSaleRepository {
    override fun findPopularProducts(
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int,
    ): List<ProductPeriodSaleDto> {
        val productIdToTotalQuantity = mutableMapOf<Long, Long>() // productId to totalQuantity

        // redis에서 집계된 데이터를 기반으로 판매량 통계
        val rangeOfDate =
            startDate
                .datesUntil(endDate.plusDays(1))
                .map {
                    CommonFormatter.toLongYYYYMMDD(it)
                }.toList()

        for (saleDate in rangeOfDate) {
            val salesData = redisProductSaleRepository.findSalesDataByDate(saleDate)
            // 각 날짜별 제품 판매량 데이터를 누적 합산
            salesData.forEach { productSale ->
                if (productSale.productId in productIdToTotalQuantity.keys) {
                    productIdToTotalQuantity[productSale.productId] =
                        productIdToTotalQuantity[productSale.productId]!! + productSale.getTotalQuantity()
                } else {
                    productIdToTotalQuantity[productSale.productId] = productSale.getTotalQuantity().toLong()
                }
            }
        }

        // 판매량 기준 상위 5개 상품 추출 및 DTO 변환
        return productIdToTotalQuantity.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { entry ->
                ProductPeriodSaleDto(
                    productId = entry.key,
                    totalSales = entry.value,
                )
            }
    }

    /**
     * ProductSale 도메인 객체 저장
     */
    override fun save(productSale: ProductSale): ProductSale = redisProductSaleRepository.save(productSale)

    /**
     * ProductSale 도메인 객체 저장
     * Write-Back 전략으로 JPA에 저장
     */
    override fun saveAllToBack(productSales: List<ProductSale>): List<ProductSale> = jpaProductSaleRepository.saveAll(productSales)

    /**
     * ProductSale 도메인 객체 조회
     * Write-Back 전략 전 현재 데이터 조회
     */
    override fun findSaleDataFromBack(saleDate: Long): List<ProductSale> = jpaProductSaleRepository.findAllBySaleDate(saleDate)

    /**
     * 특정 날짜의 Redis 판매량 데이터 조회
     */
    override fun findSalesDataByDate(saleDate: Long): List<ProductSale> = redisProductSaleRepository.findSalesDataByDate(saleDate)
}
