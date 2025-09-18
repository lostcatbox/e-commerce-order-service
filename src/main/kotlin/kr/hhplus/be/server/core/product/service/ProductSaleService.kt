package kr.hhplus.be.server.core.product.service

import jakarta.transaction.Transactional
import kr.hhplus.be.server.core.product.domain.ProductSale
import kr.hhplus.be.server.core.product.repository.ProductRepository
import kr.hhplus.be.server.core.product.repository.ProductSaleRepository
import kr.hhplus.be.server.core.product.service.dto.PopularProductDto
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class ProductSaleService(
    private val productSaleRepository: ProductSaleRepository,
    private val productRepository: ProductRepository,
) : ProductSaleServiceInterface {
    companion object {
        private val log = LoggerFactory.getLogger(ProductSaleService::class.java)
    }

    /**
     * 최근 3일간 가장 많이 팔린 상위 5개 상품 조회
     * 캐시에만 의존하며, 캐시 미스 시 빈 결과 반환
     *
     * @Cacheable: 캐시된 데이터가 있으면 반환, 없으면 빈 결과 반환
     * - 캐시 키: "popular_products:top5" (고정 키 사용)
     */
    @Cacheable(
        value = ["popular_products"],
        key = "'top5'",
    )
    override fun getPopularProducts(rankDate: LocalDate): List<PopularProductDto> {
        // 캐시 미스 시 빈 결과 반환
        log.warn("인기 상품 캐시 미스 발생 - 빈 결과 반환. 배치 프로세스에서 캐시가 곧 갱신됩니다.")
        return emptyList()
    }

    /**
     * 상품 판매량 기록
     */
    @Transactional
    override fun recordProductSale(
        productId: Long,
        quantity: Int,
    ) {
        validateProductId(productId)
        validateQuantity(quantity)

        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val saleDate = today.format(formatter).toLong()

        val productSale =
            ProductSale.createNewSale(
                productId = productId,
                saleDate = saleDate,
                quantity = quantity,
            )

        productSaleRepository.save(productSale)

        log.debug("상품 판매량 기록 완료: productId={}, quantity={}", productId, quantity)
    }

    /**
     * 인기 상품 캐시 갱신 배치 작업
     * (1분마다 실행되어 Redis Sorted Set에서 최신 데이터를 조회하여 캐시 갱신)
     */
    @CachePut(value = ["popular_products"], key = "'top5'")
    @Scheduled(fixedRate = 3000) // 1분마다 실행
    fun updatePopularProductsCache(): List<PopularProductDto> =
        try {
            val today = LocalDate.now()
            val threeDaysAgo = today.minusDays(3)

            log.info("인기 상품 캐시 갱신 시작")

            // 최근 3일간 데이터 조회
            val popularProductsInfo = productSaleRepository.findPopularProductsInfo(threeDaysAgo, today)

            // 상품 정보와 함께 DTO 생성
            val result =
                popularProductsInfo.mapNotNull { salesInfo ->
                    productRepository.findByProductId(salesInfo.productId)?.let { product ->
                        PopularProductDto(
                            productId = salesInfo.productId,
                            productName = product.name,
                            productDescription = product.description,
                            productPrice = product.price,
                            totalSales = salesInfo.totalSales,
                        )
                    }
                }

            log.info("인기 상품 캐시 갱신 완료: {} 개 상품", result.size)
            result
        } catch (exception: Exception) {
            log.error("인기 상품 캐시 갱신 실패", exception)
            // 실패 시 빈 리스트 반환하여 캐시 무효화 방지
            emptyList()
        }

    /**
     * 상품 ID 유효성 검증
     */
    private fun validateProductId(productId: Long) {
        require(productId > 0) { "상품 ID는 0보다 커야 합니다. 입력된 ID: $productId" }
    }

    /**
     * 수량 유효성 검증
     */
    private fun validateQuantity(quantity: Int) {
        require(quantity > 0) { "판매 수량은 0보다 커야 합니다. 입력된 수량: $quantity" }
    }
}
