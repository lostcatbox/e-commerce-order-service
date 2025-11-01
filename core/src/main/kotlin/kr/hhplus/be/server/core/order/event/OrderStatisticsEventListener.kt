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
 * 외부 통계 시스템 전송 이벤트 리스너
 *
 * OrderCompletedEvent를 처리하여 외부 통계 시스템에 주문 정보를 전송합니다.
 */
@Component
class OrderStatisticsEventListener(
    private val orderService: OrderServiceInterface, // 직접 참조 (나중에 FeignClient로 교체 예정)
    private val orderStatisticsService: OrderStatisticsServiceInterface,
) {
    private val log = LoggerFactory.getLogger(OrderStatisticsEventListener::class.java)

    /**
     * 주문 완료 시 외부 통계 시스템 전송 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleOrderCompleted(event: OrderCompletedEvent) {
        try {
            log.info("외부 통계 시스템 전송 시작 - 주문 ID: {}", event.orderId)

            // 주문 정보 조회 (나중에 FeignClient로 교체 예정)
            val order = orderService.getOrder(event.orderId)
            val sendOrderStatisticCommand = SendOrderStatisticCommand(finalOrder = order)

            orderStatisticsService.sendOrderStatistics(sendOrderStatisticCommand)

            log.info("외부 통계 시스템 전송 완료 - 주문 ID: {}", event.orderId)
        } catch (e: Exception) {
            log.error("외부 통계 시스템 전송 실패 - 주문 ID: {}, 오류: {}", event.orderId, e.message, e)
        }
    }
}
