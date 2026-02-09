package io.github.lugf027.blog.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "access_log")
data class AccessLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 100)
    val ipAddress: String,

    @Column(nullable = false, length = 500)
    val requestUrl: String,

    @Column(length = 10)
    val requestMethod: String? = null,

    @Column(length = 500)
    val userAgent: String? = null,

    @Column(length = 500)
    val referer: String? = null,

    @Column(name = "access_time", nullable = false)
    val accessTime: LocalDateTime = LocalDateTime.now(),

    @Column(name = "response_status")
    val responseStatus: Int? = null,

    @Column(name = "response_time")
    val responseTime: Long? = null
)
