package kr.hhplus.be.server.controller.product

import kr.hhplus.be.server.controller.product.dto.*
import kr.hhplus.be.server.core.product.service.ProductSaleServiceInterface
import kr.hhplus.be.server.core.product.service.ProductServiceInterface
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

/**
 * 상품 유스케이스
 */
@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val productService: ProductServiceInterface,
    private val productSaleService: ProductSaleServiceInterface,
) {
    /**
     * 상품 정보 조회
     * GET /api/v1/products/{productId}
     */
    @GetMapping("/{productId}")
    fun getProduct(
        @PathVariable productId: Long,
    ): ResponseEntity<ProductResponse> {
        val product = productService.getProduct(productId)

        val response =
            ProductResponse(
                productId = product.productId,
                name = product.name,
                description = product.description,
                price = product.price,
                stock = product.getStock(),
            )
        return ResponseEntity.ok(response)
    }

    /**
     * 인기 판매 상품 조회 (최근 3일간 상위 5개)
     * GET /api/v1/products/popular
     */
    @GetMapping("/popular")
    fun getPopularProducts(): ResponseEntity<PopularProductsResponse> {
        val now = LocalDate.now()
        val popularProducts = productSaleService.getPopularProducts(now)

        val productsInfo =
            popularProducts.map { product ->
                PopularProductInfo(
                    productId = product.productId,
                    name = product.productName,
                    description = product.productDescription,
                    price = product.productPrice,
                    salesCount = product.totalSales,
                )
            }

        val response =
            PopularProductsResponse(
                products = productsInfo,
                generatedAt = System.currentTimeMillis(),
            )
        return ResponseEntity.ok(response)
    }
}
