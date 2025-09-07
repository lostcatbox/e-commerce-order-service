package kr.hhplus.be.server.core.product.service.dto

data class PopularProductDto(
    val productId: Long,
    val productName: String,
    val productDescription: String,
    val productPrice: Long,
    val totalSales: Long,
)
