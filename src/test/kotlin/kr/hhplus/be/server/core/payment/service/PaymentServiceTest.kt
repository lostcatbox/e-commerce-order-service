package kr.hhplus.be.server.core.payment.service

import kr.hhplus.be.server.core.coupon.domain.Coupon
import kr.hhplus.be.server.core.coupon.domain.CouponStatus
import kr.hhplus.be.server.core.coupon.domain.UserCoupon
import kr.hhplus.be.server.core.coupon.domain.UserCouponStatus
import kr.hhplus.be.server.core.coupon.service.CouponServiceInterface
import kr.hhplus.be.server.core.coupon.service.UserCouponServiceInterface
import kr.hhplus.be.server.core.order.domain.Order
import kr.hhplus.be.server.core.payment.domain.Payment
import kr.hhplus.be.server.core.payment.repository.PaymentRepository
import kr.hhplus.be.server.core.payment.service.dto.ProcessPaymentCommand
import kr.hhplus.be.server.core.point.service.PointServiceInterface
import kr.hhplus.be.server.fake.event.FakePaymentEventPublisher
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
@DisplayName("PaymentService 비즈니스 로직 테스트")
class PaymentServiceTest {
    @Mock
    private lateinit var paymentRepository: PaymentRepository

    @Mock
    private lateinit var pointService: PointServiceInterface

    @Mock
    private lateinit var userCouponService: UserCouponServiceInterface

    @Mock
    private lateinit var couponService: CouponServiceInterface

    private val fakePaymentEventPublisher = FakePaymentEventPublisher()

    private lateinit var paymentService: PaymentService

    @BeforeEach
    fun setup() {
        paymentService =
            PaymentService(
                paymentRepository,
                pointService,
                userCouponService,
                couponService,
                fakePaymentEventPublisher,
            )
        clearInvocations(paymentRepository, pointService)
    }

    @Test
    @DisplayName("쿠폰 없이 결제 처리 성공")
    fun `쿠폰 없이 결제 처리 성공`() {
        // given
        val userId = 1L
        val orderAmount = 50000L

        val order = createPaymentReadyOrder(userId, orderAmount)
        val command = ProcessPaymentCommand(order = order, coupon = null)

        val expectedPayment = Payment.createPayment(orderAmount, 0L)
        expectedPayment.success() // 성공 상태로 변경

        whenever(paymentRepository.save(any<Payment>())).thenReturn(expectedPayment)

        // when
        val result = paymentService.processPayment(command)

        // then
        assertEquals(orderAmount, result.originalAmount)
        assertEquals(0L, result.discountAmount)
        assertEquals(orderAmount, result.finalAmount)
        assertTrue(result.isSuccess())

        verify(pointService).usePoint(userId, orderAmount)
        verify(paymentRepository).save(any<Payment>())
    }

    @Test
    @DisplayName("쿠폰 객체를 직접 전달하여 결제 처리 성공")
    fun `쿠폰 객체를 직접 전달하여 결제 처리 성공`() {
        // given
        val userId = 1L
        val orderAmount = 50000L
        val discountAmount = 10000L
        val finalAmount = orderAmount - discountAmount

        val order = createPaymentReadyOrder(userId, orderAmount)
        val coupon =
            Coupon(
                couponId = 500L,
                description = "10000원 할인 쿠폰",
                discountAmount = discountAmount,
                stock = 100,
                couponStatus = CouponStatus.OPENED,
            )
        val command = ProcessPaymentCommand(order = order, coupon = coupon)

        val expectedPayment = Payment.createPayment(orderAmount, discountAmount)
        expectedPayment.success()

        whenever(paymentRepository.save(any<Payment>())).thenReturn(expectedPayment)

        // when
        val result = paymentService.processPayment(command)

        // then
        assertEquals(orderAmount, result.originalAmount)
        assertEquals(discountAmount, result.discountAmount)
        assertEquals(finalAmount, result.finalAmount)
        assertTrue(result.isSuccess())

        // 쿠폰 서비스는 호출되지 않고 직접 전달된 쿠폰 사용
        verify(userCouponService, never()).useCoupon(any())
        verify(couponService, never()).getCouponInfo(any())
        verify(pointService).usePoint(userId, finalAmount)
        verify(paymentRepository).save(any<Payment>())
    }

    @Test
    @DisplayName("주문에 쿠폰 ID가 설정된 경우 쿠폰 서비스를 통한 결제 처리")
    fun `주문에 쿠폰 ID가 설정된 경우 쿠폰 서비스를 통한 결제 처리`() {
        // given
        val userId = 1L
        val orderAmount = 50000L
        val discountAmount = 10000L
        val finalAmount = orderAmount - discountAmount
        val userCouponId = 200L
        val couponId = 500L

        val order = createPaymentReadyOrder(userId, orderAmount)
        // 주문에 쿠폰 ID 설정 (실제 운영 환경과 동일)
        val orderWithCoupon = Order(userId = userId, usedCouponId = userCouponId)
        orderWithCoupon.addOrderItem(productId = 1L, quantity = 1, unitPrice = orderAmount)
        orderWithCoupon.prepareProducts()
        orderWithCoupon.readyForPayment()

        val command = ProcessPaymentCommand(order = orderWithCoupon, coupon = null)

        // Mock 설정
        val mockUserCoupon =
            UserCoupon(
                userCouponId = userCouponId,
                userId = userId,
                couponId = couponId,
                status = UserCouponStatus.USED,
            )
        val mockCoupon =
            Coupon(
                couponId = couponId,
                description = "10000원 할인 쿠폰",
                discountAmount = discountAmount,
                stock = 100,
                couponStatus = CouponStatus.OPENED,
            )

        whenever(userCouponService.useCoupon(userCouponId)).thenReturn(mockUserCoupon)
        whenever(couponService.getCouponInfo(couponId)).thenReturn(mockCoupon)

        val expectedPayment = Payment.createPayment(orderAmount, discountAmount)
        expectedPayment.success()
        whenever(paymentRepository.save(any<Payment>())).thenReturn(expectedPayment)

        // when
        val result = paymentService.processPayment(command)

        // then
        assertEquals(orderAmount, result.originalAmount)
        assertEquals(discountAmount, result.discountAmount)
        assertEquals(finalAmount, result.finalAmount)
        assertTrue(result.isSuccess())

        // 쿠폰 서비스가 호출되었는지 확인
        verify(userCouponService).useCoupon(userCouponId)
        verify(couponService).getCouponInfo(couponId)
        verify(pointService).usePoint(userId, finalAmount)
        verify(paymentRepository).save(any<Payment>())
    }

    @Test
    @DisplayName("포인트 사용 실패 시 결제 실패 처리")
    fun `포인트 사용 실패 시 결제 실패 처리`() {
        // given
        val userId = 1L
        val orderId = 100L

        val orderAmount = 50000L

        val order = createPaymentReadyOrder(userId, orderAmount)
        val command = ProcessPaymentCommand(order = order, coupon = null)

        val failedPayment = Payment.createPayment(orderAmount, 0L)
        failedPayment.fail() // 실패 상태로 변경

        whenever(pointService.usePoint(userId, orderAmount)).thenThrow(RuntimeException("포인트 부족"))
        whenever(paymentRepository.save(any<Payment>())).thenReturn(failedPayment)

        // when & then
        val exception =
            assertThrows<RuntimeException> {
                paymentService.processPayment(command)
            }
        assertEquals("포인트 부족", exception.message)

        verify(pointService).usePoint(userId, orderAmount)
        verify(paymentRepository, atLeastOnce()).save(any<Payment>()) // 실패 처리로 인해 최소 1번 호출
    }

    @Test
    @DisplayName("결제 대기 상태가 아닌 주문으로 결제 시 예외 발생")
    fun `결제 대기 상태가 아닌 주문으로 결제 시 예외 발생`() {
        // given
        val order = Order(userId = 1L)
        order.addOrderItem(productId = 1L, quantity = 1, unitPrice = 10000L)
        // orderStatus는 기본값인 OrderStatus.REQUESTED (결제 대기가 아닌 잘못된 상태)
        val command = ProcessPaymentCommand(order = order, coupon = null)

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                paymentService.processPayment(command)
            }
        assertTrue(exception.message!!.contains("결제 가능한 상태의 주문이 아닙니다"))

        verify(paymentRepository, never()).save(any())
        verify(pointService, never()).usePoint(any(), any())
    }

    @Test
    @DisplayName("존재하지 않는 결제 조회 시 예외 발생")
    fun `존재하지 않는 결제 조회 시 예외 발생`() {
        // given
        val paymentId = 999L
        whenever(paymentRepository.findByPaymentId(paymentId)).thenReturn(null)

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                paymentService.getPayment(paymentId)
            }
        assertTrue(exception.message!!.contains("존재하지 않는 결제입니다"))
        assertTrue(exception.message!!.contains("999"))

        verify(paymentRepository).findByPaymentId(paymentId)
    }

    @Test
    @DisplayName("주문별 결제 조회 - 결제가 없는 경우")
    fun `주문별 결제 조회 - 결제가 없는 경우`() {
        // given
        val orderId = 100L
        whenever(paymentRepository.findByOrderId(orderId)).thenReturn(null)

        // when
        val result = paymentService.getPaymentByOrderId(orderId)

        // then
        assertNull(result)
        verify(paymentRepository).findByOrderId(orderId)
    }

    @Test
    @DisplayName("유효하지 않은 결제 ID로 조회 시 예외 발생")
    fun `유효하지 않은 결제 ID로 조회 시 예외 발생`() {
        // given
        val invalidPaymentId = 0L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                paymentService.getPayment(invalidPaymentId)
            }
        assertTrue(exception.message!!.contains("결제 ID는 0보다 커야 합니다"))

        verify(paymentRepository, never()).findByPaymentId(any())
    }

    @Test
    @DisplayName("유효하지 않은 주문 ID로 결제 조회 시 예외 발생")
    fun `유효하지 않은 주문 ID로 결제 조회 시 예외 발생`() {
        // given
        val invalidOrderId = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                paymentService.getPaymentByOrderId(invalidOrderId)
            }
        assertTrue(exception.message!!.contains("주문 ID는 0보다 커야 합니다"))

        verify(paymentRepository, never()).findByOrderId(any())
    }

    @Test
    @DisplayName("최소 금액 결제 처리 성공")
    fun `최소 금액 결제 처리 성공`() {
        // given
        val userId = 1L
        val orderId = 100L

        val minAmount = 1L

        val order = createPaymentReadyOrder(userId, minAmount)
        val command = ProcessPaymentCommand(order = order, coupon = null)

        val expectedPayment = Payment.createPayment(minAmount, 0L)
        expectedPayment.success() // 성공 상태로 변경

        whenever(paymentRepository.save(any<Payment>())).thenReturn(expectedPayment)

        // when
        val result = paymentService.processPayment(command)

        // then
        assertEquals(minAmount, result.finalAmount)
        verify(pointService).usePoint(userId, minAmount)
    }

    /**
     * 테스트용 결제 대기 상태 주문 생성
     */
    private fun createPaymentReadyOrder(
        userId: Long,
        totalAmount: Long,
    ): Order {
        val unitPrice = if (totalAmount > 0) totalAmount else 1L
        val order = Order(userId = userId)
        order.addOrderItem(productId = 1L, quantity = 1, unitPrice = unitPrice)
        order.prepareProducts()
        order.readyForPayment()
        return order
    }
}
