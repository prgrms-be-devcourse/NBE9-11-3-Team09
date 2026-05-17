package com.example.parking.global.security

import com.example.parking.domain.user.entity.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date

// [CUS-08] 로그인 - JWT 토큰 생성 및 검증을 위한 유틸리티 클래스
@Component
class JwtUtil(
    @Value("\${spring.jwt.secret}") secret: String
) {
    private val secretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    private val accessTokenExpirationMillis = 1000L * 60 * 30
    private val refreshTokenExpirationMillis = 1000L * 60 * 60 * 24 * 7

    fun createAccessToken(user: User): String =
        createToken(user, accessTokenExpirationMillis, "access")

    fun createRefreshToken(user: User): String =
        createToken(user, refreshTokenExpirationMillis, "refresh")

    fun validateToken(token: String) {
        parseClaims(token)
    }

    fun getUserId(token: String): Long {
        val claims = parseClaims(token)
        return when (val userId = claims["userId"]) {
            is Int -> userId.toLong()
            is Long -> userId
            else -> claims.subject.toLong()
        }
    }

    fun getUserEmail(token: String): String =
        parseClaims(token)["userEmail", String::class.java]

    fun getRole(token: String): String =
        parseClaims(token)["role", String::class.java]

    fun getTokenType(token: String): String =
        parseClaims(token)["type", String::class.java]

    fun getExpiration(token: String): Date =
        parseClaims(token).expiration

    private fun createToken(user: User, expirationMillis: Long, type: String): String {
        val now = Date()
        val expiration = Date(now.time + expirationMillis)

        return Jwts.builder()
            .subject(user.id.toString())
            .claim("userId", user.id)
            .claim("userEmail", user.email)
            .claim("role", user.role.name)
            .claim("type", type)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)
            .compact()
    }

    private fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
}