package io.github.lugf027.blog.interceptor

import io.github.lugf027.blog.entity.AccessLog
import io.github.lugf027.blog.repository.AccessLogRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.time.LocalDateTime

@Component
class AccessLogInterceptor(
    private val accessLogRepository: AccessLogRepository
) : HandlerInterceptor {

    private val startTimeThreadLocal = ThreadLocal<Long>()

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        startTimeThreadLocal.set(System.currentTimeMillis())
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        try {
            val startTime = startTimeThreadLocal.get() ?: System.currentTimeMillis()
            val responseTime = System.currentTimeMillis() - startTime

            val ipAddress = getClientIpAddress(request)
            val requestUrl = request.requestURI
            val requestMethod = request.method
            val userAgent = request.getHeader("User-Agent")
            val referer = request.getHeader("Referer")
            val responseStatus = response.status

            val accessLog = AccessLog(
                ipAddress = ipAddress,
                requestUrl = requestUrl,
                requestMethod = requestMethod,
                userAgent = userAgent,
                referer = referer,
                accessTime = LocalDateTime.now(),
                responseStatus = responseStatus,
                responseTime = responseTime
            )

            accessLogRepository.save(accessLog)
        } catch (e: Exception) {
            // Log error but don't fail the request
            e.printStackTrace()
        } finally {
            startTimeThreadLocal.remove()
        }
    }

    private fun getClientIpAddress(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",")[0].trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        return request.remoteAddr ?: "unknown"
    }
}
