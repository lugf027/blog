package io.github.lugf027.blog.dto

import io.github.lugf027.blog.entity.BlogPost

/**
 * 创建/更新文章的请求 DTO
 */
data class CreatePostRequest(
    val title: String,
    val content: String,
    val summary: String? = null,
    val coverImage: String? = null,
    val author: String? = null,
    val status: String = "draft"
) {
    /**
     * 转换为实体类
     */
    fun toEntity(): BlogPost {
        return BlogPost(
            title = title,
            content = content,
            summary = summary,
            coverImage = coverImage,
            author = author,
            status = status,
            viewCount = 0
        )
    }
}

/**
 * 更新文章的请求 DTO
 */
data class UpdatePostRequest(
    val title: String? = null,
    val content: String? = null,
    val summary: String? = null,
    val coverImage: String? = null,
    val author: String? = null,
    val status: String? = null
) {
    /**
     * 将更新应用到现有实体
     */
    fun applyTo(existingPost: BlogPost): BlogPost {
        return existingPost.copy(
            title = title ?: existingPost.title,
            content = content ?: existingPost.content,
            summary = summary ?: existingPost.summary,
            coverImage = coverImage ?: existingPost.coverImage,
            author = author ?: existingPost.author,
            status = status ?: existingPost.status
        )
    }
}
