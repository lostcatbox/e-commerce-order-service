package kr.hhplus.be.server.infra.persistence.product

import kr.hhplus.be.server.core.product.domain.Product
import kr.hhplus.be.server.core.product.repository.ProductRepository
import kr.hhplus.be.server.infra.persistence.product.jpa.JpaProductRepository
import kr.hhplus.be.server.infra.persistence.product.jpa.JpaProductSaleRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 상품 Repository 구현체
 */
@Repository
class ProductRepositoryImpl(
    private val jpaProductRepository: JpaProductRepository,
    private val jpaProductSaleRepository: JpaProductSaleRepository,
) : ProductRepository {
    override fun findByProductId(productId: Long): Product? = jpaProductRepository.findByProductId(productId)

    override fun save(product: Product): Product = jpaProductRepository.save(product)

    override fun findPopularProducts(): List<Product> {
        // 현재 날짜와 3일 전 날짜를 YYYYMMDD 형태로 계산
        val today = LocalDate.now()
        val threeDaysAgo = today.minusDays(3)
        
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val startDate = threeDaysAgo.format(formatter).toLong()
        val endDate = today.format(formatter).toLong()

        // ProductSale 테이블에서 상위 5개 상품 ID 조회
        val popularProductIds = jpaProductSaleRepository.findTopProductIdsBySalesInPeriod(
            startDate = startDate,
            endDate = endDate,
            limit = 5
        )

        // 상품 ID들로 실제 Product 정보 조회
        return popularProductIds.mapNotNull { productId ->
            jpaProductRepository.findByProductId(productId)
        }
    }
}
