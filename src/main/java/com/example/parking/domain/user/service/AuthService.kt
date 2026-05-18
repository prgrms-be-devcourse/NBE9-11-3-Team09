package com.example.parking.domain.user.service

import com.example.parking.domain.user.dto.LoginReqDto
import com.example.parking.domain.user.dto.LoginResDto
import com.example.parking.domain.user.dto.RefreshTokenReqDto
import com.example.parking.domain.user.entity.RefreshToken
import com.example.parking.domain.user.entity.UserStatus
import com.example.parking.domain.user.repository.RefreshTokenRepository
import com.example.parking.domain.user.repository.UserRepository
import com.example.parking.global.security.JwtUtil
import io.jsonwebtoken.JwtException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil
) {
    @Transactional
    fun login(reqDto: LoginReqDto): LoginResDto {
        val user = userRepository.findByEmail(reqDto.userEmail)
            .orElseThrow { IllegalArgumentException("존재하지 않는 이메일입니다.") }

        if (!passwordEncoder.matches(reqDto.password, user.password)) {
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
        }

        if (user.status != UserStatus.ACTIVE) {
            throw IllegalArgumentException("탈퇴한 사용자는 로그인할 수 없습니다.")
        }

        val accessToken = jwtUtil.createAccessToken(user)
        val refreshToken = jwtUtil.createRefreshToken(user)

        val userId = checkNotNull(user.id) {
            "저장되지 않은 사용자는 refresh token을 저장할 수 없습니다."
        }

        saveOrUpdateRefreshToken(userId, refreshToken)

        return LoginResDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer"
        )
    }

    @Transactional
    fun refresh(reqDto: RefreshTokenReqDto): LoginResDto {
        val refreshTokenValue = reqDto.refreshToken

        try {
            jwtUtil.validateToken(refreshTokenValue)
        } catch (e: JwtException) {
            throw IllegalArgumentException("유효하지 않은 refresh token입니다.")
        }

        if (jwtUtil.getTokenType(refreshTokenValue) != "refresh") {
            throw IllegalArgumentException("refresh token이 아닙니다.")
        }

        val savedToken = refreshTokenRepository.findByToken(refreshTokenValue)
            .orElseThrow { IllegalArgumentException("저장된 refresh token이 없습니다.") }

        val userId = jwtUtil.getUserId(refreshTokenValue)

        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }

        if (savedToken.userId != userId) {
            throw IllegalArgumentException("refresh token 사용자 정보가 일치하지 않습니다.")
        }

        if (user.status != UserStatus.ACTIVE) {
            throw IllegalArgumentException("탈퇴한 사용자는 token을 재발급할 수 없습니다.")
        }

        val newAccessToken = jwtUtil.createAccessToken(user)

        return LoginResDto(
            accessToken = newAccessToken,
            refreshToken = refreshTokenValue,
            tokenType = "Bearer"
        )
    }

    @Transactional
    fun logout(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }

        if (user.status != UserStatus.ACTIVE) {
            throw IllegalArgumentException("탈퇴한 사용자는 로그아웃할 수 없습니다.")
        }

        refreshTokenRepository.deleteByUserId(userId)
    }

    @Transactional
    fun saveOrUpdateRefreshToken(userId: Long, refreshToken: String) {
        val expiresAt = LocalDateTime.ofInstant(
            jwtUtil.getExpiration(refreshToken).toInstant(),
            ZoneId.systemDefault()
        )

        refreshTokenRepository.findByUserId(userId)
            .ifPresentOrElse(
                { savedToken ->
                    savedToken.updateToken(refreshToken, expiresAt)
                },
                {
                    refreshTokenRepository.save(
                        RefreshToken.of(
                            userId = userId,
                            token = refreshToken,
                            expiresAt = expiresAt
                        )
                    )
                }
            )
    }
}