package kr.hhplus.be.server.core.product.event

import kr.hhplus.be.server.core.common.event.DomainEvent

/**
 * 재고 확보 성공 이벤트
 */
data class ProductStockReservedEvent(
    val orderId: Long,
    val products: List<ProductReservationData>,
) : DomainEvent()

/**
 * 상품 예약 데이터
 */
data class ProductReservationData(
    val productId: Long,
    val quantity: Int,
    val reservedStock: Int,
)
