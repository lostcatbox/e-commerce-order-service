package kr.hhplus.be.server.core.product.service.dto

import kr.hhplus.be.server.core.order.service.dto.OrderItemCommand

/**
 * 상품 판매 처리 커맨드
 */
data class SaleProductsCommand(
    val orderItems: List<OrderItemCommand>,
) {
    init {
        require(orderItems.isNotEmpty()) { "주문 상품은 1개 이상이어야 합니다." }
        require(orderItems.all { it.productId > 0 }) { "모든 상품 ID는 0보다 커야 합니다." }
        require(orderItems.all { it.quantity >= 1 }) { "모든 주문 수량은 1 이상이어야 합니다." }
    }
}
