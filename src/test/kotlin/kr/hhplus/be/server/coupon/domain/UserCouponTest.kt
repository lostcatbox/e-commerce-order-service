package kr.hhplus.be.server.coupon.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("유저 쿠폰 도메인 테스트")
class UserCouponTest {
    @Nested
    @DisplayName("쿠폰 발급 테스트")
    inner class CouponIssueTest {
        @Test
        @DisplayName("정상적으로 유저에게 쿠폰을 발급할 수 있다")
        fun `정상적으로_유저에게_쿠폰을_발급할_수_있다`() {
            // given
            val userId = 1L
            val couponId = 1L

            // when
            val userCoupon = UserCoupon.issueCoupon(userId, couponId)

            // then
            assertEquals(userId, userCoupon.userId)
            assertEquals(couponId, userCoupon.couponId)
            assertEquals(UserCouponStatus.ISSUED, userCoupon.getStatus())
            assertNotNull(userCoupon.issuedAt)
            assertNull(userCoupon.getUsedAt())
        }

        @Test
        @DisplayName("사용자 ID가 0 이하인 경우 예외가 발생한다")
        fun `사용자_ID가_0_이하인_경우_예외가_발생한다`() {
            // given
            val invalidUserId = 0L
            val couponId = 1L

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    UserCoupon.issueCoupon(invalidUserId, couponId)
                }

            assertEquals("사용자 ID는 0보다 커야 합니다. 입력된 ID: $invalidUserId", exception.message)
        }

        @Test
        @DisplayName("쿠폰 ID가 0 이하인 경우 예외가 발생한다")
        fun `쿠폰_ID가_0_이하인_경우_예외가_발생한다`() {
            // given
            val userId = 1L
            val invalidCouponId = 0L

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    UserCoupon.issueCoupon(userId, invalidCouponId)
                }

            assertEquals("쿠폰 ID는 0보다 커야 합니다. 입력된 ID: $invalidCouponId", exception.message)
        }
    }

    @Nested
    @DisplayName("쿠폰 사용 테스트")
    inner class CouponUseTest {
        @Test
        @DisplayName("발급된 쿠폰을 사용할 수 있다")
        fun `발급된_쿠폰을_사용할_수_있다`() {
            // given
            val userCoupon = UserCoupon.issueCoupon(1L, 1L)

            // when
            userCoupon.use()

            // then
            assertEquals(UserCouponStatus.USED, userCoupon.getStatus())
            assertNotNull(userCoupon.getUsedAt())
        }

        @Test
        @DisplayName("이미 사용된 쿠폰은 다시 사용할 수 없다")
        fun `이미_사용된_쿠폰은_다시_사용할_수_없다`() {
            // given
            val userCoupon = UserCoupon.issueCoupon(1L, 1L)
            userCoupon.use()

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    userCoupon.use()
                }

            assertEquals("쿠폰이 사용 가능한 상태가 아닙니다. 현재 상태: USED", exception.message)
        }
    }

    @Nested
    @DisplayName("쿠폰 사용 가능 여부 확인 테스트")
    inner class CouponUsabilityTest {
        @Test
        @DisplayName("발급된 쿠폰은 사용 가능하다")
        fun `발급된_쿠폰은_사용_가능하다`() {
            // given
            val userCoupon = UserCoupon.issueCoupon(1L, 1L)

            // when & then
            assertTrue(userCoupon.isUsable())
        }

        @Test
        @DisplayName("사용된 쿠폰은 사용 불가능하다")
        fun `사용된_쿠폰은_사용_불가능하다`() {
            // given
            val userCoupon = UserCoupon.issueCoupon(1L, 1L)
            userCoupon.use()

            // when & then
            assertFalse(userCoupon.isUsable())
        }
    }

    @Nested
    @DisplayName("유저 쿠폰 생성 테스트")
    inner class CouponInitTest {
        @Test
        @DisplayName("직접 생성자로 유저 쿠폰을 생성할 수 있다")
        fun `직접_생성자로_유저_쿠폰을_생성할_수_있다`() {
            // given
            val userId = 1L
            val couponId = 1L
            val status = UserCouponStatus.ISSUED
            val issuedAt = System.currentTimeMillis()

            // when
            val userCoupon =
                UserCoupon(
                    userId = userId,
                    couponId = couponId,
                    status = status,
                    issuedAt = issuedAt,
                    usedAt = null,
                )

            // then
            assertEquals(userId, userCoupon.userId)
            assertEquals(couponId, userCoupon.couponId)
            assertEquals(status, userCoupon.getStatus())
            assertEquals(issuedAt, userCoupon.issuedAt)
            assertNull(userCoupon.getUsedAt())
        }
    }
}
