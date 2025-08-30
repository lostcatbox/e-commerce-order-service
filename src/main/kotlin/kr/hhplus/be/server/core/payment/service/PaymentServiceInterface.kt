package kr.hhplus.be.server.core.payment.service

import kr.hhplus.be.server.core.payment.domain.Payment
import kr.hhplus.be.server.core.payment.domain.ProcessPaymentCommand

/**
 * 결제 서비스 인터페이스
 */
interface PaymentServiceInterface {
    /**
     * 결제 처리
     */
    fun processPayment(command: ProcessPaymentCommand): Payment

    /**
     * 결제 조회
     */
    fun getPayment(paymentId: Long): Payment

    /**
     * 주문별 결제 조회
     */
    fun getPaymentByOrderId(orderId: Long): Payment?
}
