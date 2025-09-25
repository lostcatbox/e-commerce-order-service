package kr.hhplus.be.server.core.order.event

import kr.hhplus.be.server.core.order.service.OrderServiceInterface
import kr.hhplus.be.server.core.order.service.OrderStatisticsServiceInterface
import kr.hhplus.be.server.core.payment.event.PaymentFailedEvent
import kr.hhplus.be.server.core.payment.event.PaymentSucceededEvent
import kr.hhplus.be.server.core.product.event.ProductStockInsufficientEvent
import kr.hhplus.be.server.core.product.event.ProductStockReservedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 주문 도메인 Event-Driven 리스너
 *
 * 다른 도메인에서 발생한 이벤트를 수신하여 주문 상태를 변경하는 역할
 */
@Component
class OrderEventListener(
    private val orderService: OrderServiceInterface,
    private val orderStatisticsService: OrderStatisticsServiceInterface,
) {
    private val log = LoggerFactory.getLogger(OrderEventListener::class.java)

    /**
     * 결제 성공 시 주문 완료 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentSucceeded(event: PaymentSucceededEvent) {
        try {
            log.info("주문 완료 처리 시작 - 주문 ID: {}, 결제 ID: {}", event.orderId, event.paymentId)

            // 결제 성공 상태로 변경
            orderService.changePaymentComplete(event.orderId, event.paymentId)

            // 주문 완료 상태로 변경 (이벤트 발행을 통해 후속 처리)
            orderService.changeCompleted(event.orderId)

            log.info("주문 완료 처리 완료 - 주문 ID: {}", event.orderId)
        } catch (e: Exception) {
            log.error("주문 완료 처리 실패 - 주문 ID: {}, 오류: {}", event.orderId, e.message)
            orderService.changeFailed(event.orderId, e.message ?: "Order completion failed", "ORDER_COMPLETION")
        }
    }

    /**
     * 재고 확보 성공 시 주문 상태 상품 준비 완료로 변경
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleProductStockReserved(event: ProductStockReservedEvent) {
        try {
            log.info("주문 상품 준비 완료로 변경 시작 - 주문 ID: {}", event.orderId)
            // 재고 확보 성공 후
            orderService.changeProductReserved(event.orderId)
            log.info("주문 상품 준비 완료로 변경 완료 - 주문 ID: {}", event.orderId)
        } catch (e: Exception) {
            log.error("주문 상품 준비 완료로 변경 실패 - 주문 ID: {}, 오류: {}", event.orderId, e.message, e)
            try {
                orderService.changeFailed(event.orderId, e.message ?: "Payment ready failed", "PAYMENT_READY")
            } catch (ex: Exception) {
                log.error("주문 상품 준비 완료로 변경 중 오류 발생 - 주문 ID: {}, 오류: {}", event.orderId, ex.message, ex)
            }
        }
    }

    /**
     * 주문 상태가 상품 준비 완료로 변경 시 주문 상태를 결제 대기로 변경
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleProductStockReserved(event: OrderProductReservedEvent) {
        try {
            log.info("주문 결제 대기 시작 - 주문 ID: {}", event.orderId)
            // 재고 확보 성공 후 바로 결제 대기 상태로 변경
            orderService.changePaymentReady(event.orderId)
            log.info("주문 결제 대기 완료 - 주문 ID: {}", event.orderId)
        } catch (e: Exception) {
            log.error("주문 결제 대기 실패 - 주문 ID: {}, 오류: {}", event.orderId, e.message, e)
            try {
                orderService.changeFailed(event.orderId, e.message ?: "Payment ready failed", "PAYMENT_READY")
            } catch (ex: Exception) {
                log.error("주문 실패 처리 중 오류 발생 - 주문 ID: {}, 오류: {}", event.orderId, ex.message, ex)
            }
        }
    }

    /**
     * 재고 부족 시 주문 실패 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleProductStockInsufficient(event: ProductStockInsufficientEvent) {
        orderService.changeFailed(event.orderId, event.reason, "PRODUCT_STOCK")
    }

    /**
     * 결제 실패 시 주문 실패 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentFailed(event: PaymentFailedEvent) {
        orderService.changeFailed(event.orderId, event.failureReason, "PAYMENT")
    }
}
