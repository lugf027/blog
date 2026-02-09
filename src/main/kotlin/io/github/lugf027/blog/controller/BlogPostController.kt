package io.github.lugf027.blog.controller

import io.github.lugf027.blog.entity.BlogPost
import io.github.lugf027.blog.service.BlogPostService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/posts")
class BlogPostController(
    private val blogPostService: BlogPostService
) {

    @GetMapping
    fun getAllPosts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Map<String, Any>> {
        val posts = blogPostService.getAllPosts(page, size)
        val response = mapOf(
            "posts" to posts.content,
            "currentPage" to posts.number,
            "totalItems" to posts.totalElements,
            "totalPages" to posts.totalPages
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/published")
    fun getPublishedPosts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Map<String, Any>> {
        val posts = blogPostService.getPublishedPosts(page, size)
        val response = mapOf(
            "posts" to posts.content,
            "currentPage" to posts.number,
            "totalItems" to posts.totalElements,
            "totalPages" to posts.totalPages
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun getPostById(@PathVariable id: Long): ResponseEntity<BlogPost> {
        return blogPostService.getPostById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/{id}/view")
    fun viewPost(@PathVariable id: Long): ResponseEntity<BlogPost> {
        return try {
            val post = blogPostService.incrementViewCount(id)
            ResponseEntity.ok(post)
        } catch (e: RuntimeException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun createPost(@RequestBody post: BlogPost): ResponseEntity<BlogPost> {
        val createdPost = blogPostService.createPost(post)
        return ResponseEntity.ok(createdPost)
    }

    @PutMapping("/{id}")
    fun updatePost(@PathVariable id: Long, @RequestBody post: BlogPost): ResponseEntity<BlogPost> {
        return try {
            val updatedPost = blogPostService.updatePost(id, post)
            ResponseEntity.ok(updatedPost)
        } catch (e: RuntimeException) {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/{id}/publish")
    fun publishPost(@PathVariable id: Long): ResponseEntity<BlogPost> {
        return try {
            val publishedPost = blogPostService.publishPost(id)
            ResponseEntity.ok(publishedPost)
        } catch (e: RuntimeException) {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deletePost(@PathVariable id: Long): ResponseEntity<Void> {
        blogPostService.deletePost(id)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/search")
    fun searchPosts(
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Map<String, Any>> {
        val posts = blogPostService.searchPosts(keyword, page, size)
        val response = mapOf(
            "posts" to posts.content,
            "currentPage" to posts.number,
            "totalItems" to posts.totalElements,
            "totalPages" to posts.totalPages
        )
        return ResponseEntity.ok(response)
    }
}
