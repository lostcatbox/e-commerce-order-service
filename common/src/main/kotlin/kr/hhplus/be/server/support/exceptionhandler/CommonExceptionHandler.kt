package kr.hhplus.be.server.support.exceptionhandler

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

data class ErrorResponse(
    val code: String,
    val message: String,
)

@RestControllerAdvice
class CommonExceptionHandler : ResponseEntityExceptionHandler() {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.warn("IllegalArgumentException occurred: ${e.message}")
        return ResponseEntity(
            ErrorResponse("400", e.message ?: "잘못된 요청입니다."),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(e: IllegalStateException): ResponseEntity<ErrorResponse> {
        logger.warn("IllegalStateException occurred: ${e.message}")
        return ResponseEntity(
            ErrorResponse("400", e.message ?: "잘못된 상태입니다."),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected exception occurred", e)
        return ResponseEntity(
            ErrorResponse("500", "에러가 발생했습니다."),
            HttpStatus.INTERNAL_SERVER_ERROR,
        )
    }
}
