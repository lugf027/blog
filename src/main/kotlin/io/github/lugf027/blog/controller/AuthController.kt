package io.github.lugf027.blog.controller

import io.github.lugf027.blog.service.AdminUserService
import io.github.lugf027.blog.util.JwtUtil
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val adminUserService: AdminUserService,
    private val jwtUtil: JwtUtil
) {

    data class LoginRequest(
        val username: String,
        val password: String
    )

    data class LoginResponse(
        val success: Boolean,
        val message: String,
        val token: String? = null,
        val username: String? = null,
        val nickname: String? = null
    )

    data class RegisterRequest(
        val username: String,
        val password: String,
        val nickname: String?,
        val email: String?
    )

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val user = adminUserService.login(request.username, request.password)
        return if (user != null) {
            val token = jwtUtil.generateToken(user.username)
            ResponseEntity.ok(
                LoginResponse(
                    success = true,
                    message = "登录成功",
                    token = token,
                    username = user.username,
                    nickname = user.nickname
                )
            )
        } else {
            ResponseEntity.ok(
                LoginResponse(
                    success = false,
                    message = "用户名或密码错误"
                )
            )
        }
    }

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<Map<String, Any>> {
        if (adminUserService.existsByUsername(request.username)) {
            return ResponseEntity.ok(
                mapOf(
                    "success" to false,
                    "message" to "用户名已存在"
                )
            )
        }

        val user = adminUserService.createUser(
            request.username,
            request.password,
            request.nickname,
            request.email
        )

        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "message" to "注册成功",
                "user" to mapOf(
                    "id" to user.id,
                    "username" to user.username,
                    "nickname" to user.nickname
                )
            )
        )
    }

    @PostMapping("/validate")
    fun validateToken(@RequestHeader("Authorization") authHeader: String): ResponseEntity<Map<String, Any>> {
        val token = authHeader.removePrefix("Bearer ")
        val isValid = jwtUtil.validateToken(token)
        val username = if (isValid) jwtUtil.getUsernameFromToken(token) ?: "" else ""

       return ResponseEntity.ok(
            mapOf(
                "valid" to isValid,
                "username" to username
            )
        )
    }
}
