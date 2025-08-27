package kr.hhplus.be.server.coupon.service

import com.sun.org.apache.xalan.internal.lib.ExsltDatetime.time
import kr.hhplus.be.server.coupon.domain.Coupon
import kr.hhplus.be.server.coupon.domain.CouponStatus
import kr.hhplus.be.server.coupon.domain.UserCoupon
import kr.hhplus.be.server.coupon.domain.UserCouponStatus
import kr.hhplus.be.server.coupon.repository.CouponRepository
import kr.hhplus.be.server.coupon.repository.UserCouponRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

@DisplayName("쿠폰 서비스 테스트")
class CouponServiceTest {
    private lateinit var couponRepository: CouponRepository
    private lateinit var userCouponRepository: UserCouponRepository
    private lateinit var couponService: CouponService

    @BeforeEach
    fun setUp() {
        couponRepository = mock(CouponRepository::class.java)
        userCouponRepository = mock(UserCouponRepository::class.java)
        couponService = CouponService(couponRepository, userCouponRepository)
    }

    @Nested
    @DisplayName("쿠폰 정보 조회 테스트")
    inner class CouponInfoTest {
        @Test
        @DisplayName("존재하는 쿠폰 ID로 쿠폰 정보를 조회할 수 있다")
        fun `존재하는_쿠폰_ID로_쿠폰_정보를_조회할_수_있다`() {
            // given
            val couponId = 1L
            val expectedCoupon =
                Coupon(
                    couponId = couponId,
                    description = "1000원 할인 쿠폰",
                    discountAmount = 1000L,
                    stock = 100,
                    couponStatus = CouponStatus.OPENED,
                )

            `when`(couponRepository.findByCouponId(couponId)).thenReturn(expectedCoupon)

            // when
            val result = couponService.getCouponInfo(couponId)

            // then
            assertEquals(expectedCoupon, result)
            verify(couponRepository).findByCouponId(couponId)
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰 ID로 조회 시 예외가 발생한다")
        fun `존재하지_않는_쿠폰_ID로_조회_시_예외가_발생한다`() {
            // given
            val couponId = 999L

            `when`(couponRepository.findByCouponId(couponId)).thenReturn(null)

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    couponService.getCouponInfo(couponId)
                }

            assertEquals("존재하지 않는 쿠폰입니다. 쿠폰 ID: $couponId", exception.message)
        }

        @Test
        @DisplayName("유효하지 않은 쿠폰 ID로 조회 시 예외가 발생한다")
        fun `유효하지_않은_쿠폰_ID로_조회_시_예외가_발생한다`() {
            // given
            val invalidCouponId = 0L

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    couponService.getCouponInfo(invalidCouponId)
                }

            assertEquals("쿠폰 ID는 0보다 커야 합니다. 입력된 ID: $invalidCouponId", exception.message)
        }
    }

    @Nested
    @DisplayName("쿠폰 발급 테스트")
    inner class CouponIssueTest {
        @Test
        @DisplayName("정상적으로 쿠폰을 발급할 수 있다")
        fun `정상적으로_쿠폰을_발급할_수_있다`() {
            // given
            val userId = 1L
            val couponId = 1L
            val coupon =
                Coupon(
                    couponId = couponId,
                    description = "1000원 할인 쿠폰",
                    discountAmount = 1000L,
                    stock = 100,
                    couponStatus = CouponStatus.OPENED,
                )
            val issuedCoupon = coupon.issueCoupon()
            val userCoupon = UserCoupon.issueCoupon(userId, couponId)

            `when`(couponRepository.findByCouponId(couponId)).thenReturn(coupon)
            `when`(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(null)
            `when`(couponRepository.save(issuedCoupon)).thenReturn(issuedCoupon)
            `when`(userCouponRepository.save(any())).thenReturn(userCoupon)

            // when
            val result = couponService.issueCoupon(userId, couponId)

            // then
            assertEquals(userId, result.userId)
            assertEquals(couponId, result.couponId)
            assertEquals(UserCouponStatus.ISSUED, result.status)

            verify(couponRepository).findByCouponId(couponId)
            verify(userCouponRepository).findByUserIdAndCouponId(userId, couponId)
            verify(couponRepository).save(issuedCoupon)
            verify(userCouponRepository).save(any())
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰 발급 시 예외가 발생한다")
        fun `존재하지_않는_쿠폰_발급_시_예외가_발생한다`() {
            // given
            val userId = 1L
            val couponId = 999L

            `when`(couponRepository.findByCouponId(couponId)).thenReturn(null)

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    couponService.issueCoupon(userId, couponId)
                }

            assertEquals("존재하지 않는 쿠폰입니다. 쿠폰 ID: $couponId", exception.message)
        }

        @Test
        @DisplayName("이미 발급받은 쿠폰을 다시 발급받으려 하면 예외가 발생한다")
        fun `이미_발급받은_쿠폰을_다시_발급받으려_하면_예외가_발생한다`() {
            // given
            val userId = 1L
            val couponId = 1L
            val coupon =
                Coupon(
                    couponId = couponId,
                    description = "1000원 할인 쿠폰",
                    discountAmount = 1000L,
                    stock = 100,
                    couponStatus = CouponStatus.OPENED,
                )
            val existingUserCoupon = UserCoupon.issueCoupon(userId, couponId)

            `when`(couponRepository.findByCouponId(couponId)).thenReturn(coupon)
            `when`(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(existingUserCoupon)

            // when & then
            val exception =
                assertThrows<IllegalStateException> {
                    couponService.issueCoupon(userId, couponId)
                }

            assertEquals("이미 발급받은 쿠폰입니다. 사용자 ID: $userId, 쿠폰 ID: $couponId", exception.message)
        }

        @Test
        @DisplayName("재고가 부족한 쿠폰 발급 시 예외가 발생한다")
        fun `재고가_부족한_쿠폰_발급_시_예외가_발생한다`() {
            // given
            val userId = 1L
            val couponId = 1L
            val coupon =
                Coupon(
                    couponId = couponId,
                    description = "1000원 할인 쿠폰",
                    discountAmount = 1000L,
                    stock = 0,
                    couponStatus = CouponStatus.OPENED,
                )

            `when`(couponRepository.findByCouponId(couponId)).thenReturn(coupon)
            `when`(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(null)

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    couponService.issueCoupon(userId, couponId)
                }

            assertEquals("쿠폰 재고가 부족합니다. 현재 재고: 0", exception.message)
        }

        @Test
        @DisplayName("닫힌 쿠폰 발급 시 예외가 발생한다")
        fun `닫힌_쿠폰_발급_시_예외가_발생한다`() {
            // given
            val userId = 1L
            val couponId = 1L
            val coupon =
                Coupon(
                    couponId = couponId,
                    description = "1000원 할인 쿠폰",
                    discountAmount = 1000L,
                    stock = 100,
                    couponStatus = CouponStatus.CLOSED,
                )

            `when`(couponRepository.findByCouponId(couponId)).thenReturn(coupon)
            `when`(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(null)

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    couponService.issueCoupon(userId, couponId)
                }

            assertEquals("쿠폰이 사용 가능한 상태가 아닙니다. 현재 상태: CLOSED", exception.message)
        }

        @Test
        @DisplayName("유효하지 않은 사용자 ID로 발급 시 예외가 발생한다")
        fun `유효하지_않은_사용자_ID로_발급_시_예외가_발생한다`() {
            // given
            val invalidUserId = 0L
            val couponId = 1L

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    couponService.issueCoupon(invalidUserId, couponId)
                }

            assertEquals("사용자 ID는 0보다 커야 합니다. 입력된 ID: $invalidUserId", exception.message)
        }

        @Test
        @DisplayName("유효하지 않은 쿠폰 ID로 발급 시 예외가 발생한다")
        fun `유효하지_않은_쿠폰_ID로_발급_시_예외가_발생한다`() {
            // given
            val userId = 1L
            val invalidCouponId = 0L

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    couponService.issueCoupon(userId, invalidCouponId)
                }

            assertEquals("쿠폰 ID는 0보다 커야 합니다. 입력된 ID: $invalidCouponId", exception.message)
        }
    }

    @Nested
    @DisplayName("사용자 쿠폰 목록 조회 테스트")
    inner class UserCouponListTest {
        @Test
        @DisplayName("사용자의 쿠폰 목록을 조회할 수 있다")
        fun `사용자의_쿠폰_목록을_조회할_수_있다`() {
            // given
            val userId = 1L
            val userCoupons =
                listOf(
                    UserCoupon.issueCoupon(userId, 1L),
                    UserCoupon.issueCoupon(userId, 2L),
                )

            `when`(userCouponRepository.findByUserId(userId)).thenReturn(userCoupons)

            // when
            val result = couponService.getUserCoupons(userId)

            // then
            assertEquals(2, result.size)
            assertEquals(userCoupons, result)
            verify(userCouponRepository).findByUserId(userId)
        }

        @Test
        @DisplayName("쿠폰이 없는 사용자의 경우 빈 목록을 반환한다")
        fun `쿠폰이_없는_사용자의_경우_빈_목록을_반환한다`() {
            // given
            val userId = 1L

            `when`(userCouponRepository.findByUserId(userId)).thenReturn(emptyList())

            // when
            val result = couponService.getUserCoupons(userId)

            // then
            assertEquals(0, result.size)
            verify(userCouponRepository).findByUserId(userId)
        }

        @Test
        @DisplayName("유효하지 않은 사용자 ID로 조회 시 예외가 발생한다")
        fun `유효하지_않은_사용자_ID로_조회_시_예외가_발생한다`() {
            // given
            val invalidUserId = 0L

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    couponService.getUserCoupons(invalidUserId)
                }

            assertEquals("사용자 ID는 0보다 커야 합니다. 입력된 ID: $invalidUserId", exception.message)
        }
    }
}
