package kr.hhplus.be.server.core.coupon.service

import kr.hhplus.be.server.core.coupon.domain.UserCoupon
import kr.hhplus.be.server.core.coupon.domain.UserCouponStatus
import kr.hhplus.be.server.core.coupon.repository.UserCouponRepository
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
@DisplayName("UserCouponService 비즈니스 로직 테스트")
class UserCouponServiceTest {

    @Mock
    private lateinit var userCouponRepository: UserCouponRepository

    @InjectMocks
    private lateinit var userCouponService: UserCouponService

    @BeforeEach
    fun setup() {
        clearInvocations(userCouponRepository)
    }

    @Test
    @DisplayName("사용자 쿠폰 발급 성공")
    fun `사용자 쿠폰 발급 성공`() {
        // given
        val userId = 1L
        val couponId = 100L
        val expectedUserCoupon = UserCoupon.issueCoupon(userId, couponId)

        whenever(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(null)
        whenever(userCouponRepository.save(any<UserCoupon>())).thenReturn(expectedUserCoupon)

        // when
        val result = userCouponService.createUserCoupon(userId, couponId)

        // then
        assertEquals(userId, result.userId)
        assertEquals(couponId, result.couponId)
        assertEquals(UserCouponStatus.ISSUED, result.getStatus())

        verify(userCouponRepository).findByUserIdAndCouponId(userId, couponId)
        verify(userCouponRepository).save(any<UserCoupon>())
    }

    @Test
    @DisplayName("이미 발급받은 쿠폰으로 재발급 시 예외 발생")
    fun `이미 발급받은 쿠폰으로 재발급 시 예외 발생`() {
        // given
        val userId = 1L
        val couponId = 100L
        val existingUserCoupon = UserCoupon.issueCoupon(userId, couponId)

        whenever(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(existingUserCoupon)

        // when & then
        val exception = assertThrows<IllegalStateException> {
            userCouponService.createUserCoupon(userId, couponId)
        }
        assertTrue(exception.message!!.contains("이미 발급받은 쿠폰입니다"))
        assertTrue(exception.message!!.contains("사용자 ID: $userId"))
        assertTrue(exception.message!!.contains("쿠폰 ID: $couponId"))

        verify(userCouponRepository).findByUserIdAndCouponId(userId, couponId)
        verify(userCouponRepository, never()).save(any())
    }

    @Test
    @DisplayName("유효하지 않은 사용자 ID로 쿠폰 발급 시 예외 발생")
    fun `유효하지 않은 사용자 ID로 쿠폰 발급 시 예외 발생`() {
        // given
        val invalidUserId = 0L
        val couponId = 100L

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            userCouponService.createUserCoupon(invalidUserId, couponId)
        }
        assertTrue(exception.message!!.contains("사용자 ID는 0보다 커야 합니다"))

        verify(userCouponRepository, never()).findByUserIdAndCouponId(any(), any())
        verify(userCouponRepository, never()).save(any())
    }

    @Test
    @DisplayName("유효하지 않은 쿠폰 ID로 쿠폰 발급 시 예외 발생")
    fun `유효하지 않은 쿠폰 ID로 쿠폰 발급 시 예외 발생`() {
        // given
        val userId = 1L
        val invalidCouponId = -1L

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            userCouponService.createUserCoupon(userId, invalidCouponId)
        }
        assertTrue(exception.message!!.contains("쿠폰 ID는 0보다 커야 합니다"))

        verify(userCouponRepository, never()).findByUserIdAndCouponId(any(), any())
        verify(userCouponRepository, never()).save(any())
    }

    @Test
    @DisplayName("사용자 쿠폰 목록 조회 성공")
    fun `사용자 쿠폰 목록 조회 성공`() {
        // given
        val userId = 1L
        val expectedUserCoupons = listOf(
            UserCoupon.issueCoupon(userId, 100L),
            UserCoupon.issueCoupon(userId, 200L)
        )

        whenever(userCouponRepository.findByUserId(userId)).thenReturn(expectedUserCoupons)

        // when
        val result = userCouponService.getUserCoupons(userId)

        // then
        assertEquals(2, result.size)
        assertEquals(expectedUserCoupons, result)
        verify(userCouponRepository).findByUserId(userId)
    }

    @Test
    @DisplayName("사용자 쿠폰 목록이 비어있는 경우")
    fun `사용자 쿠폰 목록이 비어있는 경우`() {
        // given
        val userId = 1L
        whenever(userCouponRepository.findByUserId(userId)).thenReturn(emptyList())

        // when
        val result = userCouponService.getUserCoupons(userId)

        // then
        assertTrue(result.isEmpty())
        verify(userCouponRepository).findByUserId(userId)
    }

    @Test
    @DisplayName("유효하지 않은 사용자 ID로 쿠폰 목록 조회 시 예외 발생")
    fun `유효하지 않은 사용자 ID로 쿠폰 목록 조회 시 예외 발생`() {
        // given
        val invalidUserId = 0L

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            userCouponService.getUserCoupons(invalidUserId)
        }
        assertTrue(exception.message!!.contains("사용자 ID는 0보다 커야 합니다"))

        verify(userCouponRepository, never()).findByUserId(any())
    }

    @Test
    @DisplayName("사용 가능한 쿠폰 목록 조회 성공")
    fun `사용 가능한 쿠폰 목록 조회 성공`() {
        // given
        val userId = 1L
        val usableCoupons = listOf(
            UserCoupon.issueCoupon(userId, 100L),
            UserCoupon.issueCoupon(userId, 200L)
        )

        whenever(userCouponRepository.findUsableCouponsByUserId(userId)).thenReturn(usableCoupons)

        // when
        val result = userCouponService.getUsableCoupons(userId)

        // then
        assertEquals(2, result.size)
        assertEquals(usableCoupons, result)
        assertTrue(result.all { it.isUsable() })
        verify(userCouponRepository).findUsableCouponsByUserId(userId)
    }

    @Test
    @DisplayName("사용자 쿠폰 사용 성공")
    fun `사용자 쿠폰 사용 성공`() {
        // given
        val userCouponId = 1L
        val userId = 1L
        val couponId = 100L
        val userCoupon = UserCoupon.issueCoupon(userId, couponId)
        val usedUserCoupon = UserCoupon.issueCoupon(userId, couponId).apply { use() }

        whenever(userCouponRepository.findByUserCouponId(userCouponId)).thenReturn(userCoupon)
        whenever(userCouponRepository.save(any<UserCoupon>())).thenReturn(usedUserCoupon)

        // when
        val result = userCouponService.useCoupon(userCouponId)

        // then
        assertEquals(UserCouponStatus.USED, result.getStatus())
        assertNotNull(result.getUsedAt())

        verify(userCouponRepository).findByUserCouponId(userCouponId)
        verify(userCouponRepository).save(userCoupon)
    }

    @Test
    @DisplayName("존재하지 않는 사용자 쿠폰 사용 시 예외 발생")
    fun `존재하지 않는 사용자 쿠폰 사용 시 예외 발생`() {
        // given
        val userCouponId = 999L
        whenever(userCouponRepository.findByUserCouponId(userCouponId)).thenReturn(null)

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            userCouponService.useCoupon(userCouponId)
        }
        assertTrue(exception.message!!.contains("존재하지 않는 사용자 쿠폰입니다"))
        assertTrue(exception.message!!.contains("999"))

        verify(userCouponRepository).findByUserCouponId(userCouponId)
        verify(userCouponRepository, never()).save(any())
    }

    @Test
    @DisplayName("유효하지 않은 사용자 쿠폰 ID로 사용 시 예외 발생")
    fun `유효하지 않은 사용자 쿠폰 ID로 사용 시 예외 발생`() {
        // given
        val invalidUserCouponId = 0L

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            userCouponService.useCoupon(invalidUserCouponId)
        }
        assertTrue(exception.message!!.contains("사용자 쿠폰 ID는 0보다 커야 합니다"))

        verify(userCouponRepository, never()).findByUserCouponId(any())
        verify(userCouponRepository, never()).save(any())
    }








}
