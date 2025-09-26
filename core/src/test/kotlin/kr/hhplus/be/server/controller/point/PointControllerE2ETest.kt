package kr.hhplus.be.server.controller.point

import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import kr.hhplus.be.server.E2ETestSupport
import kr.hhplus.be.server.controller.point.dto.PointChargeRequest
import kr.hhplus.be.server.core.point.domain.UserPoint
import kr.hhplus.be.server.core.point.repository.UserPointRepository
import kr.hhplus.be.server.core.user.domain.User
import kr.hhplus.be.server.infra.persistence.user.jpa.JpaUserRepository
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

@DisplayName("PointController E2E 테스트")
class PointControllerE2ETest : E2ETestSupport() {
    @Autowired
    private lateinit var jpaUserRepository: JpaUserRepository

    @Autowired
    private lateinit var userPointRepository: UserPointRepository

    @DisplayName("존재하지 않는 신규 사용자의 포인트 조회 시 0원으로 조회된다.")
    @Test
    fun getPointBalanceNotFound() {
        // given
        val nonExistentUserId = 999L

        // when & then
        given()
            .`when`()
            .get("/api/v1/points/{userId}", nonExistentUserId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.OK.value())
            .body("balance", equalTo(0))
            .body("lastUpdatedAt", notNullValue())
    }

    @DisplayName("사용자 포인트 잔액을 조회한다.")
    @Test
    fun getPointBalance() {
        // given
        val user = User(userId = 200L, name = "포인트테스트유저")
        jpaUserRepository.save(user)

        val userPoint = UserPoint(userId = user.userId)
        userPoint.charge(50_000L)
        userPointRepository.save(userPoint)

        // when & then
        given()
            .`when`()
            .get("/api/v1/points/{userId}", user.userId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.OK.value())
            .body("userId", equalTo(user.userId.toInt()))
            .body("balance", equalTo(50_000))
            .body("lastUpdatedAt", notNullValue())
    }

    @DisplayName("초기 포인트 잔액이 0인 사용자의 포인트 조회")
    @Test
    fun getInitialPointBalance() {
        // given
        val user = User(userId = 201L, name = "초기유저")
        jpaUserRepository.save(user)

        val userPoint = UserPoint(userId = user.userId)
        userPointRepository.save(userPoint)

        // when & then
        given()
            .`when`()
            .get("/api/v1/points/{userId}", user.userId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.OK.value())
            .body("userId", equalTo(user.userId.toInt()))
            .body("balance", equalTo(0))
            .body("lastUpdatedAt", notNullValue())
    }

    @DisplayName("최소 충전 금액보다 작은 금액으로 충전 시 400 에러가 발생한다.")
    @Test
    fun chargePointWithInvalidAmount() {
        // given
        val user = User(userId = 202L, name = "충전테스트유저")
        jpaUserRepository.save(user)

        val userPoint = UserPoint(userId = user.userId)
        userPointRepository.save(userPoint)

        val request = PointChargeRequest(amount = 0L) // 최소 충전 금액보다 작음

        // when & then
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .patch("/api/v1/points/{userId}/charge", user.userId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @DisplayName("최대 충전 금액보다 큰 금액으로 충전 시 400 에러가 발생한다.")
    @Test
    fun chargePointWithExcessiveAmount() {
        // given
        val user = User(userId = 203L, name = "충전테스트유저")
        jpaUserRepository.save(user)

        val userPoint = UserPoint(userId = user.userId)
        userPointRepository.save(userPoint)

        val request = PointChargeRequest(amount = 3_000_000L) // 최대 충전 금액 초과

        // when & then
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .patch("/api/v1/points/{userId}/charge", user.userId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @DisplayName("최대 잔액을 초과하여 충전 시 400 에러가 발생한다.")
    @Test
    fun chargePointExceedingMaxBalance() {
        // given
        val user = User(userId = 204L, name = "충전테스트유저")
        jpaUserRepository.save(user)

        val userPoint = UserPoint(userId = user.userId)
        userPoint.charge(1_900_000L) // 기존 잔액
        userPointRepository.save(userPoint)

        val request = PointChargeRequest(amount = 200_000L) // 최대 잔액 초과하도록 충전

        // when & then
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .patch("/api/v1/points/{userId}/charge", user.userId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @DisplayName("새로운 사용자의 포인트 충전 시, 정상적인 충전된다.")
    @Test
    fun chargePointForNonExistentUser() {
        // given
        val nonExistentUserId = 999L
        val request = PointChargeRequest(amount = 10_000L)

        // when & then
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .patch("/api/v1/points/{userId}/charge", nonExistentUserId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.OK.value())
            .body("chargedAmount", equalTo(request.amount.toInt()))
            .body("previousBalance", equalTo(0))
            .body("currentBalance", equalTo(request.amount.toInt()))
            .body("chargedAt", notNullValue())
    }

    @DisplayName("사용자 포인트를 충전한다.")
    @Test
    fun chargePoint() {
        // given
        val user = User(userId = 205L, name = "충전테스트유저")
        jpaUserRepository.save(user)

        val userPoint = UserPoint(userId = user.userId)
        userPoint.charge(30_000L) // 기존 잔액
        userPointRepository.save(userPoint)

        val chargeAmount = 20_000L
        val request = PointChargeRequest(amount = chargeAmount)

        // when & then
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .patch("/api/v1/points/{userId}/charge", user.userId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.OK.value())
            .body("userId", equalTo(user.userId.toInt()))
            .body("chargedAmount", equalTo(chargeAmount.toInt()))
            .body("previousBalance", equalTo(30_000))
            .body("currentBalance", equalTo(50_000))
            .body("chargedAt", notNullValue())
    }

    @DisplayName("처음 포인트를 충전한다.")
    @Test
    fun chargePointForFirstTime() {
        // given
        val user = User(userId = 206L, name = "첫충전유저")
        jpaUserRepository.save(user)

        val userPoint = UserPoint(userId = user.userId) // 초기 잔액 0
        userPointRepository.save(userPoint)

        val chargeAmount = 100_000L
        val request = PointChargeRequest(amount = chargeAmount)

        // when & then
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .patch("/api/v1/points/{userId}/charge", user.userId)
            .then()
            .log()
            .all()
            .statusCode(HttpStatus.OK.value())
            .body("userId", equalTo(user.userId.toInt()))
            .body("chargedAmount", equalTo(chargeAmount.toInt()))
            .body("previousBalance", equalTo(0))
            .body("currentBalance", equalTo(chargeAmount.toInt()))
            .body("chargedAt", notNullValue())
    }
}
