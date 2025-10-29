package kr.hhplus.be.server.core.product.repository

import kr.hhplus.be.server.core.product.domain.ProductSale
import kr.hhplus.be.server.core.product.service.dto.ProductPeriodSaleDto
import java.time.LocalDate

interface ProductSaleRepository {
    /**
     * 특정 기간 동안 상위 판매량 상품 조회 (상위 5개)
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param limit 조회할 상품 개수
     * @return 인기 상품 판매량 정보 목록 (판매량 기준 내림차순)
     */
    fun findPopularProducts(
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int,
    ): List<ProductPeriodSaleDto>

    /**
     * 상품 판매량 데이터 저장
     * @param productSale 저장할 ProductSale 도메인 객체
     * @return 저장된 ProductSale 도메인 객체
     */
    fun save(productSale: ProductSale): ProductSale

    /**
     * ProductSale 도메인 객체 저장
     * Write-Back 전략으로 JPA에 저장
     */
    fun saveAllToBack(productSales: List<ProductSale>): List<ProductSale>

    /**
     * 특정 날짜의 Redis 판매량 데이터 조회
     * @param saleDate 판매 날짜 (YYYYMMDD 형태)
     * @return 해당 날짜의 판매량 데이터 목록
     */
    fun findSalesDataByDate(saleDate: Long): List<ProductSale>

    /**
     * 특정 날짜의 JPA 판매량 데이터 조회 (Write-Back 용도)
     * @param saleDate 판매 날짜 (YYYYMMDD 형태)
     * @return 해당 날짜의 판매량 데이터 목록
     */
    fun findSaleDataFromBack(saleDate: Long): List<ProductSale>
}
