package io.github.lugf027.blog.repository

import io.github.lugf027.blog.entity.AccessLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccessLogRepository : JpaRepository<AccessLog, Long> {
    fun findByIpAddress(ipAddress: String, pageable: Pageable): Page<AccessLog>
    fun findByRequestUrlContaining(url: String, pageable: Pageable): Page<AccessLog>
}
