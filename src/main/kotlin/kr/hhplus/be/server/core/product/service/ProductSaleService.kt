package kr.hhplus.be.server.core.product.service

import jakarta.transaction.Transactional
import kr.hhplus.be.server.core.product.domain.ProductSale
import kr.hhplus.be.server.core.product.repository.ProductRepository
import kr.hhplus.be.server.core.product.repository.ProductSaleRepository
import kr.hhplus.be.server.core.product.service.dto.PopularProductDto
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class ProductSaleService(
    private val productSaleRepository: ProductSaleRepository,
    private val productRepository: ProductRepository,
) : ProductSaleServiceInterface {
    /**
     * 최근 3일간 가장 많이 팔린 상위 5개 상품 조회
     * ProductSale 테이블만 사용하여 판매량 집계 후 상품 정보 조회
     */
    override fun getPopularProducts(): List<PopularProductDto> {
        val now = LocalDate.now()
        val threeDaysAgo = now.minusDays(3)

        // DB 에서 3일간 판매량을 집계하여 조회 (ProductSale 테이블만 사용)(실시간 데이터 아님)
        // TODO : Product의 name, description, price 때문에 N+1 문제가 발생 중 -> 반정규화 또는 캐싱 고려
        return productSaleRepository
            .findPopularProductsInfo(threeDaysAgo, now)
            .mapNotNull { salesInfo ->
                // 각 상품 정보를 별도로 조회
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
    }

    /**
     * 상품 판매량 기록
     * 기존 판매 데이터가 있으면 수량을 누적하고, 없으면 새로 생성
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

        // 기존 판매 데이터 조회
        val existingSale = productSaleRepository.findByProductIdAndSaleDate(productId, saleDate)

        val productSale =
            if (existingSale != null) {
                // 판매 데이터에 수량 추가
                existingSale.addSaleQuantity(quantity)
            } else {
                // 새 판매 데이터 생성
                ProductSale.createNewSale(
                    productId = productId,
                    saleDate = saleDate,
                    quantity = quantity,
                )
            }

        // 판매 데이터 저장
        productSaleRepository.save(productSale)
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
