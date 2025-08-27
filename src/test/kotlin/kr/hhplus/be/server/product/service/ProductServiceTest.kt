package kr.hhplus.be.server.product.service

import kr.hhplus.be.server.product.domain.Product
import kr.hhplus.be.server.product.repository.ProductRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any

@ExtendWith(MockitoExtension::class)
@DisplayName("ProductService 테스트")
class ProductServiceTest {
    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var productService: ProductService

    @Test
    @DisplayName("상품 조회 성공")
    fun `상품 조회 성공`() {
        // given
        val productId = 1L
        val expectedProduct =
            Product(
                productId = productId,
                name = "테스트 상품",
                description = "테스트 상품 설명",
                price = 10000L,
                stock = 100,
            )

        `when`(productRepository.findByProductId(productId)).thenReturn(expectedProduct)

        // when
        val result = productService.getProduct(productId)

        // then
        assertEquals(expectedProduct, result)
        verify(productRepository, times(1)).findByProductId(productId)
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 예외 발생")
    fun `존재하지 않는 상품 조회 시 예외 발생`() {
        // given
        val productId = 999L

        `when`(productRepository.findByProductId(productId)).thenReturn(null)

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                productService.getProduct(productId)
            }
        assertTrue(exception.message!!.contains("존재하지 않는 상품입니다"))
        assertTrue(exception.message!!.contains(productId.toString()))
        verify(productRepository, times(1)).findByProductId(productId)
    }

    @Test
    @DisplayName("유효하지 않은 상품 ID로 조회 시 예외 발생")
    fun `유효하지 않은 상품 ID로 조회 시 예외 발생`() {
        // given
        val invalidProductId = 0L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                productService.getProduct(invalidProductId)
            }
        assertTrue(exception.message!!.contains("상품 ID는 0보다 커야 합니다"))
        assertTrue(exception.message!!.contains(invalidProductId.toString()))
        verify(productRepository, never()).findByProductId(any())
    }

    @Test
    @DisplayName("음수 상품 ID로 조회 시 예외 발생")
    fun `음수 상품 ID로 조회 시 예외 발생`() {
        // given
        val negativeProductId = -1L

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                productService.getProduct(negativeProductId)
            }
        assertTrue(exception.message!!.contains("상품 ID는 0보다 커야 합니다"))
        assertTrue(exception.message!!.contains(negativeProductId.toString()))
        verify(productRepository, never()).findByProductId(any())
    }

    @Test
    @DisplayName("인기 상품 목록 조회 성공")
    fun `인기 상품 목록 조회 성공`() {
        // given
        val expectedProducts =
            listOf(
                Product(1L, "인기상품 1", "인기상품 1 설명", 15000L, 50),
                Product(2L, "인기상품 2", "인기상품 2 설명", 20000L, 30),
                Product(3L, "인기상품 3", "인기상품 3 설명", 12000L, 80),
                Product(4L, "인기상품 4", "인기상품 4 설명", 25000L, 20),
                Product(5L, "인기상품 5", "인기상품 5 설명", 18000L, 60),
            )

        `when`(productRepository.findPopularProducts()).thenReturn(expectedProducts)

        // when
        val result = productService.getPopularProducts()

        // then
        assertEquals(expectedProducts.size, result.size)
        assertEquals(expectedProducts, result)
        verify(productRepository, times(1)).findPopularProducts()
    }

    @Test
    @DisplayName("인기 상품 목록이 비어있는 경우")
    fun `인기 상품 목록이 비어있는 경우`() {
        // given
        val emptyProductList = emptyList<Product>()

        `when`(productRepository.findPopularProducts()).thenReturn(emptyProductList)

        // when
        val result = productService.getPopularProducts()

        // then
        assertTrue(result.isEmpty())
        verify(productRepository, times(1)).findPopularProducts()
    }

    @Test
    @DisplayName("상품 조회 성공 ")
    fun `상품 조회 성공 - 경계값 테스트`() {
        // given
        val productId = 1L // 최소 유효 ID
        val product =
            Product(
                productId = productId,
                name = "경계값 테스트 상품",
                description = "경계값 테스트 설명",
                price = 1L, // 최소 가격
                stock = 0, // 최소 재고
            )

        `when`(productRepository.findByProductId(productId)).thenReturn(product)

        // when
        val result = productService.getProduct(productId)

        // then
        assertEquals(product, result)
        assertEquals(1L, result.price)
        assertEquals(0, result.stock)
        verify(productRepository, times(1)).findByProductId(productId)
    }
}
