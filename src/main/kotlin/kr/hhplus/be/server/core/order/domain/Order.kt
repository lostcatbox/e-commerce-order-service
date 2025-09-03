package kr.hhplus.be.server.core.order.domain

import jakarta.persistence.*

/**
 * 주문 Aggregate Root
 * OrderItem들의 생명주기를 완전히 관리함
 */
@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    val orderId: Long = 0L,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "order_items",
        joinColumns = [JoinColumn(name = "order_id")],
    )
    private val _orderItems: MutableList<OrderItem> = mutableListOf(),
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private var orderStatus: OrderStatus = OrderStatus.REQUESTED,
    @Column(name = "used_coupon_id")
    val usedCouponId: Long? = null,
    @Column(name = "payment_id")
    private var paymentId: Long? = null,
    @Column(name = "created_at", nullable = false)
    private val createdAt: Long = System.currentTimeMillis(),
    @Column(name = "updated_at", nullable = false)
    private var updatedAt: Long = System.currentTimeMillis(),
) {
    init {
        require(userId > 0) { "사용자 ID는 0보다 커야 합니다. 입력된 ID: $userId" }
    }

    /**
     * 현재 주문 상태 조회
     */
    fun getOrderStatus(): OrderStatus = orderStatus

    /**
     * 결제 ID 조회
     */
    fun getPaymentId(): Long? = paymentId

    /**
     * 생성 시간 조회
     */
    fun getCreatedAt(): Long = createdAt

    /**
     * OrderItem 목록 조회 (읽기 전용)
     */
    val orderItems: List<OrderItem> get() = _orderItems.toList()

    /**
     * 주문 총 금액 계산
     */
    fun calculateTotalAmount(): Long = _orderItems.sumOf { it.totalPrice }

    /**
     * 상품 준비 완료 처리
     * 주문이 요청된 후 상품을 준비하는 단계로 전환
     */
    fun prepareProducts() {
        changeStatus(OrderStatus.PRODUCT_READY)
    }

    /**
     * 결제 대기 상태로 전환
     * 상품이 준비된 후 결제를 위한 대기 상태로 전환
     */
    fun readyForPayment() {
        changeStatus(OrderStatus.PAYMENT_READY)
    }

    /**
     * 결제 성공 처리
     * 결제가 완료되었음을 기록하고 상태를 변경
     */
    fun paid(paymentId: Long) {
        require(paymentId > 0) { "결제 ID는 0보다 커야 합니다. 입력된 ID: $paymentId" }
        this.paymentId = paymentId
        changeStatus(OrderStatus.PAYMENT_COMPLETED)
    }

    /**
     * 주문 완료 처리
     * 모든 프로세스가 완료되어 주문을 최종 완료 상태로 전환
     */
    fun complete() {
        changeStatus(OrderStatus.COMPLETED)
    }

    /**
     * 주문 실패 처리
     * 어떤 단계에서든 문제가 발생했을 때 실패 상태로 전환
     */
    fun fail() {
        changeStatus(OrderStatus.FAILED)
    }

    /**
     * 주문 완료 여부 확인
     */
    fun isCompleted(): Boolean = orderStatus == OrderStatus.COMPLETED

    /**
     * 주문 실패 여부 확인
     */
    fun isFailed(): Boolean = orderStatus == OrderStatus.FAILED

    /**
     * 결제 가능 상태인지 확인
     */
    fun isReadyForPayment(): Boolean = orderStatus == OrderStatus.PAYMENT_READY

    /**
     * 주문 상품 추가 (Aggregate Root를 통한 일관성 보장)
     */
    fun addOrderItem(
        productId: Long,
        quantity: Int,
        unitPrice: Long,
    ) {
        require(productId > 0) { "상품 ID는 0보다 커야 합니다. 입력된 ID: $productId" }
        require(quantity >= OrderItem.MIN_QUANTITY) { "주문 수량은 ${OrderItem.MIN_QUANTITY} 이상이어야 합니다. 입력된 수량: $quantity" }
        require(quantity <= OrderItem.MAX_QUANTITY) { "주문 수량은 ${OrderItem.MAX_QUANTITY} 이하여야 합니다. 입력된 수량: $quantity" }
        require(unitPrice > 0) { "상품 단가는 0보다 커야 합니다. 입력된 단가: $unitPrice" }

        val orderItem = OrderItem(productId, quantity, unitPrice)
        _orderItems.add(orderItem)
        updateTimestamp()
    }

    /**
     * 주문 상품이 있는지 확인
     */
    fun isNotEmpty(): Boolean = _orderItems.isNotEmpty()

    /**
     * 업데이트 시간 갱신
     */
    private fun updateTimestamp() {
        this.updatedAt = System.currentTimeMillis()
    }

    /**
     * 상태 변경 시 업데이트 시간 갱신
     */
    private fun changeStatus(newStatus: OrderStatus) {
        require(orderStatus.canChangeTo(newStatus)) {
            "주문 상태를 ${orderStatus.description}에서 ${newStatus.description}로 변경할 수 없습니다."
        }
        this.orderStatus = newStatus
        updateTimestamp()
    }
}
