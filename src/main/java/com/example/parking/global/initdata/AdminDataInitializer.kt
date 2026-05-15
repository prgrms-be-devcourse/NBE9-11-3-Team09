package com.example.parking.global.initdata

import com.example.parking.domain.user.entity.User
import com.example.parking.domain.user.entity.UserRole
import com.example.parking.domain.user.entity.UserStatus
import com.example.parking.domain.user.entity.VehicleType
import com.example.parking.domain.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class AdminDataInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,

    @Value("\${app.admin.email:}")
    private val adminEmail: String,

    @Value("\${app.admin.password:}")
    private val adminPassword: String,

    @Value("\${app.admin.name:관리자}")
    private val adminName: String,

    @Value("\${app.admin.plate-number:00가0000}")
    private val adminPlateNumber: String,

    @Value("\${app.admin.vehicle-type:SMALL}")
    private val adminVehicleType: String
) : CommandLineRunner {

    override fun run(vararg args: String) {
        if (adminEmail.isBlank()) {
            return
        }

        if (adminPassword.isBlank()) {
            return
        }

        if (userRepository.existsByEmail(adminEmail)) {
            return
        }

        val encodedPassword = requireNotNull(passwordEncoder.encode(adminPassword)) {
            "관리자 비밀번호 암호화에 실패했습니다."
        }

        val admin = User(
            email = adminEmail,
            password = encodedPassword,
            name = adminName,
            plateNumber = adminPlateNumber,
            vehicleType = VehicleType.valueOf(adminVehicleType),
            role = UserRole.ADMIN,
            status = UserStatus.ACTIVE
        )

        userRepository.save(admin)
    }
}
