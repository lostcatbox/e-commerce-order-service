package kr.hhplus.be.server.core.product.repository

import kr.hhplus.be.server.core.product.domain.ProductSale
import kr.hhplus.be.server.core.product.service.dto.ProductPeriodSaleDto
import java.time.LocalDate

interface ProductSaleRepository {
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
     * 상품 판매량 데이터 저장
     * @param productSale 저장할 ProductSale 도메인 객체
     * @return 저장된 ProductSale 도메인 객체
     */
    fun save(productSale: ProductSale): ProductSale

    /**
     * 특정 상품의 특정 날짜 판매 데이터 조회
     * @param productId 상품 ID
     * @param saleDate 판매 날짜 (YYYYMMDD 형태)
     * @return 해당 조건의 ProductSale 또는 null
     */
    fun findByProductIdAndSaleDate(
        productId: Long,
        saleDate: Long,
    ): ProductSale?
}
