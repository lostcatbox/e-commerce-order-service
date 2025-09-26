package kr.hhplus.be.server.core.product.event

/**
 * 상품 도메인 이벤트 발행 인터페이스
 */
interface ProductEventPublisherInterface {
    /**
     * 상품 재고 확보 성공 이벤트 발행
     */
    fun publishProductStockReserved(
        orderId: Long,
        products: List<ProductReservationData>
    )

    /**
     * 상품 재고 부족 이벤트 발행
     */
    fun publishProductStockInsufficient(
        orderId: Long,
        insufficientProducts: List<ProductInsufficientData>,
        reason: String
    )
}
