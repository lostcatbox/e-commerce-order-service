package kr.hhplus.be.server.controller.product

import kr.hhplus.be.server.controller.product.dto.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 상품 유스케이스
 */
@RestController
@RequestMapping("/api/v1/products")
class ProductController {
    /**
     * 인기 판매 상품 조회 (최근 3일간 상위 5개)
     * GET /api/v1/products/popular
     */
    @GetMapping("/popular")
    fun getPopularProducts(): ResponseEntity<PopularProductsResponse> {
        // TODO: 인기 상품 서비스 호출
        val mockProducts =
            listOf(
                PopularProductInfo(
                    productId = 1L,
                    name = "인기상품 1",
                    description = "인기상품 1 설명",
                    price = 15000L,
                    stock = 50,
                    salesCount = 1000,
                ),
                PopularProductInfo(
                    productId = 2L,
                    name = "인기상품 2",
                    description = "인기상품 2 설명",
                    price = 20000L,
                    stock = 30,
                    salesCount = 800,
                ),
                PopularProductInfo(
                    productId = 3L,
                    name = "인기상품 3",
                    description = "인기상품 3 설명",
                    price = 12000L,
                    stock = 80,
                    salesCount = 600,
                ),
                PopularProductInfo(
                    productId = 4L,
                    name = "인기상품 4",
                    description = "인기상품 4 설명",
                    price = 25000L,
                    stock = 20,
                    salesCount = 500,
                ),
                PopularProductInfo(
                    productId = 5L,
                    name = "인기상품 5",
                    description = "인기상품 5 설명",
                    price = 18000L,
                    stock = 60,
                    salesCount = 400,
                ),
            )

        val mockResponse =
            PopularProductsResponse(
                products = mockProducts,
                generatedAt = System.currentTimeMillis(),
            )
        return ResponseEntity.ok(mockResponse)
    }
}
