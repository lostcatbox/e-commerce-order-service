package kr.hhplus.be.server.facade.coupon

import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest
import kr.hhplus.be.server.core.coupon.domain.UserCoupon
import kr.hhplus.be.server.core.coupon.domain.UserCouponStatus
import kr.hhplus.be.server.core.coupon.service.CouponServiceInterface
import kr.hhplus.be.server.core.coupon.service.UserCouponServiceInterface
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
@DisplayName("CouponIssueFacade 단위 테스트")
class CouponIssueFacadeTest {

    @Mock
    private lateinit var couponService: CouponServiceInterface

    @Mock
    private lateinit var userCouponService: UserCouponServiceInterface

    @InjectMocks
    private lateinit var couponIssueFacade: CouponIssueFacade

    @BeforeEach
    fun setup() {
        clearInvocations(couponService, userCouponService)
    }

    @Test
    @DisplayName("쿠폰 발급 요청 처리 성공")
    fun `쿠폰 발급 요청 처리 성공`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest.create(userId, couponId)
        val expectedUserCoupon = UserCoupon.issueCoupon(userId, couponId)

        whenever(userCouponService.createUserCoupon(userId, couponId)).thenReturn(expectedUserCoupon)

        // when
        val result = couponIssueFacade.processIssueRequest(request)

        // then
        assertEquals(userId, result.userId)
        assertEquals(couponId, result.couponId)
        assertEquals(UserCouponStatus.ISSUED, result.getStatus())

        verify(couponService).issueCoupon(couponId)
        verify(userCouponService).createUserCoupon(userId, couponId)
    }

    @Test
    @DisplayName("중복 발급 시 예외 발생")
    fun `중복 발급 시 예외 발생`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest.create(userId, couponId)

        whenever(userCouponService.createUserCoupon(userId, couponId))
            .thenThrow(IllegalStateException("이미 발급받은 쿠폰입니다. 사용자 ID: $userId, 쿠폰 ID: $couponId"))

        // when & then
        val exception = assertThrows<IllegalStateException> {
            couponIssueFacade.processIssueRequest(request)
        }

        assertTrue(exception.message!!.contains("이미 발급받은 쿠폰입니다"))
        verify(couponService).issueCoupon(couponId)
        verify(userCouponService).createUserCoupon(userId, couponId)
    }

    @Test
    @DisplayName("쿠폰 재고 부족 시 예외 발생")
    fun `쿠폰 재고 부족 시 예외 발생`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest.create(userId, couponId)

        whenever(couponService.issueCoupon(couponId))
            .thenThrow(IllegalArgumentException("쿠폰 재고가 부족합니다. 쿠폰 ID: $couponId"))

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            couponIssueFacade.processIssueRequest(request)
        }

        assertTrue(exception.message!!.contains("쿠폰 재고가 부족합니다"))
        verify(couponService).issueCoupon(couponId)
        verify(userCouponService, never()).createUserCoupon(any(), any())
    }

    @Test
    @DisplayName("요청 검증 성공")
    fun `요청 검증 성공`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest.create(userId, couponId)

        // when
        val result = couponIssueFacade.validateRequest(request)

        // then
        assertTrue(result)
    }

    @Test
    @DisplayName("유효하지 않은 사용자 ID로 요청 검증 실패")
    fun `유효하지 않은 사용자 ID로 요청 검증 실패`() {
        // given
        val invalidUserId = 0L
        val couponId = 100L
        val request = CouponIssueRequest(
            userId = invalidUserId,
            couponId = couponId,
            requestId = "test-request-id",
            timestamp = System.currentTimeMillis()
        )

        // when
        val result = couponIssueFacade.validateRequest(request)

        // then
        assertFalse(result)
    }

    @Test
    @DisplayName("유효하지 않은 쿠폰 ID로 요청 검증 실패")
    fun `유효하지 않은 쿠폰 ID로 요청 검증 실패`() {
        // given
        val userId = 1L
        val invalidCouponId = -1L
        val request = CouponIssueRequest(
            userId = userId,
            couponId = invalidCouponId,
            requestId = "test-request-id",
            timestamp = System.currentTimeMillis()
        )

        // when
        val result = couponIssueFacade.validateRequest(request)

        // then
        assertFalse(result)
    }

    @Test
    @DisplayName("빈 요청 ID로 요청 검증 실패")
    fun `빈 요청 ID로 요청 검증 실패`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest(
            userId = userId,
            couponId = couponId,
            requestId = "",
            timestamp = System.currentTimeMillis()
        )

        // when
        val result = couponIssueFacade.validateRequest(request)

        // then
        assertFalse(result)
    }

    @Test
    @DisplayName("유효하지 않은 타임스탬프로 요청 검증 실패")
    fun `유효하지 않은 타임스탬프로 요청 검증 실패`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest(
            userId = userId,
            couponId = couponId,
            requestId = "test-request-id",
            timestamp = 0L
        )

        // when
        val result = couponIssueFacade.validateRequest(request)

        // then
        assertFalse(result)
    }
}
