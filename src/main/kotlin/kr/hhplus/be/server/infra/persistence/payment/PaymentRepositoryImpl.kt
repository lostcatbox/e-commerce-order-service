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

    override fun findByOrderId(orderId: Long): Payment? {
        // Note: 현재 Payment 모델에 orderId가 없어서 임시로 구현
        // 실제로는 Order와의 관계를 통해 조회해야 함
        TODO("Order와 Payment 관계 정의 후 구현 필요")
    }
}
