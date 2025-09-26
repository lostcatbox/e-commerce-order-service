package kr.hhplus.be.server.core.payment.service.dto

/**
 * 결제 처리 커맨드
 */
data class ProcessPaymentCommand(
    val orderId: Long,
) {
    init {
        require(orderId >= 0) { "주문 ID는 0 이상이어야 합니다." }
    }
}
