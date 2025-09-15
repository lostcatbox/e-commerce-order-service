package kr.hhplus.be.server.core.coupon.service

import kr.hhplus.be.server.core.coupon.domain.Coupon
import kr.hhplus.be.server.core.coupon.repository.CouponRepository
import kr.hhplus.be.server.support.lock.DistributedLockManagerInterface
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 쿠폰 서비스 구현체 - 쿠폰 도메인만 담당
 */
@Service
class
CouponService(
    private val couponRepository: CouponRepository,
    private val distributedLockManager: DistributedLockManagerInterface,
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
     * 쿠폰 재고 차감 (발급 시)
     */
    override fun issueCoupon(couponId: Long): Coupon {
        validateCouponId(couponId)

        val lockKey = "lock:coupon-issue:$couponId"
        return distributedLockManager.executeWithLock(lockKey) {
            // 쿠폰 조회 (베타락)
            val coupon =
                couponRepository.findByCouponIdWithPessimisticLock(couponId)
                    ?: throw IllegalArgumentException("존재하지 않는 쿠폰입니다. 쿠폰 ID: $couponId")

            // 도메인 로직을 통한 쿠폰 발급 (재고 차감)
            coupon.issueCoupon()

            // 재고 차감된 쿠폰 저장
            couponRepository.save(coupon)
        }
    }

    /**
     * 쿠폰 ID 유효성 검증
     */
    private fun validateCouponId(couponId: Long) {
        require(couponId > 0) { "쿠폰 ID는 0보다 커야 합니다. 입력된 ID: $couponId" }
    }
}
