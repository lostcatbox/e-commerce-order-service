package kr.hhplus.be.server.core.order.event

import kr.hhplus.be.server.core.common.event.DomainEvent

/**
 * 주문 결제 대기 상태 이벤트
 * 쿠폰 사용이 완료되어 결제 준비가 된 상태
 */
data class OrderPaymentReadyEvent(
    val orderId: Long,
    val userId: Long,
    val totalAmount: Long,
    val discountAmount: Long,
    val finalAmount: Long,
) : DomainEvent()
