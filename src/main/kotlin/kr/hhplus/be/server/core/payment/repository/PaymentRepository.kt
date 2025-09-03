package kr.hhplus.be.server.core.payment.repository

import kr.hhplus.be.server.core.payment.domain.Payment

/**
 * 결제 레포지토리 인터페이스
 */
interface PaymentRepository {
    /**
     * 결제 저장
     */
    fun save(payment: Payment): Payment

    /**
     * 결제 ID로 조회
     */
    fun findByPaymentId(paymentId: Long): Payment?

    /**
     * 주문 ID로 결제 조회
     */
    fun findByOrderId(orderId: Long): Payment?
}
