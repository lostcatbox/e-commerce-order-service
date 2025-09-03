package kr.hhplus.be.server.core.order.domain

import jakarta.persistence.*

/**
 * 주문 상품 Value Object
 * Order Aggregate의 일부로 동작
 */
@Embeddable
data class OrderItem(
    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "quantity", nullable = false)
    val quantity: Int,

    @Column(name = "unit_price", nullable = false)
    val unitPrice: Long,
) {
    val totalPrice: Long = calculateTotalPrice()

    companion object {
        const val MIN_QUANTITY = 1 // 최소 주문 수량
        const val MAX_QUANTITY = 100 // 최대 주문 수량
    }

    init {
        require(productId > 0) { "상품 ID는 0보다 커야 합니다. 입력된 ID: $productId" }
        require(quantity >= MIN_QUANTITY) { "주문 수량은 $MIN_QUANTITY 이상이어야 합니다. 입력된 수량: $quantity" }
        require(quantity <= MAX_QUANTITY) { "주문 수량은 $MAX_QUANTITY 이하여야 합니다. 입력된 수량: $quantity" }
        require(unitPrice > 0) { "상품 단가는 0보다 커야 합니다. 입력된 단가: $unitPrice" }
    }

    /**
     * 주문 상품의 총 가격 계산
     */
    fun calculateTotalPrice(): Long = unitPrice * quantity
}
