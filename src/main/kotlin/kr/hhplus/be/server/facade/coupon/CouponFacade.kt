package kr.hhplus.be.server.facade.coupon

import kr.hhplus.be.server.core.coupon.domain.Coupon
import kr.hhplus.be.server.core.coupon.domain.UserCoupon
import kr.hhplus.be.server.core.coupon.service.CouponServiceInterface
import kr.hhplus.be.server.core.coupon.service.UserCouponServiceInterface
import kr.hhplus.be.server.core.user.service.UserServiceInterface
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 쿠폰 파사드 - 쿠폰 관련 복잡한 비즈니스 로직을 통합 관리
 *
 * 설명
 * 쿠폰 발급은 여러 도메인 서비스의 협력이 필요한 복잡한 비즈니스 프로세스입니다.
 * - 사용자 검증 (UserService)
 * - 쿠폰 재고 확인 및 차감 (CouponService)
 * - 사용자 쿠폰 발급 (UserCouponService)
 *
 * 특징
 * - 각 서비스는 자신의 도메인 로직에만 집중
 * - Facade가 전체 트랜잭션과 비즈니스 플로우를 조율
 * - MSA 환경에서 서비스 간 경계를 명확히 분리
 */
@Service
class CouponFacade(
    private val couponService: CouponServiceInterface,
    private val userCouponService: UserCouponServiceInterface,
    private val userService: UserServiceInterface,
) {
    /**
     * 쿠폰 발급 전체 프로세스
     */
    @Transactional
    fun issueCoupon(
        userId: Long,
        couponId: Long,
    ): UserCoupon {
        // 1. 사용자 검증
        userService.checkActiveUser(userId)

        // 2. 쿠폰 재고 차감
        couponService.issueCoupon(couponId)

        // 3. 사용자 쿠폰 생성
        return userCouponService.createUserCoupon(userId, couponId)
    }
}
