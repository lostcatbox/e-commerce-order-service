package kr.hhplus.be.server.core.coupon.service

import jakarta.persistence.EntityManager
import kr.hhplus.be.server.IntegrationTestSupport
import kr.hhplus.be.server.core.coupon.domain.Coupon
import kr.hhplus.be.server.core.coupon.domain.CouponStatus
import kr.hhplus.be.server.core.coupon.repository.CouponRepository
import org.junit.jupiter.api.AfterEach
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

@DisplayName("CouponService 통합 테스트")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CouponServiceIntegrationTest : IntegrationTestSupport() {
    @Autowired
    private lateinit var couponService: CouponService

    @Autowired
    private lateinit var couponRepository: CouponRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testCoupon: Coupon

    @BeforeEach
    fun setUp() {
        // 테스트용 쿠폰 데이터 생성
        testCoupon =
            Coupon(
                couponId = 1L,
                description = "통합테스트용 10000원 할인 쿠폰",
                discountAmount = 10000L,
                stock = 100,
                couponStatus = CouponStatus.OPENED,
            )
        couponRepository.save(testCoupon)
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 조회 시 예외 발생")
    @Transactional
    fun `존재하지 않는 쿠폰 조회 시 예외 발생`() {
        // given
        val nonExistentCouponId = 999L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                couponService.getCouponInfo(nonExistentCouponId)
            }

        assertTrue(exception.message!!.contains("존재하지 않는 쿠폰입니다"))
        assertTrue(exception.message!!.contains(nonExistentCouponId.toString()))
    }

    @Test
    @DisplayName("쿠폰 발급 재고 차감 검증")
    @Transactional
    fun `쿠폰 발급 재고 차감 검증`() {
        // given
        val originalStock = testCoupon.getStock()

        // when
        val result = couponService.issueCoupon(testCoupon.couponId)

        // then
        assertNotNull(result)
        assertEquals(originalStock - 1, result.getStock())

        // 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // DB에서 다시 조회하여 실제로 저장되었는지 확인
        val savedCoupon = couponRepository.findByCouponId(testCoupon.couponId)
        assertNotNull(savedCoupon)
        assertEquals(originalStock - 1, savedCoupon!!.getStock())
    }

    @Test
    @DisplayName("재고가 0인 쿠폰 발급 시 예외 발생")
    @Transactional
    fun `재고가 0인 쿠폰 발급 시 예외 발생`() {
        // given - 재고를 0으로 만들기
        val zeroStockCoupon =
            Coupon(
                couponId = 2L,
                description = "재고 0 테스트 쿠폰",
                discountAmount = 5000L,
                stock = 0,
                couponStatus = CouponStatus.OPENED,
            )
        couponRepository.save(zeroStockCoupon)

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                couponService.issueCoupon(zeroStockCoupon.couponId)
            }

        assertTrue(exception.message!!.contains("쿠폰 재고가 부족합니다"))
    }

    @Test
    @DisplayName("닫힌 상태의 쿠폰 발급 시 예외 발생")
    @Transactional
    fun `닫힌 상태의 쿠폰 발급 시 예외 발생`() {
        // given - 닫힌 상태의 쿠폰 생성
        val closedCoupon =
            Coupon(
                couponId = 3L,
                description = "닫힌 상태 테스트 쿠폰",
                discountAmount = 5000L,
                stock = 50,
                couponStatus = CouponStatus.CLOSED,
            )
        couponRepository.save(closedCoupon)

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                couponService.issueCoupon(closedCoupon.couponId)
            }

        assertTrue(exception.message!!.contains("쿠폰이 사용 가능한 상태가 아닙니다"))
    }

    @Test
    @DisplayName("동시성 테스트 - 여러 스레드에서 동시에 쿠폰 발급")
    fun `동시성 테스트 여러 스레드에서 동시에 쿠폰 발급`() {
        // given
        val concurrentCoupon =
            Coupon(
                couponId = 4L,
                description = "동시성 테스트 쿠폰",
                discountAmount = 1000L,
                stock = 5, // 5개 재고로 축소
                couponStatus = CouponStatus.OPENED,
            )
        couponRepository.save(concurrentCoupon)

        val threadCount = 8 // 재고보다 많은 스레드
        val executor = Executors.newFixedThreadPool(threadCount)
        val results = mutableListOf<CompletableFuture<Boolean>>()

        // when - 동시에 쿠폰 발급 시도
        repeat(threadCount) {
            val future =
                CompletableFuture.supplyAsync({
                    try {
                        couponService.issueCoupon(concurrentCoupon.couponId)
                        true // 성공
                    } catch (e: Exception) {
                        false // 실패
                    }
                }, executor)
            results.add(future)
        }

        // 모든 작업 완료 대기
        val completedResults = results.map { it.get() }
        executor.shutdown()

        // then
        val successCount = completedResults.count { it }
        val failureCount = completedResults.count { !it }

        // 성공한 발급 수는 재고 수와 같아야 함
        assertEquals(5, successCount)
        assertEquals(3, failureCount)

        // 영속성 컨텍스트 초기화 commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // 최종 재고 확인
        val finalCoupon = couponRepository.findByCouponId(concurrentCoupon.couponId)
        assertEquals(0, finalCoupon!!.getStock())
    }

    @Test
    @DisplayName("트랜잭션 롤백 테스트 - 예외 발생 시 데이터 롤백 확인")
    @Transactional
    fun `트랜잭션 롤백 테스트 예외 발생 시 데이터 롤백 확인`() {
        // given
        val originalStock = testCoupon.getStock()

        // when & then - 잘못된 ID로 발급 시도 후 원본 데이터 확인
        assertThrows<IllegalArgumentException> {
            // 이 호출은 실패해야 함
            couponService.getCouponInfo(-1L) // 잘못된 ID
        }

        // 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // 트랜잭션 롤백으로 인해 원본 데이터가 변경되지 않았는지 확인
        val unchangedCoupon = couponRepository.findByCouponId(testCoupon.couponId)
        assertNotNull(unchangedCoupon)
        assertEquals(originalStock, unchangedCoupon!!.getStock())
    }
}
