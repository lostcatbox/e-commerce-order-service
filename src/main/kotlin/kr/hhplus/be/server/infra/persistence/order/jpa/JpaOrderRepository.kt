package kr.hhplus.be.server.infra.persistence.order.jpa

import kr.hhplus.be.server.core.order.domain.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * 주문 JPA Repository
 * @ElementCollection을 통해 OrderItem들도 자동으로 처리됨
 */
interface JpaOrderRepository : JpaRepository<Order, Long> {

    /**
     * 주문 ID로 조회 (OrderItem들도 함께 조회됨)
     */
    fun findByOrderId(orderId: Long): Order?

    /**
     * 사용자 ID로 주문 목록 조회 (각 Order의 OrderItem들도 함께 조회됨)
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    fun findByUserIdOrderByCreatedAtDesc(@Param("userId") userId: Long): List<Order>
}
