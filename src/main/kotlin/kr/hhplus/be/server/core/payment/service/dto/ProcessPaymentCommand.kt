package kr.hhplus.be.server.core.payment.service.dto

import kr.hhplus.be.server.core.coupon.domain.Coupon
import kr.hhplus.be.server.core.order.domain.Order

/**
 * 결제 처리 커맨드
 */
data class ProcessPaymentCommand(
    val order: Order,
    val coupon: Coupon?,
) {
    init {
        require(order.orderId > 0) { "주문 ID는 0보다 커야 합니다." }
        require(order.calculateTotalAmount() > 0) { "주문 총 금액은 0보다 커야 합니다." }
    }
}
