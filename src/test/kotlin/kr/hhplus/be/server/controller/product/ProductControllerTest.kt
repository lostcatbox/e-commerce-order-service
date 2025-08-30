package kr.hhplus.be.server.controller.product

import kr.hhplus.be.server.core.product.domain.Product
import kr.hhplus.be.server.core.product.service.ProductServiceInterface
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(ProductController::class)
@DisplayName("상품 컨트롤러 테스트")
class ProductControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var productService: ProductServiceInterface

    @DisplayName("인기 상품 목록을 조회한다.")
    @Test
    fun getPopularProducts() {
        // given
        val popularProducts =
            listOf(
                Product(
                    productId = 1L,
                    name = "인기상품 1",
                    description = "인기상품 1 설명",
                    price = 15000L,
                    stock = 100,
                ),
                Product(
                    productId = 2L,
                    name = "인기상품 2",
                    description = "인기상품 2 설명",
                    price = 25000L,
                    stock = 80,
                ),
            )

        given(productService.getPopularProducts()).willReturn(popularProducts)

        // when & then
        mockMvc
            .perform(
                get("/api/v1/products/popular"),
            ).andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.products").isArray)
            .andExpect(jsonPath("$.products.length()").value(2))
            .andExpect(jsonPath("$.products[0].productId").value(1L))
            .andExpect(jsonPath("$.products[0].name").value("인기상품 1"))
            .andExpect(jsonPath("$.products[0].stock").value(100))
            .andExpect(jsonPath("$.products[1].productId").value(2L))
            .andExpect(jsonPath("$.products[1].name").value("인기상품 2"))
            .andExpect(jsonPath("$.products[1].stock").value(80))
            .andExpect(jsonPath("$.generatedAt").isNumber)

        verify(productService).getPopularProducts()
    }

    @DisplayName("상품 상세 정보를 조회한다.")
    @Test
    fun getProduct() {
        // given
        val productId = 1L
        val product =
            Product(
                productId = productId,
                name = "테스트 상품",
                description = "테스트 상품 설명",
                price = 20000L,
                stock = 50,
            )

        given(productService.getProduct(productId)).willReturn(product)

        // when & then
        mockMvc
            .perform(
                get("/api/v1/products/{productId}", productId),
            ).andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.productId").value(productId))
            .andExpect(jsonPath("$.name").value("테스트 상품"))
            .andExpect(jsonPath("$.description").value("테스트 상품 설명"))
            .andExpect(jsonPath("$.price").value(20000L))
            .andExpect(jsonPath("$.stock").value(50))

        verify(productService).getProduct(productId)
    }
}
