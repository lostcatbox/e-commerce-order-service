package kr.hhplus.be.server.facade.coupon

import kr.hhplus.be.server.core.coupon.domain.CouponIssueResponse
import kr.hhplus.be.server.core.coupon.service.CouponIssueQueueService
import kr.hhplus.be.server.core.coupon.service.CouponServiceInterface
import kr.hhplus.be.server.core.user.service.UserServiceInterface
import org.springframework.stereotype.Service

/**
 * 비동기 쿠폰 발급 파사드
 *
 * 설명:
 * 기존 동기 쿠폰 발급 시스템을 비동기로 개선한 파사드입니다.
 * - 즉시 응답: 발급 요청에 대해 즉시 성공/실패 응답
 * - 대기열 관리: Redis List를 활용한 FIFO 대기열
 * - 부하 분산: DB 부하를 줄이고 높은 처리량 확보
 *
 * 특징:
 * - 발급 요청 시점에 재고만 확인하고 즉시 응답
 * - 실제 발급 처리는 스케줄러가 비동기로 수행
 * - Redis 기반 대기열로 선착순 보장
 */
@Service
class CouponFacade(
    private val couponService: CouponServiceInterface,
    private val userService: UserServiceInterface,
    private val couponIssueQueueService: CouponIssueQueueService,
) {
    /**
     * 쿠폰 발급 요청 (비동기)
     *
     * 처리 과정:
     * 1. 사용자 검증
     * 2. 쿠폰 재고 확인 (RDB에서 조회만)
     * 3. 재고가 있으면 대기열에 추가 후 즉시 응답
     * 4. 실제 발급은 스케줄러가 비동기로 처리
     *
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 발급 요청 응답 (즉시 응답)
     */
    fun requestCouponIssue(
        userId: Long,
        couponId: Long,
    ): CouponIssueResponse =
        try {
            // 1. 사용자 검증
            userService.checkActiveUser(userId)

            // 2. 쿠폰 재고 확인 (조회만, 차감하지 않음)
            val coupon = validateCouponStock(couponId)

            // 3. 대기열에 추가
            val requestId = addToQueue(userId, couponId)

            // 4. 즉시 성공 응답
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
     * @param couponId 쿠폰 ID
     * @return 대기열 크기
     */
    fun getQueueSize(couponId: Long): Long = couponIssueQueueService.getQueueSize(couponId)

    /**
     * 쿠폰 재고 검증 (조회만)
     * @param couponId 쿠폰 ID
     * @return 쿠폰 정보
     * @throws IllegalArgumentException 재고 부족 시
     */
    private fun validateCouponStock(couponId: Long) =
        couponService.getCouponInfo(couponId).also { coupon ->
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
}
