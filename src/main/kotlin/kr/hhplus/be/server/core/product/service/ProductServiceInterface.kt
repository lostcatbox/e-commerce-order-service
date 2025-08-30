package kr.hhplus.be.server.core.product.service

import kr.hhplus.be.server.core.product.domain.Product
import kr.hhplus.be.server.core.product.domain.SaleProductsCommand

/**
 * 상품 서비스 인터페이스
 */
interface ProductServiceInterface {
    /**
     * 상품 정보 조회
     * @param productId 상품 ID
     * @return 상품 정보
     */
    fun getProduct(productId: Long): Product

    /**
     * 최근 3일간 가장 많이 팔린 상위 5개 상품 조회
     * @return 인기 상품 목록
     */
    fun getPopularProducts(): List<Product>

    /**
     * 주문 상품들 판매 처리 (재고 차감)
     * @param command 상품 판매 처리 커맨드
     */
    fun saleOrderProducts(command: SaleProductsCommand)
}
