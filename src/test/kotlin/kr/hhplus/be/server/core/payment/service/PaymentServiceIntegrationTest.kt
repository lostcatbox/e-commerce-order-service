package kr.hhplus.be.server.core.payment.service

import jakarta.persistence.EntityManager
import kr.hhplus.be.server.IntegrationTestSupport
import kr.hhplus.be.server.core.coupon.domain.Coupon
import kr.hhplus.be.server.core.coupon.domain.CouponStatus
import kr.hhplus.be.server.core.coupon.domain.UserCoupon
import kr.hhplus.be.server.core.coupon.domain.UserCouponStatus
import kr.hhplus.be.server.core.coupon.repository.CouponRepository
import kr.hhplus.be.server.core.coupon.repository.UserCouponRepository
import kr.hhplus.be.server.core.order.domain.Order
import kr.hhplus.be.server.core.order.repository.OrderRepository
import kr.hhplus.be.server.core.order.service.OrderService
import kr.hhplus.be.server.core.payment.domain.PaymentStatus
import kr.hhplus.be.server.core.payment.repository.PaymentRepository
import kr.hhplus.be.server.core.payment.service.dto.ProcessPaymentCommand
import kr.hhplus.be.server.core.point.domain.UserPoint
import kr.hhplus.be.server.core.point.repository.UserPointRepository
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

@DisplayName("PaymentService 통합 테스트")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PaymentServiceIntegrationTest : IntegrationTestSupport() {
    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var paymentService: PaymentService

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var userPointRepository: UserPointRepository

    @Autowired
    private lateinit var couponRepository: CouponRepository

    @Autowired
    private lateinit var userCouponRepository: UserCouponRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private val testUserId = 100L
    private lateinit var testProduct: Product
    private lateinit var testOrder: Order
    private lateinit var testUserPoint: UserPoint

    @BeforeEach
    fun setUp() {
        // 테스트용 상품 생성
        testProduct =
            Product(
                productId = 1L,
                name = "결제 테스트 상품",
                description = "결제 테스트용 상품 설명",
                price = 10000L,
                stock = 100,
            )
        productRepository.save(testProduct)

        // 테스트용 주문 생성 (결제 대기 상태로)
        testOrder = Order(userId = testUserId, usedCouponId = null)
        testOrder.addOrderItem(testProduct.productId, 2, testProduct.price)
        testOrder.reservedProducts()
        testOrder.readyForPayment()
        orderRepository.save(testOrder)

        // 테스트용 사용자 포인트 생성 (충분한 잔액)
        testUserPoint = UserPoint(userId = testUserId)
        testUserPoint.charge(100000L) // 10만원 충전
        userPointRepository.save(testUserPoint)

        // 테스트 데이터 준비 완료
    }

    @Test
    @DisplayName("결제 처리 성공")
    @Transactional
    fun `결제 처리 성공`() {
        // given
        val command = ProcessPaymentCommand(orderId = testOrder.orderId)
        val originalPointBalance = testUserPoint.getBalance()
        val expectedPaymentAmount = testOrder.calculateTotalAmount()

        // when
        val result = paymentService.processPayment(command)

        // then
        assertNotNull(result)
        assertEquals(expectedPaymentAmount, result.originalAmount)
        assertEquals(0L, result.discountAmount)
        assertEquals(expectedPaymentAmount, result.finalAmount)
        assertEquals(PaymentStatus.SUCCESS, result.getPaymentStatus())
        assertTrue(result.isSuccess())

        // 포인트가 차감되었는지 확인
        val updatedUserPoint = userPointRepository.findByUserId(testUserId)
        assertEquals(originalPointBalance - expectedPaymentAmount, updatedUserPoint!!.getBalance())

        // 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // DB에서 다시 조회하여 실제로 저장되었는지 확인
        val savedPayment = paymentRepository.findByPaymentId(result.paymentId)
        assertNotNull(savedPayment)
        assertEquals(result.paymentId, savedPayment!!.paymentId)
        assertEquals(PaymentStatus.SUCCESS, savedPayment.getPaymentStatus())
    }

    @Test
    @DisplayName("쿠폰이 설정된 주문으로 결제 처리")
    @Transactional
    fun `쿠폰이 설정된 주문으로 결제 처리`() {
        // given - 실제 쿠폰과 사용자 쿠폰 데이터 생성
        val discountCoupon =
            Coupon(
                couponId = 1L,
                description = "5000원 할인 쿠폰",
                discountAmount = 5000L,
                stock = 100,
                couponStatus = CouponStatus.OPENED,
            )

        // 쿠폰을 DB에 저장
        couponRepository.save(discountCoupon)

        // 사용자 쿠폰 생성 및 저장
        val userCoupon = UserCoupon.issueCoupon(testUserId, discountCoupon.couponId)
        userCouponRepository.save(userCoupon)

        // 쿠폰이 설정된 주문 생성
        val orderWithCoupon = Order(userId = testUserId, usedCouponId = userCoupon.userCouponId)
        orderWithCoupon.addOrderItem(testProduct.productId, 2, testProduct.price)
        orderWithCoupon.reservedProducts()
        orderWithCoupon.readyForPayment()
        orderRepository.save(orderWithCoupon)

        val command = ProcessPaymentCommand(orderId = orderWithCoupon.orderId)
        val originalPointBalance = testUserPoint.getBalance()
        val originalAmount = orderWithCoupon.calculateTotalAmount()
        val expectedFinalAmount = originalAmount - discountCoupon.discountAmount

        // when
        val result = paymentService.processPayment(command)

        // then
        assertNotNull(result)
        assertEquals(originalAmount, result.originalAmount)
        assertEquals(discountCoupon.discountAmount, result.discountAmount)
        assertEquals(expectedFinalAmount, result.finalAmount)
        assertEquals(PaymentStatus.SUCCESS, result.getPaymentStatus())
        assertTrue(result.isSuccess())

        // 할인된 금액만큼 포인트가 차감되었는지 확인
        val updatedUserPoint = userPointRepository.findByUserId(testUserId)
        assertEquals(originalPointBalance - expectedFinalAmount, updatedUserPoint!!.getBalance())

        // 사용자 쿠폰이 사용됨 상태로 변경되었는지 확인
        val updatedUserCoupon = userCouponRepository.findByUserCouponId(userCoupon.userCouponId)
        assertEquals(UserCouponStatus.USED, updatedUserCoupon!!.getStatus())

        // 영속성 컨텍스트 초기화
        entityManager.flush()
        entityManager.clear()

        // DB에서 다시 조회하여 실제로 저장되었는지 확인
        val savedPayment = paymentRepository.findByPaymentId(result.paymentId)
        assertNotNull(savedPayment)
        assertEquals(discountCoupon.discountAmount, savedPayment!!.discountAmount)
        assertEquals(expectedFinalAmount, savedPayment.finalAmount)
        assertEquals(PaymentStatus.SUCCESS, savedPayment.getPaymentStatus())
    }

    @Test
    @DisplayName("쿠폰 없이 일반 결제 처리 - 추가 검증")
    @Transactional
    fun `쿠폰 없이 일반 결제 처리 - 추가 검증`() {
        // given
        val command = ProcessPaymentCommand(orderId = testOrder.orderId)
        val originalPointBalance = testUserPoint.getBalance()
        val originalAmount = testOrder.calculateTotalAmount()

        // when
        val result = paymentService.processPayment(command)

        // then
        assertNotNull(result)
        assertEquals(originalAmount, result.originalAmount)
        assertEquals(0L, result.discountAmount)
        assertEquals(originalAmount, result.finalAmount)
        assertEquals(PaymentStatus.SUCCESS, result.getPaymentStatus())
        assertTrue(result.isSuccess())

        // 원래 금액만큼 포인트가 차감되었는지 확인
        val updatedUserPoint = userPointRepository.findByUserId(testUserId)
        assertEquals(originalPointBalance - originalAmount, updatedUserPoint!!.getBalance())

        // 영속성 컨텍스트 초기화
        entityManager.flush()
        entityManager.clear()

        // DB에서 다시 조회하여 실제로 저장되었는지 확인
        val savedPayment = paymentRepository.findByPaymentId(result.paymentId)
        assertNotNull(savedPayment)
        assertEquals(0L, savedPayment!!.discountAmount)
        assertEquals(originalAmount, savedPayment.finalAmount)
    }

    @Test
    @DisplayName("포인트 부족으로 결제 실패 ")
    @Transactional
    fun `포인트 부족으로 결제 실패`() {
        // given - 포인트 부족 상태로 설정
        testUserPoint.use(99000L) // 1000원만 남김
        userPointRepository.save(testUserPoint)

        val command = ProcessPaymentCommand(orderId = testOrder.orderId)
        val originalPointBalance = testUserPoint.getBalance()

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                paymentService.processPayment(command)
            }

        // 포인트 사용 실패로 인한 예외 확인
        assertTrue(exception.message!!.contains("잔액이") || exception.message!!.contains("미만이 될 수 없습니다"))

        // 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // 포인트가 변경되지 않았는지 확인
        val unchangedUserPoint = userPointRepository.findByUserId(testUserId)
        assertEquals(originalPointBalance, unchangedUserPoint!!.getBalance())

        // 실패한 결제가 저장되었는지 확인
        val payments = paymentRepository.findByOrderId(testOrder.orderId)
        if (payments != null) {
            assertEquals(PaymentStatus.FAILED, payments.getPaymentStatus())
            assertTrue(payments.isFailed())
        }
    }

    @Test
    @DisplayName("잘못된 주문 상태로 결제 시 예외 발생")
    @Transactional
    fun `잘못된 주문 상태로 결제 시 예외 발생`() {
        // given - 주문 상태를 결제 불가 상태로 변경
        testOrder.fail() // 주문 상태를 실패로 변경
        orderRepository.save(testOrder)

        val command = ProcessPaymentCommand(orderId = testOrder.orderId)

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                paymentService.processPayment(command)
            }

        assertTrue(exception.message!!.contains("결제 가능한 상태의 주문이 아닙니다"))
    }

    @Test
    @DisplayName("결제 조회")
    @Transactional
    fun `결제 조회`() {
        // given
        val command = ProcessPaymentCommand(orderId = testOrder.orderId)
        val createdPayment = paymentService.processPayment(command)

        // when
        val result = paymentService.getPayment(createdPayment.paymentId)

        // then
        assertNotNull(result)
        assertEquals(createdPayment.paymentId, result.paymentId)
        assertEquals(createdPayment.originalAmount, result.originalAmount)
        assertEquals(createdPayment.discountAmount, result.discountAmount)
        assertEquals(createdPayment.finalAmount, result.finalAmount)
        assertEquals(createdPayment.getPaymentStatus(), result.getPaymentStatus())
    }

    @Test
    @DisplayName("존재하지 않는 결제 조회 시 예외 발생")
    @Transactional
    fun `존재하지 않는 결제 조회 시 예외 발생`() {
        // given
        val nonExistentPaymentId = 999L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                paymentService.getPayment(nonExistentPaymentId)
            }

        assertTrue(exception.message!!.contains("존재하지 않는 결제입니다"))
        assertTrue(exception.message!!.contains(nonExistentPaymentId.toString()))
    }

    @Test
    @DisplayName("잘못된 결제 ID로 조회 시 예외 발생")
    @Transactional
    fun `잘못된 결제 ID로 조회 시 예외 발생`() {
        // given
        val invalidPaymentId = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                paymentService.getPayment(invalidPaymentId)
            }

        assertTrue(exception.message!!.contains("결제 ID는 0보다 커야 합니다"))
    }

    @Test
    @DisplayName("주문별 결제 조회")
    @Transactional
    fun `주문별 결제 조회`() {
        // given
        val command = ProcessPaymentCommand(orderId = testOrder.orderId)
        val createdPayment = paymentService.processPayment(command)
        testOrder.paid(createdPayment.paymentId) // 주문 상태를 결제 완료로 변경
        orderRepository.save(testOrder)

        // when
        val result = paymentService.getPaymentByOrderId(testOrder.orderId)

        entityManager.flush()
        entityManager.clear()
        // then
        assertNotNull(result)
        assertEquals(createdPayment.paymentId, result!!.paymentId)
        assertEquals(createdPayment.finalAmount, result.finalAmount)
    }

    @Test
    @DisplayName("결제가 없는 주문의 결제 조회")
    @Transactional
    fun `결제가 없는 주문의 결제 조회`() {
        // given - 새로운 주문 생성 (결제하지 않음)
        val newOrder = Order(userId = testUserId, usedCouponId = null)
        newOrder.addOrderItem(testProduct.productId, 1, testProduct.price)
        newOrder.reservedProducts()
        newOrder.readyForPayment()
        val savedOrder = orderRepository.save(newOrder)

        // when
        val result = paymentService.getPaymentByOrderId(savedOrder.orderId)

        // then
        assertNull(result)
    }

    @Test
    @DisplayName("잘못된 주문 ID로 결제 조회 시 예외 발생")
    @Transactional
    fun `잘못된 주문 ID로 결제 조회 시 예외 발생`() {
        // given
        val invalidOrderId = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                paymentService.getPaymentByOrderId(invalidOrderId)
            }

        assertTrue(exception.message!!.contains("주문 ID는 0보다 커야 합니다"))
    }
}
