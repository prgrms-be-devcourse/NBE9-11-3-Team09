package com.example.parking.domain.user.dto

import com.example.parking.domain.user.entity.VehicleType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class SignupReqDto(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "이메일 형식이 올바르지 않습니다.")
    val userEmail: String = "",

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    val password: String = "",

    @field:NotBlank(message = "이름은 필수입니다.")
    val name: String = "",

    @field:NotBlank(message = "차량 번호는 필수입니다.")
    val plateNumber: String = "",

    @field:NotNull(message = "차량 종류는 필수입니다.")
    val vehicleType: VehicleType? = null
)