package com.example.parking.domain.user.dto

import com.example.parking.domain.user.entity.VehicleType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class VehicleUpdateReqDto(
    @field:NotBlank(message = "차량번호는 필수입니다.")
    @field:Size(max = 20, message = "차량번호는 20자 이하로 입력해야 합니다.")
    var plateNumber: String = "",

    @field:NotNull(message = "차량종류는 필수입니다.")
    var vehicleType: VehicleType? = null
)