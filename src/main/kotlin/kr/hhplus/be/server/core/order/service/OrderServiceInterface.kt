package kr.hhplus.be.server.core.order.service

import kr.hhplus.be.server.core.order.domain.Order
import kr.hhplus.be.server.core.order.service.dto.CreateOrderCommand

/**
 * 주문 서비스 인터페이스
 */
interface OrderServiceInterface {
    /**
     * 주문 생성
     */
    fun createOrder(command: CreateOrderCommand): Order

    /**
     * 주문 상태를 상품 준비 완료로 변경
     */
    fun changeProductReady(orderId: Long): Order

    /**
     * 주문 상태를 결제 대기로 변경
     */
    fun changePaymentReady(orderId: Long): Order

    /**
     * 주문 상태를 결제 완료로 변경
     */
    fun changePaymentComplete(
        orderId: Long,
        paymentId: Long,
    ): Order

    /**
     * 주문 상태를 완료로 변경
     */
    fun changeCompleted(orderId: Long): Order

    /**
     * 주문 상태를 실패로 변경
     */
    fun changeFailed(orderId: Long): Order

    /**
     * 주문 조회
     */
    fun getOrder(orderId: Long): Order

    /**
     * 사용자별 주문 목록 조회
     */
    fun getUserOrders(userId: Long): List<Order>
}
