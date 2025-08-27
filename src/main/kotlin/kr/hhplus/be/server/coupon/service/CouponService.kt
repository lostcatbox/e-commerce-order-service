package kr.hhplus.be.server.coupon.service

import kr.hhplus.be.server.coupon.domain.Coupon
import kr.hhplus.be.server.coupon.domain.UserCoupon
import kr.hhplus.be.server.coupon.repository.CouponRepository
import kr.hhplus.be.server.coupon.repository.UserCouponRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 쿠폰 서비스 구현체
 */
@Service
@Transactional
class CouponService(
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository,
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
     * 선착순 쿠폰 발급
     */
    @Transactional
    override fun issueCoupon(userId: Long, couponId: Long): UserCoupon {
        validateUserId(userId)
        validateCouponId(couponId)

        // 쿠폰 조회
        val coupon = couponRepository.findByCouponId(couponId)
            ?: throw IllegalArgumentException("존재하지 않는 쿠폰입니다. 쿠폰 ID: $couponId")

        // 이미 발급받은 쿠폰인지 확인
        val existingUserCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
        if (existingUserCoupon != null) {
            throw IllegalStateException("이미 발급받은 쿠폰입니다. 사용자 ID: $userId, 쿠폰 ID: $couponId")
        }

        // 도메인 로직을 통한 쿠폰 발급 (재고 차감)
        val issuedCoupon = coupon.issueCoupon()
        
        // 재고 차감된 쿠폰 저장
        couponRepository.save(issuedCoupon)

        // 유저 쿠폰 생성 및 저장
        val userCoupon = UserCoupon.issueCoupon(userId, couponId)
        return userCouponRepository.save(userCoupon)
    }

    /**
     * 사용자의 쿠폰 목록 조회
     */
    @Transactional(readOnly = true)
    override fun getUserCoupons(userId: Long): List<UserCoupon> {
        validateUserId(userId)
        
        return userCouponRepository.findByUserId(userId)
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
}
