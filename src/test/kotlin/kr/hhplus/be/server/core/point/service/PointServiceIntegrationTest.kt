package kr.hhplus.be.server.core.point.service

import jakarta.persistence.EntityManager
import kr.hhplus.be.server.IntegrationTestSupport
import kr.hhplus.be.server.core.point.domain.UserPoint
import kr.hhplus.be.server.core.point.repository.UserPointRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("PointService 통합 테스트")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PointServiceIntegrationTest : IntegrationTestSupport() {
    @Autowired
    private lateinit var pointService: PointService

    @Autowired
    private lateinit var userPointRepository: UserPointRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private val testUserId = 100L
    private lateinit var testUserPoint: UserPoint

    @BeforeEach
    fun setUp() {
        // 테스트용 사용자 포인트 데이터 생성
        testUserPoint = UserPoint(userId = testUserId)
        testUserPoint.charge(50000L) // 초기 잔액 50,000원
        userPointRepository.save(testUserPoint)
    }

    @Test
    @DisplayName("사용자 포인트 잔액 조회 성공")
    @Transactional
    fun `사용자 포인트 잔액 조회 성공`() {
        // when
        val result = pointService.getPointBalance(testUserId)

        // then
        assertNotNull(result)
        assertEquals(testUserId, result.userId)
        assertEquals(50000L, result.getBalance())
    }

    @Test
    @DisplayName("신규 사용자 포인트 조회 - 0 잔액으로 반환")
    @Transactional
    fun `신규 사용자 포인트 조회 0 잔액으로 반환`() {
        // given
        val newUserId = 999L

        // when
        val result = pointService.getPointBalance(newUserId)

        // then
        assertNotNull(result)
        assertEquals(newUserId, result.userId)
        assertEquals(0L, result.getBalance())
    }

    @Test
    @DisplayName("잘못된 사용자 ID로 포인트 조회 시 예외 발생")
    @Transactional
    fun `잘못된 사용자 ID로 포인트 조회 시 예외 발생`() {
        // given
        val invalidUserId = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                pointService.getPointBalance(invalidUserId)
            }

        assertTrue(exception.message!!.contains("사용자 ID는 0 이상 이여야 합니다"))
    }

    @Test
    @DisplayName("사용자 포인트 충전")
    @Transactional
    fun `사용자 포인트 충전`() {
        // given
        val chargeAmount = 10000L
        val originalBalance = testUserPoint.getBalance()

        // when
        val result = pointService.chargePoint(testUserId, chargeAmount)

        // then
        assertNotNull(result)
        assertEquals(testUserId, result.userId)
        assertEquals(originalBalance + chargeAmount, result.getBalance())

// 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // DB에서 다시 조회하여 실제로 저장되었는지 확인
        val savedUserPoint = userPointRepository.findByUserId(testUserId)
        assertNotNull(savedUserPoint)
        assertEquals(originalBalance + chargeAmount, savedUserPoint!!.getBalance())
    }

    @Test
    @DisplayName("신규 사용자 포인트 충전 - 0에서 시작")
    @Transactional
    fun `신규 사용자 포인트 충전 0에서 시작`() {
        // given
        val newUserId = 200L
        val chargeAmount = 15000L

        // when
        val result = pointService.chargePoint(newUserId, chargeAmount)

        // then
        assertNotNull(result)
        assertEquals(newUserId, result.userId)
        assertEquals(chargeAmount, result.getBalance())

        // 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // DB에서 조회하여 저장되었는지 확인
        val savedUserPoint = userPointRepository.findByUserId(newUserId)
        assertNotNull(savedUserPoint)
        assertEquals(chargeAmount, savedUserPoint!!.getBalance())
    }

    @Test
    @DisplayName("최대 충전 금액 초과 시 예외 발생")
    @Transactional
    fun `최대 충전 금액 초과 시 예외 발생`() {
        // given
        val invalidChargeAmount = UserPoint.MAX_CHARGE_AMOUNT + 1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                pointService.chargePoint(testUserId, invalidChargeAmount)
            }

        assertTrue(exception.message!!.contains("충전 금액은"))
        assertTrue(exception.message!!.contains("이하여야 합니다"))
    }

    @Test
    @DisplayName("최소 충전 금액 미만 시 예외 발생")
    @Transactional
    fun `최소 충전 금액 미만 시 예외 발생`() {
        // given
        val invalidChargeAmount = 0L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                pointService.chargePoint(testUserId, invalidChargeAmount)
            }

        assertTrue(exception.message!!.contains("충전 금액은"))
        assertTrue(exception.message!!.contains("이상이어야 합니다"))
    }

    @Test
    @DisplayName("사용자 포인트 사용")
    @Transactional
    fun `사용자 포인트 사용`() {
        // given
        val useAmount = 20000L
        val originalBalance = testUserPoint.getBalance()

        // when
        val result = pointService.usePoint(testUserId, useAmount)

        // then
        assertNotNull(result)
        assertEquals(testUserId, result.userId)
        assertEquals(originalBalance - useAmount, result.getBalance())

        // DB에서 다시 조회하여 실제로 저장되었는지 확인
        val savedUserPoint = userPointRepository.findByUserId(testUserId)
        assertNotNull(savedUserPoint)
        assertEquals(originalBalance - useAmount, savedUserPoint!!.getBalance())
    }

    @Test
    @DisplayName("잔액 부족 시 포인트 사용 예외 발생")
    @Transactional
    fun `잔액 부족 시 포인트 사용 예외 발생`() {
        // given
        val currentBalance = testUserPoint.getBalance()
        val useAmount = currentBalance + 1000L // 잔액보다 많은 금액

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                pointService.usePoint(testUserId, useAmount)
            }

        assertTrue(exception.message!!.contains("잔액이"))
        assertTrue(exception.message!!.contains("미만이 될 수 없습니다"))

        // 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // 잔액이 변경되지 않았는지 확인
        val unchangedUserPoint = userPointRepository.findByUserId(testUserId)
        assertEquals(currentBalance, unchangedUserPoint!!.getBalance())
    }

    @Test
    @DisplayName("존재하지 않는 사용자 포인트 사용 시 예외 발생")
    @Transactional
    fun `존재하지 않는 사용자 포인트 사용 시 예외 발생`() {
        // given
        val nonExistentUserId = 999L
        val useAmount = 1000L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                pointService.usePoint(nonExistentUserId, useAmount)
            }

        assertTrue(exception.message!!.contains("존재하지 않는 사용자의 포인트입니다"))
        assertTrue(exception.message!!.contains(nonExistentUserId.toString()))
    }

    @Test
    @DisplayName("최소 사용 금액 미만 시 예외 발생")
    @Transactional
    fun `최소 사용 금액 미만 시 예외 발생`() {
        // given
        val invalidUseAmount = 0L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                pointService.usePoint(testUserId, invalidUseAmount)
            }

        assertTrue(exception.message!!.contains("사용 금액은"))
        assertTrue(exception.message!!.contains("이상이어야 합니다"))
    }

    @Test
    @DisplayName("최대 사용 금액 초과 시 예외 발생")
    @Transactional
    fun `최대 사용 금액 초과 시 예외 발생`() {
        // given
        val invalidUseAmount = UserPoint.MAX_USE_AMOUNT + 1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                pointService.usePoint(testUserId, invalidUseAmount)
            }

        assertTrue(exception.message!!.contains("사용 금액은"))
        assertTrue(exception.message!!.contains("이하여야 합니다"))
    }

    @Test
    @DisplayName("동시성 테스트 - 여러 스레드에서 동시에 포인트 충전(기대값 : 5번 동시 요청 시 실패없이 성공)")
    fun `동시성 테스트 여러 스레드에서 동시에 포인트 충전 (5번 동시 요청 시 실패없이 성공)`() {
        val beforeChargingPoint = testUserPoint.getBalance()

        val threadCount = 5
        val chargeAmountPerThread = 10000L // 스레드당 10,000원 충전
        val executor = Executors.newFixedThreadPool(threadCount)
        val countDownLatch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        // when - 동시에 포인트 충전 시도
        repeat(threadCount) {
            executor.submit {
                try {
                    pointService.chargePoint(testUserId, chargeAmountPerThread)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failureCount.incrementAndGet()
                } finally {
                    countDownLatch.countDown()
                }
            }
        }
        // 모든 작업 완료 대기
        val completed = countDownLatch.await(30, TimeUnit.SECONDS)
        if (!completed) {
            println("30초 내에 모든 작업이 완료되지 않았습니다!")
        }
        executor.shutdown()

        // then
        // 포인트 충전 요청은 최종적으로 전부 성공해야함.
        assertTrue(successCount.toInt() == threadCount)
        assertTrue(failureCount.toInt() == 0)

        // 최종 잔액 확인 (성공한 만큼만 충전 되어야 함)
        val finalUserPoint = userPointRepository.findByUserId(testUserId)
        val expectedBalance = beforeChargingPoint + (threadCount * chargeAmountPerThread)
        assertEquals(expectedBalance, finalUserPoint!!.getBalance())
    }

    @Test
    @DisplayName("동시성 테스트 - 여러 스레드에서 동시에 포인트 사용(베타락 적용됨)(단, 한번의 시도로 성공)")
    fun `동시성 테스트 여러 스레드에서 동시에 포인트 사용(베타락 적용됨)(단, 한번의 시도로 성공)`() {
        // given - 충분한 포인트 충전
        pointService.chargePoint(testUserId, 100000L) // 총 150,000원

        val threadCount = 20
        val useAmountPerThread = 10000L // 스레드당 10,000원 사용
        val executor = Executors.newFixedThreadPool(threadCount)
        val countDownLatch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        // when - 동시에 포인트 사용 시도
        repeat(threadCount) {
            executor.submit {
                try {
                    pointService.usePoint(testUserId, useAmountPerThread)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failureCount.incrementAndGet()
                } finally {
                    countDownLatch.countDown()
                }
            }
        }

        // 모든 작업 완료 대기
        val completed = countDownLatch.await(60, TimeUnit.SECONDS)
        if (!completed) {
            println("60초 내에 모든 작업이 완료되지 않았습니다!")
        }
        executor.shutdown()

        // then
        // 일부는 성공하고 일부는 실패해야 함 (잔액 부족으로)
        assertTrue(successCount.toInt() == 15) // 150,000 / 10,000 = 15번 성공
        assertTrue(failureCount.toInt() == 5) // 나머지 5번은 실패

        // 최종 잔액 확인 (성공한 만큼만 차감되어야 함)
        val finalUserPoint = userPointRepository.findByUserId(testUserId)
        val expectedBalance = 0L // 150,000 - (15 * 10,000) = 0
        assertEquals(expectedBalance, finalUserPoint!!.getBalance())
    }

    @Test
    @DisplayName("트랜잭션 롤백 테스트 - 예외 발생 시 포인트 변경 없음")
    @Transactional
    fun `트랜잭션 롤백 테스트 예외 발생 시 포인트 변경 없음`() {
        // given
        val originalBalance = testUserPoint.getBalance()

        // when & then - 잘못된 사용자 ID로 사용 시도
        assertThrows<IllegalArgumentException> {
            pointService.getPointBalance(-1L) // 잘못된 ID로 조회
        }

        // 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // 트랜잭션 롤백으로 인해 원본 데이터가 변경되지 않았는지 확인
        val unchangedUserPoint = userPointRepository.findByUserId(testUserId)
        assertNotNull(unchangedUserPoint)
        assertEquals(originalBalance, unchangedUserPoint!!.getBalance())
    }
}
