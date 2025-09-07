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
) : ProductRepository {
    override fun findByProductId(productId: Long): Product? = jpaProductRepository.findByProductId(productId)

    override fun save(product: Product): Product = jpaProductRepository.save(product)
}
