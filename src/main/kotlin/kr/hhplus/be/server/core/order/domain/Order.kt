package kr.hhplus.be.server.core.order.domain

/**
 * 주문 도메인 모델
 */
class Order(
    val orderId: Long,
    val userId: Long,
    val orderItems: List<OrderItem>,
    private var orderStatus: OrderStatus = OrderStatus.REQUESTED,
    val usedCouponId: Long? = null,
    private var paymentId: Long? = null,
    private val createdAt: Long = System.currentTimeMillis(),
) {
    init {
        require(orderId > 0) { "주문 ID는 0보다 커야 합니다. 입력된 ID: $orderId" }
        require(userId > 0) { "사용자 ID는 0보다 커야 합니다. 입력된 ID: $userId" }
        require(orderItems.isNotEmpty()) { "주문 상품은 1개 이상이어야 합니다." }
        require(orderItems.all { it.quantity >= 1 }) { "모든 주문 상품의 수량은 1 이상이어야 합니다." }
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
     * 주문 총 금액 계산
     */
    fun calculateTotalAmount(): Long = orderItems.sumOf { it.totalPrice }

    /**
     * 주문 상태 변경
     */
    private fun changeStatus(newStatus: OrderStatus) {
        require(orderStatus.canChangeTo(newStatus)) {
            "주문 상태를 ${orderStatus.description}에서 ${newStatus.description}로 변경할 수 없습니다."
        }
        this.orderStatus = newStatus
    }

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
}
