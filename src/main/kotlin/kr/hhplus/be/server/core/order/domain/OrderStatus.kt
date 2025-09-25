package kr.hhplus.be.server.core.order.domain

/**
 * 주문 상태 열거형
 */
enum class OrderStatus(
    val description: String,
) {
    REQUESTED("주문 요청됨"),
    PRODUCT_READY("상품 준비 완료"),
    PAYMENT_READY("결제 대기"),
    PAYMENT_COMPLETED("결제 완료"),
    COMPLETED("주문 완료됨"),
    FAILED("주문 실패됨"),
    ;

    /**
     * 주문 상태 변경 가능 여부 확인
     */
    fun canChangeTo(newStatus: OrderStatus): Boolean =
        when (this) {
            REQUESTED -> newStatus in listOf(PRODUCT_READY, FAILED)
            PRODUCT_READY -> newStatus in listOf(PAYMENT_READY, FAILED)
            PAYMENT_READY -> newStatus in listOf(PAYMENT_COMPLETED, FAILED)
            PAYMENT_COMPLETED -> newStatus in listOf(COMPLETED, FAILED, FAILED)
            COMPLETED -> false // 완료된 주문은 상태 변경 불가
            FAILED -> false // 실패한 주문은 상태 변경 불가
        }
}
