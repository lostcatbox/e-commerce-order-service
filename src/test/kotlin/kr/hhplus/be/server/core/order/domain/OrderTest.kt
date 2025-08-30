package kr.hhplus.be.server.core.order.domain

import kr.hhplus.be.server.core.order.domain.Order
import kr.hhplus.be.server.core.order.domain.OrderItem
import kr.hhplus.be.server.core.order.domain.OrderItem.Companion.MAX_QUANTITY
import kr.hhplus.be.server.core.order.domain.OrderItem.Companion.MIN_QUANTITY
import kr.hhplus.be.server.core.order.domain.OrderStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Order 도메인 모델 테스트")
class OrderTest {
    @Test
    @DisplayName("정상적인 Order 생성")
    fun `정상적인 Order 생성`() {
        // given
        val orderId = 1L
        val userId = 1L
        val orderItems =
            listOf(
                OrderItem(productId = 1L, quantity = 2, unitPrice = 10000L),
                OrderItem(productId = 2L, quantity = 1, unitPrice = 20000L),
            )

        // when
        val order = Order(orderId, userId, orderItems)

        // then
        assertEquals(orderId, order.orderId)
        assertEquals(userId, order.userId)
        assertEquals(orderItems, order.orderItems)
        assertEquals(OrderStatus.REQUESTED, order.getOrderStatus())
        assertNull(order.usedCouponId)
        assertNull(order.getPaymentId())
        assertTrue(order.getCreatedAt() > 0)
    }

    @Test
    @DisplayName("쿠폰 사용 주문 생성")
    fun `쿠폰 사용 주문 생성`() {
        // given
        val orderId = 1L
        val userId = 1L
        val orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L))
        val usedCouponId = 100L

        // when
        val order = Order(orderId, userId, orderItems, usedCouponId = usedCouponId)

        // then
        assertEquals(usedCouponId, order.usedCouponId)
    }

    @Test
    @DisplayName("주문 ID가 0보다 작거나 같으면 예외 발생")
    fun `주문 ID가 0보다 작거나 같으면 예외 발생`() {
        // given
        val invalidOrderId = 0L
        val userId = 1L
        val orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L))

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                Order(invalidOrderId, userId, orderItems)
            }
        assertTrue(exception.message!!.contains("주문 ID는 0보다 커야 합니다"))
    }

    @Test
    @DisplayName("사용자 ID가 0보다 작거나 같으면 예외 발생")
    fun `사용자 ID가 0보다 작거나 같으면 예외 발생`() {
        // given
        val orderId = 1L
        val invalidUserId = 0L
        val orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L))

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                Order(orderId, invalidUserId, orderItems)
            }
        assertTrue(exception.message!!.contains("사용자 ID는 0보다 커야 합니다"))
    }

    @Test
    @DisplayName("주문 상품이 비어있으면 예외 발생")
    fun `주문 상품이 비어있으면 예외 발생`() {
        // given
        val orderId = 1L
        val userId = 1L
        val emptyOrderItems = emptyList<OrderItem>()

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                Order(orderId, userId, emptyOrderItems)
            }
        assertTrue(exception.message!!.contains("주문 상품은 1개 이상이어야 합니다"))
    }

    @Test
    @DisplayName("주문 상품 중 수량이 1보다 작은 것이 있으면 예외 발생")
    fun `주문 상품 중 수량이 1보다 작은 것이 있으면 예외 발생`() {
        // given - OrderItem 생성 시점에서 예외 발생
        val orderId = 1L
        val userId = 1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                val invalidOrderItem = OrderItem(productId = 2L, quantity = 0, unitPrice = 20000L) // 잘못된 수량
                Order(orderId, userId, listOf(invalidOrderItem))
            }
        assertTrue(exception.message!!.contains("주문 수량은 1 이상이어야 합니다"))
    }

    @Test
    @DisplayName("주문 총 금액 계산")
    fun `주문 총 금액 계산`() {
        // given
        val order =
            Order(
                orderId = 1L,
                userId = 1L,
                orderItems =
                    listOf(
                        OrderItem(productId = 1L, quantity = 2, unitPrice = 10000L), // 20,000원
                        OrderItem(productId = 2L, quantity = 1, unitPrice = 30000L), // 30,000원
                    ),
            )

        // when
        val totalAmount = order.calculateTotalAmount()

        // then
        assertEquals(50000L, totalAmount)
    }

    @Test
    @DisplayName("상품 준비 완료 상태로 변경")
    fun `상품 준비 완료 상태로 변경`() {
        // given
        val order =
            Order(
                orderId = 1L,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
            )

        // when
        order.prepareProducts()

        // then
        assertEquals(OrderStatus.PRODUCT_READY, order.getOrderStatus())
    }

    @Test
    @DisplayName("결제 대기 상태로 변경")
    fun `결제 대기 상태로 변경`() {
        // given
        val order =
            Order(
                orderId = 1L,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
            )
        order.prepareProducts() // PRODUCT_READY 상태로 먼저 변경

        // when
        order.readyForPayment()

        // then
        assertEquals(OrderStatus.PAYMENT_READY, order.getOrderStatus())
    }

    @Test
    @DisplayName("결제 완료 처리")
    fun `결제 완료 처리`() {
        // given
        val order =
            Order(
                orderId = 1L,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
            )
        order.prepareProducts()
        order.readyForPayment() // PAYMENT_READY 상태로 변경
        val paymentId = 100L

        // when
        order.paid(paymentId)

        // then
        assertEquals(OrderStatus.PAYMENT_COMPLETED, order.getOrderStatus())
        assertEquals(paymentId, order.getPaymentId())
    }

    @Test
    @DisplayName("주문 완료 처리")
    fun `주문 완료 처리`() {
        // given
        val order =
            Order(
                orderId = 1L,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
            )
        order.prepareProducts()
        order.readyForPayment()
        order.paid(100L) // PAYMENT_COMPLETED 상태로 변경

        // when
        order.complete()

        // then
        assertEquals(OrderStatus.COMPLETED, order.getOrderStatus())
        assertTrue(order.isCompleted())
    }

    @Test
    @DisplayName("주문 실패 처리")
    fun `주문 실패 처리`() {
        // given
        val order =
            Order(
                orderId = 1L,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
            )

        // when
        order.fail()

        // then
        assertEquals(OrderStatus.FAILED, order.getOrderStatus())
        assertTrue(order.isFailed())
    }

    @Test
    @DisplayName("결제 가능 상태 확인 - REQUESTED 상태는 결제 불가")
    fun `결제 가능 상태 확인 - REQUESTED 상태는 결제 불가`() {
        // given
        val order =
            Order(
                orderId = 1L,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
            )

        // when & then
        assertFalse(order.isReadyForPayment())
    }

    @Test
    @DisplayName("잘못된 상태 변경 시 예외 발생 - REQUESTED에서 PAYMENT_READY로 직접 변경")
    fun `잘못된 상태 변경 시 예외 발생 - REQUESTED에서 PAYMENT_READY로 직접 변경`() {
        // given
        val order =
            Order(
                orderId = 1L,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
            )

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                order.readyForPayment()
            }
        assertTrue(exception.message!!.contains("주문 상태를"))
        assertTrue(exception.message!!.contains("변경할 수 없습니다"))
    }

    @Test
    @DisplayName("완료된 주문의 상태 변경 시 예외 발생 - COMPLETED에서 다른 상태로 변경 시도")
    fun `완료된 주문의 상태 변경 시 예외 발생`() {
        // given
        val order =
            Order(
                orderId = 1L,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
            )
        order.prepareProducts()
        order.readyForPayment()
        order.paid(100L)
        order.complete()

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                order.fail()
            }
        assertTrue(exception.message!!.contains("주문 상태를"))
        assertTrue(exception.message!!.contains("변경할 수 없습니다"))
    }

    @Test
    @DisplayName("결제 ID가 0보다 작거나 같으면 예외 발생")
    fun `결제 ID가 0보다 작거나 같으면 예외 발생`() {
        // given
        val order =
            Order(
                orderId = 1L,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
            )
        order.prepareProducts()
        order.readyForPayment()
        val invalidPaymentId = 0L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                order.paid(invalidPaymentId)
            }
        assertTrue(exception.message!!.contains("결제 ID는 0보다 커야 합니다"))
    }

    @Test
    @DisplayName("경계값 테스트 - 단일 상품 주문")
    fun `경계값 테스트 - 단일 상품 주문`() {
        // given
        val orderId = 1L
        val userId = 1L
        val orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 1L))

        // when
        val order = Order(orderId, userId, orderItems)

        // then
        assertEquals(1L, order.calculateTotalAmount())
    }

    @Test
    @DisplayName("경계값 테스트 - 0개의 상품 주문")
    fun `경계값 테스트 - 0개 상품 주문`() {
        // given
        val orderId = 1L
        val userId = 1L
        val orderItems = emptyList<OrderItem>()

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                Order(orderId, userId, orderItems)
            }
        assertTrue(exception.message!!.contains("주문 상품은 ${MIN_QUANTITY}개 이상이어야 합니다"))
    }

    @Test
    @DisplayName("경계값 테스트 - 최대 수량 주문")
    fun `경계값 테스트 - 최대 수량 주문`() {
        // given
        val orderId = 1L
        val userId = 1L
        val orderItems =
            listOf(
                OrderItem(productId = 1L, quantity = MAX_QUANTITY, unitPrice = 1000L),
            )

        // when
        val order = Order(orderId, userId, orderItems)

        // then
        assertEquals(MAX_QUANTITY * 1000L, order.calculateTotalAmount())
    }

    @Test
    @DisplayName("경계값 테스트 - 최대 수량+1 주문")
    fun `경계값 테스트 - 최대 수량+1 주문`() {
        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                OrderItem(productId = 1L, quantity = MAX_QUANTITY + 1, unitPrice = 1000L)
            }
        assertTrue(exception.message!!.contains("주문 수량은 ${MAX_QUANTITY} 이하여야 합니다."))
    }
}
