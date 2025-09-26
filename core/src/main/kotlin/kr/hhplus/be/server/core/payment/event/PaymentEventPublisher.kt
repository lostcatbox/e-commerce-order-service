package kr.hhplus.be.server.core.payment.event

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * 결제 도메인 이벤트 발행 구현체
 */
@Component
class PaymentEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : PaymentEventPublisherInterface {

    override fun publishPaymentSucceeded(
        orderId: Long,
        paymentId: Long,
        finalAmount: Long
    ) {
        applicationEventPublisher.publishEvent(
            PaymentSucceededEvent(
                orderId = orderId,
                paymentId = paymentId,
                finalAmount = finalAmount
            )
        )
    }

    override fun publishPaymentFailed(
        orderId: Long,
        paymentId: Long,
        failureReason: String,
        orderItems: List<kr.hhplus.be.server.core.order.service.dto.OrderItemCommand>
    ) {
        applicationEventPublisher.publishEvent(
            PaymentFailedEvent(
                orderId = orderId,
                paymentId = paymentId,
                failureReason = failureReason,
                orderItems = orderItems
            )
        )
    }
}
