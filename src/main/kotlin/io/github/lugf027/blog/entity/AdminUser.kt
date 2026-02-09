package io.github.lugf027.blog.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "admin_user")
data class AdminUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 50)
    var username: String = "",

    @Column(nullable = false, length = 100)
    var password: String = "",

    @Column(length = 50)
    var nickname: String? = null,

    @Column(length = 100)
    var email: String? = null,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,

    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null
) {
    @PrePersist
    protected fun onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now()
        }
    }
}
