package io.github.lugf027.blog.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.nio.charset.StandardCharsets

/**
 * 请求日志过滤器 - 记录所有 HTTP 请求和响应
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val wrappedRequest = ContentCachingRequestWrapper(request, 100)
        val wrappedResponse = ContentCachingResponseWrapper(response)

        val startTime = System.currentTimeMillis()

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse)
        } finally {
            val duration = System.currentTimeMillis() - startTime
            logRequest(wrappedRequest, wrappedResponse, duration)
            wrappedResponse.copyBodyToResponse()
        }
    }

    private fun logRequest(
        request: ContentCachingRequestWrapper,
        response: ContentCachingResponseWrapper,
        duration: Long
    ) {
        val requestBody = getRequestBody(request)
        val responseBody = getResponseBody(response)
        val status = response.status

        // 根据状态码选择日志级别
        val logMessage = buildLogMessage(request, response, duration, requestBody, responseBody)

        when {
            status >= 500 -> log.error(logMessage)
            status >= 400 -> log.warn(logMessage)
            else -> log.info(logMessage)
        }
    }

    private fun buildLogMessage(
        request: ContentCachingRequestWrapper,
        response: ContentCachingResponseWrapper,
        duration: Long,
        requestBody: String,
        responseBody: String
    ): String {
        val status = response.status
        val statusEmoji = when {
            status >= 500 -> "❌"
            status >= 400 -> "⚠️"
            status >= 300 -> "↪️"
            else -> "✅"
        }

        return """
            |
            |$statusEmoji ==================== HTTP 请求日志 ====================
            |📍 请求: ${request.method} ${request.requestURI}${request.queryString?.let { "?$it" } ?: ""}
            |📊 状态: $status | 耗时: ${duration}ms
            |🌐 客户端: ${request.remoteAddr}
            |🔑 Authorization: ${request.getHeader("Authorization")?.take(50)?.let { "$it..." } ?: "无"}
            |📥 请求体: ${truncateBody(requestBody)}
            |📤 响应体: ${truncateBody(responseBody)}
            |===============================================================
        """.trimMargin()
    }

    private fun getRequestBody(request: ContentCachingRequestWrapper): String {
        return try {
            val content = request.contentAsByteArray
            if (content.isNotEmpty()) {
                String(content, StandardCharsets.UTF_8)
            } else {
                "(空)"
            }
        } catch (e: Exception) {
            "(无法读取)"
        }
    }

    private fun getResponseBody(response: ContentCachingResponseWrapper): String {
        return try {
            val content = response.contentAsByteArray
            if (content.isNotEmpty()) {
                String(content, StandardCharsets.UTF_8)
            } else {
                "(空)"
            }
        } catch (e: Exception) {
            "(无法读取)"
        }
    }

    private fun truncateBody(body: String, maxLength: Int = 500): String {
        return if (body.length > maxLength) {
            body.take(maxLength) + "... (截断，共${body.length}字符)"
        } else {
            body
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        // 排除静态资源
        val path = request.requestURI
        return path.startsWith("/api/images/") || 
               path.endsWith(".js") || 
               path.endsWith(".css") || 
               path.endsWith(".ico")
    }
}
