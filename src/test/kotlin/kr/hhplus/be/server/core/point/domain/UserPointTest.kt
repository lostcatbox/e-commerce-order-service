package kr.hhplus.be.server.core.point.domain

import kr.hhplus.be.server.core.point.domain.UserPoint
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("UserPoint 도메인 모델 테스트")
class UserPointTest {
    @Test
    @DisplayName("정상적인 UserPoint 생성")
    fun `정상적인 UserPoint 생성`() {
        // given
        val userId = 1L
        val balance = 100000L

        // when
        val userPoint = UserPoint(userId, balance)

        // then
        assertEquals(userId, userPoint.userId)
        assertEquals(balance, userPoint.getBalance())
        assertTrue(userPoint.getLastUpdatedAt() > 0)
    }

    @Test
    @DisplayName("잔액이 최소값보다 작으면 예외 발생")
    fun `잔액이 최소값보다 작으면 예외 발생`() {
        // given
        val userId = 1L
        val invalidBalance = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                UserPoint(userId, invalidBalance)
            }
        assertTrue(exception.message!!.contains("잔액은 0 이상이어야 합니다"))
    }

    @Test
    @DisplayName("잔액이 최대값보다 크면 예외 발생")
    fun `잔액이 최대값보다 크면 예외 발생`() {
        // given
        val userId = 1L
        val invalidBalance = 2_000_001L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                UserPoint(userId, invalidBalance)
            }
        assertTrue(exception.message!!.contains("잔액은 2000000 이하여야 합니다"))
    }

    @Test
    @DisplayName("정상적인 포인트 충전")
    fun `정상적인 포인트 충전`() {
        // given
        val userPoint = UserPoint(1L, 50000L)
        val chargeAmount = 30000L

        // when
        val originalUpdatedAt = userPoint.getLastUpdatedAt()
        userPoint.charge(chargeAmount)

        // then
        assertEquals(80000L, userPoint.getBalance())
        assertTrue(userPoint.getLastUpdatedAt() >= originalUpdatedAt)
    }

    @Test
    @DisplayName("충전 금액이 최소값보다 작으면 예외 발생")
    fun `충전 금액이 최소값보다 작으면 예외 발생`() {
        // given
        val userPoint = UserPoint(1L, 50000L)
        val invalidAmount = 0L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                userPoint.charge(invalidAmount)
            }
        assertTrue(exception.message!!.contains("충전 금액은 1 이상이어야 합니다"))
    }

    @Test
    @DisplayName("충전 금액이 최대값보다 크면 예외 발생")
    fun `충전 금액이 최대값보다 크면 예외 발생`() {
        // given
        val userPoint = UserPoint(1L, 50000L)
        val invalidAmount = 2_000_001L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                userPoint.charge(invalidAmount)
            }
        assertTrue(exception.message!!.contains("충전 금액은 2000000 이하여야 합니다"))
    }

    @Test
    @DisplayName("충전 후 잔액이 최대값을 초과하면 예외 발생")
    fun `충전 후 잔액이 최대값을 초과하면 예외 발생`() {
        // given
        val userPoint = UserPoint(1L, 1_900_000L)
        val chargeAmount = 200_000L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                userPoint.charge(chargeAmount)
            }
        assertTrue(exception.message!!.contains("잔액이 2000000 초과할 수 없습니다"))
    }

    @Test
    @DisplayName("정상적인 포인트 사용")
    fun `정상적인 포인트 사용`() {
        // given
        val userPoint = UserPoint(1L, 50000L)
        val useAmount = 30000L

        // when
        val originalUpdatedAt = userPoint.getLastUpdatedAt()
        userPoint.use(useAmount)

        // then
        assertEquals(20000L, userPoint.getBalance())
        assertTrue(userPoint.getLastUpdatedAt() >= originalUpdatedAt)
    }

    @Test
    @DisplayName("사용 금액이 최소값보다 작으면 예외 발생")
    fun `사용 금액이 최소값보다 작으면 예외 발생`() {
        // given
        val userPoint = UserPoint(1L, 50000L)
        val invalidAmount = 0L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                userPoint.use(invalidAmount)
            }
        assertTrue(exception.message!!.contains("사용 금액은 1 이상이어야 합니다"))
    }

    @Test
    @DisplayName("사용 금액이 최대값보다 크면 예외 발생")
    fun `사용 금액이 최대값보다 크면 예외 발생`() {
        // given
        val userPoint = UserPoint(1L, 50000L)
        val invalidAmount = 2_000_001L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                userPoint.use(invalidAmount)
            }
        assertTrue(exception.message!!.contains("사용 금액은 2000000 이하여야 합니다"))
    }

    @Test
    @DisplayName("사용 후 잔액이 최소값보다 작으면 예외 발생")
    fun `사용 후 잔액이 최소값보다 작으면 예외 발생`() {
        // given
        val userPoint = UserPoint(1L, 10000L)
        val useAmount = 20000L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                userPoint.use(useAmount)
            }
        assertTrue(exception.message!!.contains("잔액이 0 미만이 될 수 없습니다"))
    }

    @Test
    @DisplayName("경계값 테스트 - 최대 잔액으로 생성")
    fun `경계값 테스트 - 최대 잔액으로 생성`() {
        // given
        val userId = 1L
        val maxBalance = UserPoint.MAX_BALANCE

        // when
        val userPoint = UserPoint(userId, maxBalance)

        // then
        assertEquals(maxBalance, userPoint.getBalance())
    }

    @Test
    @DisplayName("경계값 테스트 - 최소 잔액으로 생성")
    fun `경계값 테스트 - 최소 잔액으로 생성`() {
        // given
        val userId = 1L
        val minBalance = UserPoint.MIN_BALANCE

        // when
        val userPoint = UserPoint(userId, minBalance)

        // then
        assertEquals(minBalance, userPoint.getBalance())
    }

    @Test
    @DisplayName("경계값 테스트 - 최대 금액 충전")
    fun `경계값 테스트 - 최대 금액 충전`() {
        // given
        val userPoint = UserPoint(1L, 0L)
        val maxChargeAmount = UserPoint.MAX_CHARGE_AMOUNT

        // when
        userPoint.charge(maxChargeAmount)

        // then
        assertEquals(maxChargeAmount, userPoint.getBalance())
    }

    @Test
    @DisplayName("경계값 테스트 - 최소 금액 충전")
    fun `경계값 테스트 - 최소 금액 충전`() {
        // given
        val userPoint = UserPoint(1L, 0L)
        val minChargeAmount = UserPoint.MIN_CHARGE_AMOUNT

        // when
        userPoint.charge(minChargeAmount)

        // then
        assertEquals(minChargeAmount, userPoint.getBalance())
    }
}
