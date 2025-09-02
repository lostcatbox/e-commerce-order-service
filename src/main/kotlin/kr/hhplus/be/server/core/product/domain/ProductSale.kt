package kr.hhplus.be.server.core.product.domain

import jakarta.persistence.*

/**
 * 상품 판매량 집계 도메인 모델
 * 하루마다 ProductId별로 판매량이 집계되는 테이블
 */
@Entity
@Table(name = "product_sale")
class ProductSale(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_sale_id")
    val productSaleId: Long = 0L,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "sale_date", nullable = false)
    val saleDate: Long, // YYYYMMDD 형태의 날짜 (예: 20231225)

    @Column(name = "total_quantity", nullable = false)
    val totalQuantity: Int,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),
) {
    init {
        require(productId > 0) { "상품 ID는 0보다 커야 합니다. 입력된 ID: $productId" }
        require(saleDate > 0) { "판매 날짜는 0보다 커야 합니다. 입력된 날짜: $saleDate" }
        require(totalQuantity >= 0) { "총 판매량은 0 이상이어야 합니다. 입력된 판매량: $totalQuantity" }
    }
}
