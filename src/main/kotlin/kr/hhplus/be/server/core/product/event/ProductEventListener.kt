package kr.hhplus.be.server.core.product.event

import kr.hhplus.be.server.core.order.event.OrderCompletedEvent
import kr.hhplus.be.server.core.order.event.OrderProductReadyEvent
import kr.hhplus.be.server.core.order.service.dto.OrderItemCommand
import kr.hhplus.be.server.core.product.service.ProductService
import kr.hhplus.be.server.core.product.service.dto.SaleProductsCommand
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 상품 도메인 이벤트 리스너
 */
@Component
class ProductEventListener(
    private val productService: ProductService,
) {
    private val log = LoggerFactory.getLogger(ProductEventListener::class.java)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleOrderProductReady(event: OrderProductReadyEvent) {
        try {
            log.info("재고 처리 시작 - 주문 ID: {}", event.orderId)

            // 이벤트에서 직접 상품 정보 사용 (Cross-domain 호출 최소화)
            val saleCommand =
                SaleProductsCommand(
                    orderItems =
                        event.orderItems.map { orderItem ->
                            OrderItemCommand(orderItem.productId, orderItem.quantity)
                        },
                )

            // ProductService에서 비즈니스 로직 처리 및 이벤트 발행
            productService.processOrderProductStock(event.orderId, saleCommand)

            log.info("재고 처리 성공 - 주문 ID: {}", event.orderId)
        } catch (e: Exception) {
            log.error("재고 처리 실패 - 주문 ID: {}, 오류: {}", event.orderId, e.message)
        }
    }
}
