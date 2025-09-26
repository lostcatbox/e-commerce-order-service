package kr.hhplus.be.server.core.common.event

import java.util.*

/**
 * 모든 도메인 이벤트의 상위 추상 클래스
 * 간단하게 자동 생성되는 eventId만 포함
 */
abstract class DomainEvent {
    val eventId: String = UUID.randomUUID().toString()
}
