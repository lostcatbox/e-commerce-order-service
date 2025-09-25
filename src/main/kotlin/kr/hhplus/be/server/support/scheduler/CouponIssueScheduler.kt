package kr.hhplus.be.server.support.scheduler

import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest
import kr.hhplus.be.server.core.coupon.service.CouponIssueProcessor
import kr.hhplus.be.server.core.coupon.service.CouponServiceInterface
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 쿠폰 발급 스케줄러 (Redis 큐 전용)
 *
 * 설명:
 * Redis 큐를 사용할 때만 활성화되는 스케줄러입니다.
 * Kafka를 사용하는 경우에는 Consumer가 직접 처리하므로 비활성화됩니다.
 *
 * 특징:
 * - Redis 큐 전용 (app.queue.type=redis일 때만 활성화)
 * - 1초마다 1개씩 처리로 DB 부하 분산
 * - CouponIssueProcessor를 통한 공통 처리 로직 사용
 * - 예외 처리를 통한 스케줄러 안정성 확보
 *
 * 역할:
 * - Redis 대기열에서 요청 조회
 * - CouponIssueProcessor에 처리 위임
 * - 결과 로깅 및 예외 처리
 */
@Component
@ConditionalOnProperty(name = ["app.queue.type"], havingValue = "redis", matchIfMissing = true)
class CouponIssueScheduler(
    private val couponService: CouponServiceInterface,
    private val couponIssueProcessor: CouponIssueProcessor,
) {
    /**
     * 쿠폰 발급 처리 스케줄러
     * 1초마다 실행되어 대기열에서 1개의 요청을 처리합니다.
     */
    @Scheduled(fixedRate = 1000) // 1초마다 실행
    fun processCouponIssue() {
        try {
            // 대기열에서 다음 요청 조회
            val request = couponService.getNextCouponIssueRequest()

            if (request != null) {
                processRequest(request)
            }
        } catch (e: Exception) {
            // 스케줄러 전체가 중단되지 않도록 예외 처리
            println("쿠폰 발급 스케줄러 처리 중 오류 발생: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 개별 쿠폰 발급 요청 처리
     *
     * CouponIssueProcessor를 통해 공통 처리 로직 사용
     *
     * @param request 쿠폰 발급 요청
     */
    private fun processRequest(request: CouponIssueRequest) {
        try {
            val result = couponIssueProcessor.processRequest(request)

            if (result.isSuccess()) {
                println("스케줄러 쿠폰 발급 성공 - RequestId: ${request.requestId}, " +
                        "UserCouponId: ${result.userCoupon?.userCouponId}")
            } else {
                println("스케줄러 쿠폰 발급 실패 - RequestId: ${request.requestId}, " +
                        "Status: ${result.status}, Error: ${result.errorMessage}")
            }
        } catch (e: Exception) {
            println("스케줄러 쿠폰 발급 처리 중 예상치 못한 오류 - RequestId: ${request.requestId}, Error: ${e.message}")
            e.printStackTrace()
        }
    }
}
