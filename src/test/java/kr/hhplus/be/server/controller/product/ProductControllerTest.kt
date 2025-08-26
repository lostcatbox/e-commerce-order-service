package kr.hhplus.be.server.controller.product

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(ProductController::class)
class ProductControllerTest {

	@Autowired
	lateinit var mockMvc: MockMvc

	@DisplayName("인기 상품 목록을 조회한다.")
	@Test
	fun getPopularProducts() {
		mockMvc.perform(
			get("/api/v1/products/popular")
		)
			.andDo(print())
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.products").isArray)
			.andExpect(jsonPath("$.generatedAt").isNumber)
	}
}
