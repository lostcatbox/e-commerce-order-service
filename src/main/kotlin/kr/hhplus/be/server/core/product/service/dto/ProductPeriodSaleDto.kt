package kr.hhplus.be.server.core.product.service.dto

/**
 * 상품 판매량 정보 DTO
 * ProductSale 테이블 집계 결과를 담는 객체
 */
data class ProductPeriodSaleDto(
    val productId: Long,
    val totalSales: Long,
)
