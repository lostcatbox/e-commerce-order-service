package kr.hhplus.be.server.core.product.service.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 인기 상품 정보 DTO
 * Redis 캐시 직렬화/역직렬화 지원
 */
data class PopularProductDto @JsonCreator constructor(
    @JsonProperty("productId") val productId: Long,
    @JsonProperty("productName") val productName: String,
    @JsonProperty("productDescription") val productDescription: String,
    @JsonProperty("productPrice") val productPrice: Long,
    @JsonProperty("totalSales") val totalSales: Long,
)
