package io.github.lugf027.blog.repository

import io.github.lugf027.blog.entity.BlogPost
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BlogPostRepository : JpaRepository<BlogPost, Long> {
    fun findByStatus(status: String, pageable: Pageable): Page<BlogPost>
    fun findByStatusOrderByCreatedAtDesc(status: String): List<BlogPost>
    fun findByTitleContainingOrContentContaining(
        title: String,
        content: String,
        pageable: Pageable
    ): Page<BlogPost>
}
