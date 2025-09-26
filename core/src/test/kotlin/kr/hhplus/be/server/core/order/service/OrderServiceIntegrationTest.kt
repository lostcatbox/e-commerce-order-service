package kr.hhplus.be.server.core.order.service

import jakarta.persistence.EntityManager
import kr.hhplus.be.server.IntegrationTestSupport
import kr.hhplus.be.server.core.order.domain.OrderStatus
import kr.hhplus.be.server.core.order.repository.OrderRepository
import kr.hhplus.be.server.core.order.service.dto.CreateOrderCommand
import kr.hhplus.be.server.core.order.service.dto.OrderItemCommand
import kr.hhplus.be.server.core.product.domain.Product
import kr.hhplus.be.server.core.product.repository.ProductRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional

@DisplayName("OrderService 통합 테스트")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderServiceIntegrationTest : IntegrationTestSupport() {
    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private val testUserId = 100L
    private lateinit var testProduct1: Product
    private lateinit var testProduct2: Product

    @BeforeEach
    fun setUp() {
        // 테스트용 상품 데이터 생성
        testProduct1 =
            Product(
                productId = 1L,
                name = "주문 테스트 상품1",
                description = "주문 테스트용 첫 번째 상품",
                price = 10000L,
                stock = 100,
            )
        testProduct2 =
            Product(
                productId = 2L,
                name = "주문 테스트 상품2",
                description = "주문 테스트용 두 번째 상품",
                price = 15000L,
                stock = 50,
            )
        productRepository.save(testProduct1)
        productRepository.save(testProduct2)
    }

    @Test
    @DisplayName("주문 생성 성공")
    @Transactional
    fun `주문 생성 성공`() {
        // given
        val orderItems =
            listOf(
                OrderItemCommand(testProduct1.productId, 2),
                OrderItemCommand(testProduct2.productId, 1),
            )
        val command = CreateOrderCommand(testUserId, orderItems)

        // when
        val result = orderService.createOrder(command)

        // then
        assertNotNull(result)
        assertEquals(testUserId, result.userId)
        assertEquals(OrderStatus.REQUESTED, result.getOrderStatus())
        assertNull(result.usedCouponId)

        // 주문 상품들이 올바르게 추가되었는지 확인
        val orderItemList = result.orderItems
        assertEquals(2, orderItemList.size)

        // 첫 번째 주문 상품 확인
        val orderItem1 = orderItemList.find { it.productId == testProduct1.productId }
        assertNotNull(orderItem1)
        assertEquals(2, orderItem1!!.quantity)
        assertEquals(testProduct1.price, orderItem1.unitPrice)

        // 두 번째 주문 상품 확인
        val orderItem2 = orderItemList.find { it.productId == testProduct2.productId }
        assertNotNull(orderItem2)
        assertEquals(1, orderItem2!!.quantity)
        assertEquals(testProduct2.price, orderItem2.unitPrice)

        // 총 금액 확인
        val expectedTotalAmount = (testProduct1.price * 2) + (testProduct2.price * 1)
        assertEquals(expectedTotalAmount, result.calculateTotalAmount())

        // 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // DB에서 다시 조회하여 실제로 저장되었는지 확인
        val savedOrder = orderRepository.findByOrderId(result.orderId)
        assertNotNull(savedOrder)
        assertEquals(result.orderId, savedOrder!!.orderId)
    }

    @Test
    @DisplayName("쿠폰을 사용한 주문 생성")
    @Transactional
    fun `쿠폰을 사용한 주문 생성 `() {
        // given
        val orderItems = listOf(OrderItemCommand(testProduct1.productId, 1))
        val usedCouponId = 1L
        val command = CreateOrderCommand(testUserId, orderItems, usedCouponId)

        // when
        val result = orderService.createOrder(command)

        // then
        assertNotNull(result)
        assertEquals(testUserId, result.userId)
        assertEquals(usedCouponId, result.usedCouponId)
        assertEquals(OrderStatus.REQUESTED, result.getOrderStatus())
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 주문 생성 시 예외 발생")
    @Transactional
    fun `존재하지 않는 상품으로 주문 생성 시 예외 발생`() {
        // given
        val nonExistentProductId = 999L
        val orderItems = listOf(OrderItemCommand(nonExistentProductId, 1))
        val command = CreateOrderCommand(testUserId, orderItems)

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                orderService.createOrder(command)
            }

        assertTrue(exception.message!!.contains("존재하지 않는 상품입니다"))
        assertTrue(exception.message!!.contains(nonExistentProductId.toString()))
    }

    @Test
    @DisplayName("빈 주문 상품 리스트로 주문 생성 시 예외 발생")
    @Transactional
    fun `빈 주문 상품 리스트로 주문 생성 시 예외 발생`() {
        // given
        val emptyOrderItems = emptyList<OrderItemCommand>()

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                CreateOrderCommand(testUserId, emptyOrderItems)
            }

        assertTrue(exception.message!!.contains("주문 상품은 1개 이상이어야 합니다"))
    }

    @Test
    @DisplayName("주문 상태를 상품 준비 완료로 변경")
    @Transactional
    fun `주문 상태를 상품 준비 완료로 변경`() {
        // given
        val orderItems = listOf(OrderItemCommand(testProduct1.productId, 1))
        val command = CreateOrderCommand(testUserId, orderItems)
        val order = orderService.createOrder(command)

        // when
        val result = orderService.changeProductReserved(order.orderId)

        // then
        assertEquals(OrderStatus.PRODUCT_READY, result.getOrderStatus())

        // 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // DB에서 다시 조회하여 상태 변경이 저장되었는지 확인
        val savedOrder = orderRepository.findByOrderId(order.orderId)
        assertEquals(OrderStatus.PRODUCT_READY, savedOrder!!.getOrderStatus())
    }

    @Test
    @DisplayName("주문 상태를 결제 대기로 변경")
    @Transactional
    fun `주문 상태를 결제 대기로 변경`() {
        // given
        val orderItems = listOf(OrderItemCommand(testProduct1.productId, 1))
        val command = CreateOrderCommand(testUserId, orderItems)
        val order = orderService.createOrder(command)
        orderService.changeProductReserved(order.orderId)

        // when
        val result = orderService.changePaymentReady(order.orderId)

        // then
        assertEquals(OrderStatus.PAYMENT_READY, result.getOrderStatus())

        // 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // DB에서 다시 조회하여 상태 변경이 저장되었는지 확인
        val savedOrder = orderRepository.findByOrderId(order.orderId)
        assertEquals(OrderStatus.PAYMENT_READY, savedOrder!!.getOrderStatus())
    }

    @Test
    @DisplayName("주문 상태를 결제 완료로 변경")
    @Transactional
    fun `주문 상태를 결제 완료로 변경`() {
        // given
        val orderItems = listOf(OrderItemCommand(testProduct1.productId, 1))
        val command = CreateOrderCommand(testUserId, orderItems)
        val order = orderService.createOrder(command)
        orderService.changeProductReserved(order.orderId)
        orderService.changePaymentReady(order.orderId)
        val paymentId = 100L

        // when
        val result = orderService.changePaymentComplete(order.orderId, paymentId)

        // then
        assertEquals(OrderStatus.PAYMENT_COMPLETED, result.getOrderStatus())
        assertEquals(paymentId, result.getPaymentId())

        // 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // DB에서 다시 조회하여 상태 변경이 저장되었는지 확인
        val savedOrder = orderRepository.findByOrderId(order.orderId)
        assertEquals(OrderStatus.PAYMENT_COMPLETED, savedOrder!!.getOrderStatus())
        assertEquals(paymentId, savedOrder.getPaymentId())
    }

    @Test
    @DisplayName("주문 상태를 완료로 변경")
    @Transactional
    fun `주문 상태를 완료로 변경`() {
        // given
        val orderItems = listOf(OrderItemCommand(testProduct1.productId, 1))
        val command = CreateOrderCommand(testUserId, orderItems)
        val order = orderService.createOrder(command)
        orderService.changeProductReserved(order.orderId)
        orderService.changePaymentReady(order.orderId)
        orderService.changePaymentComplete(order.orderId, 100L)

        // when
        val result = orderService.changeCompleted(order.orderId)

        // then
        assertEquals(OrderStatus.COMPLETED, result.getOrderStatus())

        // 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // DB에서 다시 조회하여 상태 변경이 저장되었는지 확인
        val savedOrder = orderRepository.findByOrderId(order.orderId)
        assertEquals(OrderStatus.COMPLETED, savedOrder!!.getOrderStatus())
    }

    @Test
    @DisplayName("주문 상태를 실패로 변경")
    @Transactional
    fun `주문 상태를 실패로 변경`() {
        // given
        val orderItems = listOf(OrderItemCommand(testProduct1.productId, 1))
        val command = CreateOrderCommand(testUserId, orderItems)
        val order = orderService.createOrder(command)

        // when
        val result = orderService.changeFailed(order.orderId)

        // then
        assertEquals(OrderStatus.FAILED, result.getOrderStatus())

        // 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // DB에서 다시 조회하여 상태 변경이 저장되었는지 확인
        val savedOrder = orderRepository.findByOrderId(order.orderId)
        assertEquals(OrderStatus.FAILED, savedOrder!!.getOrderStatus())
    }

    @Test
    @DisplayName("주문 조회")
    @Transactional
    fun `주문 조회 `() {
        // given
        val orderItems = listOf(OrderItemCommand(testProduct1.productId, 1))
        val command = CreateOrderCommand(testUserId, orderItems)
        val createdOrder = orderService.createOrder(command)

        // when
        val result = orderService.getOrder(createdOrder.orderId)

        // then
        assertNotNull(result)
        assertEquals(createdOrder.orderId, result.orderId)
        assertEquals(createdOrder.userId, result.userId)
        assertEquals(createdOrder.getOrderStatus(), result.getOrderStatus())
    }

    @Test
    @DisplayName("존재하지 않는 주문 조회 시 예외 발생")
    @Transactional
    fun `존재하지 않는 주문 조회 시 예외 발생`() {
        // given
        val nonExistentOrderId = 999L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                orderService.getOrder(nonExistentOrderId)
            }

        assertTrue(exception.message!!.contains("존재하지 않는 주문입니다"))
        assertTrue(exception.message!!.contains(nonExistentOrderId.toString()))
    }

    @Test
    @DisplayName("잘못된 주문 ID로 조회 시 예외 발생")
    @Transactional
    fun `잘못된 주문 ID로 조회 시 예외 발생`() {
        // given
        val invalidOrderId = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                orderService.getOrder(invalidOrderId)
            }

        assertTrue(exception.message!!.contains("주문 ID는 0보다 커야 합니다"))
    }

    @Test
    @DisplayName("사용자별 주문 목록 조회 - ")
    @Transactional
    fun `사용자별 주문 목록 조회 `() {
        // given - 여러 주문 생성
        val orderItems1 = listOf(OrderItemCommand(testProduct1.productId, 1))
        val orderItems2 = listOf(OrderItemCommand(testProduct2.productId, 2))
        val command1 = CreateOrderCommand(testUserId, orderItems1)
        val command2 = CreateOrderCommand(testUserId, orderItems2)

        val order1 = orderService.createOrder(command1)
        val order2 = orderService.createOrder(command2)

        // when
        val result = orderService.getUserOrders(testUserId)

        // then
        assertEquals(2, result.size)
        assertTrue(result.any { it.orderId == order1.orderId })
        assertTrue(result.any { it.orderId == order2.orderId })
        assertTrue(result.all { it.userId == testUserId })
    }

    @Test
    @DisplayName("주문이 없는 사용자의 주문 목록 조회")
    @Transactional
    fun `주문이 없는 사용자의 주문 목록 조회`() {
        // given
        val userWithNoOrders = 999L

        // when
        val result = orderService.getUserOrders(userWithNoOrders)

        // then
        assertTrue(result.isEmpty())
    }

    @Test
    @DisplayName("잘못된 사용자 ID로 주문 목록 조회 시 예외 발생")
    @Transactional
    fun `잘못된 사용자 ID로 주문 목록 조회 시 예외 발생`() {
        // given
        val invalidUserId = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                orderService.getUserOrders(invalidUserId)
            }

        assertTrue(exception.message!!.contains("사용자 ID는 0보다 커야 합니다"))
    }

    @Test
    @DisplayName("존재하지 않는 주문 상태 변경 시 예외 발생")
    @Transactional
    fun `존재하지 않는 주문 상태 변경 시 예외 발생`() {
        // given
        val nonExistentOrderId = 999L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                orderService.changeProductReserved(nonExistentOrderId)
            }

        assertTrue(exception.message!!.contains("존재하지 않는 주문입니다"))
        assertTrue(exception.message!!.contains(nonExistentOrderId.toString()))
    }
}
