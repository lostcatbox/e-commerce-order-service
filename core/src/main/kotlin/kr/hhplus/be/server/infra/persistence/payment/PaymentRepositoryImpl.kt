package kr.hhplus.be.server.infra.persistence.payment

import kr.hhplus.be.server.core.payment.domain.Payment
import kr.hhplus.be.server.core.payment.repository.PaymentRepository
import kr.hhplus.be.server.infra.persistence.payment.jpa.JpaPaymentRepository
import org.springframework.stereotype.Repository

/**
 * 결제 Repository 구현체
 */
@Repository
class PaymentRepositoryImpl(
    private val jpaPaymentRepository: JpaPaymentRepository,
) : PaymentRepository {
    override fun save(payment: Payment): Payment = jpaPaymentRepository.save(payment)

    override fun findByPaymentId(paymentId: Long): Payment? = jpaPaymentRepository.findByPaymentId(paymentId)

    override fun findByOrderId(orderId: Long): Payment? = jpaPaymentRepository.findByOrderId(orderId)
}
