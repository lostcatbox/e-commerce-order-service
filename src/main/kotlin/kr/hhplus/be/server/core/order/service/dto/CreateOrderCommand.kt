package kr.hhplus.be.server.core.order.service.dto

/**
 * 주문 생성 커맨드
 */
data class CreateOrderCommand(
    val userId: Long,
    val orderItems: List<OrderItemCommand>,
    val usedCouponId: Long? = null,
) {
    init {
        require(userId > 0) { "사용자 ID는 0보다 커야 합니다. 입력된 ID: $userId" }
        require(orderItems.isNotEmpty()) { "주문 상품은 1개 이상이어야 합니다." }
        require(orderItems.all { it.quantity >= 1 }) { "모든 주문 상품의 수량은 1 이상이어야 합니다." }
    }
}
