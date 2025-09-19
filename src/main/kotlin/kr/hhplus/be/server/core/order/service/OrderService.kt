package kr.hhplus.be.server.core.order.service

import kr.hhplus.be.server.core.order.domain.Order
import kr.hhplus.be.server.core.order.event.OrderCompletedEvent
import kr.hhplus.be.server.core.order.event.OrderEventPublisher
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
    private val orderEventPublisher: OrderEventPublisher,
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

        return orderRepository.save(order)
    }

    /**
     * 주문 상태를 상품 준비 완료로 변경
     */
    @Transactional
    override fun changeProductReady(orderId: Long): Order {
        val order = getOrder(orderId)
        order.prepareProducts()
        return orderRepository.save(order)
    }

    /**
     * 주문 상태를 결제 대기로 변경
     */
    @Transactional
    override fun changePaymentReady(orderId: Long): Order {
        val order = getOrder(orderId)
        order.readyForPayment()
        return orderRepository.save(order)
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
     * 주문이 완료되면 OrderCompletedEvent를 발행하여
     * 외부 통계 시스템 전송 및 판매량 통계 업데이트를 비동기적으로 처리합니다.
     */
    @Transactional
    override fun changeCompleted(orderId: Long): Order {
        val order = getOrder(orderId)
        order.complete()
        val savedOrder = orderRepository.save(order)

        // 주문 완료 이벤트 발행
        val event = OrderCompletedEvent.from(savedOrder)
        orderEventPublisher.publishOrderCompleted(event)

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
