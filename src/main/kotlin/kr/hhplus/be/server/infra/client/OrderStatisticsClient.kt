package kr.hhplus.be.server.infra.client

import kr.hhplus.be.server.core.order.client.OrderStatisticsClientInterface
import kr.hhplus.be.server.core.order.service.dto.SendOrderStatisticCommand
import org.springframework.stereotype.Component

@Component
class OrderStatisticsClient : OrderStatisticsClientInterface {
    override fun sendOrderStatistics(sendOrderStatisticCommand: SendOrderStatisticCommand) {
        // 주문 통계 전송 로직
        return
    }
}
