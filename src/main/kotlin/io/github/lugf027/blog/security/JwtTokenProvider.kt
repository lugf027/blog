package io.github.lugf027.blog.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*

@Component
class JwtTokenProvider {
    private val jwtSecret: Key = Keys.secretKeyFor(SignatureAlgorithm.HS512)
    private val jwtExpirationMs: Long = 86400000 // 24 hours

    fun generateToken(username: String): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationMs)

        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(jwtSecret, SignatureAlgorithm.HS512)
            .compact()
    }

    fun getUsernameFromToken(token: String): String {
        val claims: Claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .build()
            .parseClaimsJws(token)
            .body

        return claims.subject
    }

    fun validateToken(token: String): Boolean {
        try {
            Jwts.parser()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
