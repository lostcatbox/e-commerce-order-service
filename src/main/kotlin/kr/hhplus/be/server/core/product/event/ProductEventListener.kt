package kr.hhplus.be.server.core.product.event

import kr.hhplus.be.server.core.order.event.OrderCompletedEvent
import kr.hhplus.be.server.core.product.service.ProductSaleService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 제품 이벤트 구독 서비스
 *
 * 제품 도메인과 관련된 이벤트를 구독하여 제품 관련 비즈니스 로직을 처리합니다.
 * 주문 완료 이벤트를 구독하여 제품 판매량 통계를 업데이트합니다.
 */
@Component
class ProductEventListener(
    private val productSaleService: ProductSaleService,
) {
    companion object {
        private val log = LoggerFactory.getLogger(ProductEventListener::class.java)
    }

    /**
     * 주문 완료 시 제품 판매량 통계 업데이트
     *
     * 주문이 완료되었을 때 해당 주문에 포함된 모든 제품의 판매량을 통계에 반영합니다.
     *
     * @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
     * - 주문 트랜잭션이 성공적으로 커밋된 후에만 처리
     * - 통계 업데이트 실패가 주문 처리에 영향을 주지 않음
     *
     * @Async
     * - 통계 업데이트를 비동기로 수행하여 별도 트랜잭션에서 실행
     *
     * @param event 주문 완료 이벤트
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleOrderCompleted(event: OrderCompletedEvent) {
        try {
            log.info("제품 판매량 통계 업데이트 시작 - 주문 ID: {}", event.orderId)

            event.orderItems.forEach { item ->
                productSaleService.recordProductSale(item.productId, item.quantity)
                log.debug("제품 판매량 업데이트 완료 - 제품 ID: {}, 수량: {}", item.productId, item.quantity)
            }

            log.info("제품 판매량 통계 업데이트 완료 - 주문 ID: {}", event.orderId)
        } catch (e: Exception) {
            log.error("제품 판매량 통계 업데이트 중 오류 발생 - 주문 ID: {}, 오류: {}", event.orderId, e.message, e)
        }
    }
}
