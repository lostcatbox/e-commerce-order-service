// package kr.hhplus.be.server.controller.order
//
// import io.restassured.RestAssured.given
// import io.restassured.http.ContentType
// import kr.hhplus.be.server.controller.order.dto.OrderCreateRequest
// import kr.hhplus.be.server.controller.order.dto.OrderItemRequest
// import kr.hhplus.be.server.core.coupon.domain.Coupon
// import kr.hhplus.be.server.core.coupon.domain.CouponStatus
// import kr.hhplus.be.server.core.coupon.domain.UserCoupon
// import kr.hhplus.be.server.core.coupon.domain.UserCouponStatus
// import kr.hhplus.be.server.core.coupon.repository.CouponRepository
// import kr.hhplus.be.server.core.coupon.repository.UserCouponRepository
// import kr.hhplus.be.server.core.point.domain.UserPoint
// import kr.hhplus.be.server.core.point.repository.UserPointRepository
// import kr.hhplus.be.server.core.product.domain.Product
// import kr.hhplus.be.server.core.product.repository.ProductRepository
// import kr.hhplus.be.server.core.user.domain.User
// import kr.hhplus.be.server.infrastructure.persistence.user.jpa.JpaUserRepository
// import kr.hhplus.be.server.support.E2ETestSupport
// import org.junit.jupiter.api.Disabled
// import org.junit.jupiter.api.DisplayName
// import org.junit.jupiter.api.Test
// import org.springframework.beans.factory.annotation.Autowired
// import org.springframework.http.HttpStatus
//
// @DisplayName("OrderController E2E 테스트")
//
// class OrderControllerE2ETest : E2ETestSupport() {
//    @Autowired
//    private lateinit var jpaUserRepository: JpaUserRepository
//
//    @Autowired
//    private lateinit var productRepository: ProductRepository
//
//    @Autowired
//    private lateinit var userPointRepository: UserPointRepository
//
//    @Autowired
//    private lateinit var userCouponRepository: UserCouponRepository
//
//    @Autowired
//    private lateinit var couponRepository: CouponRepository
//
//    @DisplayName("주문 생성 후 Event-Driven 처리로 잔액 부족 실패")
//    @Test
//    fun orderPaymentWithInsufficientBalance() {
//        // given
//        val user = User(userId = 1L, name = "항플")
//        jpaUserRepository.save(user)
//
//        val userPoint = UserPoint(userId = user.userId)
//        userPointRepository.save(userPoint)
//
//        val product1 = Product(productId = 1L, name = "항해 블랙뱃지", description = "블랙뱃지 상품", price = 100_000L, stock = 100)
//        val product2 =
//            Product(productId = 2L, name = "항해 화이트뱃지", description = "화이트뱃지 상품", price = 200_000L, stock = 200)
//        productRepository.save(product1)
//        productRepository.save(product2)
//
//        val request =
//            OrderCreateRequest(
//                userId = user.userId,
//                orderItems =
//                    listOf(
//                        OrderItemRequest(productId = product1.productId, quantity = 1),
//                        OrderItemRequest(productId = product2.productId, quantity = 2),
//                    ),
//                couponId = null,
//            )
//
//        // when: Event-Driven 방식에서는 일단 주문 생성 성공
//        val response =
//            given()
//                .contentType(ContentType.JSON)
//                .body(request)
//                .`when`()
//                .post("/api/v1/orders")
//                .then()
//                .log()
//                .all()
//                .statusCode(HttpStatus.OK.value()) // 주문 생성은 성공
//                .extract()
//                .response()
//
//        val orderId = response.jsonPath().getLong("orderId")
//
//        // then: 비동기 처리 후 주문 상태가 FAILED로 변경되어야 함
//        Thread.sleep(3000) // Event-Driven 비동기 처리 대기
//
//        // 주문 상태 확인 (실패 상태가 되어야 함)
//        given()
//            .`when`()
//            .get("/api/v1/orders/$orderId")
//            .then()
//            .log()
//            .all()
//            .statusCode(HttpStatus.OK.value())
//            .body("orderStatus", org.hamcrest.Matchers.equalTo("FAILED"))
//    }
//
//    @DisplayName("주문 생성 후 Event-Driven 처리로 재고 부족 실패")
//    @Test
//    fun orderPaymentWithInsufficientStock() {
//        // given
//        val user = User(userId = 2L, name = "항플")
//        jpaUserRepository.save(user)
//
//        val userPoint = UserPoint(userId = user.userId)
//        userPoint.charge(1_000_000L)
//        userPointRepository.save(userPoint)
//
//        val product1 = Product(productId = 3L, name = "항해 블랙뱃지", description = "블랙뱃지 상품", price = 100_000L, stock = 0)
//        val product2 = Product(productId = 4L, name = "항해 화이트뱃지", description = "화이트뱃지 상품", price = 200_000L, stock = 0)
//        productRepository.save(product1)
//        productRepository.save(product2)
//
//        val request =
//            OrderCreateRequest(
//                userId = user.userId,
//                orderItems =
//                    listOf(
//                        OrderItemRequest(productId = product1.productId, quantity = 1),
//                        OrderItemRequest(productId = product2.productId, quantity = 2),
//                    ),
//                couponId = null,
//            )
//
//        // when: Event-Driven 방식에서는 일단 주문 생성 성공
//        val response =
//            given()
//                .contentType(ContentType.JSON)
//                .body(request)
//                .`when`()
//                .post("/api/v1/orders")
//                .then()
//                .log()
//                .all()
//                .statusCode(HttpStatus.OK.value()) // 주문 생성은 성공
//                .extract()
//                .response()
//
//        val orderId = response.jsonPath().getLong("orderId")
//
//        // then: 비동기 처리 후 주문 상태가 FAILED로 변경되어야 함
//        Thread.sleep(3000) // Event-Driven 비동기 처리 대기
//
//        // 주문 상태 확인 (실패 상태가 되어야 함)
//        given()
//            .`when`()
//            .get("/api/v1/orders/$orderId")
//            .then()
//            .log()
//            .all()
//            .statusCode(HttpStatus.OK.value())
//            .body("orderStatus", org.hamcrest.Matchers.equalTo("FAILED"))
//    }
//
//    @DisplayName("주문 생성 후 Event-Driven 처리로 쿠폰 사용 불가 실패")
//    @Test
//    fun orderPaymentWithInvalidCoupon() {
//        // given
//        val user = User(userId = 3L, name = "항플")
//        jpaUserRepository.save(user)
//
//        val coupon =
//            Coupon(
//                couponId = 1L,
//                description = "쿠폰명1",
//                discountAmount = 10_000L,
//                stock = 10,
//                couponStatus = CouponStatus.OPENED,
//            )
//        couponRepository.save(coupon)
//
//        val userCoupon =
//            UserCoupon(
//                userId = user.userId,
//                couponId = coupon.couponId,
//                status = UserCouponStatus.USED,
//            )
//        userCouponRepository.save(userCoupon)
//
//        val userPoint = UserPoint(userId = user.userId)
//        userPoint.charge(1_000_000L)
//        userPointRepository.save(userPoint)
//
//        val product1 = Product(productId = 5L, name = "항해 블랙뱃지", description = "블랙뱃지 상품", price = 100_000L, stock = 100)
//        productRepository.save(product1)
//
//        val request =
//            OrderCreateRequest(
//                userId = user.userId,
//                orderItems =
//                    listOf(
//                        OrderItemRequest(productId = product1.productId, quantity = 1),
//                    ),
//                couponId = coupon.couponId,
//            )
//
//        // when: Event-Driven 방식에서는 일단 주문 생성 성공
//        val response =
//            given()
//                .contentType(ContentType.JSON)
//                .body(request)
//                .`when`()
//                .post("/api/v1/orders")
//                .then()
//                .log()
//                .all()
//                .statusCode(HttpStatus.OK.value()) // 주문 생성은 성공
//                .extract()
//                .response()
//
//        val orderId = response.jsonPath().getLong("orderId")
//
//        // then: 비동기 처리 후 주문 상태가 FAILED로 변경되어야 함
//        Thread.sleep(3000) // Event-Driven 비동기 처리 대기
//
//        // 주문 상태 확인 (실패 상태가 되어야 함)
//        given()
//            .`when`()
//            .get("/api/v1/orders/$orderId")
//            .then()
//            .log()
//            .all()
//            .statusCode(HttpStatus.OK.value())
//            .body("orderStatus", org.hamcrest.Matchers.equalTo("FAILED"))
//    }
//
//    @DisplayName("주문 생성 후 Event-Driven 처리로 정상 완료")
//    @Test
//    fun orderPayment() {
//        // given
//        val user = User(userId = 4L, name = "항플")
//        jpaUserRepository.save(user)
//
//        val userPoint = UserPoint(userId = user.userId)
//        userPoint.charge(1_000_000L)
//        userPointRepository.save(userPoint)
//
//        val product1 = Product(productId = 6L, name = "항해 블랙뱃지", description = "블랙뱃지 상품", price = 100_000L, stock = 100)
//        val product2 =
//            Product(productId = 7L, name = "항해 화이트뱃지", description = "화이트뱃지 상품", price = 200_000L, stock = 200)
//        productRepository.save(product1)
//        productRepository.save(product2)
//
//        val request =
//            OrderCreateRequest(
//                userId = user.userId,
//                orderItems =
//                    listOf(
//                        OrderItemRequest(productId = product1.productId, quantity = 1),
//                        OrderItemRequest(productId = product2.productId, quantity = 2),
//                    ),
//                couponId = null,
//            )
//
//        // when: Event-Driven 방식에서는 일단 주문 생성 성공
//        val response =
//            given()
//                .contentType(ContentType.JSON)
//                .body(request)
//                .`when`()
//                .post("/api/v1/orders")
//                .then()
//                .log()
//                .all()
//                .statusCode(HttpStatus.OK.value()) // 주문 생성은 성공
//                .extract()
//                .response()
//
//        val orderId = response.jsonPath().getLong("orderId")
//
//        // then: 비동기 처리 후 주문 상태가 COMPLETED로 변경되어야 함
//        // Event-Driven 처리 완료까지 폴링으로 확인
//        var attempts = 0
//        var currentStatus = ""
//        while (attempts < 20) { // 최대 10초 대기
//            Thread.sleep(500)
//
//            val statusResponse =
//                given()
//                    .`when`()
//                    .get("/api/v1/orders/$orderId")
//                    .then()
//                    .statusCode(HttpStatus.OK.value())
//                    .extract()
//                    .response()
//
//            currentStatus = statusResponse.jsonPath().getString("orderStatus")
//
//            if (currentStatus == "COMPLETED" || currentStatus == "FAILED") {
//                break
//            }
//            attempts++
//        }
//
//        // 주문 상태 확인 (완료 상태가 되어야 함)
//        given()
//            .`when`()
//            .get("/api/v1/orders/$orderId")
//            .then()
//            .log()
//            .all()
//            .statusCode(HttpStatus.OK.value())
//            .body("orderStatus", org.hamcrest.Matchers.equalTo("COMPLETED"))
//    }
// }
