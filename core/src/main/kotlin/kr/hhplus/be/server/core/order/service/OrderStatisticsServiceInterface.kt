package kr.hhplus.be.server.core.order.service

import kr.hhplus.be.server.core.order.service.dto.SendOrderStatisticCommand

interface OrderStatisticsServiceInterface {
    /**
     * 주문 통계 전송
     * @param sendOrderStatisticCommand 주문 통계 전송 커맨드
     */
    fun sendOrderStatistics(sendOrderStatisticCommand: SendOrderStatisticCommand)
}
