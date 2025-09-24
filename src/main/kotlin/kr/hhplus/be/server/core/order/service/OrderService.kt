package kr.hhplus.be.server.core.order.service

import kr.hhplus.be.server.core.order.domain.Order
import kr.hhplus.be.server.core.order.event.OrderCompletedEvent
import kr.hhplus.be.server.core.order.event.OrderEventPublisherInterface
import kr.hhplus.be.server.core.order.repository.OrderRepository
import kr.hhplus.be.server.core.order.service.dto.*
import kr.hhplus.be.server.core.product.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 주문 서비스 구현체
 */
@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val orderEventPublisher: OrderEventPublisherInterface
) : OrderServiceInterface {
    /**
     * 주문 생성 (Aggregate Root 패턴)
     */
    @Transactional
    override fun createOrder(command: CreateOrderCommand): Order {
        // 주문 생성 (Aggregate Root)
        val order =
            Order(
                userId = command.userId,
                usedCouponId = command.usedCouponId,
            )

        // 상품 정보 조회 및 주문 상품 추가 (Order를 통해서만 추가)
        command.orderItems.forEach { orderItemCommand ->
            val product =
                productRepository.findByProductId(orderItemCommand.productId)
                    ?: throw IllegalArgumentException("존재하지 않는 상품입니다. 상품 ID: ${orderItemCommand.productId}")

            // Order Aggregate Root를 통해 OrderItem 추가
            order.addOrderItem(
                productId = orderItemCommand.productId,
                quantity = orderItemCommand.quantity,
                unitPrice = product.price,
            )
        }

        // 주문이 비어있는지 검증
        order.validNotEmptyOrderItems()

        val savedOrder = orderRepository.save(order)
        
        // OrderCreatedEvent 발행하여 Event-Driven 처리 시작
        val orderItemEventData = savedOrder.orderItems.map { orderItem ->
            kr.hhplus.be.server.core.order.event.OrderItemEventData(
                productId = orderItem.productId,
                quantity = orderItem.quantity,
                unitPrice = orderItem.unitPrice
            )
        }
        
        orderEventPublisher.publishOrderCreated(
            orderId = savedOrder.orderId,
            userId = savedOrder.userId,
            orderItems = orderItemEventData,
            usedCouponId = savedOrder.usedCouponId
        )

        return savedOrder
    }

    /**
     * 주문 상태를 상품 준비 완료로 변경
     */
    @Transactional
    override fun changeProductReady(orderId: Long): Order {
        val order = getOrder(orderId)
        order.prepareProducts()
        val savedOrder = orderRepository.save(order)
        
        // OrderProductReadyEvent 발행
        val orderItemEventData = savedOrder.orderItems.map { orderItem ->
            kr.hhplus.be.server.core.order.event.OrderItemEventData(
                productId = orderItem.productId,
                quantity = orderItem.quantity,
                unitPrice = orderItem.unitPrice
            )
        }
        
        orderEventPublisher.publishOrderProductReady(
            orderId = savedOrder.orderId,
            userId = savedOrder.userId,
            orderItems = orderItemEventData
        )
        
        return savedOrder
    }

    /**
     * 주문 상태를 결제 대기로 변경
     */
    @Transactional
    override fun changePaymentReady(orderId: Long): Order {
        val order = getOrder(orderId)
        order.readyForPayment()
        val savedOrder = orderRepository.save(order)
        
        // OrderPaymentReadyEvent 발행
        val totalAmount = savedOrder.calculateTotalAmount()
        val discountAmount = 0L // TODO: 실제 할인 금액은 쿠폰 처리에서 계산 필요
        val finalAmount = totalAmount - discountAmount
        
        orderEventPublisher.publishOrderPaymentReady(
            orderId = savedOrder.orderId,
            userId = savedOrder.userId,
            totalAmount = totalAmount,
            discountAmount = discountAmount,
            finalAmount = finalAmount
        )
        
        return savedOrder
    }

    /**
     * 주문 상태를 결제 대기로 변경 (할인 금액 포함)
     */
    @Transactional
    override fun changePaymentReady(orderId: Long, discountAmount: Long): Order {
        val order = getOrder(orderId)
        order.readyForPayment()
        val savedOrder = orderRepository.save(order)
        
        // OrderPaymentReadyEvent 발행 (할인 금액 포함)
        val totalAmount = savedOrder.calculateTotalAmount()
        val finalAmount = totalAmount - discountAmount
        
        orderEventPublisher.publishOrderPaymentReady(
            orderId = savedOrder.orderId,
            userId = savedOrder.userId,
            totalAmount = totalAmount,
            discountAmount = discountAmount,
            finalAmount = finalAmount
        )
        
        return savedOrder
    }

    /**
     * 주문 상태를 결제 완료로 변경
     */
    @Transactional
    override fun changePaymentComplete(
        orderId: Long,
        paymentId: Long,
    ): Order {
        val order = getOrder(orderId)
        order.paid(paymentId)
        return orderRepository.save(order)
    }

    /**
     * 주문 상태를 완료로 변경
     *
     * 주문이 완료되면 OrderCompletedEvent와 함께 
     * 10번, 11번 기능을 위한 별도 통계 이벤트들을 발행합니다.
     */
    @Transactional
    override fun changeCompleted(orderId: Long): Order {
        val order = getOrder(orderId)
        order.complete()
        val savedOrder = orderRepository.save(order)

        // 주문 완료 이벤트 발행 (통계 처리는 이 이벤트를 통해 자동으로 처리됨)
        val orderCompletedEvent = OrderCompletedEvent.from(savedOrder)
        orderEventPublisher.publishOrderCompleted(orderCompletedEvent)

        return savedOrder
    }

    /**
     * 주문 상태를 실패로 변경
     */
    @Transactional
    override fun changeFailed(orderId: Long): Order {
        val order = getOrder(orderId)
        order.fail()
        return orderRepository.save(order)
    }

    /**
     * 주문 상태를 실패로 변경 (실패 이벤트 발행 포함)
     */
    @Transactional
    override fun changeFailed(orderId: Long, reason: String, failedStep: String): Order {
        val order = getOrder(orderId)
        order.fail()
        val savedOrder = orderRepository.save(order)

        // OrderFailedEvent 발행
        orderEventPublisher.publishOrderFailed(
            orderId = savedOrder.orderId,
            failureReason = reason,
            failedStep = failedStep
        )

        return savedOrder
    }

    /**
     * 주문 조회
     */
    @Transactional(readOnly = true)
    override fun getOrder(orderId: Long): Order {
        validateOrderId(orderId)
        return orderRepository.findByOrderId(orderId)
            ?: throw IllegalArgumentException("존재하지 않는 주문입니다. 주문 ID: $orderId")
    }

    /**
     * 사용자별 주문 목록 조회
     */
    @Transactional(readOnly = true)
    override fun getUserOrders(userId: Long): List<Order> {
        validateUserId(userId)
        return orderRepository.findByUserId(userId)
    }

    private fun validateOrderId(orderId: Long) {
        require(orderId > 0) { "주문 ID는 0보다 커야 합니다. 입력된 ID: $orderId" }
    }

    private fun validateUserId(userId: Long) {
        require(userId > 0) { "사용자 ID는 0보다 커야 합니다. 입력된 ID: $userId" }
    }
}
