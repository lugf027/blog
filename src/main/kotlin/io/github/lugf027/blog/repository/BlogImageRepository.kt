package io.github.lugf027.blog.repository

import io.github.lugf027.blog.entity.BlogImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BlogImageRepository : JpaRepository<BlogImage, Long>
