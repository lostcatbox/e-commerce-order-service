package kr.hhplus.be.server.infra.persistence.payment.jpa

import kr.hhplus.be.server.core.payment.domain.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * 결제 JPA Repository 인터페이스
 */
interface JpaPaymentRepository : JpaRepository<Payment, Long> {
    /**
     * 결제 ID로 결제 조회
     * @param paymentId 결제 ID
     * @return Payment 또는 null
     */
    fun findByPaymentId(paymentId: Long): Payment?
}
