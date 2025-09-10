package kr.hhplus.be.server.infra.persistence.product.jpa

import jakarta.persistence.LockModeType
import kr.hhplus.be.server.core.product.domain.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

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

    /**
     * 상품 ID로 상품 조회
     * @param productId 상품 ID
     * @return Product 또는 null
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithPessimisticLockByProductId(productId: Long): Product?
}
