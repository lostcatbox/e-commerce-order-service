package kr.hhplus.be.server.core.payment.service

import kr.hhplus.be.server.core.coupon.service.CouponServiceInterface
import kr.hhplus.be.server.core.order.domain.Order
import kr.hhplus.be.server.core.order.service.dto.OrderItemCommand
import kr.hhplus.be.server.core.payment.domain.Payment
import kr.hhplus.be.server.core.payment.event.PaymentEventPublisherInterface
import kr.hhplus.be.server.core.payment.repository.PaymentRepository
import kr.hhplus.be.server.core.payment.service.dto.ProcessPaymentCommand
import kr.hhplus.be.server.core.point.service.PointServiceInterface
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 결제 서비스 구현체
 *
 * 정합성 매우 중요한 영역
 * 결제 성공/실패 이벤트 발행
 * 외부 서비스 호출 (포인트, 쿠폰)
 */
@Service
@Transactional
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val pointService: PointServiceInterface,
    private val couponService: CouponServiceInterface,
    private val paymentEventPublisher: PaymentEventPublisherInterface,
) : PaymentServiceInterface {
    /**
     * 결제 처리
     */
    @Transactional
    override fun processPayment(command: ProcessPaymentCommand): Payment {
        validateOrder(command.order)

        // 결제 금액 계산
        val originalAmount = command.order.calculateTotalAmount()

        try {
            // 1. 쿠폰 처리 (있을 경우)
            var discountAmount = 0L
            if (command.order.usedCouponId != null) {
                val usedCoupon = couponService.useCoupon(command.order.usedCouponId)
                val couponInfo = couponService.getCouponInfo(usedCoupon.couponId)
                discountAmount = couponInfo.discountAmount
            } else if (command.coupon != null) {
                // 테스트를 위한 직접적인 쿠폰 처리 (실제 운영에서는 order.usedCouponId 사용)
                discountAmount = command.coupon.discountAmount
            }

            // 2. 결제 생성
            val payment = Payment.createPayment(originalAmount, discountAmount)

            // 3. 포인트 결제 처리
            pointService.usePoint(command.order.userId, payment.finalAmount)

            // 4. 결제 성공 처리
            payment.success()
            val savedPayment = paymentRepository.save(payment)

            // 5. 결제 성공 이벤트 발행
            paymentEventPublisher.publishPaymentSucceeded(
                orderId = command.order.orderId,
                paymentId = savedPayment.paymentId,
                finalAmount = savedPayment.finalAmount,
            )

            return savedPayment
        } catch (e: Exception) {
            // 결제 실패 처리
            val payment = Payment.createPayment(originalAmount, 0L)
            payment.fail()
            val savedPayment = paymentRepository.save(payment)

            // 결제 실패 이벤트 발행 (재고 복구는 이벤트 리스너에서 비동기 처리)
            paymentEventPublisher.publishPaymentFailed(
                orderId = command.order.orderId,
                paymentId = savedPayment.paymentId,
                failureReason = e.message ?: "Payment failed",
                orderItems = command.order.orderItems.map {
                    OrderItemCommand(it.productId, it.quantity)
                },
            )

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
