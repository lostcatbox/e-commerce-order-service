package kr.hhplus.be.server.core.order.event

import kr.hhplus.be.server.core.common.event.DomainEvent

/**
 * 주문 상품 준비 완료 이벤트
 * 주문이 생성되어 상품 준비가 완료된 상태
 */
data class OrderProductReadyEvent(
    val orderId: Long,
    val userId: Long,
    val orderItems: List<OrderItemEventData>,
) : DomainEvent()
