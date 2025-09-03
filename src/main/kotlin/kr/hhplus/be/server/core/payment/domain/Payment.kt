package kr.hhplus.be.server.core.payment.domain

import jakarta.persistence.*

/**
 * 결제 도메인 모델
 */
@Entity
@Table(name = "payment")
class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    val paymentId: Long,
    @Column(name = "original_amount", nullable = false)
    val originalAmount: Long,
    @Column(name = "discount_amount", nullable = false)
    val discountAmount: Long,
    @Column(name = "final_amount", nullable = false)
    val finalAmount: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private var paymentStatus: PaymentStatus = PaymentStatus.REQUESTED,
    @Column(name = "created_at", nullable = false)
    private val createdAt: Long = System.currentTimeMillis(),
    @Column(name = "updated_at", nullable = false)
    private var updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        /**
         * 결제 생성 팩토리 메서드
         */
        fun createPayment(
            originalAmount: Long,
            discountAmount: Long = 0L,
        ): Payment {
            require(originalAmount >= 0) { "원본 금액은 0 이상이어야 합니다. 입력된 금액: $originalAmount" }
            require(discountAmount >= 0) { "할인 금액은 0 이상이어야 합니다. 입력된 금액: $discountAmount" }
            require(originalAmount >= discountAmount) {
                "할인 금액은 원본 금액을 초과할 수 없습니다. 원본 금액: $originalAmount, 할인 금액: $discountAmount"
            }

            return Payment(
                paymentId = 0L,
                originalAmount = originalAmount,
                discountAmount = discountAmount,
                finalAmount = originalAmount - discountAmount,
            )
        }
    }

    init {
        require(originalAmount >= 0) { "원본 금액은 0 이상이어야 합니다. 입력된 금액: $originalAmount" }
        require(discountAmount >= 0) { "할인 금액은 0 이상이어야 합니다. 입력된 금액: $discountAmount" }
        require(finalAmount >= 0) { "최종 금액은 0 이상이어야 합니다. 입력된 금액: $finalAmount" }
        require(originalAmount >= discountAmount) {
            "할인 금액은 원본 금액을 초과할 수 없습니다. 원본 금액: $originalAmount, 할인 금액: $discountAmount"
        }
        require(originalAmount >= finalAmount) {
            "최종 금액은 원본 금액을 초과할 수 없습니다. 원본 금액: $originalAmount, 최종 금액: $finalAmount"
        }
        require(finalAmount == originalAmount - discountAmount) {
            "최종 금액이 올바르지 않습니다. 예상: ${originalAmount - discountAmount}, 실제: $finalAmount"
        }
    }

    /**
     * 현재 결제 상태 조회
     */
    fun getPaymentStatus(): PaymentStatus = paymentStatus

    /**
     * 생성 시간 조회
     */
    fun getCreatedAt(): Long = createdAt

    /**
     * 결제 상태 변경 (내부 사용)
     */
    private fun changeStatus(newStatus: PaymentStatus) {
        require(paymentStatus.canChangeTo(newStatus)) {
            "결제 상태를 ${paymentStatus.description}에서 ${newStatus.description}로 변경할 수 없습니다."
        }
        this.paymentStatus = newStatus
        this.updatedAt = System.currentTimeMillis()
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
