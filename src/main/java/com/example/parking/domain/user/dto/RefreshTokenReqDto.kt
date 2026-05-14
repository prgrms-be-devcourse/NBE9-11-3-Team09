package com.example.parking.domain.user.dto

import jakarta.validation.constraints.NotBlank

data class RefreshTokenReqDto(
    @field:NotBlank(message = "리프레시 토큰은 필수입니다.")
    var refreshToken: String = ""
)