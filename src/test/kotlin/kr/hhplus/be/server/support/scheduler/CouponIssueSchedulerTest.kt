package kr.hhplus.be.server.support.scheduler

import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest
import kr.hhplus.be.server.core.coupon.domain.UserCoupon
import kr.hhplus.be.server.core.coupon.service.CouponIssueQueueService
import kr.hhplus.be.server.facade.coupon.CouponIssueFacade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
@DisplayName("CouponIssueScheduler 단위 테스트")
class CouponIssueSchedulerTest {

    @Mock
    private lateinit var couponIssueQueueService: CouponIssueQueueService

    @Mock
    private lateinit var couponIssueFacade: CouponIssueFacade

    @InjectMocks
    private lateinit var couponIssueScheduler: CouponIssueScheduler

    @BeforeEach
    fun setup() {
        clearInvocations(couponIssueQueueService, couponIssueFacade)
    }

    @Test
    @DisplayName("스케줄러 처리 성공 - 대기열에 요청이 있는 경우")
    fun `스케줄러 처리 성공 - 대기열에 요청이 있는 경우`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest.create(userId, couponId)
        val userCoupon = UserCoupon.issueCoupon(userId, couponId)

        whenever(couponIssueQueueService.getNextCouponIssueRequest()).thenReturn(request)
        whenever(couponIssueFacade.validateRequest(request)).thenReturn(true)
        whenever(couponIssueFacade.processIssueRequest(request)).thenReturn(userCoupon)

        // when
        couponIssueScheduler.processCouponIssue()

        // then
        verify(couponIssueQueueService).getNextCouponIssueRequest()
        verify(couponIssueFacade).validateRequest(request)
        verify(couponIssueFacade).processIssueRequest(request)
    }

    @Test
    @DisplayName("스케줄러 처리 - 대기열이 비어있는 경우")
    fun `스케줄러 처리 - 대기열이 비어있는 경우`() {
        // given
        whenever(couponIssueQueueService.getNextCouponIssueRequest()).thenReturn(null)

        // when
        couponIssueScheduler.processCouponIssue()

        // then
        verify(couponIssueQueueService).getNextCouponIssueRequest()
        verify(couponIssueFacade, never()).validateRequest(any())
        verify(couponIssueFacade, never()).processIssueRequest(any())
    }

    @Test
    @DisplayName("스케줄러 처리 - 요청 검증 실패")
    fun `스케줄러 처리 - 요청 검증 실패`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest.create(userId, couponId)

        whenever(couponIssueQueueService.getNextCouponIssueRequest()).thenReturn(request)
        whenever(couponIssueFacade.validateRequest(request)).thenReturn(false)

        // when
        couponIssueScheduler.processCouponIssue()

        // then
        verify(couponIssueQueueService).getNextCouponIssueRequest()
        verify(couponIssueFacade).validateRequest(request)
        verify(couponIssueFacade, never()).processIssueRequest(any())
    }

    @Test
    @DisplayName("스케줄러 처리 - 중복 발급 예외 처리")
    fun `스케줄러 처리 - 중복 발급 예외 처리`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest.create(userId, couponId)

        whenever(couponIssueQueueService.getNextCouponIssueRequest()).thenReturn(request)
        whenever(couponIssueFacade.validateRequest(request)).thenReturn(true)
        whenever(couponIssueFacade.processIssueRequest(request))
            .thenThrow(IllegalStateException("이미 발급받은 쿠폰입니다. 사용자 ID: $userId, 쿠폰 ID: $couponId"))

        // when
        couponIssueScheduler.processCouponIssue()

        // then
        verify(couponIssueQueueService).getNextCouponIssueRequest()
        verify(couponIssueFacade).validateRequest(request)
        verify(couponIssueFacade).processIssueRequest(request)
        // 예외가 발생해도 스케줄러는 중단되지 않아야 함
    }

    @Test
    @DisplayName("스케줄러 처리 - 재고 부족 예외 처리")
    fun `스케줄러 처리 - 재고 부족 예외 처리`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest.create(userId, couponId)

        whenever(couponIssueQueueService.getNextCouponIssueRequest()).thenReturn(request)
        whenever(couponIssueFacade.validateRequest(request)).thenReturn(true)
        whenever(couponIssueFacade.processIssueRequest(request))
            .thenThrow(IllegalArgumentException("쿠폰 재고가 부족합니다. 쿠폰 ID: $couponId"))

        // when
        couponIssueScheduler.processCouponIssue()

        // then
        verify(couponIssueQueueService).getNextCouponIssueRequest()
        verify(couponIssueFacade).validateRequest(request)
        verify(couponIssueFacade).processIssueRequest(request)
        // 예외가 발생해도 스케줄러는 중단되지 않아야 함
    }

    @Test
    @DisplayName("스케줄러 처리 - 예상치 못한 예외 처리")
    fun `스케줄러 처리 - 예상치 못한 예외 처리`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest.create(userId, couponId)

        whenever(couponIssueQueueService.getNextCouponIssueRequest()).thenReturn(request)
        whenever(couponIssueFacade.validateRequest(request)).thenReturn(true)
        whenever(couponIssueFacade.processIssueRequest(request))
            .thenThrow(RuntimeException("예상치 못한 오류"))

        // when
        couponIssueScheduler.processCouponIssue()

        // then
        verify(couponIssueQueueService).getNextCouponIssueRequest()
        verify(couponIssueFacade).validateRequest(request)
        verify(couponIssueFacade).processIssueRequest(request)
        // 예외가 발생해도 스케줄러는 중단되지 않아야 함
    }

    @Test
    @DisplayName("스케줄러 전체 처리 예외 - 대기열 조회 실패")
    fun `스케줄러 전체 처리 예외 - 대기열 조회 실패`() {
        // given
        whenever(couponIssueQueueService.getNextCouponIssueRequest())
            .thenThrow(RuntimeException("Redis 연결 오류"))

        // when
        couponIssueScheduler.processCouponIssue()

        // then
        verify(couponIssueQueueService).getNextCouponIssueRequest()
        verify(couponIssueFacade, never()).validateRequest(any())
        verify(couponIssueFacade, never()).processIssueRequest(any())
        // 전체 스케줄러가 예외로 중단되지 않아야 함
    }
}
