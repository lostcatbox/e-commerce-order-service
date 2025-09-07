package kr.hhplus.be.server.core.product.service

import kr.hhplus.be.server.core.product.service.dto.PopularProductDto

/**
 * 상품 서비스 인터페이스
 */
interface ProductSaleServiceInterface {
    /**
     * 최근 3일간 가장 많이 팔린 상위 5개 상품 조회
     * ProductSale 테이블만 사용하여 판매량 집계 후 상품 정보 조회
     */
    fun getPopularProducts(): List<PopularProductDto>

    /**
     * 특정 상품의 판매 기록 저장
     * @param productId 상품 ID
     * @param quantity 판매 수량
     */
    fun recordProductSale(
        productId: Long,
        quantity: Int,
    )
}
