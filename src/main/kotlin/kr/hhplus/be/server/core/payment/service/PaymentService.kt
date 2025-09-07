package kr.hhplus.be.server.core.payment.service

import kr.hhplus.be.server.core.order.domain.Order
import kr.hhplus.be.server.core.payment.domain.Payment
import kr.hhplus.be.server.core.payment.repository.PaymentRepository
import kr.hhplus.be.server.core.payment.service.dto.ProcessPaymentCommand
import kr.hhplus.be.server.core.point.service.PointServiceInterface
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 결제 서비스 구현체
 *
 * TODO : 현재 PaymentService에는 Repository와 PointService가 주입되어, Service(동등한 레이어)가 직접 사용되므로 해당 아키텍처가 적절한지 검토하다.
 */
@Service
@Transactional
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val pointService: PointServiceInterface,
) : PaymentServiceInterface {
    /**
     * 결제 처리
     */
    @Transactional
    override fun processPayment(command: ProcessPaymentCommand): Payment {
        validateOrder(command.order)

        // 결제 금액 계산
        val originalAmount = command.order.calculateTotalAmount()
        // 쿠폰 할인 금액 (쿠폰이 없으면 0)
        val discountAmount = command.coupon?.discountAmount ?: 0L

        // 결제 생성
        val payment = Payment.createPayment(originalAmount, discountAmount)

        try {
            // 포인트 결제 처리
            pointService.usePoint(command.order.userId, payment.finalAmount)

            // 결제 성공 처리
            payment.success()
            val savedPayment = paymentRepository.save(payment)

            return savedPayment
        } catch (e: Exception) {
            // 결제 실패 처리
            payment.fail()
            paymentRepository.save(payment)
            throw e
        }
    }

    /**
     * 결제 조회
     */
    @Transactional(readOnly = true)
    override fun getPayment(paymentId: Long): Payment {
        validatePaymentId(paymentId)
        return paymentRepository.findByPaymentId(paymentId)
            ?: throw IllegalArgumentException("존재하지 않는 결제입니다. 결제 ID: $paymentId")
    }

    /**
     * 주문별 결제 조회
     */
    @Transactional(readOnly = true)
    override fun getPaymentByOrderId(orderId: Long): Payment? {
        validateOrderId(orderId)
        return paymentRepository.findByOrderId(orderId)
    }

    private fun validateOrder(order: Order) {
        require(order.isReadyForPayment()) { "결제 가능한 상태의 주문이 아닙니다. 현재 상태: ${order.getOrderStatus()}" }
        require(order.calculateTotalAmount() > 0) { "결제 금액은 0보다 커야 합니다." }
    }

    private fun validatePaymentId(paymentId: Long) {
        require(paymentId > 0) { "결제 ID는 0보다 커야 합니다. 입력된 ID: $paymentId" }
    }

    private fun validateOrderId(orderId: Long) {
        require(orderId > 0) { "주문 ID는 0보다 커야 합니다. 입력된 ID: $orderId" }
    }
}
