package com.example.parking.domain.user.dto

import com.example.parking.domain.user.entity.VehicleType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class SignupReqDto(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "이메일 형식이 올바르지 않습니다.")
    var userEmail: String = "",

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    var password: String = "",

    @field:NotBlank(message = "이름은 필수입니다.")
    var name: String = "",

    @field:NotBlank(message = "차량 번호는 필수입니다.")
    var plateNumber: String = "",

    @field:NotBlank(message = "차량 종류는 필수입니다.")
    var vehicleType: VehicleType? = null
)