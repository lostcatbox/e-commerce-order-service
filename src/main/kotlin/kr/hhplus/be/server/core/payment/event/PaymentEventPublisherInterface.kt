package kr.hhplus.be.server.core.payment.event

import kr.hhplus.be.server.core.order.service.dto.OrderItemCommand

/**
 * 결제 도메인 이벤트 발행 인터페이스
 */
interface PaymentEventPublisherInterface {
    /**
     * 결제 성공 이벤트 발행
     */
    fun publishPaymentSucceeded(
        orderId: Long,
        paymentId: Long,
        finalAmount: Long
    )

    /**
     * 결제 실패 이벤트 발행
     */
    fun publishPaymentFailed(
        orderId: Long,
        paymentId: Long,
        failureReason: String,
        orderItems: List<OrderItemCommand>
    )
}
