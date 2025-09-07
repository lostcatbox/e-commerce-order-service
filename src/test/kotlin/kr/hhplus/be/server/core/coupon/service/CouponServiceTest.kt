package kr.hhplus.be.server.core.coupon.service

import kr.hhplus.be.server.core.coupon.domain.Coupon
import kr.hhplus.be.server.core.coupon.domain.CouponStatus
import kr.hhplus.be.server.core.coupon.repository.CouponRepository
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
@DisplayName("CouponService 비즈니스 로직 테스트")
class CouponServiceTest {
    @Mock
    private lateinit var couponRepository: CouponRepository

    @InjectMocks
    private lateinit var couponService: CouponService

    @BeforeEach
    fun setup() {
        clearInvocations(couponRepository)
    }

    @Test
    @DisplayName("쿠폰 정보 조회 성공")
    fun `쿠폰 정보 조회 성공`() {
        // given
        val couponId = 1L
        val expectedCoupon =
            Coupon(
                couponId = couponId,
                description = "10000원 할인 쿠폰",
                discountAmount = 10000L,
                stock = 100,
                couponStatus = CouponStatus.OPENED,
            )

        whenever(couponRepository.findByCouponId(couponId)).thenReturn(expectedCoupon)

        // when
        val result = couponService.getCouponInfo(couponId)

        // then
        assertEquals(expectedCoupon, result)
        verify(couponRepository).findByCouponId(couponId)
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 조회 시 예외 발생")
    fun `존재하지 않는 쿠폰 조회 시 예외 발생`() {
        // given
        val couponId = 999L
        whenever(couponRepository.findByCouponId(couponId)).thenReturn(null)

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                couponService.getCouponInfo(couponId)
            }
        assertTrue(exception.message!!.contains("존재하지 않는 쿠폰입니다"))
        assertTrue(exception.message!!.contains("999"))

        verify(couponRepository).findByCouponId(couponId)
    }

    @Test
    @DisplayName("유효하지 않은 쿠폰 ID로 조회 시 예외 발생")
    fun `유효하지 않은 쿠폰 ID로 조회 시 예외 발생`() {
        // given
        val invalidCouponId = 0L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                couponService.getCouponInfo(invalidCouponId)
            }
        assertTrue(exception.message!!.contains("쿠폰 ID는 0보다 커야 합니다"))

        verify(couponRepository, never()).findByCouponId(any())
    }

    @Test
    @DisplayName("쿠폰 재고 차감 성공")
    fun `쿠폰 재고 차감 성공`() {
        // given
        val couponId = 1L
        val originalCoupon =
            Coupon(
                couponId = couponId,
                description = "10000원 할인 쿠폰",
                discountAmount = 10000L,
                stock = 100,
                couponStatus = CouponStatus.OPENED,
            )
        val updatedCoupon =
            Coupon(
                couponId = couponId,
                description = "10000원 할인 쿠폰",
                discountAmount = 10000L,
                stock = 99,
                couponStatus = CouponStatus.OPENED,
            )

        whenever(couponRepository.findByCouponId(couponId)).thenReturn(originalCoupon)
        whenever(couponRepository.save(any<Coupon>())).thenReturn(updatedCoupon)

        // when
        val result = couponService.issueCoupon(couponId)

        // then
        assertEquals(updatedCoupon, result)
        verify(couponRepository).findByCouponId(couponId)
        verify(couponRepository).save(originalCoupon)
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰으로 재고 차감 시 예외 발생")
    fun `존재하지 않는 쿠폰으로 재고 차감 시 예외 발생`() {
        // given
        val couponId = 999L
        whenever(couponRepository.findByCouponId(couponId)).thenReturn(null)

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                couponService.issueCoupon(couponId)
            }
        assertTrue(exception.message!!.contains("존재하지 않는 쿠폰입니다"))

        verify(couponRepository).findByCouponId(couponId)
        verify(couponRepository, times(0)).save(any())
    }
}
