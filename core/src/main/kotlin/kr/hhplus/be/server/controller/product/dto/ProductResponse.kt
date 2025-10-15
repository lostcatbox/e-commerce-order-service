package kr.hhplus.be.server.controller.product.dto

/**
 * 상품 조회 응답
 */
data class ProductResponse(
    val productId: Long,
    val name: String,
    val description: String,
    val price: Long,
    val stock: Int
)
