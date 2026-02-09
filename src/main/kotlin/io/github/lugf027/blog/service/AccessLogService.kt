package io.github.lugf027.blog.service

import io.github.lugf027.blog.entity.AccessLog
import io.github.lugf027.blog.repository.AccessLogRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class AccessLogService(
    private val accessLogRepository: AccessLogRepository
) {

    fun getAllLogs(page: Int, size: Int): Page<AccessLog> {
        val pageable: Pageable = PageRequest.of(
            page, 
            size, 
            Sort.by(Sort.Direction.DESC, "accessTime")
        )
        return accessLogRepository.findAll(pageable)
    }

    fun getLogsByIp(ipAddress: String, page: Int, size: Int): Page<AccessLog> {
        val pageable: Pageable = PageRequest.of(
            page, 
            size, 
            Sort.by(Sort.Direction.DESC, "accessTime")
        )
        return accessLogRepository.findByIpAddress(ipAddress, pageable)
    }

    fun getLogsByUrl(url: String, page: Int, size: Int): Page<AccessLog> {
        val pageable: Pageable = PageRequest.of(
            page, 
            size, 
            Sort.by(Sort.Direction.DESC, "accessTime")
        )
        return accessLogRepository.findByRequestUrlContaining(url, pageable)
    }

    fun deleteLog(id: Long) {
        accessLogRepository.deleteById(id)
    }

    fun clearAllLogs() {
        accessLogRepository.deleteAll()
    }
}
