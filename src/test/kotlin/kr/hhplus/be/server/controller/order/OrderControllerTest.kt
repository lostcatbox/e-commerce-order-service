package kr.hhplus.be.server.controller.order

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(OrderController::class)
@DisplayName("주문 컨트롤러 테스트")
class OrderControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @DisplayName("주문을 생성하고 결제한다.")
    @Test
    fun createOrder() {
        val request =
            mapOf(
                "userId" to 1L,
                "orderItems" to
                    listOf(
                        mapOf("productId" to 1L, "quantity" to 2),
                        mapOf("productId" to 2L, "quantity" to 1),
                    ),
                "couponId" to 10L,
            )

        mockMvc
            .perform(
                post("/api/v1/orders")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON),
            ).andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(1L))
            .andExpect(jsonPath("$.orderId").isNumber)
            .andExpect(jsonPath("$.orderItems").isArray)
            .andExpect(jsonPath("$.finalAmount").isNumber)
    }
}
