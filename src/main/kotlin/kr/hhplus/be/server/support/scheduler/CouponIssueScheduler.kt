package kr.hhplus.be.server.support.scheduler

import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest
import kr.hhplus.be.server.core.coupon.service.CouponServiceInterface
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 쿠폰 발급 스케줄러
 *
 * 설명:
 * Redis 대기열에 쌓인 쿠폰 발급 요청을 1초마다 1개씩 처리하는 스케줄러입니다.
 * DB 부하를 조절하면서 안정적으로 쿠폰을 발급합니다.
 *
 * 특징:
 * - 1초마다 1개씩 처리로 DB 부하 분산
 * - CouponService에 비즈니스 로직 위임 (Facade 패턴 제거)
 * - 스케줄링 관심사와 비즈니스 로직 분리
 * - 예외 처리를 통한 스케줄러 안정성 확보
 * - 단일 의존성으로 간소화된 구조
 *
 * 역할:
 * - 대기열에서 요청 조회
 * - 비즈니스 로직 실행 (CouponService에 위임)
 * - 결과 로깅 및 예외 처리
 */
@Component
class CouponIssueScheduler(
    private val couponService: CouponServiceInterface,
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
     * 처리 과정:
     * 1. 요청 검증
     * 2. CouponService에 비즈니스 로직 위임
     * 3. 결과 로깅
     *
     * @param request 쿠폰 발급 요청
     */
    private fun processRequest(request: CouponIssueRequest) {
        try {
            println("쿠폰 발급 처리 시작 - RequestId: ${request.requestId}, UserId: ${request.userId}, CouponId: ${request.couponId}")

            // 1. 요청 기본 검증
            if (!couponService.validateRequest(request)) {
                println("쿠폰 발급 요청 검증 실패 - RequestId: ${request.requestId}")
                return
            }

            // 2. CouponService에 비즈니스 로직 위임
            val userCoupon = couponService.issueCoupon(request)

            println("쿠폰 발급 처리 완료 - RequestId: ${request.requestId}, UserCouponId: ${userCoupon.userCouponId}")
        } catch (e: IllegalStateException) {
            if (e.message?.contains("이미 발급받은 쿠폰") == true) {
                // 중복 발급 요청인 경우 로그만 출력하고 무시
                println("중복 발급 요청 무시 - RequestId: ${request.requestId}, UserId: ${request.userId}, CouponId: ${request.couponId}")
            } else {
                println("쿠폰 발급 처리 실패 - RequestId: ${request.requestId}, Error: ${e.message}")
            }
        } catch (e: IllegalArgumentException) {
            if (e.message?.contains("재고") == true) {
                // 재고 부족인 경우
                println("쿠폰 재고 부족 - RequestId: ${request.requestId}, CouponId: ${request.couponId}")
            } else {
                println("쿠폰 발급 처리 실패 - RequestId: ${request.requestId}, Error: ${e.message}")
            }
        } catch (e: Exception) {
            println("쿠폰 발급 처리 중 예상치 못한 오류 - RequestId: ${request.requestId}, Error: ${e.message}")
            e.printStackTrace()
        }
    }
}
