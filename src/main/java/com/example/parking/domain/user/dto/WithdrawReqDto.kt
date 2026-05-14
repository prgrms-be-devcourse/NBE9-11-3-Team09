package com.example.parking.domain.user.dto

import jakarta.validation.constraints.NotBlank

data class WithdrawReqDto(
    @field:NotBlank(message = "비밀번호는 필수입니다.")
    var password: String = ""
)