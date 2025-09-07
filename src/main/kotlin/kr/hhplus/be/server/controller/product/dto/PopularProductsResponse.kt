package kr.hhplus.be.server.controller.product.dto

/**
 * 인기 상품 조회 응답
 */
data class PopularProductsResponse(
    val products: List<PopularProductInfo>,
    val generatedAt: Long,
)

/**
 * 인기 상품 정보
 */
data class PopularProductInfo(
    val productId: Long,
    val name: String,
    val description: String,
    val price: Long,
    val salesCount: Long,
)
