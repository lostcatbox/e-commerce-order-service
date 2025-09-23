package kr.hhplus.be.server.core.payment.event

import kr.hhplus.be.server.core.order.event.OrderPaymentReadyEvent
import kr.hhplus.be.server.core.order.service.OrderServiceInterface
import kr.hhplus.be.server.core.payment.service.PaymentServiceInterface
import kr.hhplus.be.server.core.payment.service.dto.ProcessPaymentCommand
import kr.hhplus.be.server.core.product.service.ProductServiceInterface
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 결제 도메인 이벤트 리스너
 */
@Component
class PaymentEventListener(
    private val paymentService: PaymentServiceInterface,
    private val orderService: OrderServiceInterface, // 직접 참조 (나중에 FeignClient로 교체 예정)
    private val productService: ProductServiceInterface,
) {
    private val log = LoggerFactory.getLogger(PaymentEventListener::class.java)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleOrderPaymentReady(event: OrderPaymentReadyEvent) {
        try {
            log.info("결제 처리 시작 - 주문 ID: {}, 최종 금액: {}", event.orderId, event.finalAmount)
            
            // 주문 정보 조회 (나중에 FeignClient로 교체 예정)
            val order = orderService.getOrder(event.orderId)
            val processPaymentCommand = ProcessPaymentCommand(
                order = order,
                coupon = null // 쿠폰 할인은 이미 event.discountAmount에 반영됨
            )
            
            // PaymentService에서 비즈니스 로직 처리 및 이벤트 발행
            val payment = paymentService.processPayment(processPaymentCommand)
            
            log.info("결제 처리 성공 - 주문 ID: {}, 결제 ID: {}", event.orderId, payment.paymentId)
        } catch (e: Exception) {
            log.error("결제 처리 실패 - 주문 ID: {}, 오류: {}", event.orderId, e.message)
            // PaymentService에서 이미 실패 이벤트 발행됨
        }
    }

    /**
     * 결제 실패 시 재고 복구 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handlePaymentFailedStockRestore(event: PaymentFailedEvent) {
        try {
            log.info("결제 실패로 인한 재고 복구 시작 - 주문 ID: {}, 결제 ID: {}", event.orderId, event.paymentId)
            
            // 별도 트랜잭션에서 재고 복구 처리
            productService.restoreStock(event.orderItems)
            
            log.info("결제 실패로 인한 재고 복구 완료 - 주문 ID: {}", event.orderId)
        } catch (e: Exception) {
            log.error("재고 복구 실패 - 주문 ID: {}, 결제 ID: {}, 오류: {}", 
                event.orderId, event.paymentId, e.message)
            // 재고 복구 실패는 데이터 정합성 이슈이므로 별도 알림/모니터링 필요
            // TODO: 재고 복구 실패 시 알림 시스템 연동 또는 재시도 로직 구현
        }
    }

}
