package kr.hhplus.be.server.core.product.event

import kr.hhplus.be.server.core.order.event.OrderCompletedEvent
import kr.hhplus.be.server.core.product.service.ProductSaleServiceInterface
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 판매량 통계 업데이트 이벤트 리스너 (11번 기능)
 *
 * OrderCompletedEvent를 처리하여 상품별 판매량 통계를 업데이트합니다.
 */
@Component
class ProductSaleStatisticsEventListener(
    private val productSaleService: ProductSaleServiceInterface,
) {
    private val log = LoggerFactory.getLogger(ProductSaleStatisticsEventListener::class.java)

    /**
     * 주문 완료 시 판매량 통계 업데이트 처리 (11번 기능)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleOrderCompleted(event: OrderCompletedEvent) {
        try {
            log.info("판매량 통계 업데이트 시작 - 주문 ID: {}", event.orderId)

            // 각 상품별 판매량 기록
            event.orderItems.forEach { orderItem ->
                productSaleService.recordProductSale(
                    productId = orderItem.productId,
                    quantity = orderItem.quantity,
                )
                log.debug("상품 판매량 기록 완료 - 상품 ID: {}, 수량: {}", orderItem.productId, orderItem.quantity)
            }

            log.info("판매량 통계 업데이트 완료 - 주문 ID: {}", event.orderId)
        } catch (e: Exception) {
            log.error("판매량 통계 업데이트 실패 - 주문 ID: {}, 오류: {}", event.orderId, e.message, e)
        }
    }
}
