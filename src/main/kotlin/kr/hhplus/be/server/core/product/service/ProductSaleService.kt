package kr.hhplus.be.server.core.product.service

import com.sun.org.apache.xalan.internal.lib.ExsltDatetime.dateTime
import jakarta.transaction.Transactional
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

        // Database에서 3일간 판매량을 집계하여 조회 (ProductSale 테이블만 사용)
        // TODO : ProductName 때문에 N+1 문제가 발생할 수 있음 -> 반정규화 또는 캐싱 고려
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

    @Transactional
    override fun recordProductSale(
        productId: Long,
        quantity: Int,
    ) {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val saleDate = today.format(formatter).toLong()

        productSaleRepository.saveProductSale(
            productId = productId,
            saleDate = saleDate,
            quantity = quantity,
        )
    }
}
