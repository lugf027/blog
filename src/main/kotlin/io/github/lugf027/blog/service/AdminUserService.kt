package io.github.lugf027.blog.service

import io.github.lugf027.blog.entity.AdminUser
import io.github.lugf027.blog.repository.AdminUserRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class AdminUserService(
    private val adminUserRepository: AdminUserRepository,
    private val passwordEncoder: BCryptPasswordEncoder = BCryptPasswordEncoder()
) {

    fun login(username: String, password: String): AdminUser? {
        val userOptional = adminUserRepository.findByUsername(username)
        if (userOptional.isPresent) {
            val user = userOptional.get()
            if (user.enabled && passwordEncoder.matches(password, user.password)) {
                user.lastLoginAt = LocalDateTime.now()
                return adminUserRepository.save(user)
            }
        }
        return null
    }

    fun createUser(username: String, password: String, nickname: String?, email: String?): AdminUser {
        val encodedPassword = passwordEncoder.encode(password)
        val user = AdminUser(
            username = username,
            password = encodedPassword!!,
            nickname = nickname,
            email = email
        )
        return adminUserRepository.save(user)
    }

    fun findByUsername(username: String): Optional<AdminUser> {
        return adminUserRepository.findByUsername(username)
    }

    fun existsByUsername(username: String): Boolean {
        return adminUserRepository.existsByUsername(username)
    }

    fun getAllUsers(): List<AdminUser> {
        return adminUserRepository.findAll()
    }

    fun updateUser(id: Long, nickname: String?, email: String?): AdminUser? {
        val userOptional = adminUserRepository.findById(id)
        if (userOptional.isPresent) {
            val user = userOptional.get()
            val updatedUser = user.copy(
                nickname = nickname ?: user.nickname,
                email = email ?: user.email
            )
            return adminUserRepository.save(updatedUser)
        }
        return null
    }

    fun changePassword(id: Long, oldPassword: String, newPassword: String): Boolean {
        val userOptional = adminUserRepository.findById(id)
        if (userOptional.isPresent) {
            val user = userOptional.get()
            if (passwordEncoder.matches(oldPassword, user.password)) {
                val updatedUser = user.copy(password = passwordEncoder.encode(newPassword)!!)
                adminUserRepository.save(updatedUser)
                return true
            }
        }
        return false
    }

    fun toggleUserStatus(id: Long): AdminUser? {
        val userOptional = adminUserRepository.findById(id)
        if (userOptional.isPresent) {
            val user = userOptional.get()
            val updatedUser = user.copy(enabled = !user.enabled)
            return adminUserRepository.save(updatedUser)
        }
        return null
    }

    fun deleteUser(id: Long) {
        adminUserRepository.deleteById(id)
    }
}
