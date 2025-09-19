package kr.hhplus.be.server.core.order.event

import kr.hhplus.be.server.core.order.service.OrderServiceInterface
import kr.hhplus.be.server.core.order.service.OrderStatisticsServiceInterface
import kr.hhplus.be.server.core.order.service.dto.SendOrderStatisticCommand
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 주문 이벤트 구독 서비스
 *
 * 주문 도메인에서 발생하는 이벤트를 구독하여 주문 관련 비즈니스 로직을 처리합니다.
 * 주문 완료 시 외부 통계 시스템에 주문 정보를 전송하는 역할을 담당합니다.
 */
@Component
class OrderEventListener(
    private val orderService: OrderServiceInterface,
    private val orderStatisticsService: OrderStatisticsServiceInterface,
) {
    companion object {
        private val log = LoggerFactory.getLogger(OrderEventListener::class.java)
    }

    /**
     * 주문 완료 이벤트 처리
     *
     * 주문이 완료되었을 때 외부 통계 시스템에 주문 정보를 전송합니다.
     * 이는 주문 도메인의 관심사로서, 주문 완료 후 외부 시스템과의 통합을 담당합니다.
     *
     * @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
     * - 메인 트랜잭션이 성공적으로 커밋된 후에만 이벤트 처리
     * - 외부 시스템 호출 실패가 메인 비즈니스 로직에 영향을 주지 않음
     *
     * @Async
     * - 이벤트 처리를 비동기로 수행하여 새로운 스레드에서 실행(별도 트랜잭션 생성됨)
     *
     * @param event 주문 완료 이벤트
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleOrderCompleted(event: OrderCompletedEvent) {
        try {
            log.info("외부 통계 시스템 전송 시작 - 주문 ID: {}", event.orderId)
            sendOrderStatistics(event)
            log.info("외부 통계 시스템 전송 완료 - 주문 ID: {}", event.orderId)
        } catch (e: Exception) {
            log.error("외부 통계 시스템 전송 중 오류 발생 - 주문 ID: {}, 오류: {}", event.orderId, e.message, e)
        }
    }

    /**
     * 외부 통계 시스템에 주문 정보 전송
     *
     * 현재 SendOrderStatisticCommand가 Order 객체를 요구하므로
     * 이벤트 처리에서 주문 ID로 Order를 다시 조회합니다.
     */
    private fun sendOrderStatistics(event: OrderCompletedEvent) {
        val order = orderService.getOrder(event.orderId)
        val command = SendOrderStatisticCommand(order)
        orderStatisticsService.sendOrderStatistics(command)
    }

}
