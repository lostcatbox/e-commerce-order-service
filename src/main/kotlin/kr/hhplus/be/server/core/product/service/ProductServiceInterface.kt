package kr.hhplus.be.server.core.product.service

import kr.hhplus.be.server.core.product.domain.Product
import kr.hhplus.be.server.core.product.service.dto.SaleProductsCommand

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
     * 주문 상품들 판매 처리 (재고 차감)
     * @param command 상품 판매 처리 커맨드
     */
    fun saleOrderProducts(command: SaleProductsCommand)
}
