package kr.hhplus.be.server.core.coupon.service

import kr.hhplus.be.server.core.coupon.domain.Coupon
import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest
import kr.hhplus.be.server.core.coupon.domain.CouponStatus
import kr.hhplus.be.server.core.coupon.repository.CouponRepository
import kr.hhplus.be.server.core.coupon.service.CouponIssueQueueServiceInterface
import kr.hhplus.be.server.fake.lock.FakeDistributedLockManager
import kr.hhplus.be.server.support.lock.DistributedLockManagerInterface
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

    private lateinit var distributedLockManager: DistributedLockManagerInterface
    private lateinit var couponService: CouponService

    @BeforeEach
    fun setup() {
        distributedLockManager = FakeDistributedLockManager()
        // Mock된 의존성들로 CouponService 생성 (테스트에서는 실제 통합 기능 테스트하지 않음)
        couponService = CouponService(
            couponRepository, 
            distributedLockManager,
            mock(), // userCouponService
            mock<CouponIssueQueueServiceInterface>(), // couponIssueQueueService  
            mock()  // userService
        )
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

    // 쿠폰 재고 차감 테스트는 이제 issueCoupon(request) 메서드를 통한 통합 테스트로 대체됨

    // Note: issueCoupon 메서드는 이제 CouponIssueRequest를 받으므로 별도의 통합 테스트에서 검증

    // Note: issueCoupon 메서드는 이제 CouponIssueRequest를 받으므로 별도의 통합 테스트에서 검증
}
