package kr.hhplus.be.server.controller.point

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(PointController::class)
class PointControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @DisplayName("포인트 잔액을 조회한다.")
    @Test
    fun getPointBalance() {
        mockMvc
            .perform(
                get("/api/v1/points/{userId}", 1L),
            ).andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(1L))
            .andExpect(jsonPath("$.balance").isNumber)
    }

    @DisplayName("포인트를 충전한다.")
    @Test
    fun chargePoint() {
        val request = mapOf("amount" to 10000L)

        mockMvc
            .perform(
                patch("/api/v1/points/{userId}/charge", 1L)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON),
            ).andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(1L))
            .andExpect(jsonPath("$.chargedAmount").value(10000L))
            .andExpect(jsonPath("$.currentBalance").isNumber)
    }
}
