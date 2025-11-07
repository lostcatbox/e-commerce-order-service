package kr.hhplus.be.server.core.product.event

import kr.hhplus.be.server.event.DomainEvent

/**
 * 재고 부족 이벤트
 */
data class ProductStockInsufficientEvent(
    val orderId: Long,
    val reason: String,
) : DomainEvent()
