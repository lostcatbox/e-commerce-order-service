package kr.hhplus.be.server.infrastructure.persistence.product.jpa

import kr.hhplus.be.server.core.product.domain.ProductSale
import kr.hhplus.be.server.core.product.service.dto.ProductPeriodSaleDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * 상품 판매량 집계 JPA Repository 인터페이스
 */
interface JpaProductSaleRepository : JpaRepository<ProductSale, Long> {
    /**
     * 특정 기간 동안 상위 판매량 상품 ID 목록 조회
     * @param startDate 시작 날짜 (YYYYMMDD 형태)
     * @param endDate 종료 날짜 (YYYYMMDD 형태)
     * @param limit 조회할 상품 개수
     * @return 판매량 기준 상위 상품 ID 목록
     */
    @Query(
        """
        SELECT ps.productId
        FROM ProductSale ps
        WHERE ps.saleDate BETWEEN :startDate AND :endDate
        GROUP BY ps.productId
        ORDER BY SUM(ps.totalQuantity) DESC
        LIMIT :limit
    """,
    )
    fun findTopProductIdsBySalesInPeriod(
        startDate: Long,
        endDate: Long,
        limit: Int = 5,
    ): List<Long>

    /**
     * 특정 날짜의 판매량 데이터 조회
     * @param saleDate 판매 날짜 (YYYYMMDD 형태)
     * @return 해당 날짜의 판매량 데이터 목록
     */
    fun findBySaleDate(saleDate: Long): List<ProductSale>

    /**
     * 특정 상품의 특정 날짜 판매량 데이터 조회
     * @param productId 상품 ID
     * @param saleDate 판매 날짜 (YYYYMMDD 형태)
     * @return 판매량 데이터 또는 null
     */
    fun findByProductIdAndSaleDate(
        productId: Long,
        saleDate: Long,
    ): ProductSale?

    /**
     * 특정 기간 동안 상위 판매량 상품 조회 (ProductSale 테이블만 사용)
     * @param startDate 시작 날짜 (YYYYMMDD 형태)
     * @param endDate 종료 날짜 (YYYYMMDD 형태)
     * @param limit 조회할 상품 개수
     * @return 인기 상품 판매량 정보 목록 (판매량 기준 내림차순)
     */
    @Query(
        """
        SELECT new kr.hhplus.be.server.core.product.service.dto.ProductPeriodSaleDto(
            ps.productId,
            SUM(ps.totalQuantity)
        )
        FROM ProductSale ps
        WHERE ps.saleDate BETWEEN :startDate AND :endDate
        GROUP BY ps.productId
        ORDER BY SUM(ps.totalQuantity) DESC
        LIMIT :limit
    """,
    )
    fun findPopularProductsInfo(
        startDate: Long,
        endDate: Long,
        limit: Int = 5,
    ): List<ProductPeriodSaleDto>
}
