package io.github.lugf027.blog.service

import io.github.lugf027.blog.entity.AdminUser
import io.github.lugf027.blog.repository.AdminUserRepository
import io.github.lugf027.blog.security.JwtTokenProvider
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthService(
    private val adminUserRepository: AdminUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenProvider: JwtTokenProvider
) {

    fun login(username: String, password: String): String {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(username, password)
        )

        // Update last login time
        adminUserRepository.findByUsername(username).ifPresent { user ->
            user.lastLoginAt = LocalDateTime.now()
            adminUserRepository.save(user)
        }

        return jwtTokenProvider.generateToken(username)
    }

    @Transactional
    fun register(username: String, password: String, email: String?, nickname: String?): AdminUser {
        if (adminUserRepository.existsByUsername(username)) {
            throw RuntimeException("Username already exists")
        }

        val adminUser = AdminUser(
            username = username,
            password = passwordEncoder.encode(password)!!,
            email = email,
            nickname = nickname,
            enabled = true
        )

        return adminUserRepository.save(adminUser)
    }

    fun getCurrentUser(username: String): AdminUser? {
        return adminUserRepository.findByUsername(username).orElse(null)
    }
}
