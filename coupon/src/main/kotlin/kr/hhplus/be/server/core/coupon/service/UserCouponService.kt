package kr.hhplus.be.server.core.coupon.service

import kr.hhplus.be.server.core.coupon.domain.UserCoupon
import kr.hhplus.be.server.core.coupon.repository.UserCouponRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 사용자 쿠폰 서비스 구현체 (내부 전용)
 *
 * 설명:
 * - CouponService에서만 사용되는 내부 서비스
 * - 외부에서 직접 사용하지 않고 CouponService를 통해서만 접근
 * - @Component로 변경하여 일반적인 서비스가 아님을 명시
 */
@Component
class UserCouponService(
    private val userCouponRepository: UserCouponRepository,
) {
    /**
     * 사용자 쿠폰 단건 조회
     */
    @Transactional(readOnly = true)
    fun getUserCoupon(userCouponId: Long): UserCoupon {
        validateUserCouponId(userCouponId)

        return userCouponRepository.findByUserCouponId(userCouponId)
            ?: throw IllegalArgumentException("존재하지 않는 사용자 쿠폰입니다. 사용자 쿠폰 ID: $userCouponId")
    }

    /**
     * 사용자 쿠폰 발급
     */
    @Transactional(propagation = Propagation.REQUIRED)
    fun createUserCoupon(
        userId: Long,
        couponId: Long,
    ): UserCoupon {
        validateUserId(userId)
        validateCouponId(couponId)

        // 이미 발급받은 쿠폰인지 확인
        val isAlreadyIssuedUserCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId) != null
        if (isAlreadyIssuedUserCoupon) {
            throw IllegalStateException("이미 발급받은 쿠폰입니다. 사용자 ID: $userId, 쿠폰 ID: $couponId")
        }

        // 유저 쿠폰 생성 및 저장
        val userCoupon = UserCoupon.issueCoupon(userId, couponId)
        return userCouponRepository.save(userCoupon)
    }

    /**
     * 사용자의 쿠폰 목록 조회
     */
    @Transactional(readOnly = true)
    fun getUserCoupons(userId: Long): List<UserCoupon> {
        validateUserId(userId)
        return userCouponRepository.findByUserId(userId)
    }

    /**
     * 사용자의 사용 가능한 쿠폰 목록 조회
     */
    @Transactional(readOnly = true)
    fun getUsableCoupons(userId: Long): List<UserCoupon> {
        validateUserId(userId)
        return userCouponRepository.findUsableCouponsByUserId(userId)
    }

    /**
     * 사용자 쿠폰 사용
     */
    @Transactional
    fun useCoupon(userCouponId: Long): UserCoupon {
        validateUserCouponId(userCouponId)

        // 사용자 쿠폰 조회
        val userCoupon =
            userCouponRepository.findByUserCouponId(userCouponId)
                ?: throw IllegalArgumentException("존재하지 않는 사용자 쿠폰입니다. 사용자 쿠폰 ID: $userCouponId")

        // 도메인 로직을 통한 쿠폰 사용
        userCoupon.use()

        // 사용된 사용자 쿠폰 저장
        return userCouponRepository.save(userCoupon)
    }

    /**
     * 사용자와 쿠폰 ID로 사용자 쿠폰 조회
     */
    fun findByUserIdAndCouponId(
        userId: Long,
        couponId: Long,
    ): UserCoupon? {
        validateUserId(userId)
        validateCouponId(couponId)
        return userCouponRepository.findByUserIdAndCouponId(userId, couponId)
    }

    /**
     * 사용자 ID 유효성 검증
     */
    private fun validateUserId(userId: Long) {
        require(userId > 0) { "사용자 ID는 0보다 커야 합니다. 입력된 ID: $userId" }
    }

    /**
     * 쿠폰 ID 유효성 검증
     */
    private fun validateCouponId(couponId: Long) {
        require(couponId > 0) { "쿠폰 ID는 0보다 커야 합니다. 입력된 ID: $couponId" }
    }

    /**
     * 사용자 쿠폰 ID 유효성 검증
     */
    private fun validateUserCouponId(userCouponId: Long) {
        require(userCouponId > 0) { "사용자 쿠폰 ID는 0보다 커야 합니다. 입력된 ID: $userCouponId" }
    }
}
