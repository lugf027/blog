package io.github.lugf027.blog.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "access_log")
data class AccessLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 100)
    var ipAddress: String = "",

    @Column(nullable = false, length = 500)
    var requestUrl: String = "",

    @Column(length = 10)
    var requestMethod: String? = null,

    @Column(length = 500)
    var userAgent: String? = null,

    @Column(length = 500)
    var referer: String? = null,

    @Column(name = "access_time", nullable = false)
    var accessTime: LocalDateTime? = null,

    @Column(name = "response_status")
    var responseStatus: Int? = null,

    @Column(name = "response_time")
    var responseTime: Long? = null
) {
    @PrePersist
    protected fun onCreate() {
        if (accessTime == null) {
            accessTime = LocalDateTime.now()
        }
    }
}
