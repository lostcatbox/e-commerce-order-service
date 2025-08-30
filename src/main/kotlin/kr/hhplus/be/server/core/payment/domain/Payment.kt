package kr.hhplus.be.server.core.payment.domain

/**
 * 결제 도메인 모델
 */
class Payment(
    val paymentId: Long,
    val orderId: Long,
    val originalAmount: Long,
    private var discountAmount: Long = 0L,
    private var paymentStatus: PaymentStatus = PaymentStatus.REQUESTED,
    private val createdAt: Long = System.currentTimeMillis(),
) {
    init {
        require(paymentId > 0) { "결제 ID는 0보다 커야 합니다. 입력된 ID: $paymentId" }
        require(orderId > 0) { "주문 ID는 0보다 커야 합니다. 입력된 ID: $orderId" }
        require(originalAmount >= 0) { "원본 금액은 0 이상이어야 합니다. 입력된 금액: $originalAmount" }
        require(discountAmount >= 0) { "할인 금액은 0 이상이어야 합니다. 입력된 금액: $discountAmount" }
    }

    // 최종 결제 금액 조회
    val finalAmount: Long
        get() = calculateFinalAmount()

    /**
     * 현재 결제 상태 조회
     */
    fun getPaymentStatus(): PaymentStatus = paymentStatus

    /**
     * 할인 금액 조회
     */
    fun getDiscountAmount(): Long = discountAmount

    /**
     * 생성 시간 조회
     */
    fun getCreatedAt(): Long = createdAt

    /**
     * 최종 결제 금액 계산
     */
    private fun calculateFinalAmount(): Long {
        val finalAmount = originalAmount - discountAmount
        require(finalAmount >= 0) {
            "최종 결제 금액은 0 이상이어야 합니다. 원본 금액: $originalAmount, 할인 금액: $discountAmount"
        }
        return finalAmount
    }

    /**
     * 할인 적용
     * 할인 금액을 누적하여 적용
     */
    fun addDiscountAmount(discountAmount: Long) {
        require(discountAmount >= 0) { "할인 금액은 0 이상이어야 합니다. 입력된 금액: $discountAmount" }

        this.discountAmount += discountAmount

        // 최종 금액이 음수가 되지 않도록 검증
        require(calculateFinalAmount() >= 0) {
            "할인 적용 후 최종 금액이 음수가 될 수 없습니다. 원본 금액: $originalAmount, 할인 금액: $discountAmount"
        }
    }

    /**
     * 결제 상태 변경 (내부 사용)
     */
    private fun changeStatus(newStatus: PaymentStatus) {
        require(paymentStatus.canChangeTo(newStatus)) {
            "결제 상태를 ${paymentStatus.description}에서 ${newStatus.description}로 변경할 수 없습니다."
        }
        this.paymentStatus = newStatus
    }

    /**
     * 결제 성공 처리
     * 결제 프로세스가 성공적으로 완료되었을 때 호출
     */
    fun success() {
        changeStatus(PaymentStatus.SUCCESS)
    }

    /**
     * 결제 실패 처리
     * 결제 프로세스가 실패했을 때 호출
     */
    fun fail() {
        changeStatus(PaymentStatus.FAILED)
    }

    /**
     * 결제 성공 여부 확인
     */
    fun isSuccess(): Boolean = paymentStatus == PaymentStatus.SUCCESS

    /**
     * 결제 실패 여부 확인
     */
    fun isFailed(): Boolean = paymentStatus == PaymentStatus.FAILED
}
