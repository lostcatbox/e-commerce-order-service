package kr.hhplus.be.server.core.product.event

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * 상품 도메인 이벤트 발행 구현체
 */
@Component
class ProductEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : ProductEventPublisherInterface {
    override fun publishProductStockReserved(
        orderId: Long,
        products: List<ProductReservationData>,
    ) {
        applicationEventPublisher.publishEvent(
            ProductStockReservedEvent(
                orderId = orderId,
                products = products,
            ),
        )
    }

    override fun publishProductStockInsufficient(
        orderId: Long,
        reason: String,
    ) {
        applicationEventPublisher.publishEvent(
            ProductStockInsufficientEvent(
                orderId = orderId,
                reason = reason,
            ),
        )
    }
}
