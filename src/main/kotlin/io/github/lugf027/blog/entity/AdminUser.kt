package io.github.lugf027.blog.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "admin_user")
data class AdminUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 50)
    val username: String,

    @Column(nullable = false, length = 100)
    val password: String,

    @Column(length = 50)
    val nickname: String? = null,

    @Column(length = 100)
    val email: String? = null,

    @Column(nullable = false)
    val enabled: Boolean = true,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null
)
