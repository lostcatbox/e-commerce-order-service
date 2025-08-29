package kr.hhplus.be.server.controller.order.dto

import kr.hhplus.be.server.facade.OrderCriteria
import kr.hhplus.be.server.facade.OrderItemCriteria

/**
 * 주문 생성 요청
 */
data class OrderCreateRequest(
    val userId: Long,
    val orderItems: List<OrderItemRequest>,
    val couponId: Long? = null,
) {
    fun toOrderCriteria(): OrderCriteria =
        OrderCriteria(
            userId = userId,
            orderItems =
                orderItems.map {
                    OrderItemCriteria(
                        productId = it.productId,
                        quantity = it.quantity,
                    )
                },
            usedCouponId = couponId,
        )
}

/**
 * 주문 상품 요청
 */
data class OrderItemRequest(
    val productId: Long,
    val quantity: Int,
)
