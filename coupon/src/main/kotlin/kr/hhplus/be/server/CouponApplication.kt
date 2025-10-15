package kr.hhplus.be.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * 쿠폰 애플리케이션
 */
@SpringBootApplication
class CouponApplication

fun main(args: Array<String>) {
    runApplication<CouponApplication>(*args)
}
