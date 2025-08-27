package kr.hhplus.be.server.product.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Product 도메인 모델 테스트")
class ProductTest {
    @Test
    @DisplayName("정상적인 Product 생성")
    fun `정상적인 Product 생성`() {
        // given
        val productId = 1L
        val name = "테스트 상품"
        val description = "테스트 상품 설명"
        val price = 10000L
        val stock = 100

        // when
        val product = Product(productId, name, description, price, stock)

        // then
        assertEquals(productId, product.productId)
        assertEquals(name, product.name)
        assertEquals(description, product.description)
        assertEquals(price, product.price)
        assertEquals(stock, product.stock)
    }

    @Test
    @DisplayName("재고량이 최소값보다 작으면 예외 발생")
    fun `재고량이 최소값보다 작으면 예외 발생`() {
        // given
        val productId = 1L
        val name = "테스트 상품"
        val description = "테스트 상품 설명"
        val price = 10000L
        val invalidStock = -1

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                Product(productId, name, description, price, invalidStock)
            }
        assertTrue(exception.message!!.contains("재고량은 0 이상이어야 합니다"))
    }

    @Test
    @DisplayName("재고량이 최대값보다 크면 예외 발생")
    fun `재고량이 최대값보다 크면 예외 발생`() {
        // given
        val productId = 1L
        val name = "테스트 상품"
        val description = "테스트 상품 설명"
        val price = 10000L
        val invalidStock = 1001

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                Product(productId, name, description, price, invalidStock)
            }
        assertTrue(exception.message!!.contains("재고량은 1000 이하여야 합니다"))
    }

    @Test
    @DisplayName("상품 가격이 0보다 작거나 같으면 예외 발생")
    fun `상품 가격이 0보다 작거나 같으면 예외 발생`() {
        // given
        val productId = 1L
        val name = "테스트 상품"
        val description = "테스트 상품 설명"
        val invalidPrice = 0L
        val stock = 100

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                Product(productId, name, description, invalidPrice, stock)
            }
        assertTrue(exception.message!!.contains("상품 가격은 0보다 커야 합니다"))
    }

    @Test
    @DisplayName("상품 이름이 비어있으면 예외 발생")
    fun `상품 이름이 비어있으면 예외 발생`() {
        // given
        val productId = 1L
        val emptyName = ""
        val description = "테스트 상품 설명"
        val price = 10000L
        val stock = 100

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                Product(productId, emptyName, description, price, stock)
            }
        assertTrue(exception.message!!.contains("상품 이름은 비어있을 수 없습니다"))
    }

    @Test
    @DisplayName("정상적인 상품 판매")
    fun `정상적인 상품 판매`() {
        // given
        val product = Product(1L, "테스트 상품", "테스트 상품 설명", 10000L, 100)
        val sellQuantity = 30

        // when
        val soldProduct = product.sellProduct(sellQuantity)

        // then
        assertEquals(70, soldProduct.stock)
        assertEquals(product.productId, soldProduct.productId)
        assertEquals(product.name, soldProduct.name)
        assertEquals(product.description, soldProduct.description)
        assertEquals(product.price, soldProduct.price)
    }

    @Test
    @DisplayName("판매 수량이 최소값보다 작으면 예외 발생")
    fun `판매 수량이 최소값보다 작으면 예외 발생`() {
        // given
        val product = Product(1L, "테스트 상품", "테스트 상품 설명", 10000L, 100)
        val invalidQuantity = 0

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                product.sellProduct(invalidQuantity)
            }
        assertTrue(exception.message!!.contains("판매 수량은 1 이상이어야 합니다"))
    }

    @Test
    @DisplayName("판매 후 재고가 음수가 되면 예외 발생")
    fun `판매 후 재고가 음수가 되면 예외 발생`() {
        // given
        val product = Product(1L, "테스트 상품", "테스트 상품 설명", 10000L, 10)
        val excessiveQuantity = 20

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                product.sellProduct(excessiveQuantity)
            }
        assertTrue(exception.message!!.contains("재고량이 0 미만이 될 수 없습니다"))
    }

    @Test
    @DisplayName("정상적인 재고 추가")
    fun `정상적인 재고 추가`() {
        // given
        val product = Product(1L, "테스트 상품", "테스트 상품 설명", 10000L, 100)
        val addQuantity = 50

        // when
        val updatedProduct = product.addStock(addQuantity)

        // then
        assertEquals(150, updatedProduct.stock)
        assertEquals(product.productId, updatedProduct.productId)
        assertEquals(product.name, updatedProduct.name)
    }

    @Test
    @DisplayName("재고 추가 시 수량이 0보다 작거나 같으면 예외 발생")
    fun `재고 추가 시 수량이 0보다 작거나 같으면 예외 발생`() {
        // given
        val product = Product(1L, "테스트 상품", "테스트 상품 설명", 10000L, 100)
        val invalidQuantity = 0

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                product.addStock(invalidQuantity)
            }
        assertTrue(exception.message!!.contains("추가할 재고 수량은 0보다 커야 합니다"))
    }

    @Test
    @DisplayName("재고 추가 후 최대 재고량을 초과하면 예외 발생")
    fun `재고 추가 후 최대 재고량을 초과하면 예외 발생`() {
        // given
        val product = Product(1L, "테스트 상품", "테스트 상품 설명", 10000L, 950)
        val excessiveQuantity = 100

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                product.addStock(excessiveQuantity)
            }
        assertTrue(exception.message!!.contains("재고량이 1000 초과할 수 없습니다"))
    }

    @Test
    @DisplayName("재고 충분 여부 확인 - 충분한 경우")
    fun `재고 충분 여부 확인 - 충분한 경우`() {
        // given
        val product = Product(1L, "테스트 상품", "테스트 상품 설명", 10000L, 100)
        val requestQuantity = 50

        // when
        val hasEnoughStock = product.hasEnoughStock(requestQuantity)

        // then
        assertTrue(hasEnoughStock)
    }

    @Test
    @DisplayName("재고 충분 여부 확인 - 부족한 경우")
    fun `재고 충분 여부 확인 - 부족한 경우`() {
        // given
        val product = Product(1L, "테스트 상품", "테스트 상품 설명", 10000L, 10)
        val requestQuantity = 20

        // when
        val hasEnoughStock = product.hasEnoughStock(requestQuantity)

        // then
        assertFalse(hasEnoughStock)
    }

    @Test
    @DisplayName("재고 충분 여부 확인 - 요청 수량이 0보다 작거나 같은 경우")
    fun `재고 충분 여부 확인 - 요청 수량이 0보다 작거나 같은 경우`() {
        // given
        val product = Product(1L, "테스트 상품", "테스트 상품 설명", 10000L, 100)
        val invalidQuantity = 0

        // when
        val hasEnoughStock = product.hasEnoughStock(invalidQuantity)

        // then
        assertFalse(hasEnoughStock)
    }

    @Test
    @DisplayName("경계값 테스트 - 최대 재고량으로 생성")
    fun `경계값 테스트 - 최대 재고량으로 생성`() {
        // given
        val productId = 1L
        val name = "테스트 상품"
        val description = "테스트 상품 설명"
        val price = 10000L
        val maxStock = Product.MAX_STOCK

        // when
        val product = Product(productId, name, description, price, maxStock)

        // then
        assertEquals(maxStock, product.stock)
    }

    @Test
    @DisplayName("경계값 테스트 - 최소 재고량으로 생성")
    fun `경계값 테스트 - 최소 재고량으로 생성`() {
        // given
        val productId = 1L
        val name = "테스트 상품"
        val description = "테스트 상품 설명"
        val price = 10000L
        val minStock = Product.MIN_STOCK

        // when
        val product = Product(productId, name, description, price, minStock)

        // then
        assertEquals(minStock, product.stock)
    }

    @Test
    @DisplayName("경계값 테스트 - 정확히 재고만큼 판매")
    fun `경계값 테스트 - 정확히 재고만큼 판매`() {
        // given
        val product = Product(1L, "테스트 상품", "테스트 상품 설명", 10000L, 50)
        val sellQuantity = 50

        // when
        val soldProduct = product.sellProduct(sellQuantity)

        // then
        assertEquals(0, soldProduct.stock)
    }

    @Test
    @DisplayName("경계값 테스트 - 최소 수량 판매")
    fun `경계값 테스트 - 최소 수량 판매`() {
        // given
        val product = Product(1L, "테스트 상품", "테스트 상품 설명", 10000L, 100)
        val minQuantity = Product.MIN_QUANTITY

        // when
        val soldProduct = product.sellProduct(minQuantity)

        // then
        assertEquals(99, soldProduct.stock)
    }
}
