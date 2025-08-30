package kr.hhplus.be.server.core.coupon.domain

import kr.hhplus.be.server.core.coupon.domain.Coupon
import kr.hhplus.be.server.core.coupon.domain.CouponStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("쿠폰 도메인 테스트")
class CouponTest {
    @Nested
    @DisplayName("쿠폰 생성 테스트")
    inner class CouponInitTest {
        @Test
        @DisplayName("올바른 정보로 쿠폰을 생성할 수 있다")
        fun `올바른_정보로_쿠폰을_생성할_수_있다`() {
            // given
            val couponId = 1L
            val description = "1000원 할인 쿠폰"
            val discountAmount = 1000L
            val stock = 100
            val couponStatus = CouponStatus.OPENED

            // when
            val coupon =
                Coupon(
                    couponId = couponId,
                    description = description,
                    discountAmount = discountAmount,
                    stock = stock,
                    couponStatus = couponStatus,
                )

            // then
            assertEquals(couponId, coupon.couponId)
            assertEquals(description, coupon.description)
            assertEquals(discountAmount, coupon.discountAmount)
            assertEquals(stock, coupon.getStock())
            assertEquals(couponStatus, coupon.getCouponStatus())
        }

        @Test
        @DisplayName("재고가 음수이면 예외가 발생한다")
        fun `재고가_음수이면_예외가_발생한다`() {
            // given
            val invalidStock = -1

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    Coupon(
                        couponId = 1L,
                        description = "테스트 쿠폰",
                        discountAmount = 1000L,
                        stock = invalidStock,
                        couponStatus = CouponStatus.OPENED,
                    )
                }

            assertEquals("쿠폰 재고는 0 이상이어야 합니다. 현재 재고: $invalidStock", exception.message)
        }

        @Test
        @DisplayName("재고가 최대값을 초과하면 예외가 발생한다")
        fun `재고가_최대값을_초과하면_예외가_발생한다`() {
            // given
            val invalidStock = 1001

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    Coupon(
                        couponId = 1L,
                        description = "테스트 쿠폰",
                        discountAmount = 1000L,
                        stock = invalidStock,
                        couponStatus = CouponStatus.OPENED,
                    )
                }

            assertEquals("쿠폰 재고는 1000 이하여야 합니다. 현재 재고: $invalidStock", exception.message)
        }

        @Test
        @DisplayName("할인 금액이 0 이하이면 예외가 발생한다")
        fun `할인_금액이_0_이하이면_예외가_발생한다`() {
            // given
            val invalidDiscountAmount = 0L

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    Coupon(
                        couponId = 1L,
                        description = "테스트 쿠폰",
                        discountAmount = invalidDiscountAmount,
                        stock = 100,
                        couponStatus = CouponStatus.OPENED,
                    )
                }

            assertEquals("할인 금액은 1 이상이어야 합니다. 현재 할인 금액: $invalidDiscountAmount", exception.message)
        }

        @Test
        @DisplayName("설명이 비어있으면 예외가 발생한다")
        fun `설명이_비어있으면_예외가_발생한다`() {
            // given
            val emptyDescription = ""

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    Coupon(
                        couponId = 1L,
                        description = emptyDescription,
                        discountAmount = 1000L,
                        stock = 100,
                        couponStatus = CouponStatus.OPENED,
                    )
                }

            assertEquals("쿠폰 설명은 비어있을 수 없습니다.", exception.message)
        }
    }

    @Nested
    @DisplayName("쿠폰 발급 테스트")
    inner class CouponIssueTest {
        @Test
        @DisplayName("정상적으로 쿠폰을 발급할 수 있다")
        fun `정상적으로_쿠폰을_발급할_수_있다`() {
            // given
            val coupon =
                Coupon(
                    couponId = 1L,
                    description = "테스트 쿠폰",
                    discountAmount = 1000L,
                    stock = 100,
                    couponStatus = CouponStatus.OPENED,
                )

            // when
            coupon.issueCoupon()

            // then
            assertEquals(99, coupon.getStock())
        }

        @Test
        @DisplayName("쿠폰이 닫힌 상태에서는 발급할 수 없다")
        fun `쿠폰이_닫힌_상태에서는_발급할_수_없다`() {
            // given
            val coupon =
                Coupon(
                    couponId = 1L,
                    description = "테스트 쿠폰",
                    discountAmount = 1000L,
                    stock = 100,
                    couponStatus = CouponStatus.CLOSED,
                )

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    coupon.issueCoupon()
                }

            assertEquals("쿠폰이 사용 가능한 상태가 아닙니다. 현재 상태: CLOSED", exception.message)
        }

        @Test
        @DisplayName("재고가 0인 경우 발급할 수 없다")
        fun `재고가_0인_경우_발급할_수_없다`() {
            // given
            val coupon =
                Coupon(
                    couponId = 1L,
                    description = "테스트 쿠폰",
                    discountAmount = 1000L,
                    stock = 0,
                    couponStatus = CouponStatus.OPENED,
                )

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    coupon.issueCoupon()
                }

            assertEquals("쿠폰 재고가 부족합니다. 현재 재고: 0", exception.message)
        }
    }

    @Nested
    @DisplayName("할인 적용 테스트")
    inner class CouponApplyDiscountTest {
        @Test
        @DisplayName("정상적으로 할인을 적용할 수 있다")
        fun `정상적으로_할인을_적용할_수_있다`() {
            // given
            val coupon =
                Coupon(
                    couponId = 1L,
                    description = "1000원 할인 쿠폰",
                    discountAmount = 1000L,
                    stock = 100,
                    couponStatus = CouponStatus.OPENED,
                )
            val targetAmount = 5000L

            // when
            val discountedAmount = coupon.applyDiscount(targetAmount)

            // then
            assertEquals(4000L, discountedAmount)
        }

        @Test
        @DisplayName("할인 금액이 대상 금액보다 클 경우 0을 반환한다")
        fun `할인_금액이_대상_금액보다_클_경우_0을_반환한다`() {
            // given
            val coupon =
                Coupon(
                    couponId = 1L,
                    description = "1000원 할인 쿠폰",
                    discountAmount = 1000L,
                    stock = 100,
                    couponStatus = CouponStatus.OPENED,
                )
            val targetAmount = 500L

            // when
            val discountedAmount = coupon.applyDiscount(targetAmount)

            // then
            assertEquals(0L, discountedAmount)
        }

        @Test
        @DisplayName("쿠폰이 닫힌 상태에서는 할인을 적용할 수 없다")
        fun `쿠폰이_닫힌_상태에서는_할인을_적용할_수_없다`() {
            // given
            val coupon =
                Coupon(
                    couponId = 1L,
                    description = "테스트 쿠폰",
                    discountAmount = 1000L,
                    stock = 100,
                    couponStatus = CouponStatus.CLOSED,
                )

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    coupon.applyDiscount(5000L)
                }

            assertEquals("쿠폰이 사용 가능한 상태가 아닙니다. 현재 상태: CLOSED", exception.message)
        }

        @Test
        @DisplayName("대상 금액이 음수인 경우 예외가 발생한다")
        fun `대상_금액이_음수인_경우_예외가_발생한다`() {
            // given
            val coupon =
                Coupon(
                    couponId = 1L,
                    description = "테스트 쿠폰",
                    discountAmount = 1000L,
                    stock = 100,
                    couponStatus = CouponStatus.OPENED,
                )

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    coupon.applyDiscount(-1000L)
                }

            assertEquals("할인 대상 금액은 0 이상이어야 합니다. 입력된 금액: -1000", exception.message)
        }
    }

    @Nested
    @DisplayName("쿠폰 상태 확인 테스트")
    inner class CouponStatusTest {
        @Test
        @DisplayName("OPENED 상태의 쿠폰은 사용 가능하다")
        fun `OPENED_상태의_쿠폰은_사용_가능하다`() {
            // given
            val coupon =
                Coupon(
                    couponId = 1L,
                    description = "테스트 쿠폰",
                    discountAmount = 1000L,
                    stock = 100,
                    couponStatus = CouponStatus.OPENED,
                )

            // when & then
            assertTrue(coupon.isOpened())
        }

        @Test
        @DisplayName("CLOSED 상태의 쿠폰은 사용 불가능하다")
        fun `CLOSED_상태의_쿠폰은_사용_불가능하다`() {
            // given
            val coupon =
                Coupon(
                    couponId = 1L,
                    description = "테스트 쿠폰",
                    discountAmount = 1000L,
                    stock = 100,
                    couponStatus = CouponStatus.CLOSED,
                )

            // when & then
            assertFalse(coupon.isOpened())
        }

        @Test
        @DisplayName("쿠폰을 닫을 수 있다")
        fun `쿠폰을_닫을_수_있다`() {
            // given
            val coupon =
                Coupon(
                    couponId = 1L,
                    description = "테스트 쿠폰",
                    discountAmount = 1000L,
                    stock = 100,
                    couponStatus = CouponStatus.OPENED,
                )

            // when
            coupon.close()

            // then
            assertEquals(CouponStatus.CLOSED, coupon.getCouponStatus())
            assertFalse(coupon.isOpened())
        }
    }
}
