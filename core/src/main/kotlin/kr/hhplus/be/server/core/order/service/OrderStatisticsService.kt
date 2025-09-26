package kr.hhplus.be.server.core.order.service

import kr.hhplus.be.server.core.order.client.OrderStatisticsClientInterface
import kr.hhplus.be.server.core.order.service.dto.SendOrderStatisticCommand
import org.springframework.stereotype.Service

@Service
class OrderStatisticsService(
    private val orderStatisticsClient: OrderStatisticsClientInterface,
) : OrderStatisticsServiceInterface {
    override fun sendOrderStatistics(sendOrderStatisticCommand: SendOrderStatisticCommand) {
        orderStatisticsClient.sendOrderStatistics(sendOrderStatisticCommand)
    }
}
