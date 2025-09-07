package kr.hhplus.be.server.facade.order

import kr.hhplus.be.server.controller.order.dto.OrderItemInfo
import kr.hhplus.be.server.core.coupon.service.CouponServiceInterface
import kr.hhplus.be.server.core.coupon.service.UserCouponServiceInterface
import kr.hhplus.be.server.core.order.domain.Order
import kr.hhplus.be.server.core.order.service.OrderServiceInterface
import kr.hhplus.be.server.core.order.service.OrderStatisticsService
import kr.hhplus.be.server.core.order.service.dto.SendOrderStatisticCommand
import kr.hhplus.be.server.core.payment.service.PaymentServiceInterface
import kr.hhplus.be.server.core.payment.service.dto.ProcessPaymentCommand
import kr.hhplus.be.server.core.product.service.ProductSaleService
import kr.hhplus.be.server.core.product.service.ProductServiceInterface
import kr.hhplus.be.server.core.user.service.UserServiceInterface
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 주문 파사드 - 주문 및 결제 유스케이스의 복잡한 비즈니스 로직을 통합 관리
 *
 * 설명
 * 현실에서의 주문 및 결제 과정과 유사하다.
 * 예를 들어, 실제 오프라인 매장에서 고객이 상품을 구매할 때,
 * 고객(유저 서비스)이 매장에 방문하여 상품(상품 서비스)을 선택하고
 * 할인 쿠폰(쿠폰 서비스)을 제시한 후 결제(결제 서비스)를 완료하는 일련의 과정을 거친다.
 *
 * 특징
 * - DownStream = OrderService <-- UpStream = user, product, coupon, payment
 * - Criteria -> Command 변환하여, 서비스 계층에 전달하여, 각 서비스 계층은 자신의 도메인 로직에만 집중할 수 있도록 한다.
 */
@Service
class OrderFacade(
    private val orderService: OrderServiceInterface,
    private val orderStatisticsService: OrderStatisticsService,
    private val userService: UserServiceInterface,
    private val productService: ProductServiceInterface,
    private val paymentService: PaymentServiceInterface,
    private val couponService: CouponServiceInterface,
    private val userCouponService: UserCouponServiceInterface,
    private val productSaleService: ProductSaleService,
) {
    /**
     * 주문 처리 전체 프로세스
     */
    @Transactional
    fun processOrder(orderCriteria: OrderCriteria): Order {
        // 1. 유저 검증
        userService.checkActiveUser(orderCriteria.userId)

        // 2. 주문 생성
        val order = orderService.createOrder(orderCriteria.toCreateOrderCommand())

        // 3. 상품 준비중 상태로 변경
        orderService.changeProductReady(order.orderId)

        // 4. 상품 재고 확인 및 차감
        productService.saleOrderProducts(orderCriteria.toSaleProductsCommand())

        // 5. 발급된 사용자 쿠폰 사용 및 해당 쿠폰 정보 조회
        val coupon =
            if (orderCriteria.usedCouponId != null) {
                // 5-1. 사용자 쿠폰 사용 처리
                val usedUserCoupon = userCouponService.useCoupon(orderCriteria.usedCouponId)
                // 5-2. 쿠폰 정보 조회
                couponService.getCouponInfo(usedUserCoupon.couponId)
            } else {
                null
            }

        // 6. 결제 대기 상태로 변경
        orderService.changePaymentReady(order.orderId)

        // 7. 결제 처리 (결제를 위해 주문 정보가 필요하므로 조회)
        val readyForPaymentOrder = orderService.getOrder(order.orderId)
        val processPaymentCommand = ProcessPaymentCommand(order = readyForPaymentOrder, coupon = coupon)
        val payment = paymentService.processPayment(processPaymentCommand)

        // 8. 결제 성공 상태로 변경
        orderService.changePaymentComplete(order.orderId, payment.paymentId)

        // 9. 주문 완료 상태로 변경
        val finalOrder = orderService.changeCompleted(order.orderId)

        // 10. 외부 통계 시스템에 주문 정보 전송
        val orderInfo = SendOrderStatisticCommand(finalOrder)
        orderStatisticsService.sendOrderStatistics(orderInfo)

        // 11. 주문된 제품 판매량 통계 업데이트 처리
        order.orderItems.map {
            productSaleService.recordProductSale(it.productId, it.quantity)
        }

        // 11. 완료된 주문 반환
        return finalOrder
    }
}
