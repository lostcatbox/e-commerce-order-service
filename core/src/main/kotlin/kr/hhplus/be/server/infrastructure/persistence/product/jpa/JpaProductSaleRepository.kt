package kr.hhplus.be.server.infrastructure.persistence.product.jpa

import kr.hhplus.be.server.core.product.domain.ProductSale
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 상품 판매량 집계 JPA Repository 인터페이스
 */
interface JpaProductSaleRepository : JpaRepository<ProductSale, Long> {
    fun findAllBySaleDate(saleDate: Long): List<ProductSale>
}
