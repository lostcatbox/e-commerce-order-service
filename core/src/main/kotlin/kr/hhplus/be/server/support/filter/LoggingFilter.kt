package kr.hhplus.be.server.support.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * HTTP 요청 및 응답을 로깅하는 필터
 *
 * OncePerRequestFilter를 상속하여 각 요청당 한 번만 실행되도록 보장
 */
@Component
class LoggingFilter : OncePerRequestFilter() {
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestWrapper = ContentCachingRequestWrapper(request)
        val responseWrapper = ContentCachingResponseWrapper(response)

        val traceId = UUID.randomUUID().toString()
        responseWrapper.setHeader(TRACE_ID, traceId)

        val startTime = System.currentTimeMillis()

        try {
            filterChain.doFilter(requestWrapper, responseWrapper)
        } finally {
            val duration = System.currentTimeMillis() - startTime
            logRequestResponse(requestWrapper, responseWrapper, duration, traceId)

            responseWrapper.copyBodyToResponse()
        }
    }

    private fun logRequestResponse(
        request: ContentCachingRequestWrapper,
        response: ContentCachingResponseWrapper,
        duration: Long,
        traceId: String?,
    ) {
        val ip = request.getRemoteAddr()
        val method = request.getMethod()
        val url = request.getRequestURL().toString()
        val requestBody = getContent(request.getContentAsByteArray())
        val status = response.getStatus()
        val responseBody = getContent(response.getContentAsByteArray())

        log.info(
            "[API] traceId: {}, ip: {}, method: {}, url: {}, status: {}, latency: {}ms request: {}, response: {}",
            traceId, ip, method, url, status, duration, requestBody, responseBody
        )
    }

    private fun getContent(content: ByteArray): String {
        if (content.size > 0) {
            return String(content, StandardCharsets.UTF_8)
        }
        return "[EMPTY]"
    }

    companion object {
        private const val TRACE_ID = "X-Trace-Id"
        private val log = LoggerFactory.getLogger(LoggingFilter::class.java)
    }
}
