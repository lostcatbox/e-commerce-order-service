package kr.hhplus.be.server.fake.lock

import kr.hhplus.be.server.support.lock.DistributedLockManagerInterface

/**
 * 테스트용 가짜 분산락 매니저
 * 실제 락을 걸지 않고 바로 비즈니스 로직을 실행
 */
class FakeDistributedLockManager : DistributedLockManagerInterface {
    /**
     * 테스트에서는 락 없이 바로 비즈니스 로직 실행
     */
    override fun <T> executeWithLock(
        lockKey: String,
        waitTime: Long,
        leaseTime: Long,
        business: () -> T,
    ): T {
        // 락 없이 바로 비즈니스 로직 실행
        return business()
    }

    /**
     * 테스트에서는 락 없이 바로 비즈니스 로직 실행 (반환값 없음)
     */
    override fun executeWithLockVoid(
        lockKey: String,
        waitTime: Long,
        leaseTime: Long,
        business: () -> Unit,
    ) {
        // 락 없이 바로 비즈니스 로직 실행
        business()
    }
}
