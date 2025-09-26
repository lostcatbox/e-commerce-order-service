package kr.hhplus.be.server.core.payment.event

import kr.hhplus.be.server.core.common.event.DomainEvent
import kr.hhplus.be.server.core.order.service.dto.OrderItemCommand

/**
 * 결제 실패 이벤트
 */
data class PaymentFailedEvent(
    val orderId: Long,
    val paymentId: Long,
    val failureReason: String,
    val orderItems: List<OrderItemCommand>, // 재고 복구용 주문 상품 데이터
) : DomainEvent()
