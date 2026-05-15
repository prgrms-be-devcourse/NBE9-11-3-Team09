package com.example.parking.domain.user.entity

import jakarta.persistence.*

import java.time.LocalDateTime

@Entity
@Table(name = "refresh_tokens")
// 리프레시 토큰 관리 - 사용자별로 하나의 리프레시 토큰을 저장하는 엔티티
class RefreshToken(
    @Column(name = "user_id", nullable = false, unique = true)
    var userId: Long,

    @Column(name = "refresh_token", nullable = false, unique = true, length = 500)
    var token: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    var id: Long? = null
        protected set

    fun updateToken(token: String, expiresAt: LocalDateTime) {
        this.token = token
        this.expiresAt = expiresAt
    }

    companion object {
        @JvmStatic
        fun of(
            userId: Long,
            token: String,
            expiresAt: LocalDateTime
        ): RefreshToken =
            RefreshToken(
                userId = userId,
                token = token,
                expiresAt = expiresAt
            )
    }
}