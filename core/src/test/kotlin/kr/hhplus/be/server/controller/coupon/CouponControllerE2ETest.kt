package kr.hhplus.be.server.controller.coupon

import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import kr.hhplus.be.server.E2ETestSupport
import kr.hhplus.be.server.core.coupon.domain.Coupon
import kr.hhplus.be.server.core.coupon.domain.CouponStatus
import kr.hhplus.be.server.core.coupon.domain.UserCoupon
import kr.hhplus.be.server.core.coupon.domain.UserCouponStatus
import kr.hhplus.be.server.core.coupon.repository.CouponRepository
import kr.hhplus.be.server.core.coupon.repository.UserCouponRepository
import kr.hhplus.be.server.core.user.domain.User
import kr.hhplus.be.server.infra.persistence.user.jpa.JpaUserRepository
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

@DisplayName("CouponController E2E 테스트")
class CouponControllerE2ETest : E2ETestSupport() {
    @Autowired
    private lateinit var jpaUserRepository: JpaUserRepository

    @Autowired
    private lateinit var couponRepository: CouponRepository

    @Autowired
    private lateinit var userCouponRepository: UserCouponRepository

    @DisplayName("존재하지 않는 쿠폰 정보 조회 시 400 에러가 발생한다.")
    @Test
    fun getCouponInfoNotFound() {
        // given
        val nonExistentCouponId = 999L

        // when & then
        given()
            .`when`()
            .get("/api/coupons/{couponId}", nonExistentCouponId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @DisplayName("쿠폰 정보를 조회한다.")
    @Test
    fun getCouponInfo() {
        // given
        val coupon =
            Coupon(
                couponId = 300L,
                description = "신규 가입 축하 쿠폰",
                discountAmount = 5_000L,
                stock = 100,
                couponStatus = CouponStatus.OPENED,
            )
        couponRepository.save(coupon)

        // when & then
        given()
            .`when`()
            .get("/api/coupons/{couponId}", coupon.couponId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.OK.value())
            .body("couponId", equalTo(coupon.couponId.toInt()))
            .body("description", equalTo(coupon.description))
            .body("discountAmount", equalTo(coupon.discountAmount.toInt()))
            .body("stock", equalTo(coupon.getStock()))
            .body("couponStatus", equalTo(coupon.getCouponStatus().name))
    }

    @DisplayName("재고가 없는 쿠폰 발급 시 400 에러가 발생한다.")
    @Test
    fun issueCouponWithNoStock() {
        // given
        val user = User(userId = 300L, name = "쿠폰테스트유저")
        jpaUserRepository.save(user)

        val coupon =
            Coupon(
                couponId = 301L,
                description = "재고없는쿠폰",
                discountAmount = 10_000L,
                stock = 0, // 재고 없음
                couponStatus = CouponStatus.OPENED,
            )
        couponRepository.save(coupon)

        // when & then
        given()
            .param("userId", user.userId)
            .`when`()
            .post("/api/coupons/{couponId}/issue", coupon.couponId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("쿠폰 재고가 부족합니다."))
    }

    @DisplayName("닫힌 쿠폰 발급 시 400 에러가 발생한다.")
    @Test
    fun issueCouponWhenClosed() {
        // given
        val user = User(userId = 301L, name = "쿠폰테스트유저")
        jpaUserRepository.save(user)

        val coupon =
            Coupon(
                couponId = 302L,
                description = "닫힌쿠폰",
                discountAmount = 10_000L,
                stock = 10,
                couponStatus = CouponStatus.CLOSED, // 닫힌 상태
            )
        couponRepository.save(coupon)


        // when & then
        given()
            .param("userId", user.userId)
            .`when`()
            .post("/api/coupons/{couponId}/issue", coupon.couponId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("쿠폰이 사용 가능한 상태가 아닙니다."))
    }

    @DisplayName("이미 발급받은 쿠폰을 다시 발급받으려 할 때 400 에러가 발생한다.")
    @Test
    fun issueCouponAlreadyIssued() {
        // given
        val user = User(userId = 302L, name = "쿠폰테스트유저")
        jpaUserRepository.save(user)

        val coupon =
            Coupon(
                couponId = 303L,
                description = "중복발급쿠폰",
                discountAmount = 10_000L,
                stock = 10,
                couponStatus = CouponStatus.OPENED,
            )
        couponRepository.save(coupon)

        // 이미 발급받은 쿠폰
        val existingUserCoupon = UserCoupon.issueCoupon(user.userId, coupon.couponId)
        userCouponRepository.save(existingUserCoupon)


        // when & then
        given()
            .param("userId", user.userId)
            .`when`()
            .post("/api/coupons/{couponId}/issue", coupon.couponId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("이미 발급받은 쿠폰입니다."))
    }

    @DisplayName("존재하지 않는 사용자가 쿠폰 발급을 요청할 때 400 에러가 발생한다.")
    @Test
    fun issueCouponForNonExistentUser() {
        // given
        val nonExistentUserId = 999L

        val coupon =
            Coupon(
                couponId = 304L,
                description = "존재하지않는유저테스트쿠폰",
                discountAmount = 10_000L,
                stock = 10,
                couponStatus = CouponStatus.OPENED,
            )
        couponRepository.save(coupon)

        // when & then
        given()
            .param("userId", nonExistentUserId)
            .`when`()
            .post("/api/coupons/{couponId}/issue", coupon.couponId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @DisplayName("쿠폰을 발급받는다.")
    @Test
    fun issueCoupon() {
        // given
        val user = User(userId = 303L, name = "쿠폰테스트유저")
        jpaUserRepository.save(user)

        val coupon =
            Coupon(
                couponId = 305L,
                description = "정상발급쿠폰",
                discountAmount = 15_000L,
                stock = 100,
                couponStatus = CouponStatus.OPENED,
            )
        couponRepository.save(coupon)


        // when & then
        given()
            .param("userId", user.userId)
            .`when`()
            .post("/api/coupons/{couponId}/issue", coupon.couponId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.OK.value())
            .body("success", equalTo(true))
            .body("message", notNullValue())
    }

    @DisplayName("존재하지 않는 사용자의 쿠폰 목록 조회 시 빈 목록을 반환한다.")
    @Test
    fun getUserCouponsNotFound() {
        // given
        val nonExistentUserId = 999L

        // when & then
        given()
            .`when`()
            .get("/api/coupons/users/{userId}", nonExistentUserId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.OK.value())
            .body("userCoupons", hasSize<Any>(0))
    }

    @DisplayName("사용자의 쿠폰 목록을 조회한다.")
    @Test
    fun getUserCoupons() {
        // given
        val user = User(userId = 304L, name = "쿠폰목록조회유저")
        jpaUserRepository.save(user)

        val coupon1 =
            Coupon(
                couponId = 306L,
                description = "첫번째쿠폰",
                discountAmount = 5_000L,
                stock = 10,
                couponStatus = CouponStatus.OPENED,
            )
        val coupon2 =
            Coupon(
                couponId = 307L,
                description = "두번째쿠폰",
                discountAmount = 10_000L,
                stock = 20,
                couponStatus = CouponStatus.OPENED,
            )
        couponRepository.save(coupon1)
        couponRepository.save(coupon2)

        // 사용자 쿠폰 발급
        val userCoupon1 = UserCoupon.issueCoupon(user.userId, coupon1.couponId)
        val userCoupon2 = UserCoupon.issueCoupon(user.userId, coupon2.couponId)
        userCoupon2.use() // 두번째 쿠폰은 사용됨

        userCouponRepository.save(userCoupon1)
        userCouponRepository.save(userCoupon2)

        // when & then
        given()
            .`when`()
            .get("/api/coupons/users/{userId}", user.userId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.OK.value())
            .body("userCoupons", hasSize<Any>(2))
            .body("userCoupons[0].userId", equalTo(user.userId.toInt()))
            .body("userCoupons[0].couponId", anyOf<Any>(equalTo(coupon1.couponId.toInt()), equalTo(coupon2.couponId.toInt())))
            .body("userCoupons[0].status", anyOf<Any>(equalTo(UserCouponStatus.ISSUED.name), equalTo(UserCouponStatus.USED.name)))
    }

    @DisplayName("쿠폰이 없는 사용자의 쿠폰 목록 조회 시 빈 목록을 반환한다.")
    @Test
    fun getUserCouponsEmpty() {
        // given
        val user = User(userId = 305L, name = "쿠폰없는유저")
        jpaUserRepository.save(user)

        // when & then
        given()
            .`when`()
            .get("/api/coupons/users/{userId}", user.userId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.OK.value())
            .body("userCoupons", hasSize<Any>(0))
    }
}
