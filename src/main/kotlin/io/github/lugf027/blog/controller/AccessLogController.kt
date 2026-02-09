package io.github.lugf027.blog.controller

import io.github.lugf027.blog.entity.AccessLog
import io.github.lugf027.blog.service.AccessLogService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/access-logs")
class AccessLogController(
    private val accessLogService: AccessLogService
) {

    @GetMapping
    fun getAllLogs(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Map<String, Any>> {
        val logs: Page<AccessLog> = accessLogService.getAllLogs(page, size)
        val response = mapOf(
            "logs" to logs.content,
            "currentPage" to logs.number,
            "totalItems" to logs.totalElements,
            "totalPages" to logs.totalPages
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/by-ip")
    fun getLogsByIp(
        @RequestParam ipAddress: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Map<String, Any>> {
        val logs: Page<AccessLog> = accessLogService.getLogsByIp(ipAddress, page, size)
        val response = mapOf(
            "logs" to logs.content,
            "currentPage" to logs.number,
            "totalItems" to logs.totalElements,
            "totalPages" to logs.totalPages
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/by-url")
    fun getLogsByUrl(
        @RequestParam url: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Map<String, Any>> {
        val logs: Page<AccessLog> = accessLogService.getLogsByUrl(url, page, size)
        val response = mapOf(
            "logs" to logs.content,
            "currentPage" to logs.number,
            "totalItems" to logs.totalElements,
            "totalPages" to logs.totalPages
        )
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    fun deleteLog(@PathVariable id: Long): ResponseEntity<Void> {
        accessLogService.deleteLog(id)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/clear")
    fun clearAllLogs(): ResponseEntity<Map<String, String>> {
        accessLogService.clearAllLogs()
        return ResponseEntity.ok(mapOf("message" to "All logs cleared"))
    }
}
