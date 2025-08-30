package kr.hhplus.be.server.controller.order

import kr.hhplus.be.server.controller.order.dto.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 주문 및 결제 유스케이스
 */
@RestController
@RequestMapping("/api/v1/orders")
class OrderController {
    /**
     * 주문 생성 및 결제 처리
     * POST /api/v1/orders
     */
    @PostMapping
    fun createOrder(
        @RequestBody request: OrderCreateRequest,
    ): ResponseEntity<OrderCreateResponse> {
        // TODO: 주문 생성 및 결제 서비스 호출
        val mockOrderItems =
            request.orderItems.map { item ->
                OrderItemInfo(
                    productId = item.productId,
                    productName = "상품 ${item.productId}",
                    quantity = item.quantity,
                    unitPrice = 10000L,
                    totalPrice = 10000L * item.quantity,
                )
            }

        val totalAmount = mockOrderItems.sumOf { it.totalPrice }
        val discountAmount = if (request.couponId != null) 5000L else 0L

        val mockResponse =
            OrderCreateResponse(
                orderId = System.currentTimeMillis(),
                userId = request.userId,
                orderItems = mockOrderItems,
                originalAmount = totalAmount,
                discountAmount = discountAmount,
                finalAmount = totalAmount - discountAmount,
                paymentId = System.currentTimeMillis() + 1,
                orderStatus = "SUCCESS",
                paymentStatus = "SUCCESS",
                usedCouponId = request.couponId,
                createdAt = System.currentTimeMillis(),
            )
        return ResponseEntity.ok(mockResponse)
    }
}
