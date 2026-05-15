package com.example.parking.domain.user.service

import com.example.parking.domain.user.dto.EmailCheckResDto
import com.example.parking.domain.user.dto.SignupReqDto
import com.example.parking.domain.user.dto.UserProfileResDto
import com.example.parking.domain.user.dto.VehicleUpdateReqDto
import com.example.parking.domain.user.dto.WithdrawReqDto
import com.example.parking.domain.user.entity.User
import com.example.parking.domain.user.entity.UserRole
import com.example.parking.domain.user.entity.UserStatus
import com.example.parking.domain.user.repository.RefreshTokenRepository
import com.example.parking.domain.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.regex.Pattern

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun checkEmail(email: String?): EmailCheckResDto {
        val checkedEmail = email?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("이메일은 필수입니다.")

        if (!EMAIL_PATTERN.matcher(checkedEmail).matches()) {
            throw IllegalArgumentException("올바른 이메일 형식이 아닙니다.")
        }

        return if (userRepository.existsByEmail(checkedEmail)) {
            EmailCheckResDto(false, "이미 사용 중인 이메일입니다.")
        } else {
            EmailCheckResDto(true, "사용 가능한 이메일입니다.")
        }
    }

    @Transactional
    fun signup(reqDto: SignupReqDto): UserProfileResDto {
        if (userRepository.existsByEmail(reqDto.userEmail)) {
            throw IllegalArgumentException("이미 사용 중인 이메일입니다.")
        }

        if (userRepository.existsByPlateNumber(reqDto.plateNumber)) {
            throw IllegalArgumentException("이미 등록된 차량 번호입니다.")
        }

        val encodedPassword = requireNotNull(passwordEncoder.encode(reqDto.password)) {
            "비밀번호 암호화에 실패했습니다."
        }

        val vehicleType = requireNotNull(reqDto.vehicleType) {
            "차량 종류는 필수입니다."
        }

        val user = User(
            email = reqDto.userEmail,
            password = encodedPassword,
            name = reqDto.name,
            plateNumber = reqDto.plateNumber,
            vehicleType = vehicleType,
            role = UserRole.USER,
            status = UserStatus.ACTIVE
        )

        val savedUser = userRepository.save(user)
        return UserProfileResDto.from(savedUser)
    }

    fun getMyProfile(userId: Long): UserProfileResDto {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }

        if (user.status != UserStatus.ACTIVE) {
            throw IllegalArgumentException("탈퇴한 사용자는 조회할 수 없습니다.")
        }

        return UserProfileResDto.from(user)
    }

    @Transactional
    fun updateMyVehicle(userId: Long, reqDto: VehicleUpdateReqDto): UserProfileResDto {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }

        if (user.status != UserStatus.ACTIVE) {
            throw IllegalArgumentException("탈퇴한 사용자는 수정할 수 없습니다.")
        }

        if (userRepository.existsByPlateNumberAndIdNot(reqDto.plateNumber, userId)) {
            throw IllegalArgumentException("이미 등록된 차량 번호입니다.")
        }

        user.updateVehicleInfo(
            reqDto.plateNumber,
            requireNotNull(reqDto.vehicleType) { "차량 종류는 필수입니다." }
        )

        return UserProfileResDto.from(user)
    }

    @Transactional
    fun withdraw(userId: Long, reqDto: WithdrawReqDto) {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }

        if (user.status != UserStatus.ACTIVE) {
            throw IllegalArgumentException("이미 탈퇴한 사용자입니다.")
        }

        if (!passwordEncoder.matches(reqDto.password, user.password)) {
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
        }

        user.withdraw()
        refreshTokenRepository.deleteByUserId(userId)
    }

    companion object {
        private val EMAIL_PATTERN: Pattern =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    }
}
