package io.github.lugf027.blog.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "blog_post")
data class BlogPost(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var title: String = "",

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String = "",

    @Column(length = 500)
    var summary: String? = null,

    @Column(length = 500)
    var coverImage: String? = null,

    @Column(length = 100)
    var author: String? = null,

    @Column(length = 20)
    var status: String = "draft",

    @Column(name = "view_count")
    var viewCount: Int = 0,

    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,

    @Column(name = "published_at")
    var publishedAt: LocalDateTime? = null
) {
    @PrePersist
    protected fun onCreate() {
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
