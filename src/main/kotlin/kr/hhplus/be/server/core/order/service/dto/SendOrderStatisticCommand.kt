package kr.hhplus.be.server.core.order.service.dto

import kr.hhplus.be.server.core.order.domain.Order

data class SendOrderStatisticCommand(
    val finalOrder: Order,
) {
    val orderId: Long = finalOrder.orderId
    val userId: Long = finalOrder.userId
    val orderItemIdAndQuantity: Map<Long, Int> = finalOrder.orderItems.associate { it -> it.productId to it.quantity }
}
