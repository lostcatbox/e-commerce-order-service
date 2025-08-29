package kr.hhplus.be.server.controller.order

import kr.hhplus.be.server.controller.order.dto.*
import kr.hhplus.be.server.facade.OrderCriteria
import kr.hhplus.be.server.facade.OrderFacade
import kr.hhplus.be.server.facade.OrderItemCriteria
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 주문 및 결제 유스케이스
 */
@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderFacade: OrderFacade,
) {
    /**
     * 주문 생성 및 결제 처리
     * POST /api/v1/orders
     */
    @PostMapping
    fun createOrder(
        @RequestBody request: OrderCreateRequest,
    ): ResponseEntity<OrderCreateResponse> {
        try {
            // OrderFacade를 통한 주문 처리
            val completedOrder = orderFacade.processOrder(request.toOrderCriteria())

            // 응답 DTO 생성
            val orderItemInfos =
                completedOrder.orderItems.map { orderItem ->
                    OrderItemInfo(
                        productId = orderItem.productId,
                        quantity = orderItem.quantity,
                        unitPrice = orderItem.unitPrice,
                        totalPrice = orderItem.calculateTotalPrice(),
                    )
                }

            val response =
                OrderCreateResponse(
                    orderId = completedOrder.orderId,
                    userId = completedOrder.userId,
                    orderItems = orderItemInfos,
                    paymentId = completedOrder.getPaymentId() ?: 0L,
                    orderStatus = completedOrder.getOrderStatus().name,
                    usedCouponId = completedOrder.usedCouponId,
                    createdAt = completedOrder.getCreatedAt(),
                )

            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            // TODO : 적절한 예외 처리 및 응답 반환
            throw e
        }
    }
}
