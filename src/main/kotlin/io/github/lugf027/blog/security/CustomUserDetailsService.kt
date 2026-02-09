package io.github.lugf027.blog.security

import io.github.lugf027.blog.repository.AdminUserRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val adminUserRepository: AdminUserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val adminUser = adminUserRepository.findByUsername(username)
            .orElseThrow { UsernameNotFoundException("User not found: $username") }

        return User.builder()
            .username(adminUser.username)
            .password(adminUser.password)
            .disabled(!adminUser.enabled)
            .authorities("ROLE_ADMIN")
            .build()
    }
}
