package kr.hhplus.be.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * 쿠폰 발급 Consumer 애플리케이션 (CouponIssueConsumer)
 *
 * 설명:
 * - Kafka Consumer 전용 애플리케이션
 * - Core 모듈의 도메인과 서비스 로직을 공유
 * - 웹 요청을 받지 않고 Kafka 메시지만 처리
 *
 * 역할:
 * - Kafka 토픽에서 쿠폰 발급 이벤트 소비
 * - 실제 쿠폰 발급 비즈니스 로직 처리
 * - 분산락을 통한 동시성 제어
 */
@SpringBootApplication(
    scanBasePackages = [
        // Consumer 핵심 컴포넌트
        "kr.hhplus.be.server.infrastructure.kafka", // Kafka 관련 모든 설정

        // 최소 필수 컴포넌트
        "kr.hhplus.be.server.config", // 공통 설정 (DB, Cache, 분산락)
        "kr.hhplus.be.server.infra", // 모든 Repository와 인프라
        "kr.hhplus.be.server.support", // 분산락 등 지원 기능

        // 쿠폰 처리를 위한 서비스
        "kr.hhplus.be.server.core.coupon", // 쿠폰 도메인 전체
        "kr.hhplus.be.server.core.user", // 사용자 도메인
    ],
)
class CouponIssueConsumerApplication

fun main(args: Array<String>) {
    // Consumer 모드로 실행
    System.setProperty("spring.profiles.active", "consumer")
    runApplication<CouponIssueConsumerApplication>(*args)
}
