package kr.hhplus.be.server.core.order.repository

import kr.hhplus.be.server.core.order.domain.Order

/**
 * 주문 레포지토리 인터페이스
 */
interface OrderRepository {
    /**
     * 주문 저장
     */
    fun save(order: Order): Order

    /**
     * 주문 ID로 조회
     */
    fun findByOrderId(orderId: Long): Order?

    /**
     * 사용자 ID로 주문 목록 조회
     */
    fun findByUserId(userId: Long): List<Order>
}
