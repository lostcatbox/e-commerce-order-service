package kr.hhplus.be.server.infra.persistence.product.jpa

import kr.hhplus.be.server.core.product.domain.Product
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 상품 JPA Repository 인터페이스
 */
interface JpaProductRepository : JpaRepository<Product, Long> {
    /**
     * 상품 ID로 상품 조회
     * @param productId 상품 ID
     * @return Product 또는 null
     */
    fun findByProductId(productId: Long): Product?
}
