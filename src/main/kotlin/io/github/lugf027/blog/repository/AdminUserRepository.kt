package io.github.lugf027.blog.repository

import io.github.lugf027.blog.entity.AdminUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AdminUserRepository : JpaRepository<AdminUser, Long> {
    fun findByUsername(username: String): Optional<AdminUser>
    fun existsByUsername(username: String): Boolean
}
