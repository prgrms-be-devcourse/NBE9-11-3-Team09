package com.example.parking.domain.user.dto

import com.example.parking.domain.user.entity.User
import com.example.parking.domain.user.entity.UserRole
import com.example.parking.domain.user.entity.UserStatus
import com.example.parking.domain.user.entity.VehicleType
import java.time.LocalDateTime

data class AdminUserResDto(
    val userId: Long?,
    val userEmail: String,
    val userName: String,
    val plateNumber: String,
    val vehicleType: VehicleType,
    val role: UserRole,
    val userStatus: UserStatus,
    val createdTime: LocalDateTime?
) {
    companion object {
        @JvmStatic
        fun from(user: User): AdminUserResDto =
            AdminUserResDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPlateNumber(),
                user.getVehicleType(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedTime()
            )
    }
}