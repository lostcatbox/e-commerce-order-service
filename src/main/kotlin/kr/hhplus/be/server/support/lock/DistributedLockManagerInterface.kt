package kr.hhplus.be.server.support.lock

/**
 * 분산락 매니저 인터페이스
 * 락 획득 → 트랜잭션 시작 → 비즈니스로직 → 트랜잭션 종료 → 락 해제 순서 보장
 */
interface DistributedLockManagerInterface {
    /**
     * 분산락을 획득하고 트랜잭션 내에서 비즈니스 로직을 실행 (반환값 있음)
     *
     * @param lockKey 락 키
     * @param waitTime 락 획득 대기 시간 (초)
     * @param leaseTime 락 보유 시간 (초)
     * @param business 실행할 비즈니스 로직
     * @return 비즈니스 로직 실행 결과
     */
    fun <T> executeWithLock(
        lockKey: String,
        waitTime: Long = 5L,
        leaseTime: Long = 10L,
        business: () -> T,
    ): T

    /**
     * 분산락을 획득하고 트랜잭션 내에서 비즈니스 로직을 실행 (반환값 없음)
     *
     * @param lockKey 락 키
     * @param waitTime 락 획득 대기 시간 (초)
     * @param leaseTime 락 보유 시간 (초)
     * @param business 실행할 비즈니스 로직
     */
    fun executeWithLockVoid(
        lockKey: String,
        waitTime: Long = 5L,
        leaseTime: Long = 10L,
        business: () -> Unit,
    )

    /**
     * 사용자별 포인트 작업을 위한 락 키 생성
     */
    fun getUserPointLockKey(userId: Long): String

    /**
     * 쿠폰 발급을 위한 락 키 생성
     */
    fun getCouponIssueLockKey(couponId: Long): String

    /**
     * 상품 재고 차감을 위한 락 키 생성
     */
    fun getProductStockLockKey(productId: Long): String
}
