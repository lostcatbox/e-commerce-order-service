package kr.hhplus.be.server.core.product.repository

import kr.hhplus.be.server.core.product.domain.Product

/**
 * 상품 조회 관련 repository 인터페이스
 */
interface ProductRepository {
    /**
     * 상품 ID로 상품 조회
     * @param productId 상품 ID
     * @return 상품 정보, 존재하지 않으면 null
     */
    fun findByProductId(productId: Long): Product?

    /**
     * 상품 정보 저장/업데이트
     * @param product 저장할 상품 정보
     * @return 저장된 상품 정보
     */
    fun save(product: Product): Product

    /**
     * 최근 3일간 가장 많이 팔린 상위 5개 상품 조회
     * @return 인기 상품 목록 (판매량 기준 내림차순)
     */
    fun findPopularProducts(): List<Product>

    /**
     * 모든 상품 조회
     * @return 전체 상품 목록
     */
    fun findAll(): List<Product>
}
