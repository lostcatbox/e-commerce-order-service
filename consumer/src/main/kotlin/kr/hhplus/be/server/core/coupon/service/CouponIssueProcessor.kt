package kr.hhplus.be.server.core.coupon.service

import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest
import kr.hhplus.be.server.core.coupon.domain.UserCoupon
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 쿠폰 발급 전용 처리 서비스 (Consumer 전용)
 *
 * 설명:
 * - Consumer에서만 사용하는 쿠폰 발급 처리 로직
 * - Core의 CouponService에 비즈니스 로직을 위임
 * - 트랜잭션과 에러 처리를 담당
 *
 * 특징:
 * - 단일 책임: 쿠폰 발급 처리만 담당
 * - Consumer 전용: Kafka 메시지 처리에 최적화
 * - 트랜잭션 관리: 발급 처리의 원자성 보장
 * - 에러 처리: 다양한 예외 상황 대응
 */
@Service
class CouponIssueProcessor(
    private val couponService: CouponServiceInterface,
) {
    /**
     * 쿠폰 발급 요청 처리
     *
     * @param request 쿠폰 발급 요청
     * @return 발급 성공 여부와 결과
     */
    @Transactional
    fun processRequest(request: CouponIssueRequest): CouponIssueResult {
        return try {
            println("쿠폰 발급 처리 시작 - RequestId: ${request.requestId}, UserId: ${request.userId}, CouponId: ${request.couponId}")

            // 1. 요청 기본 검증
            if (!couponService.validateRequest(request)) {
                println("쿠폰 발급 요청 검증 실패 - RequestId: ${request.requestId}")
                return CouponIssueResult.validationFailed(request.requestId, "요청 검증 실패")
            }

            // 2. CouponService에 비즈니스 로직 위임
            val userCoupon = couponService.issueCoupon(request)

            println("쿠폰 발급 처리 완료 - RequestId: ${request.requestId}, UserCouponId: ${userCoupon.userCouponId}")

            CouponIssueResult.success(request.requestId, userCoupon)
        } catch (e: IllegalStateException) {
            handleBusinessError(request, e)
        } catch (e: IllegalArgumentException) {
            handleBusinessError(request, e)
        } catch (e: Exception) {
            handleSystemError(request, e)
        }
    }

    /**
     * 비즈니스 로직 에러 처리
     */
    private fun handleBusinessError(
        request: CouponIssueRequest,
        e: Exception,
    ): CouponIssueResult {
        println("쿠폰 발급 비즈니스 에러 - RequestId: ${request.requestId}, Error: ${e.message}")
        return CouponIssueResult.businessError(request.requestId, e.message ?: "비즈니스 로직 에러")
    }

    /**
     * 시스템 에러 처리
     */
    private fun handleSystemError(
        request: CouponIssueRequest,
        e: Exception,
    ): CouponIssueResult {
        println("쿠폰 발급 시스템 에러 - RequestId: ${request.requestId}, Error: ${e.message}")
        e.printStackTrace()
        return CouponIssueResult.systemError(request.requestId, "시스템 오류가 발생했습니다")
    }
}

/**
 * 쿠폰 발급 처리 결과
 */
data class CouponIssueResult(
    val requestId: String,
    val status: Status,
    val userCoupon: UserCoupon? = null,
    val errorMessage: String? = null,
) {
    enum class Status {
        SUCCESS,
        VALIDATION_FAILED,
        BUSINESS_ERROR,
        SYSTEM_ERROR,
    }

    fun isSuccess(): Boolean = status == Status.SUCCESS

    fun isRetryable(): Boolean = status == Status.SYSTEM_ERROR

    companion object {
        fun success(
            requestId: String,
            userCoupon: UserCoupon,
        ): CouponIssueResult =
            CouponIssueResult(
                requestId = requestId,
                status = Status.SUCCESS,
                userCoupon = userCoupon,
            )

        fun validationFailed(
            requestId: String,
            errorMessage: String,
        ): CouponIssueResult =
            CouponIssueResult(
                requestId = requestId,
                status = Status.VALIDATION_FAILED,
                errorMessage = errorMessage,
            )

        fun businessError(
            requestId: String,
            errorMessage: String,
        ): CouponIssueResult =
            CouponIssueResult(
                requestId = requestId,
                status = Status.BUSINESS_ERROR,
                errorMessage = errorMessage,
            )

        fun systemError(
            requestId: String,
            errorMessage: String,
        ): CouponIssueResult =
            CouponIssueResult(
                requestId = requestId,
                status = Status.SYSTEM_ERROR,
                errorMessage = errorMessage,
            )
    }
}
