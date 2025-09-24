package kr.hhplus.be.server.fake.event

import kr.hhplus.be.server.core.order.event.OrderCompletedEvent
import kr.hhplus.be.server.core.order.event.OrderEventPublisherInterface
import kr.hhplus.be.server.core.order.event.OrderItemEventData

/**
 * 테스트용 가짜 주문 이벤트 발행자
 * 실제 이벤트 발행 없이 테스트 통과만을 위한 구현체
 */
class FakeOrderEventPublisher : OrderEventPublisherInterface {
    override fun publishOrderCreated(
        orderId: Long,
        userId: Long,
        orderItems: List<OrderItemEventData>,
        usedCouponId: Long?,
    ) {
        // 테스트용 - 실제 이벤트 발행 없음
    }

    override fun publishOrderCompleted(event: OrderCompletedEvent) {
        // 테스트용 - 실제 이벤트 발행 없음
    }

    override fun publishOrderProductReady(
        orderId: Long,
        userId: Long,
        orderItems: List<OrderItemEventData>,
    ) {
        // 테스트용 - 실제 이벤트 발행 없음
    }

    override fun publishOrderPaymentReady(
        orderId: Long,
        userId: Long,
        totalAmount: Long,
        discountAmount: Long,
        finalAmount: Long,
    ) {
        // 테스트용 - 실제 이벤트 발행 없음
    }

    override fun publishOrderFailed(
        orderId: Long,
        failureReason: String,
        failedStep: String
    ) {
        // 테스트용 - 실제 이벤트 발행 없음
    }
}
