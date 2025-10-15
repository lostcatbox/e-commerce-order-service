package kr.hhplus.be.server.core.order.event

import kr.hhplus.be.server.event.DomainEvent

/**
 * 주문 실패 이벤트
 */
data class OrderFailedEvent(
    val orderId: Long,
    val failureReason: String,
    val failedStep: String,
) : DomainEvent()
