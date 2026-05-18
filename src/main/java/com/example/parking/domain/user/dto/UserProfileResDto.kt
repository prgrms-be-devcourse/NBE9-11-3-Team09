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
        fun from(user: User): UserProfileResDto =
            UserProfileResDto(
                user.id,
                user.email,
                user.name,
                user.plateNumber,
                user.vehicleType,
                user.role
            )
    }
}