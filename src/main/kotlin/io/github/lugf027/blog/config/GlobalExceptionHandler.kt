package io.github.lugf027.blog.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import jakarta.servlet.http.HttpServletRequest

/**
 * 全局异常处理器 - 统一处理并记录所有异常
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * 处理 400 Bad Request - JSON 解析错误
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<Map<String, Any>> {
        logger.error("""
            |========== 请求解析错误 (400) ==========
            |请求路径: ${request.method} ${request.requestURI}
            |客户端IP: ${request.remoteAddr}
            |Content-Type: ${request.contentType}
            |错误信息: ${ex.message}
            |根本原因: ${ex.rootCause?.message ?: "无"}
            |========================================
        """.trimMargin(), ex)

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            mapOf(
                "status" to 400,
                "error" to "Bad Request",
                "message" to "请求体解析失败: ${ex.rootCause?.message ?: ex.message}",
                "path" to request.requestURI
            )
        )
    }

    /**
     * 处理参数验证错误
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<Map<String, Any>> {
        val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        
        logger.error("""
            |========== 参数验证错误 (400) ==========
            |请求路径: ${request.method} ${request.requestURI}
            |验证错误: $errors
            |========================================
        """.trimMargin())

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            mapOf(
                "status" to 400,
                "error" to "Validation Error",
                "message" to errors.joinToString("; "),
                "path" to request.requestURI
            )
        )
    }

    /**
     * 处理参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<Map<String, Any>> {
        logger.error("""
            |========== 参数类型错误 (400) ==========
            |请求路径: ${request.method} ${request.requestURI}
            |参数名: ${ex.name}
            |期望类型: ${ex.requiredType?.simpleName}
            |实际值: ${ex.value}
            |========================================
        """.trimMargin())

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            mapOf(
                "status" to 400,
                "error" to "Type Mismatch",
                "message" to "参数 '${ex.name}' 类型错误，期望 ${ex.requiredType?.simpleName}",
                "path" to request.requestURI
            )
        )
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(
        ex: RuntimeException,
        request: HttpServletRequest
    ): ResponseEntity<Map<String, Any>> {
        logger.error("""
            |========== 运行时异常 (500) ==========
            |请求路径: ${request.method} ${request.requestURI}
            |异常类型: ${ex.javaClass.simpleName}
            |错误信息: ${ex.message}
            |========================================
        """.trimMargin(), ex)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            mapOf(
                "status" to 500,
                "error" to "Internal Server Error",
                "message" to (ex.message ?: "服务器内部错误"),
                "path" to request.requestURI
            )
        )
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<Map<String, Any>> {
        logger.error("""
            |========== 未知异常 (500) ==========
            |请求路径: ${request.method} ${request.requestURI}
            |异常类型: ${ex.javaClass.name}
            |错误信息: ${ex.message}
            |====================================
        """.trimMargin(), ex)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            mapOf(
                "status" to 500,
                "error" to "Internal Server Error",
                "message" to "服务器内部错误",
                "path" to request.requestURI
            )
        )
    }
}
