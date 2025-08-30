package kr.hhplus.be.server.controller.order.dto

/**
 * 주문 생성 응답
 */
data class OrderCreateResponse(
    val orderId: Long,
    val userId: Long,
    val orderItems: List<OrderItemInfo>,
    val originalAmount: Long, // 할인 전 금액
    val discountAmount: Long, // 할인 금액
    val finalAmount: Long, // 최종 결제 금액
    val paymentId: Long,
    val orderStatus: String, // REQUESTED, PAID, SUCCESS, FAILED
    val paymentStatus: String, // REQUESTED, SUCCESS, FAILED
    val usedCouponId: Long?,
    val createdAt: Long,
)

/**
 * 주문 상품 정보
 */
data class OrderItemInfo(
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Long,
    val totalPrice: Long,
)
