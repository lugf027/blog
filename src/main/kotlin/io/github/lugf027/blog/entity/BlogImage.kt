package io.github.lugf027.blog.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "blog_image")
data class BlogImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var fileName: String = "",

    @Column(nullable = false, length = 500)
    var filePath: String = "",

    var fileSize: Long? = null,

    @Column(length = 100)
    var contentType: String? = null,

    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null
) {
    @PrePersist
    protected fun onCreate() {
        createdAt = LocalDateTime.now()
    }
}
