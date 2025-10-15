package kr.hhplus.be.server.infrastructure.persistence.order

import kr.hhplus.be.server.core.order.domain.Order
import kr.hhplus.be.server.core.order.repository.OrderRepository
import kr.hhplus.be.server.infrastructure.persistence.order.jpa.JpaOrderRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * 주문 Repository 구현체
 * Aggregate Root 패턴: Order와 OrderItem들이 하나의 트랜잭션에서 관리됨
 */
@Repository
class OrderRepositoryImpl(
    private val jpaOrderRepository: JpaOrderRepository,
) : OrderRepository {
    @Transactional
    override fun save(order: Order): Order {
        // Order와 모든 OrderItem들이 자동으로 저장됨 (@ElementCollection)
        return jpaOrderRepository.save(order)
    }

    override fun findByOrderId(orderId: Long): Order? {
        // Order와 모든 OrderItem들이 자동으로 조회됨
        return jpaOrderRepository.findByOrderId(orderId)
    }

    override fun findByUserId(userId: Long): List<Order> {
        // 각 Order에 포함된 OrderItem들도 함께 조회됨
        return jpaOrderRepository.findByUserIdOrderByCreatedAtDesc(userId)
    }
}
