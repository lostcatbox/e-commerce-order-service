package kr.hhplus.be.server.controller.point

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.point.domain.UserPoint
import kr.hhplus.be.server.point.service.PointServiceInterface
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(PointController::class)
@DisplayName("포인트 컨트롤러 테스트")
class PointControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var pointService: PointServiceInterface

    @DisplayName("포인트 잔액을 조회한다.")
    @Test
    fun getPointBalance() {
        // given
        val userId = 1L
        val userPoint = UserPoint(
            userId = userId,
            balance = 50000L,
            lastUpdatedAt = System.currentTimeMillis()
        )

        given(pointService.getPointBalance(userId)).willReturn(userPoint)

        // when & then
        mockMvc
            .perform(
                get("/api/v1/points/{userId}", userId),
            ).andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.balance").value(50000L))
            .andExpect(jsonPath("$.lastUpdatedAt").isNumber)

        verify(pointService).getPointBalance(userId)
    }

    @DisplayName("포인트를 충전한다.")
    @Test
    fun chargePoint() {
        // given
        val userId = 1L
        val chargeAmount = 10000L
        val previousUserPoint = UserPoint(
            userId = userId,
            balance = 50000L,
            lastUpdatedAt = System.currentTimeMillis()
        )
        val chargedUserPoint = UserPoint(
            userId = userId,
            balance = 60000L,
            lastUpdatedAt = System.currentTimeMillis()
        )
        val request = mapOf("amount" to chargeAmount)

        given(pointService.getPointBalance(userId)).willReturn(previousUserPoint)
        given(pointService.chargePoint(userId, chargeAmount)).willReturn(chargedUserPoint)

        // when & then
        mockMvc
            .perform(
                patch("/api/v1/points/{userId}/charge", userId)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON),
            ).andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.chargedAmount").value(chargeAmount))
            .andExpect(jsonPath("$.previousBalance").value(50000L))
            .andExpect(jsonPath("$.currentBalance").value(60000L))
            .andExpect(jsonPath("$.chargedAt").isNumber)

        verify(pointService).getPointBalance(userId)
        verify(pointService).chargePoint(userId, chargeAmount)
    }
}