package kr.hhplus.be.server.facade

import kr.hhplus.be.server.order.domain.CreateOrderCommand
import kr.hhplus.be.server.order.domain.OrderItemCommand
import kr.hhplus.be.server.product.domain.SaleProductsCommand

/**
 * 주문 생성 기준
 */
data class OrderCriteria(
    val userId: Long,
    val orderItems: List<OrderItemCriteria>,
    val usedCouponId: Long? = null,
) {
    init {
        require(userId > 0) { "사용자 ID는 0보다 커야 합니다. 입력된 ID: $userId" }
        require(orderItems.isNotEmpty()) { "주문 상품은 1개 이상이어야 합니다." }
    }

    /**
     * OrderItemCriteria를 OrderItemCommand로 변환
     */
    fun toOrderItemCommands(): List<OrderItemCommand> =
        orderItems.map { orderItemCriteria ->
            OrderItemCommand(
                productId = orderItemCriteria.productId,
                quantity = orderItemCriteria.quantity,
            )
        }

    /**
     * CreateOrderCommand로 변환
     */
    fun toCreateOrderCommand(): CreateOrderCommand =
        CreateOrderCommand(
            userId = userId,
            orderItems = toOrderItemCommands(),
            usedCouponId = usedCouponId,
        )

    /**
     * SaleProductsCommand로 변환
     */
    fun toSaleProductsCommand(): SaleProductsCommand =
        SaleProductsCommand(
            orderItems = toOrderItemCommands(),
        )
}

/**
 * 주문 상품 기준
 */
data class OrderItemCriteria(
    val productId: Long,
    val quantity: Int,
) {
    init {
        require(productId > 0) { "상품 ID는 0보다 커야 합니다. 입력된 ID: $productId" }
        require(quantity >= 1) { "주문 수량은 1 이상이어야 합니다. 입력된 수량: $quantity" }
    }
}
