package kr.hhplus.be.server.facade.coupon

import jakarta.persistence.EntityManager
import kr.hhplus.be.server.IntegrationTestSupport
import kr.hhplus.be.server.core.coupon.domain.Coupon
import kr.hhplus.be.server.core.coupon.domain.CouponStatus
import kr.hhplus.be.server.core.coupon.repository.CouponRepository
import kr.hhplus.be.server.core.coupon.repository.UserCouponRepository
import kr.hhplus.be.server.core.coupon.service.CouponIssueQueueService
import kr.hhplus.be.server.core.coupon.service.CouponService
import kr.hhplus.be.server.core.coupon.service.CouponServiceInterface
import kr.hhplus.be.server.core.coupon.service.UserCouponServiceInterface
import kr.hhplus.be.server.core.user.domain.User
import kr.hhplus.be.server.core.user.repository.UserRepository
import kr.hhplus.be.server.fake.lock.FakeDistributedLockManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional

@DisplayName("비동기 쿠폰 발급 통합 테스트")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CouponIssueIntegrationTest : IntegrationTestSupport() {
    @Autowired
    private lateinit var couponFacade: CouponFacade

    @Autowired
    private lateinit var couponIssueFacade: CouponIssueFacade

    @Autowired
    private lateinit var couponService: CouponServiceInterface

    @Autowired
    private lateinit var userCouponService: UserCouponServiceInterface

    @Autowired
    private lateinit var couponIssueQueueService: CouponIssueQueueService

    @Autowired
    private lateinit var couponRepository: CouponRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userCouponRepository: UserCouponRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testCoupon: Coupon
    private lateinit var testUser: User
    private val testUserId = 100L
    private val testCouponId = 1L

    @BeforeEach
    fun setUpFakeDistributedLockManager() {
        val couponService = CouponService(couponRepository, FakeDistributedLockManager())
        couponIssueFacade = CouponIssueFacade(couponService, userCouponService)
    }

    @BeforeEach
    fun setUp() {
        testUser =
            User(
                userId = testUserId,
                name = "테스트 사용자",
            )
        userRepository.save(testUser)

        // 테스트용 쿠폰 생성
        testCoupon =
            Coupon(
                couponId = testCouponId,
                description = "비동기 테스트용 5000원 할인 쿠폰",
                discountAmount = 5000L,
                stock = 10,
                couponStatus = CouponStatus.OPENED,
            )
        couponRepository.save(testCoupon)
    }

    @Test
    @DisplayName("비동기 쿠폰 발급 요청 성공")
    @Transactional
    fun `비동기 쿠폰 발급 요청 성공`() {
        // when
        val response = couponFacade.requestCouponIssue(testUserId, testCouponId)

        // then
        assertTrue(response.success)
        assertTrue(response.message.contains("쿠폰 발급 요청이 정상적으로 접수되었습니다"))
        assertNotNull(response.requestId)
        assertTrue(response.requestId.isNotBlank())

        // 대기열에 추가되었는지 확인
        val queueSize = couponFacade.getQueueSize(testCouponId)
        assertEquals(1L, queueSize)
    }

    @Test
    @DisplayName("재고 부족 시 쿠폰 발급 요청 실패")
    @Transactional
    fun `재고 부족 시 쿠폰 발급 요청 실패`() {
        // given
        // 재고가 0인 쿠폰 생성
        val outOfStockCoupon =
            Coupon(
                couponId = 999L,
                description = "재고 없는 쿠폰",
                discountAmount = 1000L,
                stock = 0,
                couponStatus = CouponStatus.OPENED,
            )
        couponRepository.save(outOfStockCoupon)

        // when
        val response = couponFacade.requestCouponIssue(testUserId, 999L)

        // then
        assertFalse(response.success)
        assertTrue(response.message.contains("쿠폰 재고가 부족합니다"))
        assertEquals("", response.requestId)

        // 대기열에 추가되지 않았는지 확인
        val queueSize = couponFacade.getQueueSize(999L)
        assertEquals(0L, queueSize)
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 쿠폰 발급 요청 실패")
    @Transactional
    fun `존재하지 않는 사용자로 쿠폰 발급 요청 실패`() {
        // given
        val nonExistentUserId = 999L

        // when
        val response = couponFacade.requestCouponIssue(nonExistentUserId, testCouponId)

        // then
        assertFalse(response.success)
        assertTrue(
            response.message.contains("존재하지 않는 사용자입니다") ||
                response.message.contains("예상치 못한 오류가 발생했습니다") ||
                response.message.contains("오류가 발생했습니다"),
        )
    }

    @Test
    @DisplayName("대기열에서 쿠폰 발급 요청 처리 성공")
    @Transactional
    fun `대기열에서 쿠폰 발급 요청 처리 성공`() {
        // given
        // 대기열에 요청 추가
        val requestId = couponIssueQueueService.addCouponIssueRequest(testUserId, testCouponId)
        assertNotNull(requestId)

        // when
        // 대기열에서 요청 조회
        val request = couponIssueQueueService.getNextCouponIssueRequest()
        assertNotNull(request)

        // 쿠폰 발급 처리
        val userCoupon = couponIssueFacade.processIssueRequest(request!!)

        // then
        assertNotNull(userCoupon)
        assertEquals(testUserId, userCoupon.userId)
        assertEquals(testCouponId, userCoupon.couponId)
        assertTrue(userCoupon.isUsable())

        // 영속성 컨텍스트 동기화
        entityManager.flush()
        entityManager.clear()

        // DB에서 실제로 저장되었는지 확인
        val savedUserCoupon = userCouponRepository.findByUserIdAndCouponId(testUserId, testCouponId)
        assertNotNull(savedUserCoupon)
        assertEquals(userCoupon.userCouponId, savedUserCoupon!!.userCouponId)

        // 쿠폰 재고 차감 확인
        val updatedCoupon = couponRepository.findByCouponId(testCouponId)
        assertNotNull(updatedCoupon)
        assertEquals(9, updatedCoupon!!.getStock()) // 10 - 1 = 9
    }

    @Test
    @DisplayName("중복 발급 방지 테스트")
    @Transactional
    fun `중복 발급 방지 테스트`() {
        // given
        // 첫 번째 발급 성공
        val request1 = couponIssueQueueService.addCouponIssueRequest(testUserId, testCouponId)
        val issueRequest1 = couponIssueQueueService.getNextCouponIssueRequest()
        val userCoupon1 = couponIssueFacade.processIssueRequest(issueRequest1!!)
        assertNotNull(userCoupon1)

        // when & then
        // 두 번째 동일한 사용자 발급 시도 - 중복 발급 방지 확인
        val request2 = couponIssueQueueService.addCouponIssueRequest(testUserId, testCouponId)
        val issueRequest2 = couponIssueQueueService.getNextCouponIssueRequest()

        assertThrows(IllegalStateException::class.java) {
            couponIssueFacade.processIssueRequest(issueRequest2!!)
        }

        // 영속성 컨텍스트 동기화
        entityManager.flush()
        entityManager.clear()

        // 사용자 쿠폰이 하나만 있는지 확인
        val userCoupons = userCouponRepository.findByUserId(testUserId)
        assertEquals(1, userCoupons.size)
    }

    @Test
    @DisplayName("쿠폰 재고 한계 테스트")
    @Transactional
    fun `쿠폰 재고 한계 테스트`() {
        // given
        val limitedCoupon =
            Coupon(
                couponId = 998L,
                description = "재고 2개 제한 쿠폰",
                discountAmount = 1000L,
                stock = 2,
                couponStatus = CouponStatus.OPENED,
            )
        couponRepository.save(limitedCoupon)

        // when
        // 2명의 사용자가 요청
        val user1Id = 201L
        val user2Id = 202L
        val user3Id = 203L

        listOf(user1Id, user2Id, user3Id).forEach { userId ->
            val user =
                User(
                    userId = userId,
                    name = "테스트 사용자$userId",
                )
        }

        // 대기열에 3개 요청 추가
        couponIssueQueueService.addCouponIssueRequest(user1Id, 998L)
        couponIssueQueueService.addCouponIssueRequest(user2Id, 998L)
        couponIssueQueueService.addCouponIssueRequest(user3Id, 998L)

        // 처음 2개는 성공해야 함
        val request1 = couponIssueQueueService.getNextCouponIssueRequest()
        val request2 = couponIssueQueueService.getNextCouponIssueRequest()
        val request3 = couponIssueQueueService.getNextCouponIssueRequest()

        val userCoupon1 = couponIssueFacade.processIssueRequest(request1!!)
        val userCoupon2 = couponIssueFacade.processIssueRequest(request2!!)

        assertNotNull(userCoupon1)
        assertNotNull(userCoupon2)

        // 세 번째는 재고 부족으로 실패해야 함
        assertThrows(IllegalArgumentException::class.java) {
            couponIssueFacade.processIssueRequest(request3!!)
        }

        // then
        entityManager.flush()
        entityManager.clear()

        val finalCoupon = couponRepository.findByCouponId(998L)
        assertEquals(0, finalCoupon!!.getStock())
    }
}
