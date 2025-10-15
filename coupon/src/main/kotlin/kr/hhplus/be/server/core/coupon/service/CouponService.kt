package kr.hhplus.be.server.core.coupon.service

import kr.hhplus.be.server.core.coupon.domain.Coupon
import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest
import kr.hhplus.be.server.core.coupon.domain.CouponIssueResponse
import kr.hhplus.be.server.core.coupon.domain.UserCoupon
import kr.hhplus.be.server.core.coupon.repository.CouponRepository
import kr.hhplus.be.server.infrastructure.client.CouponApiClient
import kr.hhplus.be.server.support.lock.DistributedLockManagerInterface
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 쿠폰 서비스 구현체 - 쿠폰 도메인의 단일 진입점
 *
 * 설명:
 * - 기존 Facade 패턴을 제거하고 모든 쿠폰 관련 기능을 통합
 * - 내부 서비스들을 조합하여 완전한 쿠폰 도메인 기능 제공
 * - 외부에서는 이 클래스만 사용하면 모든 쿠폰 관련 작업 수행 가능
 */
@Service
class CouponService(
    private val couponRepository: CouponRepository,
    private val distributedLockManager: DistributedLockManagerInterface,
    private val userCouponService: UserCouponService,
    private val couponIssueQueueService: CouponIssueQueueServiceInterface,
    private val couponApiClient: CouponApiClient,
) : CouponServiceInterface {
    /**
     * 쿠폰 정보 조회
     */
    @Transactional(readOnly = true)
    override fun getCouponInfo(couponId: Long): Coupon {
        validateCouponId(couponId)

        return couponRepository.findByCouponId(couponId)
            ?: throw IllegalArgumentException("존재하지 않는 쿠폰입니다. 쿠폰 ID: $couponId")
    }

    /**
     * 쿠폰 발급 요청 (비동기)
     */
    override fun requestCouponIssueAsync(
        userId: Long,
        couponId: Long,
    ): CouponIssueResponse =
        try {
            // 1. 사용자 검증
            val userActiveResponse = couponApiClient.checkActiveUser(userId)
            if (!userActiveResponse.isActive) {
                throw IllegalArgumentException("비활성 사용자입니다. 사용자 ID: $userId")
            }

            // 2. 쿠폰 정보 조회 및 검증
            val coupon = getCouponInfo(couponId)

            // 3. 쿠폰 상태 검증 (열린 상태인지 확인)
            if (!coupon.isOpened()) {
                throw IllegalArgumentException("쿠폰이 사용 가능한 상태가 아닙니다. 쿠폰 ID: $couponId")
            }

            // 4. 쿠폰 재고 확인
            if (!coupon.hasStock()) {
                throw IllegalArgumentException("쿠폰 재고가 부족합니다. 쿠폰 ID: $couponId")
            }

            // 5. 중복 발급 확인
            val existingUserCoupon = userCouponService.findByUserIdAndCouponId(userId, couponId)
            if (existingUserCoupon != null) {
                throw IllegalArgumentException("이미 발급받은 쿠폰입니다. 사용자 ID: $userId, 쿠폰 ID: $couponId")
            }

            // 6. 대기열에 추가
            val requestId = addToQueue(userId, couponId)

            // 7. 즉시 성공 응답
            CouponIssueResponse.success(requestId)
        } catch (e: IllegalArgumentException) {
            when {
                e.message?.contains("재고") == true -> CouponIssueResponse.failureOutOfStock(couponId)
                else -> CouponIssueResponse.failure(e.message ?: "쿠폰 발급 요청 처리 중 오류가 발생했습니다.")
            }
        } catch (e: Exception) {
            CouponIssueResponse.failure("쿠폰 발급 요청 처리 중 예상치 못한 오류가 발생했습니다.")
        }

    /**
     * 특정 쿠폰의 대기열 크기 조회
     */
    override fun getQueueSize(couponId: Long): Long = couponIssueQueueService.getQueueSize(couponId)

    // ===== 쿠폰 발급 처리 기능 (기존 CouponIssueFacade) =====

    /**
     * 쿠폰 발급 처리 (스케줄러에서 사용)
     *
     * 완전한 쿠폰 발급 프로세스:
     * 1. 쿠폰 재고 차감 (분산락 + 비관적 락으로 동시성 제어)
     * 2. 사용자 쿠폰 생성 (중복 발급 검증 포함)
     *
     * 단일 트랜잭션으로 처리하여 데이터 정합성 보장
     */
    @Transactional
    override fun issueCoupon(request: CouponIssueRequest): UserCoupon {
        try {
            validateCouponId(request.couponId)

            // 1. 쿠폰 재고 차감 with 분산락
            val lockKey = "lock:coupon-issue:${request.couponId}"
            distributedLockManager.executeWithLock(lockKey) {
                // 쿠폰 조회 (비관적 락)
                val coupon =
                    couponRepository.findByCouponIdWithPessimisticLock(request.couponId)
                        ?: throw IllegalArgumentException("존재하지 않는 쿠폰입니다. 쿠폰 ID: ${request.couponId}")

                // 도메인 로직을 통한 쿠폰 발급 (재고 차감)
                coupon.issueCoupon()

                // 재고 차감된 쿠폰 저장
                couponRepository.save(coupon)
            }

            // 2. 사용자 쿠폰 생성 (내부에서 중복 발급 검증 수행)
            val userCoupon = userCouponService.createUserCoupon(request.userId, request.couponId)

            return userCoupon
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * 대기열에서 다음 쿠폰 발급 요청 조회 및 제거
     */
    override fun getNextCouponIssueRequest(): CouponIssueRequest? = couponIssueQueueService.getNextCouponIssueRequest()

    // ===== 사용자 쿠폰 관리 기능 (기존 UserCouponService) =====

    /**
     * 사용자 쿠폰 발급
     */
    @Transactional
    override fun createUserCoupon(
        userId: Long,
        couponId: Long,
    ): UserCoupon = userCouponService.createUserCoupon(userId, couponId)

    /**
     * 사용자의 쿠폰 목록 조회
     */
    @Transactional(readOnly = true)
    override fun getUserCoupons(userId: Long): List<UserCoupon> = userCouponService.getUserCoupons(userId)

    /**
     * 사용자의 사용 가능한 쿠폰 목록 조회
     */
    @Transactional(readOnly = true)
    override fun getUsableCoupons(userId: Long): List<UserCoupon> = userCouponService.getUsableCoupons(userId)

    /**
     * 사용자 쿠폰 사용
     */
    @Transactional
    override fun useCoupon(userCouponId: Long): UserCoupon = userCouponService.useCoupon(userCouponId)

    /**
     * 사용자 쿠폰 조회
     */
    @Transactional(readOnly = true)
    override fun getUserCoupon(usedCouponId: Long): UserCoupon = userCouponService.getUserCoupon(usedCouponId)

    // ===== 내부 헬퍼 메서드들 =====

    /**
     * 쿠폰 재고 검증 (조회만)
     * @param couponId 쿠폰 ID
     * @return 쿠폰 정보
     * @throws IllegalArgumentException 재고 부족 시
     */
    private fun validateCouponStock(couponId: Long) =
        getCouponInfo(couponId).also { coupon ->
            if (!coupon.hasStock()) {
                throw IllegalArgumentException("쿠폰 재고가 부족합니다. 쿠폰 ID: $couponId")
            }
        }

    /**
     * 대기열에 요청 추가
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 생성된 요청 ID
     */
    private fun addToQueue(
        userId: Long,
        couponId: Long,
    ): String = couponIssueQueueService.addCouponIssueRequest(userId, couponId)

    /**
     * 쿠폰 ID 유효성 검증
     */
    private fun validateCouponId(couponId: Long) {
        require(couponId > 0) { "쿠폰 ID는 0보다 커야 합니다. 입력된 ID: $couponId" }
    }
}
