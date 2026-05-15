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
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    @Value("\${app.admin.email:}")
    private lateinit var adminEmail: String

    @Value("\${app.admin.password:}")
    private lateinit var adminPassword: String

    @Value("\${app.admin.name:관리자}")
    private lateinit var adminName: String

    @Value("\${app.admin.plate-number:00가0000}")
    private lateinit var adminPlateNumber: String

    @Value("\${app.admin.vehicle-type:SMALL}")
    private lateinit var adminVehicleType: String

    override fun run(vararg args: String) {
        if (adminEmail.isBlank()) {
            return
        }

        if (adminPassword.isBlank()) {
            return
        }

        if (userRepository.findByEmail(adminEmail).isPresent) {
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
