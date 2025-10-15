package kr.hhplus.be.server.core.coupon.service

import jakarta.persistence.EntityManager
import kr.hhplus.be.server.core.coupon.domain.Coupon
import kr.hhplus.be.server.core.coupon.domain.CouponStatus
import kr.hhplus.be.server.core.coupon.domain.UserCouponStatus
import kr.hhplus.be.server.core.coupon.repository.CouponRepository
import kr.hhplus.be.server.core.coupon.repository.UserCouponRepository
import kr.hhplus.be.server.support.IntegrationTestSupport
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional

@DisplayName("UserCouponService 통합 테스트")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserCouponServiceIntegrationTest : IntegrationTestSupport() {
    @Autowired
    private lateinit var userCouponService: UserCouponService

    @Autowired
    private lateinit var userCouponRepository: UserCouponRepository

    @Autowired
    private lateinit var couponRepository: CouponRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testCoupon: Coupon
    private val testUserId = 100L

    @BeforeEach
    fun setUp() {
        // 테스트용 쿠폰 데이터 생성
        testCoupon =
            Coupon(
                couponId = 1L,
                description = "사용자 쿠폰 테스트용 할인 쿠폰",
                discountAmount = 5000L,
                stock = 100,
                couponStatus = CouponStatus.OPENED,
            )
        couponRepository.save(testCoupon)
    }

    @Test
    @DisplayName("사용자 쿠폰 발급 성공")
    @Transactional
    fun `사용자 쿠폰 발급 성공`() {
        // when
        val result = userCouponService.createUserCoupon(testUserId, testCoupon.couponId)

        // then
        assertNotNull(result)
        assertEquals(testUserId, result.userId)
        assertEquals(testCoupon.couponId, result.couponId)
        assertEquals(UserCouponStatus.ISSUED, result.getStatus())
        assertTrue(result.isUsable())

        // 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // DB에서 다시 조회하여 실제로 저장되었는지 확인
        val savedUserCoupon = userCouponRepository.findByUserIdAndCouponId(testUserId, testCoupon.couponId)
        assertNotNull(savedUserCoupon)
        assertEquals(result.userCouponId, savedUserCoupon!!.userCouponId)
    }

    @Test
    @DisplayName("중복 쿠폰 발급 시 예외 발생")
    @Transactional
    fun `중복 쿠폰 발급 시 예외 발생`() {
        // given - 이미 발급된 쿠폰
        userCouponService.createUserCoupon(testUserId, testCoupon.couponId)

        // when & then
        val exception =
            assertThrows<IllegalStateException> {
                userCouponService.createUserCoupon(testUserId, testCoupon.couponId)
            }

        assertTrue(exception.message!!.contains("이미 발급받은 쿠폰입니다"))
        assertTrue(exception.message!!.contains(testUserId.toString()))
        assertTrue(exception.message!!.contains(testCoupon.couponId.toString()))
    }

    @Test
    @DisplayName("잘못된 사용자 ID로 쿠폰 발급 시 예외 발생")
    @Transactional
    fun `잘못된 사용자 ID로 쿠폰 발급 시 예외 발생`() {
        // given
        val invalidUserId = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                userCouponService.createUserCoupon(invalidUserId, testCoupon.couponId)
            }

        assertTrue(exception.message!!.contains("사용자 ID는 0보다 커야 합니다"))
    }

    @Test
    @DisplayName("잘못된 쿠폰 ID로 쿠폰 발급 시 예외 발생")
    @Transactional
    fun `잘못된 쿠폰 ID로 쿠폰 발급 시 예외 발생`() {
        // given
        val invalidCouponId = 0L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                userCouponService.createUserCoupon(testUserId, invalidCouponId)
            }

        assertTrue(exception.message!!.contains("쿠폰 ID는 0보다 커야 합니다"))
    }

    @Test
    @DisplayName("사용자 쿠폰 목록 조회")
    @Transactional
    fun `사용자 쿠폰 목록 조회`() {
        // given - 여러 쿠폰 발급
        val coupon2 =
            Coupon(
                couponId = 2L,
                description = "두 번째 테스트 쿠폰",
                discountAmount = 3000L,
                stock = 50,
                couponStatus = CouponStatus.OPENED,
            )
        couponRepository.save(coupon2)

        userCouponService.createUserCoupon(testUserId, testCoupon.couponId)
        userCouponService.createUserCoupon(testUserId, coupon2.couponId)

        // when
        val result = userCouponService.getUserCoupons(testUserId)

        // then
        assertEquals(2, result.size)
        assertTrue(result.any { it.couponId == testCoupon.couponId })
        assertTrue(result.any { it.couponId == coupon2.couponId })
        assertTrue(result.all { it.userId == testUserId })
    }

    @Test
    @DisplayName("사용 가능한 쿠폰 목록 조회")
    @Transactional
    fun `사용 가능한 쿠폰 목록 조회`() {
        // given - 쿠폰 발급 후 하나는 사용
        val userCoupon1 = userCouponService.createUserCoupon(testUserId, testCoupon.couponId)

        val coupon2 =
            Coupon(
                couponId = 2L,
                description = "두 번째 테스트 쿠폰",
                discountAmount = 3000L,
                stock = 50,
                couponStatus = CouponStatus.OPENED,
            )
        couponRepository.save(coupon2)
        val userCoupon2 = userCouponService.createUserCoupon(testUserId, coupon2.couponId)

        // 첫 번째 쿠폰 사용
        userCouponService.useCoupon(userCoupon1.userCouponId)

        // when
        val result = userCouponService.getUsableCoupons(testUserId)

        // then
        assertEquals(1, result.size)
        assertEquals(userCoupon2.userCouponId, result[0].userCouponId)
        assertEquals(coupon2.couponId, result[0].couponId)
        assertTrue(result[0].isUsable())
    }

    @Test
    @DisplayName("사용자 쿠폰 사용 - 실제 DB 검증")
    @Transactional
    fun `사용자 쿠폰 사용 실제 DB 검증`() {
        // given
        val userCoupon = userCouponService.createUserCoupon(testUserId, testCoupon.couponId)
        assertTrue(userCoupon.isUsable())

        // when
        val result = userCouponService.useCoupon(userCoupon.userCouponId)

        // then
        assertNotNull(result)
        assertEquals(UserCouponStatus.USED, result.getStatus())
        assertFalse(result.isUsable())
        assertNotNull(result.getUsedAt())

        // 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // DB에서 다시 조회하여 실제로 사용 처리되었는지 확인
        val savedUserCoupon = userCouponRepository.findByUserCouponId(userCoupon.userCouponId)
        assertNotNull(savedUserCoupon)
        assertEquals(UserCouponStatus.USED, savedUserCoupon!!.getStatus())
        assertNotNull(savedUserCoupon.getUsedAt())
    }

    @Test
    @DisplayName("이미 사용된 쿠폰 재사용 시 예외 발생")
    @Transactional
    fun `이미 사용된 쿠폰 재사용 시 예외 발생`() {
        // given - 쿠폰 발급 후 사용
        val userCoupon = userCouponService.createUserCoupon(testUserId, testCoupon.couponId)
        userCouponService.useCoupon(userCoupon.userCouponId)

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                userCouponService.useCoupon(userCoupon.userCouponId)
            }

        assertTrue(exception.message!!.contains("쿠폰이 사용 가능한 상태가 아닙니다"))
    }

    @Test
    @DisplayName("존재하지 않는 사용자 쿠폰 사용 시 예외 발생")
    @Transactional
    fun `존재하지 않는 사용자 쿠폰 사용 시 예외 발생`() {
        // given
        val nonExistentUserCouponId = 999L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                userCouponService.useCoupon(nonExistentUserCouponId)
            }

        assertTrue(exception.message!!.contains("존재하지 않는 사용자 쿠폰입니다"))
        assertTrue(exception.message!!.contains(nonExistentUserCouponId.toString()))
    }

    @Test
    @DisplayName("잘못된 사용자 쿠폰 ID로 사용 시 예외 발생")
    @Transactional
    fun `잘못된 사용자 쿠폰 ID로 사용 시 예외 발생`() {
        // given
        val invalidUserCouponId = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                userCouponService.useCoupon(invalidUserCouponId)
            }

        assertTrue(exception.message!!.contains("사용자 쿠폰 ID는 0보다 커야 합니다"))
    }
}
