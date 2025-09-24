package kr.hhplus.be.server.core.order.event

import kr.hhplus.be.server.core.common.event.DomainEvent

/**
 * 주문 생성 완료 이벤트
 */
data class OrderCreatedEvent(
    val orderId: Long,
    val userId: Long,
    val orderItems: List<OrderItemEventData>,
    val usedCouponId: Long?,
) : DomainEvent()
