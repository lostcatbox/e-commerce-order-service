package kr.hhplus.be.server.controller.order

import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import kr.hhplus.be.server.E2ETestSupport
import kr.hhplus.be.server.controller.order.dto.OrderCreateRequest
import kr.hhplus.be.server.controller.order.dto.OrderItemRequest
import kr.hhplus.be.server.core.coupon.domain.Coupon
import kr.hhplus.be.server.core.coupon.domain.CouponStatus
import kr.hhplus.be.server.core.coupon.domain.UserCoupon
import kr.hhplus.be.server.core.coupon.domain.UserCouponStatus
import kr.hhplus.be.server.core.coupon.repository.CouponRepository
import kr.hhplus.be.server.core.coupon.repository.UserCouponRepository
import kr.hhplus.be.server.core.point.domain.UserPoint
import kr.hhplus.be.server.core.point.repository.UserPointRepository
import kr.hhplus.be.server.core.product.domain.Product
import kr.hhplus.be.server.core.product.repository.ProductRepository
import kr.hhplus.be.server.core.user.domain.User
import kr.hhplus.be.server.infra.persistence.user.jpa.JpaUserRepository
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

@DisplayName("OrderController E2E 테스트")
class OrderControllerE2ETest : E2ETestSupport() {
    @Autowired
    private lateinit var jpaUserRepository: JpaUserRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var userPointRepository: UserPointRepository

    @Autowired
    private lateinit var userCouponRepository: UserCouponRepository

    @Autowired
    private lateinit var couponRepository: CouponRepository

    @DisplayName("주문/결제 시, 잔액은 충분해야 한다.")
    @Test
    fun orderPaymentWithInsufficientBalance() {
        // given
        val user = User(userId = 1L, name = "항플")
        jpaUserRepository.save(user)

        val userPoint = UserPoint(userId = user.userId)
        userPointRepository.save(userPoint)

        val product1 = Product(productId = 1L, name = "항해 블랙뱃지", description = "블랙뱃지 상품", price = 100_000L, stock = 100)
        val product2 =
            Product(productId = 2L, name = "항해 화이트뱃지", description = "화이트뱃지 상품", price = 200_000L, stock = 200)
        productRepository.save(product1)
        productRepository.save(product2)

        val request =
            OrderCreateRequest(
                userId = user.userId,
                orderItems =
                    listOf(
                        OrderItemRequest(productId = product1.productId, quantity = 1),
                        OrderItemRequest(productId = product2.productId, quantity = 2),
                    ),
                couponId = null,
            )

        // when & then
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/orders")
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("잔액이 0 미만이 될 수 없습니다."))
    }

    @DisplayName("주문/결제 시, 재고는 충분해야 한다.")
    @Test
    fun orderPaymentWithInsufficientStock() {
        // given
        val user = User(userId = 2L, name = "항플")
        jpaUserRepository.save(user)

        val userPoint = UserPoint(userId = user.userId)
        userPoint.charge(1_000_000L)
        userPointRepository.save(userPoint)

        val product1 = Product(productId = 3L, name = "항해 블랙뱃지", description = "블랙뱃지 상품", price = 100_000L, stock = 0)
        val product2 = Product(productId = 4L, name = "항해 화이트뱃지", description = "화이트뱃지 상품", price = 200_000L, stock = 0)
        productRepository.save(product1)
        productRepository.save(product2)

        val request =
            OrderCreateRequest(
                userId = user.userId,
                orderItems =
                    listOf(
                        OrderItemRequest(productId = product1.productId, quantity = 1),
                        OrderItemRequest(productId = product2.productId, quantity = 2),
                    ),
                couponId = null,
            )

        // when & then
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/orders")
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("재고가 부족합니다."))
    }

    @DisplayName("주문/결제 시, 쿠폰은 사용 가능해야 한다.")
    @Test
    fun orderPaymentWithInvalidCoupon() {
        // given
        val user = User(userId = 3L, name = "항플")
        jpaUserRepository.save(user)

        val coupon =
            Coupon(
                couponId = 1L,
                description = "쿠폰명1",
                discountAmount = 10_000L,
                stock = 10,
                couponStatus = CouponStatus.OPENED,
            )
        couponRepository.save(coupon)

        val userCoupon =
            UserCoupon(
                userId = user.userId,
                couponId = coupon.couponId,
                status = UserCouponStatus.USED,
            )
        userCouponRepository.save(userCoupon)

        val userPoint = UserPoint(userId = user.userId)
        userPoint.charge(1_000_000L)
        userPointRepository.save(userPoint)

        val product1 = Product(productId = 5L, name = "항해 블랙뱃지", description = "블랙뱃지 상품", price = 100_000L, stock = 100)
        productRepository.save(product1)

        val request =
            OrderCreateRequest(
                userId = user.userId,
                orderItems =
                    listOf(
                        OrderItemRequest(productId = product1.productId, quantity = 1),
                    ),
                couponId = coupon.couponId,
            )

        // when & then
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/orders")
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("쿠폰이 사용 가능한 상태가 아닙니다."))
    }

    @DisplayName("주문/결제 한다.")
    @Test
    fun orderPayment() {
        // given
        val user = User(userId = 4L, name = "항플")
        jpaUserRepository.save(user)

        val userPoint = UserPoint(userId = user.userId)
        userPoint.charge(1_000_000L)
        userPointRepository.save(userPoint)

        val product1 = Product(productId = 6L, name = "항해 블랙뱃지", description = "블랙뱃지 상품", price = 100_000L, stock = 100)
        val product2 =
            Product(productId = 7L, name = "항해 화이트뱃지", description = "화이트뱃지 상품", price = 200_000L, stock = 200)
        productRepository.save(product1)
        productRepository.save(product2)

        val request =
            OrderCreateRequest(
                userId = user.userId,
                orderItems =
                    listOf(
                        OrderItemRequest(productId = product1.productId, quantity = 1),
                        OrderItemRequest(productId = product2.productId, quantity = 2),
                    ),
                couponId = null,
            )

        // when & then
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/orders")
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.OK.value())
    }
}
