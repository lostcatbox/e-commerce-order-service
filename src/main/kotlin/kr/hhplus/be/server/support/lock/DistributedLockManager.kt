package kr.hhplus.be.server.support.lock

import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.TimeUnit

/**
 * Redisson을 활용한 분산락 매니저
 *
 * 특이사항
 * - 반드시 락 획득 -> 트랜잭션 시작 -> 비즈니스로직 -> 트랜잭션 종료 -> 락 해제 순서로 진행
 * - try-with-resources 또는 finally 블록에서 락 해제 보장
 */
@Component
class DistributedLockManager(
    private val redissonClient: RedissonClient,
    private val transactionTemplate: TransactionTemplate,
) : DistributedLockManagerInterface {
    private val logger = LoggerFactory.getLogger(DistributedLockManager::class.java)

    companion object {
        private const val DEFAULT_WAIT_TIME = 5L
        private const val DEFAULT_LEASE_TIME = 10L
    }

    /**
     * 분산락을 획득하고 트랜잭션 내에서 비즈니스 로직을 실행
     *
     * 실행 순서:
     * 1. 분산락 획득
     * 2. 트랜잭션 시작 (TransactionTemplate)
     * 3. 비즈니스 로직 실행
     * 4. 트랜잭션 종료 (커밋/롤백)
     * 5. 분산락 해제
     *
     * @param lockKey 락 키
     * @param waitTime 락 획득 대기 시간 (초)
     * @param leaseTime 락 보유 시간 (초)
     * @param business 실행할 비즈니스 로직
     * @return 비즈니스 로직 실행 결과
     */
    override fun <T> executeWithLock(
        lockKey: String,
        waitTime: Long,
        leaseTime: Long,
        business: () -> T,
    ): T {
        val lock = redissonClient.getLock(lockKey)

        try {
            val acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS)
            if (!acquired) {
                logger.warn("분산락 획득 실패: key=$lockKey, waitTime=$waitTime")
                throw DistributedLockException("분산락 획득에 실패했습니다: $lockKey")
            }

            logger.debug("분산락 획득 성공: key=$lockKey")

            // 락 획득 후 수동으로 트랜잭션 실행
            return transactionTemplate.execute {
                business()
            } ?: throw DistributedLockException("트랜잭션 내에서 null 반환됨: $lockKey")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.error("분산락 대기 중 인터럽트 발생: key=$lockKey", e)
            throw DistributedLockException("분산락 대기 중 인터럽트가 발생했습니다: $lockKey", e)
        } finally {
            // 락이 현재 스레드에 의해 보유되고 있는 경우에만 해제
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
                logger.debug("분산락 해제 완료: key=$lockKey")
            } else {
                logger.warn("락이 현재 스레드에 의해 보유되지 않음: key=$lockKey")
            }
        }
    }

    /**
     * 반환값이 없는 비즈니스 로직을 위한 분산락 실행 (Unit 반환)
     *
     * @param lockKey 락 키
     * @param waitTime 락 획득 대기 시간 (초)
     * @param leaseTime 락 보유 시간 (초)
     * @param business 실행할 비즈니스 로직 (반환값 없음)
     */
    override fun executeWithLockVoid(
        lockKey: String,
        waitTime: Long,
        leaseTime: Long,
        business: () -> Unit,
    ) {
        val lock = redissonClient.getLock(lockKey)

        try {
            val acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS)
            if (!acquired) {
                logger.warn("분산락 획득 실패: key=$lockKey, waitTime=$waitTime")
                throw DistributedLockException("분산락 획득에 실패했습니다: $lockKey")
            }

            logger.debug("분산락 획득 성공: key=$lockKey")

            // 락 획득 후 수동으로 트랜잭션 실행
            transactionTemplate.execute<Unit> {
                business()
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.error("분산락 대기 중 인터럽트 발생: key=$lockKey", e)
            throw DistributedLockException("분산락 대기 중 인터럽트가 발생했습니다: $lockKey", e)
        } finally {
            // 락이 현재 스레드에 의해 보유되고 있는 경우에만 해제
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
                logger.debug("분산락 해제 완료: key=$lockKey")
            } else {
                logger.warn("락이 현재 스레드에 의해 보유되지 않음: key=$lockKey")
            }
        }
    }

    /**
     * 사용자별 포인트 작업을 위한 락 키 생성
     */
    override fun getUserPointLockKey(userId: Long): String = "lock:user-point:$userId"

    /**
     * 쿠폰 발급을 위한 락 키 생성
     */
    override fun getCouponIssueLockKey(couponId: Long): String = "lock:coupon-issue:$couponId"

    /**
     * 상품 재고 차감을 위한 락 키 생성
     */
    override fun getProductStockLockKey(productId: Long): String = "lock:product-stock:$productId"
}

/**
 * 분산락 관련 예외
 */
class DistributedLockException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
