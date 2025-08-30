package kr.hhplus.be.server.core.order.domain

/**
 * 주문 상품 커맨드
 */
data class OrderItemCommand(
    val productId: Long,
    val quantity: Int,
) {
    init {
        require(productId > 0) { "상품 ID는 0보다 커야 합니다. 입력된 ID: $productId" }
        require(quantity >= 1) { "주문 수량은 1 이상이어야 합니다. 입력된 수량: $quantity" }
    }
}
