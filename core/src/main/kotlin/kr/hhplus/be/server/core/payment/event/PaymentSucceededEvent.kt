package kr.hhplus.be.server.core.payment.event

import kr.hhplus.be.server.event.DomainEvent

/**
 * 결제 성공 이벤트
 */
data class PaymentSucceededEvent(
    val orderId: Long,
    val paymentId: Long,
    val finalAmount: Long,
) : DomainEvent()
