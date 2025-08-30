package kr.hhplus.be.server.controller.order.dto

/**
 * 주문 생성 요청
 */
data class OrderCreateRequest(
    val userId: Long,
    val orderItems: List<OrderItemRequest>,
    val couponId: Long? = null,
)

/**
 * 주문 상품 요청
 */
data class OrderItemRequest(
    val productId: Long,
    val quantity: Int,
)
