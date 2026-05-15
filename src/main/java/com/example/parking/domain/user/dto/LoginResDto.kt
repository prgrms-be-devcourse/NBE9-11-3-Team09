package com.example.parking.domain.user.dto

data class LoginResDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String
)