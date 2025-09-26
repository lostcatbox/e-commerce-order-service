package kr.hhplus.be.server.core.order.event

/**
 * 주문 도메인 이벤트 발행 인터페이스
 */
interface OrderEventPublisherInterface {
    /**
     * 주문 생성 이벤트 발행
     */
    fun publishOrderCreated(
        orderId: Long,
        userId: Long,
        orderItems: List<OrderItemEventData>,
        usedCouponId: Long?,
    )

    /**
     * 주문 완료 이벤트 발행
     */
    fun publishOrderCompleted(event: OrderCompletedEvent)

    /**
     * 주문 상품 준비 완료 이벤트 발행
     */
    fun publishOrderProductReaserved(
        orderId: Long,
        userId: Long,
        orderItems: List<OrderItemEventData>,
    )

    /**
     * 주문 결제 대기 이벤트 발행
     */
    fun publishOrderPaymentReady(
        orderId: Long,
        userId: Long,
        totalAmount: Long,
        discountAmount: Long,
        finalAmount: Long,
    )

    /**
     * 주문 실패 이벤트 발행
     */
    fun publishOrderFailed(
        orderId: Long,
        failureReason: String,
        failedStep: String,
    )
}
