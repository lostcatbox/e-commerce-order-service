package kr.hhplus.be.server.controller.order

import kr.hhplus.be.server.controller.order.dto.*
import kr.hhplus.be.server.core.order.service.OrderServiceInterface
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 주문 및 결제 유스케이스 (Event-Driven 방식)
 * 
 * OrderFacade를 제거하고 OrderService만 사용하여 주문을 시작
 * 이후 과정은 Event-Driven 방식으로 자동 처리됨
 */
@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderServiceInterface,
) {
    /**
     * 주문 시작 (Event-Driven 방식)
     * POST /api/v1/orders
     * 
     * 주문 생성만 수행하고 OrderCreatedEvent를 발행
     * 이후 사용자 검증, 재고 처리, 쿠폰 처리, 결제 등은 이벤트를 통해 자동 처리됨
     */
    @PostMapping
    fun createOrder(
        @RequestBody request: OrderCreateRequest,
    ): ResponseEntity<OrderCreateResponse> {
        try {
            // Event-Driven: 주문 생성만 수행
            val createdOrder = orderService.createOrder(request.toCreateOrderCommand())

            // 주문 생성 직후 응답 (아직 처리 중인 상태)
            val orderItemInfos =
                createdOrder.orderItems.map { orderItem ->
                    OrderItemInfo(
                        productId = orderItem.productId,
                        quantity = orderItem.quantity,
                        unitPrice = orderItem.unitPrice,
                        totalPrice = orderItem.calculateTotalPrice(),
                    )
                }

            val response =
                OrderCreateResponse(
                    orderId = createdOrder.orderId,
                    userId = createdOrder.userId,
                    orderItems = orderItemInfos,
                    paymentId = null, // Event-Driven 처리 중이므로 아직 없음
                    orderStatus = createdOrder.getOrderStatus().name,
                    usedCouponId = createdOrder.usedCouponId,
                    createdAt = createdOrder.getCreatedAt(),
                    message = "주문이 생성되었습니다. 결제 처리가 진행 중입니다."
                )

            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            throw e
        }
    }
}
