package kr.hhplus.be.server.fake.event

import kr.hhplus.be.server.core.product.event.ProductEventPublisherInterface
import kr.hhplus.be.server.core.product.event.ProductInsufficientData
import kr.hhplus.be.server.core.product.event.ProductReservationData

/**
 * 테스트용 가짜 상품 이벤트 발행자
 * 실제 이벤트 발행 없이 테스트 통과만을 위한 구현체
 */
class FakeProductEventPublisher : ProductEventPublisherInterface {
    
    override fun publishProductStockReserved(
        orderId: Long,
        products: List<ProductReservationData>
    ) {
        // 테스트용 - 실제 이벤트 발행 없음
    }

    override fun publishProductStockInsufficient(
        orderId: Long,
        insufficientProducts: List<ProductInsufficientData>,
        reason: String
    ) {
        // 테스트용 - 실제 이벤트 발행 없음
    }
}
