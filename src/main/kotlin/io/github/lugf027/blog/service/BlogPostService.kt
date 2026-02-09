package io.github.lugf027.blog.service

import io.github.lugf027.blog.entity.BlogPost
import io.github.lugf027.blog.repository.BlogPostRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BlogPostService(
    private val blogPostRepository: BlogPostRepository
) {

    fun getAllPosts(page: Int, size: Int): Page<BlogPost> {
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return blogPostRepository.findAll(pageable)
    }

    fun getPublishedPosts(page: Int, size: Int): Page<BlogPost> {
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"))
        return blogPostRepository.findByStatus("published", pageable)
    }

    fun getPostById(id: Long): BlogPost? {
        return blogPostRepository.findById(id).orElse(null)
    }

    @Transactional
    fun createPost(post: BlogPost): BlogPost {
        return blogPostRepository.save(post)
    }

    @Transactional
    fun updatePost(id: Long, post: BlogPost): BlogPost {
        val existingPost = blogPostRepository.findById(id)
            .orElseThrow { RuntimeException("Post not found with id: $id") }

        existingPost.title = post.title
        existingPost.content = post.content
        existingPost.summary = post.summary
        existingPost.coverImage = post.coverImage
        existingPost.author = post.author
        existingPost.status = post.status

        return blogPostRepository.save(existingPost)
    }

    @Transactional
    fun publishPost(id: Long): BlogPost {
        val post = blogPostRepository.findById(id)
            .orElseThrow { RuntimeException("Post not found with id: $id") }

        post.status = "published"
        post.publishedAt = LocalDateTime.now()

        return blogPostRepository.save(post)
    }

    @Transactional
    fun deletePost(id: Long) {
        blogPostRepository.deleteById(id)
    }

    @Transactional
    fun incrementViewCount(id: Long): BlogPost {
        val post = blogPostRepository.findById(id)
            .orElseThrow { RuntimeException("Post not found with id: $id") }

        post.viewCount++
        return blogPostRepository.save(post)
    }

    fun searchPosts(keyword: String, page: Int, size: Int): Page<BlogPost> {
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return blogPostRepository.findByTitleContainingOrContentContaining(keyword, keyword, pageable)
    }
}
