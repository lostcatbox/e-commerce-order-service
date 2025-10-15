package kr.hhplus.be.server.infrastructure.persistence.product

import kr.hhplus.be.server.core.product.domain.ProductSale
import kr.hhplus.be.server.core.product.repository.ProductSaleRepository
import kr.hhplus.be.server.core.product.service.dto.ProductPeriodSaleDto
import kr.hhplus.be.server.infrastructure.persistence.product.redis.RedisProductSaleRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * 상품 판매량 Repository 구현체
 */
@Repository
class ProductSaleRepositoryImpl(
    private val redisProductSaleRepository: RedisProductSaleRepository,
) : ProductSaleRepository {
    override fun findPopularProductsInfo(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<ProductPeriodSaleDto> = redisProductSaleRepository.findPopularProductsInfo(startDate, endDate)

    /**
     * ProductSale 도메인 객체 저장
     * Infrastructure 계층은 순수하게 데이터 접근만 담당
     */
    override fun save(productSale: ProductSale): ProductSale = redisProductSaleRepository.save(productSale)

    /**
     * 상품 ID와 판매 날짜로 ProductSale 조회
     */
    override fun findByProductIdAndSaleDate(
        productId: Long,
        saleDate: Long,
    ): ProductSale? = redisProductSaleRepository.findByProductIdAndSaleDate(productId, saleDate)
}
