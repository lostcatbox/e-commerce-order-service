package kr.hhplus.be.server.core.order.event

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * 주문 이벤트 발행 서비스
 *
 * 주문 도메인에서 발생하는 다양한 이벤트를 발행하는 역할을 담당합니다.
 */
@Component
class OrderEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    /**
     * 주문 완료 이벤트 발행
     *
     * @param event 주문 완료 이벤트 객체
     */
    fun publishOrderCompleted(event: OrderCompletedEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
