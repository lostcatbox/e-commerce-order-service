package kr.hhplus.be.server.controller.coupon

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.controller.coupon.dto.CouponIssueRequest
import kr.hhplus.be.server.coupon.domain.Coupon
import kr.hhplus.be.server.coupon.domain.CouponStatus
import kr.hhplus.be.server.coupon.domain.UserCoupon
import kr.hhplus.be.server.coupon.service.CouponServiceInterface
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(CouponController::class)
@DisplayName("쿠폰 컨트롤러 테스트")
class CouponControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var couponService: CouponServiceInterface

    @Nested
    @DisplayName("쿠폰 정보 조회 API 테스트")
    inner class CouponInfoTest {
        @Test
        @DisplayName("존재하는 쿠폰 ID로 쿠폰 정보를 조회할 수 있다")
        fun `존재하는_쿠폰_ID로_쿠폰_정보를_조회할_수_있다`() {
            // given
            val couponId = 1L
            val coupon =
                Coupon(
                    couponId = couponId,
                    description = "1000원 할인 쿠폰",
                    discountAmount = 1000L,
                    stock = 100,
                    couponStatus = CouponStatus.OPENED,
                )

            given(couponService.getCouponInfo(couponId)).willReturn(coupon)

            // when & then
            mockMvc
                .perform(get("/api/coupons/{couponId}", couponId))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.couponId").value(couponId))
                .andExpect(jsonPath("$.description").value("1000원 할인 쿠폰"))
                .andExpect(jsonPath("$.discountAmount").value(1000))
                .andExpect(jsonPath("$.stock").value(100))
                .andExpect(jsonPath("$.couponStatus").value("OPENED"))

            verify(couponService).getCouponInfo(couponId)
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰 ID로 조회 시 400 에러가 발생한다")
        fun `존재하지_않는_쿠폰_ID로_조회_시_400_에러가_발생한다`() {
            // given
            val couponId = 999L

            given(couponService.getCouponInfo(couponId))
                .willThrow(IllegalArgumentException("존재하지 않는 쿠폰입니다. 쿠폰 ID: $couponId"))

            // when & then
            mockMvc
                .perform(get("/api/coupons/{couponId}", couponId))
                .andExpect(status().isBadRequest)

            verify(couponService).getCouponInfo(couponId)
        }
    }

    @Nested
    @DisplayName("쿠폰 발급 API 테스트")
    inner class CouponIssueTest {
        @Test
        @DisplayName("정상적으로 쿠폰을 발급할 수 있다")
        fun `정상적으로_쿠폰을_발급할_수_있다`() {
            // given
            val userId = 1L
            val couponId = 1L
            val request = CouponIssueRequest(userId = userId, couponId = couponId)
            val userCoupon = UserCoupon.issueCoupon(userId, couponId)

            given(couponService.issueCoupon(userId, couponId)).willReturn(userCoupon)

            // when & then
            mockMvc
                .perform(
                    post("/api/coupons/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.couponId").value(couponId))
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.issuedAt").exists())

            verify(couponService).issueCoupon(userId, couponId)
        }

        @Test
        @DisplayName("이미 발급받은 쿠폰 발급 시 400 에러가 발생한다")
        fun `이미_발급받은_쿠폰_발급_시_400_에러가_발생한다`() {
            // given
            val userId = 1L
            val couponId = 1L
            val request = CouponIssueRequest(userId = userId, couponId = couponId)

            given(couponService.issueCoupon(userId, couponId))
                .willThrow(IllegalStateException("이미 발급받은 쿠폰입니다. 사용자 ID: $userId, 쿠폰 ID: $couponId"))

            // when & then
            mockMvc
                .perform(
                    post("/api/coupons/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)

            verify(couponService).issueCoupon(userId, couponId)
        }

        @Test
        @DisplayName("재고가 부족한 쿠폰 발급 시 400 에러가 발생한다")
        fun `재고가_부족한_쿠폰_발급_시_400_에러가_발생한다`() {
            // given
            val userId = 1L
            val couponId = 1L
            val request = CouponIssueRequest(userId = userId, couponId = couponId)

            given(couponService.issueCoupon(userId, couponId))
                .willThrow(IllegalArgumentException("쿠폰 재고가 부족합니다. 현재 재고: 0"))

            // when & then
            mockMvc
                .perform(
                    post("/api/coupons/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)

            verify(couponService).issueCoupon(userId, couponId)
        }
    }

    @Nested
    @DisplayName("사용자 쿠폰 목록 조회 API 테스트")
    inner class UserCouponTest {
        @Test
        @DisplayName("사용자의 쿠폰 목록을 조회할 수 있다")
        fun `사용자의_쿠폰_목록을_조회할_수_있다`() {
            // given
            val userId = 1L
            val userCoupons =
                listOf(
                    UserCoupon.issueCoupon(userId, 1L),
                    UserCoupon.issueCoupon(userId, 2L),
                )

            given(couponService.getUserCoupons(userId)).willReturn(userCoupons)

            // when & then
            mockMvc
                .perform(get("/api/coupons/users/{userId}", userId))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userCoupons").isArray)
                .andExpect(jsonPath("$.userCoupons.length()").value(2))
                .andExpect(jsonPath("$.userCoupons[0].userId").value(userId))
                .andExpect(jsonPath("$.userCoupons[0].couponId").value(1))
                .andExpect(jsonPath("$.userCoupons[0].status").value("ISSUED"))
                .andExpect(jsonPath("$.userCoupons[1].userId").value(userId))
                .andExpect(jsonPath("$.userCoupons[1].couponId").value(2))
                .andExpect(jsonPath("$.userCoupons[1].status").value("ISSUED"))

            verify(couponService).getUserCoupons(userId)
        }

        @Test
        @DisplayName("쿠폰이 없는 사용자의 경우 빈 목록을 반환한다")
        fun `쿠폰이_없는_사용자의_경우_빈_목록을_반환한다`() {
            // given
            val userId = 1L

            given(couponService.getUserCoupons(userId)).willReturn(emptyList())

            // when & then
            mockMvc
                .perform(get("/api/coupons/users/{userId}", userId))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userCoupons").isArray)
                .andExpect(jsonPath("$.userCoupons.length()").value(0))

            verify(couponService).getUserCoupons(userId)
        }

        @Test
        @DisplayName("유효하지 않은 사용자 ID로 조회 시 400 에러가 발생한다")
        fun `유효하지_않은_사용자_ID로_조회_시_400_에러가_발생한다`() {
            // given
            val invalidUserId = 0L

            given(couponService.getUserCoupons(invalidUserId))
                .willThrow(IllegalArgumentException("사용자 ID는 0보다 커야 합니다. 입력된 ID: $invalidUserId"))

            // when & then
            mockMvc
                .perform(get("/api/coupons/users/{userId}", invalidUserId))
                .andExpect(status().isBadRequest)

            verify(couponService).getUserCoupons(invalidUserId)
        }
    }
}
