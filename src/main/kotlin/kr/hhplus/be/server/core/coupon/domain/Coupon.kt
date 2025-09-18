package kr.hhplus.be.server.core.coupon.domain

import jakarta.persistence.*

/**
 * 선착순 쿠폰 도메인 모델
 */
@Entity
@Table(name = "coupon")
class Coupon(
    @Id
    @Column(name = "coupon_id")
    val couponId: Long,
    @Column(name = "description", nullable = false)
    val description: String,
    @Column(name = "discount_amount", nullable = false)
    val discountAmount: Long,
    @Column(name = "stock", nullable = false)
    private var stock: Int,
    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_status", nullable = false)
    private var couponStatus: CouponStatus,
) {
    companion object {
        const val MIN_STOCK = 0 // 최소 재고
        const val MAX_STOCK = 1000 // 최대 재고
        const val MIN_DISCOUNT_AMOUNT = 1L // 최소 할인 금액
    }

    init {
        require(stock >= MIN_STOCK) { "쿠폰 재고는 $MIN_STOCK 이상이어야 합니다. 현재 재고: $stock" }
        require(stock <= MAX_STOCK) { "쿠폰 재고는 $MAX_STOCK 이하여야 합니다. 현재 재고: $stock" }
        require(discountAmount >= MIN_DISCOUNT_AMOUNT) {
            "할인 금액은 $MIN_DISCOUNT_AMOUNT 이상이어야 합니다. 현재 할인 금액: $discountAmount"
        }
        require(description.isNotBlank()) { "쿠폰 설명은 비어있을 수 없습니다." }
    }

    /**
     * 현재 재고량 조회
     */
    fun getStock(): Int = stock

    /**
     * 현재 쿠폰 상태 조회
     */
    fun getCouponStatus(): CouponStatus = couponStatus

    /**
     * 쿠폰 재고가 있는지 확인
     */
    fun hasStock(): Boolean = stock > 0

    /**
     * 선착순 쿠폰 발급 (재고 차감)
     */
    fun issueCoupon() {
        require(isOpened()) { "쿠폰이 사용 가능한 상태가 아닙니다. 현재 상태: $couponStatus" }
        require(stock > 0) { "쿠폰 재고가 부족합니다. 현재 재고: $stock" }

        this.stock = stock - 1
    }

    /**
     * 할인 적용
     * @param targetAmount 할인 대상 금액
     * @return 할인된 금액
     */
    fun applyDiscount(targetAmount: Long): Long {
        require(isOpened()) { "쿠폰이 사용 가능한 상태가 아닙니다. 현재 상태: $couponStatus" }
        require(targetAmount >= 0) { "할인 대상 금액은 0 이상이어야 합니다. 입력된 금액: $targetAmount" }

        return maxOf(0L, targetAmount - discountAmount)
    }

    /**
     * 쿠폰이 사용 가능한 상태인지 확인
     * @return 사용 가능 여부
     */
    fun isOpened(): Boolean = couponStatus == CouponStatus.OPENED

    /**
     * 쿠폰 닫기
     */
    fun close() {
        this.couponStatus = CouponStatus.CLOSED
    }
}

/**
 * 쿠폰 상태 열거형
 */
enum class CouponStatus {
    OPENED, // 사용 가능
    CLOSED, // 사용 불가
}
