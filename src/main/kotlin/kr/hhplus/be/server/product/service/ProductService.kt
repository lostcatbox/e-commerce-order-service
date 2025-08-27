package kr.hhplus.be.server.product.service

import kr.hhplus.be.server.product.domain.Product
import kr.hhplus.be.server.product.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 상품 서비스 구현체
 */
@Service
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository,
) : ProductServiceInterface {
    /**
     * 상품 정보 조회
     */
    override fun getProduct(productId: Long): Product {
        validateProductId(productId)

        return productRepository.findByProductId(productId)
            ?: throw IllegalArgumentException("존재하지 않는 상품입니다. 상품 ID: $productId")
    }

    /**
     * 최근 3일간 가장 많이 팔린 상위 5개 상품 조회
     * TODO: 요구사항: 초당 1만 건 조회를 평균 0.3ms 안에 응답
     */
    override fun getPopularProducts(): List<Product> = productRepository.findPopularProducts()

    /**
     * 상품 ID 유효성 검증
     */
    private fun validateProductId(productId: Long) {
        require(productId > 0) { "상품 ID는 0보다 커야 합니다. 입력된 ID: $productId" }
    }
}
