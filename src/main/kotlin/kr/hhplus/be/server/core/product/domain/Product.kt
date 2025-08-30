package kr.hhplus.be.server.core.product.domain

/**
 * 상품 도메인 모델
 */
class Product(
    val productId: Long,
    val name: String,
    val description: String,
    val price: Long,
    private var stock: Int,
) {
    companion object {
        const val MIN_STOCK = 0 // 최소 재고량
        const val MAX_STOCK = 1_000 // 최대 재고량
        const val MIN_QUANTITY = 1 // 최소 판매 수량
    }

    init {
        require(stock >= MIN_STOCK) { "재고량은 $MIN_STOCK 이상이어야 합니다. 현재 재고량: $stock" }
        require(stock <= MAX_STOCK) { "재고량은 $MAX_STOCK 이하여야 합니다. 현재 재고량: $stock" }
        require(price > 0) { "상품 가격은 0보다 커야 합니다. 현재 가격: $price" }
        require(name.isNotBlank()) { "상품 이름은 비어있을 수 없습니다." }
    }

    /**
     * 현재 재고량 조회
     */
    fun getStock(): Int = stock

    /**
     * 상품 판매 (재고 차감)
     * @param quantity 판매할 수량
     */
    fun sellProduct(quantity: Int) {
        validateSellQuantity(quantity)
        val newStock = stock - quantity
        validateStock(newStock)

        this.stock = newStock
    }

    /**
     * 재고 추가
     * @param quantity 추가할 재고 수량
     */
    fun addStock(quantity: Int) {
        require(quantity > 0) { "추가할 재고 수량은 0보다 커야 합니다. 요청 수량: $quantity" }
        val newStock = stock + quantity
        validateStock(newStock)

        this.stock = newStock
    }

    /**
     * 재고가 충분한지 확인
     * @param requestQuantity 요청 수량
     * @return 재고 충분 여부
     */
    fun hasEnoughStock(requestQuantity: Int): Boolean = stock >= requestQuantity && requestQuantity > 0

    private fun validateSellQuantity(quantity: Int) {
        require(quantity >= MIN_QUANTITY) {
            "판매 수량은 $MIN_QUANTITY 이상이어야 합니다. 요청 수량: $quantity"
        }
    }

    private fun validateStock(newStock: Int) {
        require(newStock >= MIN_STOCK) {
            "재고량이 $MIN_STOCK 미만이 될 수 없습니다. 계산된 재고량: $newStock"
        }
        require(newStock <= MAX_STOCK) {
            "재고량이 $MAX_STOCK 초과할 수 없습니다. 계산된 재고량: $newStock"
        }
    }
}
