package kr.hhplus.be.server.core.product.service

import kr.hhplus.be.server.core.order.service.dto.OrderItemCommand
import kr.hhplus.be.server.core.product.domain.Product
import kr.hhplus.be.server.core.product.event.ProductEventPublisherInterface
import kr.hhplus.be.server.core.product.event.ProductInsufficientData
import kr.hhplus.be.server.core.product.event.ProductReservationData
import kr.hhplus.be.server.core.product.repository.ProductRepository
import kr.hhplus.be.server.core.product.service.dto.SaleProductsCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 상품 서비스 구현체
 */
@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val productEventPublisher: ProductEventPublisherInterface,
) : ProductServiceInterface {
    /**
     * 상품 정보 조회
     */
    @Transactional(readOnly = true)
    override fun getProduct(productId: Long): Product {
        validateProductId(productId)

        return productRepository.findByProductId(productId)
            ?: throw IllegalArgumentException("존재하지 않는 상품입니다. 상품 ID: $productId")
    }

    /**
     * 주문 상품들 판매 처리 (재고 차감)
     */
    @Transactional
    override fun saleOrderProducts(command: SaleProductsCommand) {
        require(command.orderItems.isNotEmpty()) { "주문 상품은 1개 이상이어야 합니다." }

        command.orderItems.forEach { orderItemCommand ->
            validateProductId(orderItemCommand.productId)
            require(
                orderItemCommand.quantity > 0,
            ) { "주문 수량은 0보다 커야 합니다. 상품 ID: ${orderItemCommand.productId}, 수량: ${orderItemCommand.quantity}" }

            // 상품 조회
            val product =
                productRepository.findByProductIdWithPessimisticLock(orderItemCommand.productId)
                    ?: throw IllegalArgumentException("존재하지 않는 상품입니다. 상품 ID: ${orderItemCommand.productId}")

            // 재고 충분한지 확인
            if (!product.hasEnoughStock(orderItemCommand.quantity)) {
                throw IllegalStateException(
                    "상품 재고가 부족합니다. 상품 ID: ${orderItemCommand.productId}, 요청 수량: ${orderItemCommand.quantity}, 현재 재고: ${product.getStock()}",
                )
            }

            // 도메인 로직을 통한 상품 판매 (재고 차감)
            product.sellProduct(orderItemCommand.quantity)

            // 재고 차감된 상품 저장
            productRepository.save(product)
        }
    }

    /**
     * 주문 상품들 재고 복구 (재고 복구)
     */
    @Transactional
    override fun restoreStock(orderItems: List<OrderItemCommand>) {
        orderItems.forEach { orderItemCommand ->
            validateProductId(orderItemCommand.productId)

            // 상품 조회
            val product =
                productRepository.findByProductIdWithPessimisticLock(orderItemCommand.productId)
                    ?: throw IllegalArgumentException("존재하지 않는 상품입니다. 상품 ID: ${orderItemCommand.productId}")

            // 도메인 로직을 통한 재고 복구
            product.restoreStock(orderItemCommand.quantity)

            // 재고 복구된 상품 저장
            productRepository.save(product)
        }
    }

    /**
     * 주문 상품 재고 처리 및 이벤트 발행
     */
    @Transactional
    fun processOrderProductStock(
        orderId: Long,
        command: SaleProductsCommand,
    ) {
        try {
            // 기존 비즈니스 로직 호출
            saleOrderProducts(command)

            // 재고 확보 성공 이벤트 발행
            val reservations: List<ProductReservationData> =
                command.orderItems.map { orderItem ->
                    ProductReservationData(
                        productId = orderItem.productId,
                        quantity = orderItem.quantity,
                        reservedStock = orderItem.quantity,
                    )
                }

            productEventPublisher.publishProductStockReserved(
                orderId = orderId,
                products = reservations,
            )
        } catch (e: Exception) {
            // 재고 부족 이벤트 발행
            val insufficientProducts = extractInsufficientProducts(e)

            productEventPublisher.publishProductStockInsufficient(
                orderId = orderId,
                insufficientProducts = insufficientProducts,
                reason = e.message ?: "Stock insufficient",
            )
            throw e
        }
    }

    /**
     * 예외에서 부족한 상품 정보 추출 (임시 구현)
     */
    private fun extractInsufficientProducts(
        @Suppress("UNUSED_PARAMETER") e: Exception,
    ): List<ProductInsufficientData> {
        // 실제로는 예외에서 부족한 상품 정보를 추출해야 함
        return emptyList<ProductInsufficientData>()
    }

    /**
     * 상품 ID 유효성 검증
     */
    private fun validateProductId(productId: Long) {
        require(productId > 0) { "상품 ID는 0보다 커야 합니다. 입력된 ID: $productId" }
    }
}
