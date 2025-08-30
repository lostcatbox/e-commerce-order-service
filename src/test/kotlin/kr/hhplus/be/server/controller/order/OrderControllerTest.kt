package kr.hhplus.be.server.controller.order

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.core.order.domain.Order
import kr.hhplus.be.server.core.order.domain.OrderItem
import kr.hhplus.be.server.core.order.domain.OrderStatus
import kr.hhplus.be.server.facade.order.OrderFacade
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
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

    @MockBean
    private lateinit var orderFacade: OrderFacade

    @DisplayName("주문을 생성하고 결제한다.")
    @Test
    fun createOrder() {
        // given
        val userId = 1L
        val orderId = 100L
        val paymentId = 200L

        val completedOrder =
            Order(
                orderId = orderId,
                userId = userId,
                orderItems =
                    listOf(
                        OrderItem(productId = 1L, quantity = 2, unitPrice = 10000L),
                        OrderItem(productId = 2L, quantity = 1, unitPrice = 20000L),
                    ),
                orderStatus = OrderStatus.COMPLETED,
                usedCouponId = 10L,
                paymentId = paymentId,
                createdAt = System.currentTimeMillis(),
            )

        val request =
            mapOf(
                "userId" to userId,
                "orderItems" to
                    listOf(
                        mapOf("productId" to 1L, "quantity" to 2),
                        mapOf("productId" to 2L, "quantity" to 1),
                    ),
                "couponId" to 10L,
            )

        given(orderFacade.processOrder(any())).willReturn(completedOrder)

        // when & then
        mockMvc
            .perform(
                post("/api/v1/orders")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON),
            ).andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.orderId").value(orderId))
            .andExpect(jsonPath("$.orderItems").isArray)
            .andExpect(jsonPath("$.orderItems.length()").value(2))
            .andExpect(jsonPath("$.orderItems[0].productId").value(1L))
            .andExpect(jsonPath("$.orderItems[0].quantity").value(2))
            .andExpect(jsonPath("$.orderItems[0].unitPrice").value(10000L))
            .andExpect(jsonPath("$.orderItems[1].productId").value(2L))
            .andExpect(jsonPath("$.orderItems[1].quantity").value(1))
            .andExpect(jsonPath("$.orderItems[1].unitPrice").value(20000L))
            .andExpect(jsonPath("$.paymentId").value(paymentId))
            .andExpect(jsonPath("$.orderStatus").value("COMPLETED"))
            .andExpect(jsonPath("$.usedCouponId").value(10L))
            .andExpect(jsonPath("$.createdAt").isNumber)

        verify(orderFacade).processOrder(any())
    }

    @DisplayName("쿠폰 없이 주문을 생성하고 결제한다.")
    @Test
    fun createOrderWithoutCoupon() {
        // given
        val userId = 1L
        val orderId = 100L
        val paymentId = 200L

        val completedOrder =
            Order(
                orderId = orderId,
                userId = userId,
                orderItems =
                    listOf(
                        OrderItem(productId = 1L, quantity = 1, unitPrice = 15000L),
                    ),
                orderStatus = OrderStatus.COMPLETED,
                usedCouponId = null,
                paymentId = paymentId,
                createdAt = System.currentTimeMillis(),
            )

        val request =
            mapOf(
                "userId" to userId,
                "orderItems" to
                    listOf(
                        mapOf("productId" to 1L, "quantity" to 1),
                    ),
            )

        given(orderFacade.processOrder(any())).willReturn(completedOrder)

        // when & then
        mockMvc
            .perform(
                post("/api/v1/orders")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON),
            ).andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.orderId").value(orderId))
            .andExpect(jsonPath("$.orderItems").isArray)
            .andExpect(jsonPath("$.orderItems.length()").value(1))
            .andExpect(jsonPath("$.paymentId").value(paymentId))
            .andExpect(jsonPath("$.orderStatus").value("COMPLETED"))
            .andExpect(jsonPath("$.usedCouponId").isEmpty)
            .andExpect(jsonPath("$.createdAt").isNumber)

        verify(orderFacade).processOrder(any())
    }
}
