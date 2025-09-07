package kr.hhplus.be.server.infra.persistence.product

import kr.hhplus.be.server.core.product.domain.ProductSale
import kr.hhplus.be.server.core.product.repository.ProductSaleRepository
import kr.hhplus.be.server.core.product.service.dto.ProductPeriodSaleDto
import kr.hhplus.be.server.infra.persistence.product.jpa.JpaProductSaleRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 상품 판매량 Repository 구현체
 */
@Repository
class ProductSaleRepositoryImpl(
    private val jpaProductSaleRepository: JpaProductSaleRepository,
) : ProductSaleRepository {
    override fun findPopularProducts(dateTime: LocalDate): List<ProductSale> {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val saleDate = dateTime.format(formatter).toLong()
        return jpaProductSaleRepository.findBySaleDate(saleDate)
    }

    override fun findPopularProductsInfo(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<ProductPeriodSaleDto> {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val startDateLong = startDate.format(formatter).toLong()
        val endDateLong = endDate.format(formatter).toLong()

        return jpaProductSaleRepository.findPopularProductsInfo(
            startDate = startDateLong,
            endDate = endDateLong,
            limit = 5,
        )
    }

    override fun saveProductSale(
        productId: Long,
        saleDate: Long,
        quantity: Int,
    ) {
        val existingProductSale = jpaProductSaleRepository.findByProductIdAndSaleDate(productId, saleDate)

        if (existingProductSale != null) {
            // 기존 데이터가 있으면 수량 업데이트 (새 엔티티 생성)
            val updatedProductSale =
                ProductSale(
                    productId = productId,
                    saleDate = saleDate,
                    totalQuantity = existingProductSale.totalQuantity + quantity,
                )
            jpaProductSaleRepository.save(updatedProductSale)
        } else {
            // 새 데이터 생성
            val newProductSale =
                ProductSale(
                    productId = productId,
                    saleDate = saleDate,
                    totalQuantity = quantity,
                )
            jpaProductSaleRepository.save(newProductSale)
        }
    }
}
