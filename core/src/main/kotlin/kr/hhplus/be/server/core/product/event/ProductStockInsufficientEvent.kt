package kr.hhplus.be.server.core.product.event

import kr.hhplus.be.server.event.DomainEvent

/**
 * 재고 부족 이벤트
 */
data class ProductStockInsufficientEvent(
    val orderId: Long,
    val insufficientProducts: List<ProductInsufficientData>,
    val reason: String,
) : DomainEvent()

/**
 * 재고 부족 상품 데이터
 */
data class ProductInsufficientData(
    val productId: Long,
    val requestedQuantity: Int,
    val availableStock: Int,
)
