package kr.hhplus.be.server.core.order.client

import kr.hhplus.be.server.core.order.service.dto.SendOrderStatisticCommand

interface OrderStatisticsClientInterface {
    fun sendOrderStatistics(sendOrderStatisticCommand: SendOrderStatisticCommand)
}
