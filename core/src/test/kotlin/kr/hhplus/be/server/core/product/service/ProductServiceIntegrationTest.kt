package kr.hhplus.be.server.core.product.service

import jakarta.persistence.EntityManager
import kr.hhplus.be.server.IntegrationTestSupport
import kr.hhplus.be.server.core.order.service.dto.OrderItemCommand
import kr.hhplus.be.server.core.product.domain.Product
import kr.hhplus.be.server.core.product.repository.ProductRepository
import kr.hhplus.be.server.core.product.service.dto.SaleProductsCommand
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("ProductService 통합 테스트")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProductServiceIntegrationTest : IntegrationTestSupport() {
    @Autowired
    private lateinit var productService: ProductService

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var entityManager: EntityManager
    private lateinit var testProduct: Product

    @BeforeEach
    fun setUp() {
        // 테스트용 상품 데이터 생성
        testProduct =
            Product(
                productId = 1L,
                name = "통합테스트용 상품",
                description = "테스트용 상품 설명",
                price = 10000L,
                stock = 100,
            )
        productRepository.save(testProduct)
    }

    @Test
    @DisplayName("상품 정보 조회 성공")
    @Transactional
    fun `상품 정보 조회 성공`() {
        // when
        val result = productService.getProduct(testProduct.productId)

        // then
        assertNotNull(result)
        assertEquals(testProduct.productId, result.productId)
        assertEquals(testProduct.name, result.name)
        assertEquals(testProduct.description, result.description)
        assertEquals(testProduct.price, result.price)
        assertEquals(testProduct.getStock(), result.getStock())
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 예외 발생")
    @Transactional
    fun `존재하지 않는 상품 조회 시 예외 발생`() {
        // given
        val nonExistentProductId = 999L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                productService.getProduct(nonExistentProductId)
            }

        assertTrue(exception.message!!.contains("존재하지 않는 상품입니다"))
        assertTrue(exception.message!!.contains(nonExistentProductId.toString()))
    }

    @Test
    @DisplayName("잘못된 상품 ID로 조회 시 예외 발생")
    @Transactional
    fun `잘못된 상품 ID로 조회 시 예외 발생`() {
        // given
        val invalidProductId = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                productService.getProduct(invalidProductId)
            }

        assertTrue(exception.message!!.contains("상품 ID는 0보다 커야 합니다"))
    }

    @Test
    @DisplayName("상품 판매 처리 재고 차감 검증")
    @Transactional
    fun `상품 판매 처리 재고 차감 검증`() {
        // given
        val originalStock = testProduct.getStock()
        val saleQuantity = 5
        val orderItems = listOf(OrderItemCommand(testProduct.productId, saleQuantity))
        val command = SaleProductsCommand(orderItems)

        // when
        productService.saleOrderProducts(command)

        // then
        // 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        // DB에서 다시 조회하여 재고가 실제로 차감되었는지 확인
        val updatedProduct = productRepository.findByProductId(testProduct.productId)
        assertNotNull(updatedProduct)
        assertEquals(originalStock - saleQuantity, updatedProduct!!.getStock())
    }

    @Test
    @DisplayName("여러 상품 동시 판매 처리")
    @Transactional
    fun `여러 상품 동시 판매 처리`() {
        // given - 추가 상품 생성
        val product2 =
            Product(
                productId = 2L,
                name = "두 번째 테스트 상품",
                description = "두 번째 상품 설명",
                price = 15000L,
                stock = 50,
            )
        productRepository.save(product2)

        val originalStock1 = testProduct.getStock()
        val originalStock2 = product2.getStock()

        val orderItems =
            listOf(
                OrderItemCommand(testProduct.productId, 3),
                OrderItemCommand(product2.productId, 7),
            )
        val command = SaleProductsCommand(orderItems)

        // when
        productService.saleOrderProducts(command)

        // then
        // 영속성 컨텍스트 초기화commit 및 초기화
        entityManager.flush()
        entityManager.clear()

        val updatedProduct1 = productRepository.findByProductId(testProduct.productId)
        val updatedProduct2 = productRepository.findByProductId(product2.productId)

        assertNotNull(updatedProduct1)
        assertNotNull(updatedProduct2)
        assertEquals(originalStock1 - 3, updatedProduct1!!.getStock())
        assertEquals(originalStock2 - 7, updatedProduct2!!.getStock())
    }

    @Test
    @DisplayName("재고 부족 상품 판매 시 예외 발생")
    @Transactional
    fun `재고 부족 상품 판매 시 예외 발생`() {
        // given - 재고보다 많은 수량 요청
        val currentStock = testProduct.getStock()
        val orderItems = listOf(OrderItemCommand(testProduct.productId, currentStock + 1))
        val command = SaleProductsCommand(orderItems)

        // when & then
        val exception =
            assertThrows<IllegalStateException> {
                productService.saleOrderProducts(command)
            }

        assertTrue(exception.message!!.contains("상품 재고가 부족합니다"))
        assertTrue(exception.message!!.contains(testProduct.productId.toString()))

        // 재고가 변경되지 않았는지 확인
        val unchangedProduct = productRepository.findByProductId(testProduct.productId)
        assertEquals(currentStock, unchangedProduct!!.getStock())
    }

    @Test
    @DisplayName("존재하지 않는 상품 판매 시 예외 발생")
    @Transactional
    fun `존재하지 않는 상품 판매 시 예외 발생`() {
        // given
        val nonExistentProductId = 999L
        val orderItems = listOf(OrderItemCommand(nonExistentProductId, 1))
        val command = SaleProductsCommand(orderItems)

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                productService.saleOrderProducts(command)
            }

        assertTrue(exception.message!!.contains("존재하지 않는 상품입니다"))
        assertTrue(exception.message!!.contains(nonExistentProductId.toString()))
    }

    @Test
    @DisplayName("빈 주문 상품 리스트로 판매 시 예외 발생")
    @Transactional
    fun `빈 주문 상품 리스트로 판매 시 예외 발생`() {
        // given
        val emptyOrderItems = emptyList<OrderItemCommand>()

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                SaleProductsCommand(emptyOrderItems)
            }

        assertTrue(exception.message!!.contains("주문 상품은 1개 이상이어야 합니다"))
    }

    @Test
    @DisplayName("동시성 테스트 - 여러 스레드에서 동시에 상품 재고 차감 (재고 10개, 20번 동시 요청)")
    fun `동시성 테스트 여러 스레드에서 동시에 상품 재고 차감 (재고 10개 20번 동시 요청)`() {
        // given - 재고 10개인 상품 생성
        val concurrencyTestProduct =
            Product(
                productId = 100L,
                name = "동시성 테스트 상품",
                description = "재고 10개 테스트용 상품",
                price = 5000L,
                stock = 10, // 재고 10개
            )
        productRepository.save(concurrencyTestProduct)

        val threadCount = 20 // 20번 동시 요청
        val requestQuantity = 1 // 각 요청마다 1개씩 차감
        val executor = Executors.newFixedThreadPool(threadCount)
        val countDownLatch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        // when - 동시에 상품 재고 차감 시도
        repeat(threadCount) {
            executor.submit {
                try {
                    val orderItems = listOf(OrderItemCommand(concurrencyTestProduct.productId, requestQuantity))
                    val command = SaleProductsCommand(orderItems)
                    productService.saleOrderProducts(command)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failureCount.incrementAndGet()
                    println("상품 재고 차감 실패: ${e.message}")
                } finally {
                    countDownLatch.countDown()
                }
            }
        }

        // 모든 작업 완료 대기 (최대 30초)
        val completed = countDownLatch.await(30, TimeUnit.SECONDS)
        if (!completed) {
            println("30초 내에 모든 작업이 완료되지 않았습니다!")
        }
        executor.shutdown()

        // then
        println("성공한 재고 차감: ${successCount.get()}, 실패한 재고 차감: ${failureCount.get()}")

        // 성공한 재고 차감은 정확히 재고 수만큼이어야 함
        assertEquals(10, successCount.toInt()) // 재고 10개 → 10번 성공
        assertEquals(10, failureCount.toInt()) // 나머지 10번은 재고 부족으로 실패

        // 최종 재고 확인 (0이어야 함)
        val finalProduct = productRepository.findByProductId(concurrencyTestProduct.productId)
        assertNotNull(finalProduct)
        assertEquals(0, finalProduct!!.getStock()) // 10개 모두 차감되어 0개
    }
}
