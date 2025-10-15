// package kr.hhplus.be.server.controller.product
//
// import io.restassured.RestAssured.given
//
// import kr.hhplus.be.server.core.product.domain.Product
// import kr.hhplus.be.server.core.product.domain.ProductSale
// import kr.hhplus.be.server.core.product.repository.ProductRepository
// import kr.hhplus.be.server.core.product.repository.ProductSaleRepository
// import org.hamcrest.Matchers.*
// import org.junit.jupiter.api.DisplayName
// import org.junit.jupiter.api.Test
// import org.springframework.beans.factory.annotation.Autowired
// import org.springframework.http.HttpStatus
// import java.time.LocalDate
// import java.time.format.DateTimeFormatter
//
// @DisplayName("ProductController E2E 테스트")
// class ProductControllerE2ETest : E2ETestSupport() {
//    @Autowired
//    private lateinit var productRepository: ProductRepository
//
//    @Autowired
//    private lateinit var productSaleRepository: ProductSaleRepository
//
//    @DisplayName("존재하지 않는 상품 조회 시 400 에러가 발생한다.")
//    @Test
//    fun getProductNotFound() {
//        // given
//        val nonExistentProductId = 999L
//
//        // when & then
//        given()
//            .`when`()
//            .get("/api/v1/products/{productId}", nonExistentProductId)
//            .then()
//            .log()
//            .all()
//            .statusCode(HttpStatus.BAD_REQUEST.value())
//    }
//
//    @DisplayName("상품 정보를 조회한다.")
//    @Test
//    fun getProduct() {
//        // given
//        val product =
//            Product(
//                productId = 100L,
//                name = "테스트 상품",
//                description = "테스트용 상품입니다",
//                price = 50_000L,
//                stock = 100,
//            )
//        productRepository.save(product)
//
//        // when & then
//        given()
//            .`when`()
//            .get("/api/v1/products/{productId}", product.productId)
//            .then()
//            .log()
//            .all()
//            .statusCode(HttpStatus.OK.value())
//            .body("productId", equalTo(product.productId.toInt()))
//            .body("name", equalTo(product.name))
//            .body("description", equalTo(product.description))
//            .body("price", equalTo(product.price.toInt()))
//            .body("stock", equalTo(product.getStock()))
//    }
//
//    @DisplayName("인기 판매 상품을 조회한다.")
//    @Test
//    fun getPopularProducts() {
//        // given
//        val today = LocalDate.now()
//        val dateString = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
//
//        // 상품 생성
//        val product1 =
//            Product(
//                productId = 101L,
//                name = "인기 상품 1",
//                description = "인기 상품 1 설명",
//                price = 30_000L,
//                stock = 50,
//            )
//        val product2 =
//            Product(
//                productId = 102L,
//                name = "인기 상품 2",
//                description = "인기 상품 2 설명",
//                price = 40_000L,
//                stock = 30,
//            )
//        val product3 =
//            Product(
//                productId = 103L,
//                name = "인기 상품 3",
//                description = "인기 상품 3 설명",
//                price = 50_000L,
//                stock = 20,
//            )
//        productRepository.save(product1)
//        productRepository.save(product2)
//        productRepository.save(product3)
//
//        // 판매 데이터 생성 (최근 3일간)
//        val saleDate = today.format(DateTimeFormatter.ofPattern("yyyyMMdd")).toLong()
//        val productSale1 =
//            ProductSale(
//                productId = product1.productId,
//                saleDate = saleDate,
//                totalQuantity = 10,
//            )
//        val productSale2 =
//            ProductSale(
//                productId = product2.productId,
//                saleDate = saleDate,
//                totalQuantity = 15,
//            )
//        val productSale3 =
//            ProductSale(
//                productId = product3.productId,
//                saleDate = saleDate,
//                totalQuantity = 5,
//            )
//        productSaleRepository.save(productSale1)
//        productSaleRepository.save(productSale2)
//        productSaleRepository.save(productSale3)
//
//        // when & then
//        given()
//            .`when`()
//            .get("/api/v1/products/popular")
//            .then()
//            .log()
//            .all()
//            .statusCode(HttpStatus.OK.value())
//            .body("products", hasSize<Any>(greaterThanOrEqualTo(1)))
//            .body("generatedAt", notNullValue())
//    }
//
//    @DisplayName("인기 판매 상품이 없는 경우 빈 목록을 반환한다.")
//    @Test
//    fun getPopularProductsEmpty() {
//        // given (판매 데이터가 없는 상태)
//
//        // when & then
//        given()
//            .`when`()
//            .get("/api/v1/products/popular")
//            .then()
//            .log()
//            .all()
//            .statusCode(HttpStatus.OK.value())
//            .body("products", hasSize<Any>(0))
//            .body("generatedAt", notNullValue())
//    }
// }
