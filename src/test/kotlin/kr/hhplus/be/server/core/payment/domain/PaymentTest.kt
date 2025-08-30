package kr.hhplus.be.server.core.payment.domain

import kr.hhplus.be.server.core.payment.domain.Payment
import kr.hhplus.be.server.core.payment.domain.PaymentStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Payment 도메인 모델 테스트")
class PaymentTest {
    @Test
    @DisplayName("정상적인 Payment 생성")
    fun `정상적인 Payment 생성`() {
        // given
        val paymentId = 1L
        val orderId = 1L
        val originalAmount = 50000L

        // when
        val payment = Payment(paymentId, orderId, originalAmount)

        // then
        assertEquals(paymentId, payment.paymentId)
        assertEquals(orderId, payment.orderId)
        assertEquals(originalAmount, payment.originalAmount)
        assertEquals(0L, payment.getDiscountAmount())
        assertEquals(PaymentStatus.REQUESTED, payment.getPaymentStatus())
        assertEquals(originalAmount, payment.finalAmount)
        assertTrue(payment.getCreatedAt() > 0)
    }

    @Test
    @DisplayName("할인 금액이 포함된 Payment 생성")
    fun `할인 금액이 포함된 Payment 생성`() {
        // given
        val paymentId = 1L
        val orderId = 1L
        val originalAmount = 50000L
        val discountAmount = 5000L

        // when
        val payment = Payment(paymentId, orderId, originalAmount, discountAmount)

        // then
        assertEquals(discountAmount, payment.getDiscountAmount())
        assertEquals(45000L, payment.finalAmount)
    }

    @Test
    @DisplayName("결제 ID가 0보다 작거나 같으면 예외 발생")
    fun `결제 ID가 0보다 작거나 같으면 예외 발생`() {
        // given
        val invalidPaymentId = 0L
        val orderId = 1L
        val originalAmount = 50000L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                Payment(invalidPaymentId, orderId, originalAmount)
            }
        assertTrue(exception.message!!.contains("결제 ID는 0보다 커야 합니다"))
    }

    @Test
    @DisplayName("주문 ID가 0보다 작거나 같으면 예외 발생")
    fun `주문 ID가 0보다 작거나 같으면 예외 발생`() {
        // given
        val paymentId = 1L
        val invalidOrderId = 0L
        val originalAmount = 50000L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                Payment(paymentId, invalidOrderId, originalAmount)
            }
        assertTrue(exception.message!!.contains("주문 ID는 0보다 커야 합니다"))
    }

    @Test
    @DisplayName("원본 금액이 음수면 예외 발생")
    fun `원본 금액이 음수면 예외 발생`() {
        // given
        val paymentId = 1L
        val orderId = 1L
        val invalidOriginalAmount = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                Payment(paymentId, orderId, invalidOriginalAmount)
            }
        assertTrue(exception.message!!.contains("원본 금액은 0 이상이어야 합니다"))
    }

    @Test
    @DisplayName("할인 금액이 음수면 예외 발생")
    fun `할인 금액이 음수면 예외 발생`() {
        // given
        val paymentId = 1L
        val orderId = 1L
        val originalAmount = 50000L
        val invalidDiscountAmount = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                Payment(paymentId, orderId, originalAmount, invalidDiscountAmount)
            }
        assertTrue(exception.message!!.contains("할인 금액은 0 이상이어야 합니다"))
    }

    @Test
    @DisplayName("최종 결제 금액 계산")
    fun `최종 결제 금액 계산`() {
        // given
        val payment = Payment(1L, 1L, 50000L)

        // when
        val finalAmount = payment.finalAmount

        // then
        assertEquals(50000L, finalAmount)
    }

    @Test
    @DisplayName("할인 적용")
    fun `할인 적용`() {
        // given
        val payment = Payment(1L, 1L, 50000L)
        val discountAmount = 10000L

        // when
        payment.addDiscountAmount(discountAmount)

        // then
        assertEquals(discountAmount, payment.getDiscountAmount())
        assertEquals(40000L, payment.finalAmount)
    }

    @Test
    @DisplayName("할인 적용 시 음수 할인 금액이면 예외 발생")
    fun `할인 적용 시 음수 할인 금액이면 예외 발생`() {
        // given
        val payment = Payment(1L, 1L, 50000L)
        val invalidDiscountAmount = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                payment.addDiscountAmount(invalidDiscountAmount)
            }
        assertTrue(exception.message!!.contains("할인 금액은 0 이상이어야 합니다"))
    }

    @Test
    @DisplayName("할인 적용 후 최종 금액이 음수가 되면 예외 발생")
    fun `할인 적용 후 최종 금액이 음수가 되면 예외 발생`() {
        // given
        val payment = Payment(1L, 1L, 10000L)
        val excessiveDiscountAmount = 20000L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                payment.addDiscountAmount(excessiveDiscountAmount)
            }
        assertTrue(
            exception.message!!.contains("최종 결제 금액은 0 이상이어야 합니다") ||
                exception.message!!.contains("할인 적용 후 최종 금액이 음수가 될 수 없습니다"),
        )
    }

    @Test
    @DisplayName("결제 성공 처리")
    fun `결제 성공 처리`() {
        // given
        val payment = Payment(1L, 1L, 50000L)

        // when
        payment.success()

        // then
        assertEquals(PaymentStatus.SUCCESS, payment.getPaymentStatus())
        assertTrue(payment.isSuccess())
        assertFalse(payment.isFailed())
    }

    @Test
    @DisplayName("결제 실패 처리")
    fun `결제 실패 처리`() {
        // given
        val payment = Payment(1L, 1L, 50000L)

        // when
        payment.fail()

        // then
        assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus())
        assertTrue(payment.isFailed())
        assertFalse(payment.isSuccess())
    }

    @Test
    @DisplayName("결제 성공 후 상태 변경 시 예외 발생")
    fun `결제 성공 후 상태 변경 시 예외 발생`() {
        // given
        val payment = Payment(1L, 1L, 50000L)
        payment.success()

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                payment.fail()
            }
        assertTrue(exception.message!!.contains("결제 상태를"))
        assertTrue(exception.message!!.contains("변경할 수 없습니다"))
    }

    @Test
    @DisplayName("결제 실패 후 상태 변경 시 예외 발생")
    fun `결제 실패 후 상태 변경 시 예외 발생`() {
        // given
        val payment = Payment(1L, 1L, 50000L)
        payment.fail()

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                payment.success()
            }
        assertTrue(exception.message!!.contains("결제 상태를"))
        assertTrue(exception.message!!.contains("변경할 수 없습니다"))
    }

    @Test
    @DisplayName("경계값 테스트 - 원본 금액 0원")
    fun `경계값 테스트 - 원본 금액 0원`() {
        // given
        val paymentId = 1L
        val orderId = 1L
        val originalAmount = 0L

        // when
        val payment = Payment(paymentId, orderId, originalAmount)

        // then
        assertEquals(0L, payment.finalAmount)
    }

    @Test
    @DisplayName("경계값 테스트 - 할인 금액 0원")
    fun `경계값 테스트 - 할인 금액 0원`() {
        // given
        val payment = Payment(1L, 1L, 50000L)
        val discountAmount = 0L

        // when
        payment.addDiscountAmount(discountAmount)

        // then
        assertEquals(0L, payment.getDiscountAmount())
        assertEquals(50000L, payment.finalAmount)
    }

    @Test
    @DisplayName("경계값 테스트 - 할인 금액이 원본 금액과 동일")
    fun `경계값 테스트 - 할인 금액이 원본 금액과 동일`() {
        // given
        val payment = Payment(1L, 1L, 50000L)
        val discountAmount = 50000L

        // when
        payment.addDiscountAmount(discountAmount)

        // then
        assertEquals(50000L, payment.getDiscountAmount())
        assertEquals(0L, payment.finalAmount)
    }

    @Test
    @DisplayName("경계값 테스트 - 최소 금액 결제")
    fun `경계값 테스트 - 최소 금액 결제`() {
        // given
        val payment = Payment(1L, 1L, 1L)

        // when & then
        assertEquals(1L, payment.finalAmount)
        assertEquals(1L, payment.originalAmount)
    }

    @Test
    @DisplayName("할인 적용 후 재할인 적용")
    fun `할인 적용 후 재할인 적용`() {
        // given
        val payment = Payment(1L, 1L, 50000L)
        payment.addDiscountAmount(10000L)

        // when
        payment.addDiscountAmount(5000L)

        // then
        assertEquals(15000L, payment.getDiscountAmount())
        assertEquals(35000L, payment.finalAmount)
    }
}
