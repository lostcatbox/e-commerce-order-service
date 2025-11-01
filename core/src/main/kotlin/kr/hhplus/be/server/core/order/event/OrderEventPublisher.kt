package kr.hhplus.be.server.core.order.event

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * 주문 도메인 이벤트 발행 구현체
 */
@Component
class OrderEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : OrderEventPublisherInterface {
    override fun publishOrderCreated(
        orderId: Long,
        userId: Long,
        orderItems: List<OrderCompletedEvent.OrderItemEventData>,
        usedCouponId: Long?,
    ) {
        applicationEventPublisher.publishEvent(
            OrderCreatedEvent(
                orderId = orderId,
                userId = userId,
                orderItems = orderItems,
                usedCouponId = usedCouponId,
            ),
        )
    }

    override fun publishOrderCompleted(event: OrderCompletedEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    override fun publishOrderProductReaserved(
        orderId: Long,
        userId: Long,
        orderItems: List<OrderCompletedEvent.OrderItemEventData>,
    ) {
        applicationEventPublisher.publishEvent(
            OrderProductReservedEvent(
                orderId = orderId,
                userId = userId,
                orderItems = orderItems,
            ),
        )
    }

    override fun publishOrderPaymentReady(
        orderId: Long,
        userId: Long,
        totalAmount: Long,
        discountAmount: Long,
        finalAmount: Long,
    ) {
        applicationEventPublisher.publishEvent(
            OrderPaymentReadyEvent(
                orderId = orderId,
                userId = userId,
                totalAmount = totalAmount,
                discountAmount = discountAmount,
                finalAmount = finalAmount,
            ),
        )
    }

    override fun publishOrderFailed(
        orderId: Long,
        failureReason: String,
        failedStep: String,
    ) {
        applicationEventPublisher.publishEvent(
            OrderFailedEvent(
                orderId = orderId,
                failureReason = failureReason,
                failedStep = failedStep,
            ),
        )
    }
}
