package kr.hhplus.be.server.point.service

import kr.hhplus.be.server.point.domain.UserPoint
import kr.hhplus.be.server.point.repository.UserPointRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("PointService 비즈니스 로직 테스트")
class PointServiceTest {
    @Mock
    private lateinit var userPointRepository: UserPointRepository

    @InjectMocks
    private lateinit var pointService: PointService

    @BeforeEach
    fun setup() {
        clearInvocations(userPointRepository)
    }

    @Test
    @DisplayName("기존 사용자 포인트 잔액 조회 성공")
    fun `기존 사용자 포인트 잔액 조회 성공`() {
        // given
        val userId = 1L
        val expectedUserPoint = UserPoint(userId, 50000L)
        whenever(userPointRepository.findByUserId(userId)).thenReturn(expectedUserPoint)

        // when
        val result = pointService.getPointBalance(userId)

        // then
        assertEquals(expectedUserPoint, result)
        verify(userPointRepository).findByUserId(userId)
    }

    @Test
    @DisplayName("신규 사용자 포인트 잔액 조회 - 0 잔액 반환")
    fun `신규 사용자 포인트 잔액 조회 - 0 잔액 반환`() {
        // given
        val userId = 1L
        whenever(userPointRepository.findByUserId(userId)).thenReturn(null)

        // when
        val result = pointService.getPointBalance(userId)

        // then
        assertEquals(userId, result.userId)
        assertEquals(0L, result.getBalance())
        verify(userPointRepository).findByUserId(userId)
    }

    @Test
    @DisplayName("유효하지 않은 사용자 ID로 잔액 조회 시 예외 발생")
    fun `유효하지 않은 사용자 ID로 잔액 조회 시 예외 발생`() {
        // given
        val invalidUserId = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                pointService.getPointBalance(invalidUserId)
            }
        assertTrue(exception.message!!.contains("사용자 ID는 0 이상 이여야 합니다."))
        verify(userPointRepository, never()).findByUserId(any())
    }

    @Test
    @DisplayName("기존 사용자 포인트 충전 성공")
    fun `기존 사용자 포인트 충전 성공`() {
        // given
        val userId = 1L
        val chargeAmount = 30000L
        val existingUserPoint = UserPoint(userId, 50000L)
        val expectedChargedUserPoint = UserPoint(userId, 80000L)

        whenever(userPointRepository.findByUserId(userId)).thenReturn(existingUserPoint)
        whenever(userPointRepository.save(any<UserPoint>())).thenReturn(expectedChargedUserPoint)

        // when
        val result = pointService.chargePoint(userId, chargeAmount)

        // then
        assertEquals(80000L, result.getBalance())
        assertEquals(userId, result.userId)

        verify(userPointRepository).findByUserId(userId)
        verify(userPointRepository).save(any<UserPoint>())
    }

    @Test
    @DisplayName("신규 사용자 포인트 충전 성공")
    fun `신규 사용자 포인트 충전 성공`() {
        // given
        val userId = 1L
        val chargeAmount = 30000L
        val expectedChargedUserPoint = UserPoint(userId, 30000L)

        whenever(userPointRepository.findByUserId(userId)).thenReturn(null)
        whenever(userPointRepository.save(any<UserPoint>())).thenReturn(expectedChargedUserPoint)

        // when
        val result = pointService.chargePoint(userId, chargeAmount)

        // then
        assertEquals(30000L, result.getBalance())
        assertEquals(userId, result.userId)

        verify(userPointRepository).findByUserId(userId)
        verify(userPointRepository).save(any<UserPoint>())
    }

    @Test
    @DisplayName("유효하지 않은 사용자 ID로 포인트 충전 시 예외 발생")
    fun `유효하지 않은 사용자 ID로 포인트 충전 시 예외 발생`() {
        // given
        val invalidUserId = -1L
        val chargeAmount = 30000L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                pointService.chargePoint(invalidUserId, chargeAmount)
            }
        assertTrue(exception.message!!.contains("사용자 ID는 0 이상 이여야 합니다."))
        verify(userPointRepository, never()).findByUserId(any())
        verify(userPointRepository, never()).save(any())
    }

    @Test
    @DisplayName("유효하지 않은 충전 금액으로 포인트 충전 시 예외 발생")
    fun `유효하지 않은 충전 금액으로 포인트 충전 시 예외 발생`() {
        // given
        val userId = 1L
        val invalidChargeAmount = 0L
        val existingUserPoint = UserPoint(userId, 50000L)

        whenever(userPointRepository.findByUserId(userId)).thenReturn(existingUserPoint)

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                pointService.chargePoint(userId, invalidChargeAmount)
            }
        assertTrue(exception.message!!.contains("충전 금액은 1 이상이어야 합니다"))
        verify(userPointRepository).findByUserId(userId)
        verify(userPointRepository, never()).save(any())
    }

    @Test
    @DisplayName("충전 후 최대 잔액 초과 시 예외 발생")
    fun `충전 후 최대 잔액 초과 시 예외 발생`() {
        // given
        val userId = 1L
        val chargeAmount = 200000L
        val existingUserPoint = UserPoint(userId, 1900000L)

        whenever(userPointRepository.findByUserId(userId)).thenReturn(existingUserPoint)

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                pointService.chargePoint(userId, chargeAmount)
            }
        assertTrue(exception.message!!.contains("잔액이 2000000 초과할 수 없습니다"))
        verify(userPointRepository).findByUserId(userId)
        verify(userPointRepository, never()).save(any())
    }

    @Test
    @DisplayName("최대 금액 충전 성공")
    fun `최대 금액 충전 성공`() {
        // given
        val userId = 1L
        val maxChargeAmount = UserPoint.MAX_CHARGE_AMOUNT
        val existingUserPoint = UserPoint(userId, 0L)
        val expectedChargedUserPoint = UserPoint(userId, maxChargeAmount)

        whenever(userPointRepository.findByUserId(userId)).thenReturn(existingUserPoint)
        whenever(userPointRepository.save(any<UserPoint>())).thenReturn(expectedChargedUserPoint)

        // when
        val result = pointService.chargePoint(userId, maxChargeAmount)

        // then
        assertEquals(maxChargeAmount, result.getBalance())
        verify(userPointRepository).save(any<UserPoint>())
    }

    @Test
    @DisplayName("최소 금액 충전 성공")
    fun `최소 금액 충전 성공`() {
        // given
        val userId = 1L
        val minChargeAmount = UserPoint.MIN_CHARGE_AMOUNT
        val existingUserPoint = UserPoint(userId, 0L)
        val expectedChargedUserPoint = UserPoint(userId, minChargeAmount)

        whenever(userPointRepository.findByUserId(userId)).thenReturn(existingUserPoint)
        whenever(userPointRepository.save(any<UserPoint>())).thenReturn(expectedChargedUserPoint)

        // when
        val result = pointService.chargePoint(userId, minChargeAmount)

        // then
        assertEquals(minChargeAmount, result.getBalance())
        verify(userPointRepository).save(any<UserPoint>())
    }
}
