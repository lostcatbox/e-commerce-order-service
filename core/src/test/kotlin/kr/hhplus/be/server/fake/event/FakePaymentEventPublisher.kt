package kr.hhplus.be.server.fake.event

import kr.hhplus.be.server.core.order.service.dto.OrderItemCommand
import kr.hhplus.be.server.core.payment.event.PaymentEventPublisherInterface

/**
 * 테스트용 가짜 결제 이벤트 발행자
 * 실제 이벤트 발행 없이 테스트 통과만을 위한 구현체
 */
class FakePaymentEventPublisher : PaymentEventPublisherInterface {
    override fun publishPaymentSucceeded(
        orderId: Long,
        paymentId: Long,
        finalAmount: Long,
    ) {
        // 테스트용 - 실제 이벤트 발행 없음
    }

    override fun publishPaymentFailed(
        orderId: Long,
        failureReason: String,
        orderItems: List<OrderItemCommand>,
    ) {
        // 테스트용 - 실제 이벤트 발행 없음
    }
}
