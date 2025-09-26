package kr.hhplus.be.server.core.common.event

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * 이벤트 처리 모니터링
 *
 * 모든 도메인 이벤트 발행을 즉시 로그로 기록하여
 * 이벤트 추적과 디버깅을 지원합니다.
 */
@Component
class EventProcessingMonitor {
    private val log = LoggerFactory.getLogger(EventProcessingMonitor::class.java)

    /**
     * 이벤트 발행 즉시 로그 기록
     *
     * @EventListener를 사용하여 동기적으로 즉시 처리
     * 이벤트 발행과 동시에 로그를 남겨 추적 가능
     */
    @EventListener
    fun recordEventProcessing(event: DomainEvent) {
        log.info(
            "이벤트 발행: ${event.javaClass.simpleName}, " +
                "이벤트 ID: ${event.eventId}",
        )
    }
}
