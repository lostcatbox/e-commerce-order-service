package kr.hhplus.be.server.core.order.service

import kr.hhplus.be.server.core.order.domain.CreateOrderCommand
import kr.hhplus.be.server.core.order.domain.Order
import kr.hhplus.be.server.core.order.domain.OrderItem
import kr.hhplus.be.server.core.order.domain.OrderItemCommand
import kr.hhplus.be.server.core.order.domain.OrderStatus
import kr.hhplus.be.server.core.order.repository.OrderRepository
import kr.hhplus.be.server.core.order.service.OrderService
import kr.hhplus.be.server.core.product.domain.Product
import kr.hhplus.be.server.core.product.repository.ProductRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
@DisplayName("OrderService 비즈니스 로직 테스트")
class OrderServiceTest {
    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var orderService: OrderService

    @BeforeEach
    fun setup() {
        clearInvocations(orderRepository, productRepository)
    }

    @Test
    @DisplayName("주문 생성 성공")
    fun `주문 생성 성공`() {
        // given
        val userId = 1L
        val orderId = 100L
        val command =
            CreateOrderCommand(
                userId = userId,
                orderItems =
                    listOf(
                        OrderItemCommand(productId = 1L, quantity = 2),
                        OrderItemCommand(productId = 2L, quantity = 1),
                    ),
            )

        val product1 = Product(1L, "상품1", "설명1", 10000L, 100)
        val product2 = Product(2L, "상품2", "설명2", 20000L, 50)

        val expectedOrder =
            Order(
                orderId = orderId,
                userId = userId,
                orderItems =
                    listOf(
                        OrderItem(productId = 1L, quantity = 2, unitPrice = 10000L),
                        OrderItem(productId = 2L, quantity = 1, unitPrice = 20000L),
                    ),
            )

        whenever(productRepository.findByProductId(1L)).thenReturn(product1)
        whenever(productRepository.findByProductId(2L)).thenReturn(product2)
        whenever(orderRepository.generateNextOrderId()).thenReturn(orderId)
        whenever(orderRepository.save(any<Order>())).thenReturn(expectedOrder)

        // when
        val result = orderService.createOrder(command)

        // then
        assertEquals(orderId, result.orderId)
        assertEquals(userId, result.userId)
        assertEquals(2, result.orderItems.size)
        assertEquals(OrderStatus.REQUESTED, result.getOrderStatus())

        verify(productRepository).findByProductId(1L)
        verify(productRepository).findByProductId(2L)
        verify(orderRepository).generateNextOrderId()
        verify(orderRepository).save(any<Order>())
    }

    @Test
    @DisplayName("쿠폰 사용 주문 생성 성공")
    fun `쿠폰 사용 주문 생성 성공`() {
        // given
        val userId = 1L
        val orderId = 100L
        val usedCouponId = 500L
        val command =
            CreateOrderCommand(
                userId = userId,
                orderItems = listOf(OrderItemCommand(productId = 1L, quantity = 1)),
                usedCouponId = usedCouponId,
            )

        val product = Product(1L, "상품1", "설명1", 10000L, 100)
        val expectedOrder =
            Order(
                orderId = orderId,
                userId = userId,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
                usedCouponId = usedCouponId,
            )

        whenever(productRepository.findByProductId(1L)).thenReturn(product)
        whenever(orderRepository.generateNextOrderId()).thenReturn(orderId)
        whenever(orderRepository.save(any<Order>())).thenReturn(expectedOrder)

        // when
        val result = orderService.createOrder(command)

        // then
        assertEquals(usedCouponId, result.usedCouponId)
        verify(orderRepository).save(any<Order>())
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 주문 생성 시 예외 발생")
    fun `존재하지 않는 상품으로 주문 생성 시 예외 발생`() {
        // given
        val command =
            CreateOrderCommand(
                userId = 1L,
                orderItems = listOf(OrderItemCommand(productId = 999L, quantity = 1)),
            )

        whenever(productRepository.findByProductId(999L)).thenReturn(null)

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                orderService.createOrder(command)
            }
        assertTrue(exception.message!!.contains("존재하지 않는 상품입니다"))
        assertTrue(exception.message!!.contains("999"))

        verify(productRepository).findByProductId(999L)
        verify(orderRepository, never()).save(any())
    }

    @Test
    @DisplayName("주문 상태를 상품 준비 완료로 변경 성공")
    fun `주문 상태를 상품 준비 완료로 변경 성공`() {
        // given
        val orderId = 1L
        val existingOrder =
            Order(
                orderId = orderId,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
                orderStatus = OrderStatus.REQUESTED,
            )
        val updatedOrder =
            Order(
                orderId = orderId,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
                orderStatus = OrderStatus.PRODUCT_READY,
            )

        whenever(orderRepository.findByOrderId(orderId)).thenReturn(existingOrder)
        whenever(orderRepository.save(any<Order>())).thenReturn(updatedOrder)

        // when
        val result = orderService.changeProductReady(orderId)

        // then
        assertEquals(OrderStatus.PRODUCT_READY, result.getOrderStatus())
        verify(orderRepository).findByOrderId(orderId)
        verify(orderRepository).save(any<Order>())
    }

    @Test
    @DisplayName("주문 상태를 결제 대기로 변경 성공")
    fun `주문 상태를 결제 대기로 변경 성공`() {
        // given
        val orderId = 1L
        val existingOrder =
            Order(
                orderId = orderId,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
            )
        existingOrder.prepareProducts() // PRODUCT_READY로 설정

        val updatedOrder =
            Order(
                orderId = orderId,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
            )
        updatedOrder.prepareProducts()
        updatedOrder.readyForPayment()

        whenever(orderRepository.findByOrderId(orderId)).thenReturn(existingOrder)
        whenever(orderRepository.save(any<Order>())).thenReturn(updatedOrder)

        // when
        val result = orderService.changePaymentReady(orderId)

        // then
        assertEquals(OrderStatus.PAYMENT_READY, result.getOrderStatus())
        verify(orderRepository).findByOrderId(orderId)
        verify(orderRepository).save(any<Order>())
    }

    @Test
    @DisplayName("주문 상태를 결제 완료로 변경 성공")
    fun `주문 상태를 결제 완료로 변경 성공`() {
        // given
        val orderId = 1L
        val paymentId = 200L
        val existingOrder =
            Order(
                orderId = orderId,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
            )
        existingOrder.prepareProducts()
        existingOrder.readyForPayment() // PAYMENT_READY로 설정

        val updatedOrder =
            Order(
                orderId = orderId,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
            )
        updatedOrder.prepareProducts()
        updatedOrder.readyForPayment()
        updatedOrder.paid(paymentId)

        whenever(orderRepository.findByOrderId(orderId)).thenReturn(existingOrder)
        whenever(orderRepository.save(any<Order>())).thenReturn(updatedOrder)

        // when
        val result = orderService.changePaymentComplete(orderId, paymentId)

        // then
        assertEquals(OrderStatus.PAYMENT_COMPLETED, result.getOrderStatus())
        verify(orderRepository).findByOrderId(orderId)
        verify(orderRepository).save(any<Order>())
    }

    @Test
    @DisplayName("주문 상태를 완료로 변경 성공")
    fun `주문 상태를 완료로 변경 성공`() {
        // given
        val orderId = 1L
        val existingOrder =
            Order(
                orderId = orderId,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
            )
        existingOrder.prepareProducts()
        existingOrder.readyForPayment()
        existingOrder.paid(200L) // PAYMENT_COMPLETED로 설정

        val updatedOrder =
            Order(
                orderId = orderId,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
            )
        updatedOrder.prepareProducts()
        updatedOrder.readyForPayment()
        updatedOrder.paid(200L)
        updatedOrder.complete()

        whenever(orderRepository.findByOrderId(orderId)).thenReturn(existingOrder)
        whenever(orderRepository.save(any<Order>())).thenReturn(updatedOrder)

        // when
        val result = orderService.changeCompleted(orderId)

        // then
        assertEquals(OrderStatus.COMPLETED, result.getOrderStatus())
        assertTrue(result.isCompleted())
        verify(orderRepository).findByOrderId(orderId)
        verify(orderRepository).save(any<Order>())
    }

    @Test
    @DisplayName("주문 상태를 실패로 변경 성공")
    fun `주문 상태를 실패로 변경 성공`() {
        // given
        val orderId = 1L
        val existingOrder =
            Order(
                orderId = orderId,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
            )

        val updatedOrder =
            Order(
                orderId = orderId,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
                orderStatus = OrderStatus.FAILED,
            )

        whenever(orderRepository.findByOrderId(orderId)).thenReturn(existingOrder)
        whenever(orderRepository.save(any<Order>())).thenReturn(updatedOrder)

        // when
        val result = orderService.changeFailed(orderId)

        // then
        assertEquals(OrderStatus.FAILED, result.getOrderStatus())
        assertTrue(result.isFailed())
        verify(orderRepository).findByOrderId(orderId)
        verify(orderRepository).save(any<Order>())
    }

    @Test
    @DisplayName("주문 조회 성공")
    fun `주문 조회 성공`() {
        // given
        val orderId = 1L
        val expectedOrder =
            Order(
                orderId = orderId,
                userId = 1L,
                orderItems = listOf(OrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)),
            )

        whenever(orderRepository.findByOrderId(orderId)).thenReturn(expectedOrder)

        // when
        val result = orderService.getOrder(orderId)

        // then
        assertEquals(expectedOrder, result)
        verify(orderRepository).findByOrderId(orderId)
    }

    @Test
    @DisplayName("존재하지 않는 주문 조회 시 예외 발생")
    fun `존재하지 않는 주문 조회 시 예외 발생`() {
        // given
        val orderId = 999L
        whenever(orderRepository.findByOrderId(orderId)).thenReturn(null)

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                orderService.getOrder(orderId)
            }
        assertTrue(exception.message!!.contains("존재하지 않는 주문입니다"))
        assertTrue(exception.message!!.contains("999"))

        verify(orderRepository).findByOrderId(orderId)
    }

    @Test
    @DisplayName("사용자별 주문 목록 조회 성공")
    fun `사용자별 주문 목록 조회 성공`() {
        // given
        val userId = 1L
        val expectedOrders =
            listOf(
                Order(1L, userId, listOf(OrderItem(1L, 1, 10000L))),
                Order(2L, userId, listOf(OrderItem(2L, 2, 20000L))),
            )

        whenever(orderRepository.findByUserId(userId)).thenReturn(expectedOrders)

        // when
        val result = orderService.getUserOrders(userId)

        // then
        assertEquals(2, result.size)
        assertEquals(expectedOrders, result)
        verify(orderRepository).findByUserId(userId)
    }

    @Test
    @DisplayName("유효하지 않은 주문 ID로 조회 시 예외 발생")
    fun `유효하지 않은 주문 ID로 조회 시 예외 발생`() {
        // given
        val invalidOrderId = 0L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                orderService.getOrder(invalidOrderId)
            }
        assertTrue(exception.message!!.contains("주문 ID는 0보다 커야 합니다"))

        verify(orderRepository, never()).findByOrderId(any())
    }

    @Test
    @DisplayName("유효하지 않은 사용자 ID로 주문 목록 조회 시 예외 발생")
    fun `유효하지 않은 사용자 ID로 주문 목록 조회 시 예외 발생`() {
        // given
        val invalidUserId = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                orderService.getUserOrders(invalidUserId)
            }
        assertTrue(exception.message!!.contains("사용자 ID는 0보다 커야 합니다"))

        verify(orderRepository, never()).findByUserId(any())
    }
}
