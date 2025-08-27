package kr.hhplus.be.server.controller.coupon

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(CouponController::class)
class CouponControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @DisplayName("선착순 쿠폰을 발급한다.")
    @Test
    fun issueCoupon() {
        val request = mapOf("userId" to 1L)

        mockMvc
            .perform(
                post("/api/v1/coupons/{couponId}/issue", 1L)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON),
            ).andDo(print())
            .andExpect(status().isOk)
    }

    @DisplayName("사용자 보유 쿠폰 목록을 조회한다.")
    @Test
    fun getUserCoupons() {
        mockMvc
            .perform(
                get("/api/v1/coupons/users/{userId}", 1L)
                    .param("status", "ALL"),
            ).andDo(print())
            .andExpect(status().isOk)
            .andExpect(
                jsonPath("$.coupons").isArray,
            )
    }
}
