package kr.hhplus.be.server.core.order.event

import kr.hhplus.be.server.core.order.domain.Order
import kr.hhplus.be.server.event.DomainEvent

/**
 * 주문 완료 이벤트
 *
 * 주문이 완료되었을 때 발행되는 도메인 이벤트
 * 외부 통계 시스템에 주문 정보를 전송하거나, 판매량 통계 업데이트 등의
 * 후속 작업을 비동기적으로 처리하기 위해 사용됩니다.
 */
data class OrderCompletedEvent(
    val orderId: Long,
    val userId: Long,
    val totalAmount: Long,
    val usedCouponId: Long?,
    val orderItems: List<OrderItemEventData>,
    val orderCreatedDateTime: Long,
) : DomainEvent() {
    companion object {
        fun from(order: Order): OrderCompletedEvent =
            OrderCompletedEvent(
                orderId = order.orderId,
                userId = order.userId,
                totalAmount = order.calculateTotalAmount(),
                usedCouponId = order.usedCouponId,
                orderItems =
                    order.orderItems.map { orderItem ->
                        OrderItemEventData(
                            productId = orderItem.productId,
                            quantity = orderItem.quantity,
                            unitPrice = orderItem.unitPrice,
                        )
                    },
                orderCreatedDateTime = order.getCreatedAt(),
            )
    }
}

/**
 * 주문 상품 이벤트 데이터
 */
data class OrderItemEventData(
    val productId: Long,
    val quantity: Int,
    val unitPrice: Long,
)
