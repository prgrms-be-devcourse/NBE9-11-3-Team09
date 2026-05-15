package com.example.parking.domain.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginReqDto (
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "이메일 형식이 올바르지 않습니다.")
    var userEmail: String = "",

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    var password: String = ""
)

