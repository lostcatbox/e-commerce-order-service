package kr.hhplus.be.server.core.payment.domain

import io.micrometer.core.instrument.config.validate.Validated.invalid
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

        val originalAmount = 50000L

        // when
        val payment = Payment(paymentId, originalAmount)

        // then
        assertEquals(paymentId, payment.paymentId)
        assertEquals(originalAmount, payment.originalAmount)
        assertEquals(0L, payment.discountAmount)
        assertEquals(PaymentStatus.REQUESTED, payment.getPaymentStatus())
        assertEquals(originalAmount, payment.finalAmount)
        assertTrue(payment.getCreatedAt() > 0)
    }

    @Test
    @DisplayName("할인 금액이 포함된 Payment 생성")
    fun `할인 금액이 포함된 Payment 생성`() {
        // given
        val paymentId = 1L

        val originalAmount = 50000L
        val discountAmount = 5000L

        // when
        val payment = Payment(paymentId, originalAmount, discountAmount)

        // then
        assertEquals(discountAmount, payment.discountAmount)
        assertEquals(45000L, payment.finalAmount)
    }

    @Test
    @DisplayName("결제 ID가 0보다 작거나 같으면 예외 발생")
    fun `결제 ID가 0보다 작거나 같으면 예외 발생`() {
        // given
        val invalidPaymentId = 0L

        val originalAmount = 50000L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                Payment(invalidPaymentId, originalAmount)
            }
        assertTrue(exception.message!!.contains("결제 ID는 0보다 커야 합니다"))
    }

    @Test
    @DisplayName("원본 금액이 음수면 예외 발생")
    fun `원본 금액이 음수면 예외 발생`() {
        // given
        val paymentId = 1L

        val invalidOriginalAmount = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                Payment(paymentId, invalidOriginalAmount)
            }
        assertTrue(exception.message!!.contains("원본 금액은 0 이상이어야 합니다"))
    }

    @Test
    @DisplayName("할인 금액이 음수면 예외 발생")
    fun `할인 금액이 음수면 예외 발생`() {
        // given
        val paymentId = 1L

        val originalAmount = 50000L
        val invalidDiscountAmount = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                Payment(paymentId, originalAmount, invalidDiscountAmount)
            }
        assertTrue(exception.message!!.contains("할인 금액은 0 이상이어야 합니다"))
    }

    @Test
    @DisplayName("최종 결제 금액 계산")
    fun `최종 결제 금액 계산`() {
        // given
        val payment = Payment(1L, 50000L)

        // when
        val finalAmount = payment.finalAmount

        // then
        assertEquals(50000L, finalAmount)
    }

    @Test
    @DisplayName("할인 금액이 적용된 결제 생성")
    fun `할인 금액이 적용된 결제 생성`() {
        // given
        val paymentId = 1L

        val originalAmount = 50000L
        val discountAmount = 10000L

        // when
        val payment = Payment(paymentId, originalAmount, discountAmount)

        // then
        assertEquals(discountAmount, payment.discountAmount)
        assertEquals(40000L, payment.finalAmount)
    }

    @Test
    @DisplayName("음수 할인 금액으로 결제 생성 시 예외 발생")
    fun `음수 할인 금액으로 결제 생성 시 예외 발생`() {
        // given
        val paymentId = 1L

        val originalAmount = 50000L
        val invalidDiscountAmount = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                Payment(paymentId, originalAmount, invalidDiscountAmount)
            }
        assertTrue(exception.message!!.contains("할인 금액은 0 이상이어야 합니다"))
    }

    @Test
    @DisplayName("할인 금액이 원본 금액을 초과하면 예외 발생")
    fun `할인 금액이 원본 금액을 초과하면 예외 발생`() {
        // given
        val paymentId = 1L

        val originalAmount = 10000L
        val excessiveDiscountAmount = 20000L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                Payment(paymentId, originalAmount, excessiveDiscountAmount)
            }
        assertTrue(exception.message!!.contains("할인 금액은 원본 금액을 초과할 수 없습니다"))
    }

    @Test
    @DisplayName("결제 성공 처리")
    fun `결제 성공 처리`() {
        // given
        val payment = Payment(1L, 50000L, 50000L)

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
        val payment = Payment(1L, 50000L, 50000L)

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
        val payment = Payment(1L, 50000L, 50000L)
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
        val payment = Payment(1L, 50000L, 50000L)
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

        val originalAmount = 0L

        // when
        val payment = Payment(paymentId, originalAmount)

        // then
        assertEquals(0L, payment.finalAmount)
    }

    @Test
    @DisplayName("경계값 테스트 - 할인 금액 0원")
    fun `경계값 테스트 - 할인 금액 0원`() {
        // given
        val paymentId = 1L

        val originalAmount = 50000L
        val discountAmount = 0L

        // when
        val payment = Payment(paymentId, originalAmount, discountAmount)

        // then
        assertEquals(0L, payment.discountAmount)
        assertEquals(50000L, payment.finalAmount)
    }

    @Test
    @DisplayName("경계값 테스트 - 할인 금액이 원본 금액과 동일")
    fun `경계값 테스트 - 할인 금액이 원본 금액과 동일`() {
        // given
        val paymentId = 1L
        val originalAmount = 50000L
        val discountAmount = 50000L

        // when
        val payment = Payment(paymentId, originalAmount, discountAmount)

        // then
        assertEquals(50000L, payment.discountAmount)
        assertEquals(0L, payment.finalAmount)
    }

    @Test
    @DisplayName("경계값 테스트 - 최소 금액 결제")
    fun `경계값 테스트 - 최소 금액 결제`() {
        // given
        val payment = Payment(1L, 1L, 1L)

        // when & then
        assertEquals(0L, payment.finalAmount)
        assertEquals(1L, payment.originalAmount)
    }
}
