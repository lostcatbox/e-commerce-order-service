package kr.hhplus.be.server.core.payment.domain

/**
 * 결제 상태 열거형
 */
enum class PaymentStatus(
    val description: String,
) {
    REQUESTED("결제 요청됨"),
    SUCCESS("결제 성공됨"),
    FAILED("결제 실패됨"),
    ;

    /**
     * 결제 상태 변경 가능 여부 확인
     */
    fun canChangeTo(newStatus: PaymentStatus): Boolean =
        when (this) {
            REQUESTED -> newStatus in listOf(SUCCESS, FAILED)
            SUCCESS -> false // 성공한 결제는 상태 변경 불가
            FAILED -> false // 실패한 결제는 상태 변경 불가
        }
}
