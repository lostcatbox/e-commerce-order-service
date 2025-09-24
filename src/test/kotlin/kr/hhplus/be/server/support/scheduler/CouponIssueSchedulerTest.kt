package kr.hhplus.be.server.support.scheduler

import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest
import kr.hhplus.be.server.core.coupon.domain.UserCoupon
import kr.hhplus.be.server.core.coupon.service.CouponServiceInterface
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
@DisplayName("CouponIssueScheduler 단위 테스트 (CouponService 통합)")
class CouponIssueSchedulerTest {

    @Mock
    private lateinit var couponService: CouponServiceInterface

    @InjectMocks
    private lateinit var couponIssueScheduler: CouponIssueScheduler

    @BeforeEach
    fun setup() {
        clearInvocations(couponService)
    }

    @Test
    @DisplayName("스케줄러 처리 성공 - 대기열에 요청이 있는 경우")
    fun `스케줄러 처리 성공 - 대기열에 요청이 있는 경우`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest.create(userId, couponId)
        val userCoupon = UserCoupon.issueCoupon(userId, couponId)

        whenever(couponService.getNextCouponIssueRequest()).thenReturn(request)
        whenever(couponService.validateRequest(request)).thenReturn(true)
        whenever(couponService.issueCoupon(request)).thenReturn(userCoupon)

        // when
        couponIssueScheduler.processCouponIssue()

        // then
        verify(couponService).getNextCouponIssueRequest()
        verify(couponService).validateRequest(request)
        verify(couponService).issueCoupon(request)
    }

    @Test
    @DisplayName("스케줄러 처리 - 대기열이 비어있는 경우")
    fun `스케줄러 처리 - 대기열이 비어있는 경우`() {
        // given
        whenever(couponService.getNextCouponIssueRequest()).thenReturn(null)

        // when
        couponIssueScheduler.processCouponIssue()

        // then
        verify(couponService).getNextCouponIssueRequest()
        verify(couponService, never()).validateRequest(any())
        verify(couponService, never()).issueCoupon(any())
    }

    @Test
    @DisplayName("스케줄러 처리 - 요청 검증 실패")
    fun `스케줄러 처리 - 요청 검증 실패`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest.create(userId, couponId)

        whenever(couponService.getNextCouponIssueRequest()).thenReturn(request)
        whenever(couponService.validateRequest(request)).thenReturn(false)

        // when
        couponIssueScheduler.processCouponIssue()

        // then
        verify(couponService).getNextCouponIssueRequest()
        verify(couponService).validateRequest(request)
        verify(couponService, never()).issueCoupon(any())
    }

    @Test
    @DisplayName("스케줄러 처리 - 중복 발급 예외 처리")
    fun `스케줄러 처리 - 중복 발급 예외 처리`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest.create(userId, couponId)

        whenever(couponService.getNextCouponIssueRequest()).thenReturn(request)
        whenever(couponService.validateRequest(request)).thenReturn(true)
        whenever(couponService.issueCoupon(request))
            .thenThrow(IllegalStateException("이미 발급받은 쿠폰입니다. 사용자 ID: $userId, 쿠폰 ID: $couponId"))

        // when
        couponIssueScheduler.processCouponIssue()

        // then
        verify(couponService).getNextCouponIssueRequest()
        verify(couponService).validateRequest(request)
        verify(couponService).issueCoupon(request)
        // 예외가 발생해도 스케줄러는 중단되지 않아야 함
    }

    @Test
    @DisplayName("스케줄러 처리 - 재고 부족 예외 처리")
    fun `스케줄러 처리 - 재고 부족 예외 처리`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest.create(userId, couponId)

        whenever(couponService.getNextCouponIssueRequest()).thenReturn(request)
        whenever(couponService.validateRequest(request)).thenReturn(true)
        whenever(couponService.issueCoupon(request))
            .thenThrow(IllegalArgumentException("쿠폰 재고가 부족합니다. 쿠폰 ID: $couponId"))

        // when
        couponIssueScheduler.processCouponIssue()

        // then
        verify(couponService).getNextCouponIssueRequest()
        verify(couponService).validateRequest(request)
        verify(couponService).issueCoupon(request)
        // 예외가 발생해도 스케줄러는 중단되지 않아야 함
    }

    @Test
    @DisplayName("스케줄러 처리 - 예상치 못한 예외 처리")
    fun `스케줄러 처리 - 예상치 못한 예외 처리`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest.create(userId, couponId)

        whenever(couponService.getNextCouponIssueRequest()).thenReturn(request)
        whenever(couponService.validateRequest(request)).thenReturn(true)
        whenever(couponService.issueCoupon(request))
            .thenThrow(RuntimeException("예상치 못한 오류"))

        // when
        couponIssueScheduler.processCouponIssue()

        // then
        verify(couponService).getNextCouponIssueRequest()
        verify(couponService).validateRequest(request)
        verify(couponService).issueCoupon(request)
        // 예외가 발생해도 스케줄러는 중단되지 않아야 함
    }

    @Test
    @DisplayName("스케줄러 전체 처리 예외 - 대기열 조회 실패")
    fun `스케줄러 전체 처리 예외 - 대기열 조회 실패`() {
        // given
        whenever(couponService.getNextCouponIssueRequest())
            .thenThrow(RuntimeException("Redis 연결 오류"))

        // when
        couponIssueScheduler.processCouponIssue()

        // then
        verify(couponService).getNextCouponIssueRequest()
        verify(couponService, never()).validateRequest(any())
        verify(couponService, never()).issueCoupon(any())
        // 전체 스케줄러가 예외로 중단되지 않아야 함
    }
}
