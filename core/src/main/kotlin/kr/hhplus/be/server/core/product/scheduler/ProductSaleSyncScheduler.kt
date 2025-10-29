package kr.hhplus.be.server.core.product.scheduler

import kr.hhplus.be.server.core.product.service.ProductSaleService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 상품 판매량 Redis → DB 동기화 스케줄러
 * Write-Back 캐싱 패턴에서 Redis에 저장된 데이터를 주기적으로 DB에 동기화
 */
@Component
class ProductSaleSyncScheduler(
    private val productSaleService: ProductSaleService,
) {
    companion object {
        private val log = LoggerFactory.getLogger(ProductSaleSyncScheduler::class.java)
    }

    /**
     * Redis → DB 동기화 스케줄러
     * 매일 자정에 실행되어 Redis에 저장된 상품 판매량 데이터를 DB로 동기화
     */
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행
    fun syncRedisToDatabase() {
        try {
            log.info("Redis → DB 동기화 스케줄러 시작")

            val success = productSaleService.syncRedisCacheToDatabase()

            if (success) {
                log.info("Redis → DB 동기화 스케줄러 완료")
            } else {
                log.error("Redis → DB 동기화 스케줄러 실패")
            }
        } catch (exception: Exception) {
            log.error("Redis → DB 동기화 스케줄러 실행 중 오류 발생", exception)
        }
    }
}
