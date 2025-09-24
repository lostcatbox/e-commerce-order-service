package kr.hhplus.be.server.controller.order.dto

/**
 * 주문 생성 응답
 */
data class OrderCreateResponse(
    val orderId: Long,
    val userId: Long,
    val orderItems: List<OrderItemInfo>,
    val paymentId: Long?,
    val orderStatus: String, // REQUESTED, PAID, SUCCESS, FAILED
    val usedCouponId: Long?,
    val createdAt: Long,
    val message: String? = null, // Event-Driven 처리 상태 메시지
)

/**
 * 주문 상품 정보
 */
data class OrderItemInfo(
    val productId: Long,
    val quantity: Int,
    val unitPrice: Long,
    val totalPrice: Long,
)
