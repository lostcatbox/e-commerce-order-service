package kr.hhplus.be.server.core.coupon.service

import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest
import kr.hhplus.be.server.core.coupon.domain.UserCoupon
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 쿠폰 발급 전용 처리 서비스
 *
 * 설명:
 * - 스케줄러와 Consumer 모두에서 사용할 수 있는 공통 처리 로직
 * - CouponService에서 쿠폰 발급 처리 로직을 분리하여 재사용성 향상
 * - 트랜잭션과 에러 처리를 담당
 *
 * 특징:
 * - 단일 책임: 쿠폰 발급 처리만 담당
 * - 재사용성: 스케줄러/Consumer에서 공통 사용
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
        val errorMessage = e.message ?: "알 수 없는 비즈니스 에러"

        return when {
            errorMessage.contains("이미 발급받은 쿠폰") -> {
                println("중복 발급 요청 무시 - RequestId: ${request.requestId}, UserId: ${request.userId}, CouponId: ${request.couponId}")
                CouponIssueResult.duplicateRequest(request.requestId, errorMessage)
            }
            errorMessage.contains("재고") -> {
                println("쿠폰 재고 부족 - RequestId: ${request.requestId}, CouponId: ${request.couponId}")
                CouponIssueResult.outOfStock(request.requestId, errorMessage)
            }
            else -> {
                println("쿠폰 발급 처리 실패 - RequestId: ${request.requestId}, Error: $errorMessage")
                CouponIssueResult.businessError(request.requestId, errorMessage)
            }
        }
    }

    /**
     * 시스템 에러 처리
     */
    private fun handleSystemError(
        request: CouponIssueRequest,
        e: Exception,
    ): CouponIssueResult {
        val errorMessage = e.message ?: "알 수 없는 시스템 에러"
        println("쿠폰 발급 처리 중 시스템 에러 - RequestId: ${request.requestId}, Error: $errorMessage")
        e.printStackTrace()

        return CouponIssueResult.systemError(request.requestId, errorMessage)
    }
}

/**
 * 쿠폰 발급 처리 결과
 */
data class CouponIssueResult(
    val requestId: String,
    val status: CouponIssueStatus,
    val userCoupon: UserCoupon? = null,
    val errorMessage: String? = null,
) {
    companion object {
        fun success(
            requestId: String,
            userCoupon: UserCoupon,
        ) = CouponIssueResult(requestId, CouponIssueStatus.SUCCESS, userCoupon)

        fun validationFailed(
            requestId: String,
            message: String,
        ) = CouponIssueResult(requestId, CouponIssueStatus.VALIDATION_FAILED, errorMessage = message)

        fun duplicateRequest(
            requestId: String,
            message: String,
        ) = CouponIssueResult(requestId, CouponIssueStatus.DUPLICATE_REQUEST, errorMessage = message)

        fun outOfStock(
            requestId: String,
            message: String,
        ) = CouponIssueResult(requestId, CouponIssueStatus.OUT_OF_STOCK, errorMessage = message)

        fun businessError(
            requestId: String,
            message: String,
        ) = CouponIssueResult(requestId, CouponIssueStatus.BUSINESS_ERROR, errorMessage = message)

        fun systemError(
            requestId: String,
            message: String,
        ) = CouponIssueResult(requestId, CouponIssueStatus.SYSTEM_ERROR, errorMessage = message)
    }

    fun isSuccess() = status == CouponIssueStatus.SUCCESS

    fun isRetryable() = status == CouponIssueStatus.SYSTEM_ERROR
}

/**
 * 쿠폰 발급 처리 상태
 */
enum class CouponIssueStatus {
    SUCCESS, // 성공
    VALIDATION_FAILED, // 검증 실패
    DUPLICATE_REQUEST, // 중복 요청
    OUT_OF_STOCK, // 재고 부족
    BUSINESS_ERROR, // 비즈니스 에러
    SYSTEM_ERROR, // 시스템 에러
}
