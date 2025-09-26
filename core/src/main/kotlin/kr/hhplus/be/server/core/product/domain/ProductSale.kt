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
) {
    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis()

    companion object {

        /**
         * 새로운 판매 데이터 생성 팩토리 메서드
         * @param productId 상품 ID
         * @param saleDate 판매 날짜 (YYYYMMDD 형태)
         * @param quantity 판매 수량
         * @return 새로운 ProductSale 인스턴스
         */
        fun createNewSale(
            productId: Long,
            saleDate: Long,
            quantity: Int,
        ): ProductSale {
            return ProductSale(
                productId = productId,
                saleDate = saleDate,
                totalQuantity = quantity,
            )
        }
    }

    init {
        require(productId > 0) { "상품 ID는 0보다 커야 합니다. 입력된 ID: $productId" }
        require(saleDate > 0) { "판매 날짜는 0보다 커야 합니다. 입력된 날짜: $saleDate" }
        require(totalQuantity >= 0) { "총 판매량은 0 이상이어야 합니다. 입력된 판매량: $totalQuantity" }
    }

    /**
     * 판매 수량 추가
     * 기존 판매량에 새로운 판매량을 추가하여 새로운 ProductSale 인스턴스를 반환
     *
     * @param additionalQuantity 추가할 판매 수량
     * @return 수량이 추가된 새로운 ProductSale 인스턴스
     */
    fun addSaleQuantity(additionalQuantity: Int): ProductSale {
        val newTotalQuantity = this.totalQuantity + additionalQuantity

        return ProductSale(
            productId = this.productId,
            saleDate = this.saleDate,
            totalQuantity = newTotalQuantity,
        )
    }
}
