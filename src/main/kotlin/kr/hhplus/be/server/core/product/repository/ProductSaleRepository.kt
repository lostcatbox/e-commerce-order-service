package kr.hhplus.be.server.core.product.repository

import kr.hhplus.be.server.core.product.domain.ProductSale
import kr.hhplus.be.server.core.product.service.dto.ProductPeriodSaleDto
import java.time.LocalDate

interface ProductSaleRepository {
    /**
     * 해당 연월일 기준으로 인기 상품 목록 전체 조회
     * @return 인기 상품 목록 (판매량 기준 내림차순)
     */
    fun findPopularProducts(dateTime: LocalDate): List<ProductSale>

    /**
     * 특정 기간 동안 상위 판매량 상품 조회 (상위 5개)
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 인기 상품 판매량 정보 목록 (판매량 기준 내림차순)
     */
    fun findPopularProductsInfo(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<ProductPeriodSaleDto>

    /**
     * 상품 판매량 데이터 저장/업데이트
     * @param productId 상품 ID
     * @param saleDate 판매 날짜 (YYYYMMDD 형태)
     * @param quantity 판매 수량
     */
    fun saveProductSale(
        productId: Long,
        saleDate: Long,
        quantity: Int,
    )
}
