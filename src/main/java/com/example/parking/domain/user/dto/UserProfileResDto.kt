package com.example.parking.domain.user.dto

import com.example.parking.domain.user.entity.User
import com.example.parking.domain.user.entity.UserRole
import com.example.parking.domain.user.entity.VehicleType

data class UserProfileResDto(
    val userId: Long?,
    val userEmail: String,
    val userName: String,
    val plateNumber: String,
    val vehicleType: VehicleType,
    val role: UserRole
) {
    companion object {
        @JvmStatic
        fun from(user: User): UserProfileResDto =
            UserProfileResDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPlateNumber(),
                user.getVehicleType(),
                user.getRole()
            )
    }
}