package com.example.parking.domain.user.service

import com.example.parking.domain.user.dto.SignupReqDto
import com.example.parking.domain.user.dto.VehicleUpdateReqDto
import com.example.parking.domain.user.dto.WithdrawReqDto
import com.example.parking.domain.user.entity.RefreshToken
import com.example.parking.domain.user.entity.User
import com.example.parking.domain.user.entity.UserRole
import com.example.parking.domain.user.entity.UserStatus
import com.example.parking.domain.user.entity.VehicleType
import com.example.parking.domain.user.repository.RefreshTokenRepository
import com.example.parking.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@Transactional
class UserServiceTest @Autowired constructor(
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder
) {
    @Test
    @DisplayName("사용 가능한 이메일이면 available=true를 반환한다")
    fun checkEmail_returnsAvailableTrue() {
        val result = userService.checkEmail("available@test.com")

        assertThat(result.available).isTrue()
    }

    @Test
    @DisplayName("이미 존재하는 이메일이면 available=false를 반환한다")
    fun checkEmail_returnsAvailableFalse_whenEmailExists() {
        createUser(email = "exists@test.com")

        val result = userService.checkEmail("exists@test.com")

        assertThat(result.available).isFalse()
    }

    @Test
    @DisplayName("회원가입 성공 시 사용자를 저장하고 비밀번호를 암호화한다")
    fun signup_savesUser() {
        val reqDto = SignupReqDto(
            userEmail = "signup@test.com",
            password = "test1234",
            name = "김철수",
            plateNumber = "23가4567",
            vehicleType = VehicleType.LARGE
        )

        val result = userService.signup(reqDto)

        val savedUser = userRepository.findByEmail("signup@test.com").orElseThrow()
        assertThat(result.userId).isEqualTo(savedUser.id)
        assertThat(result.userEmail).isEqualTo("signup@test.com")
        assertThat(result.userName).isEqualTo("김철수")
        assertThat(result.plateNumber).isEqualTo("23가4567")
        assertThat(result.vehicleType).isEqualTo(VehicleType.LARGE)
        assertThat(result.role).isEqualTo(UserRole.USER)
        assertThat(savedUser.status).isEqualTo(UserStatus.ACTIVE)
        assertThat(passwordEncoder.matches("test1234", savedUser.password)).isTrue()
    }

    @Test
    @DisplayName("회원가입 시 이메일이 중복되면 실패한다")
    fun signup_fails_whenEmailExists() {
        createUser(email = "duplicate@test.com")

        val reqDto = SignupReqDto(
            userEmail = "duplicate@test.com",
            password = "test5678",
            name = "박민수",
            plateNumber = "45나6789",
            vehicleType = VehicleType.ELECTRIC
        )

        assertThatThrownBy { userService.signup(reqDto) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("회원가입 시 차량번호가 중복되면 실패한다")
    fun signup_fails_whenPlateNumberExists() {
        createUser(email = "owner@test.com", plateNumber = "56다7890")

        val reqDto = SignupReqDto(
            userEmail = "newuser@test.com",
            password = "test5678",
            name = "정수지",
            plateNumber = "56다7890",
            vehicleType = VehicleType.SMALL
        )

        assertThatThrownBy { userService.signup(reqDto) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("ACTIVE 사용자는 내 정보를 조회할 수 있다")
    fun getMyProfile_returnsUserInfo() {
        val savedUser = createUser(
            email = "profile@test.com",
            name = "서지민",
            plateNumber = "67라8901",
            vehicleType = VehicleType.ELECTRIC
        )

        val result = userService.getMyProfile(savedUser.id!!)

        assertThat(result.userId).isEqualTo(savedUser.id)
        assertThat(result.userEmail).isEqualTo("profile@test.com")
        assertThat(result.userName).isEqualTo("서지민")
        assertThat(result.plateNumber).isEqualTo("67라8901")
        assertThat(result.vehicleType).isEqualTo(VehicleType.ELECTRIC)
    }

    @Test
    @DisplayName("탈퇴한 사용자는 내 정보를 조회할 수 없다")
    fun getMyProfile_fails_whenUserIsWithdrawn() {
        val withdrawnUser = createUser(email = "withdrawn@test.com")
        withdrawnUser.withdraw()

        assertThatThrownBy { userService.getMyProfile(withdrawnUser.id!!) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("차량 정보를 수정하면 차량번호와 차량종류가 변경된다")
    fun updateMyVehicle_updatesPlateNumberAndVehicleType() {
        val savedUser = createUser(email = "vehicle@test.com", plateNumber = "89마1234")

        val reqDto = VehicleUpdateReqDto(
            plateNumber = "99가9999",
            vehicleType = VehicleType.ELECTRIC
        )

        val result = userService.updateMyVehicle(savedUser.id!!, reqDto)

        val updatedUser = userRepository.findById(savedUser.id!!).orElseThrow()
        assertThat(result.plateNumber).isEqualTo("99가9999")
        assertThat(result.vehicleType).isEqualTo(VehicleType.ELECTRIC)
        assertThat(updatedUser.plateNumber).isEqualTo("99가9999")
        assertThat(updatedUser.vehicleType).isEqualTo(VehicleType.ELECTRIC)
    }

    @Test
    @DisplayName("차량 정보 수정 시 다른 사용자의 차량번호와 중복되면 실패한다")
    fun updateMyVehicle_fails_whenPlateNumberExists() {
        val targetUser = createUser(email = "target@test.com", plateNumber = "10가1010")
        createUser(email = "other@test.com", plateNumber = "20나2020")

        val reqDto = VehicleUpdateReqDto(
            plateNumber = "20나2020",
            vehicleType = VehicleType.ELECTRIC
        )

        assertThatThrownBy { userService.updateMyVehicle(targetUser.id!!, reqDto) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("회원탈퇴 시 상태가 WITHDRAW로 변경되고 refresh token이 삭제된다")
    fun withdraw_updatesStatusAndDeletesRefreshToken() {
        val savedUser = createUser(email = "withdraw@test.com", rawPassword = "test1234")

        refreshTokenRepository.save(
            RefreshToken.of(
                userId = savedUser.id!!,
                token = "refresh-token",
                expiresAt = LocalDateTime.now().plusDays(1)
            )
        )

        val reqDto = WithdrawReqDto(password = "test1234")

        userService.withdraw(savedUser.id!!, reqDto)

        val withdrawnUser = userRepository.findById(savedUser.id!!).orElseThrow()
        assertThat(withdrawnUser.status).isEqualTo(UserStatus.WITHDRAW)
        assertThat(refreshTokenRepository.findByUserId(savedUser.id!!)).isEmpty()
    }

    @Test
    @DisplayName("회원탈퇴 시 비밀번호가 일치하지 않으면 실패한다")
    fun withdraw_fails_whenPasswordIsWrong() {
        val savedUser = createUser(email = "withdraw-fail@test.com", rawPassword = "test1234")

        val reqDto = WithdrawReqDto(password = "wrong1234")

        assertThatThrownBy { userService.withdraw(savedUser.id!!, reqDto) }
            .isInstanceOf(IllegalArgumentException::class.java)

        val notWithdrawnUser = userRepository.findById(savedUser.id!!).orElseThrow()
        assertThat(notWithdrawnUser.status).isEqualTo(UserStatus.ACTIVE)
    }

    private fun createUser(
        email: String,
        rawPassword: String = "test1234",
        name: String = "테스트유저",
        plateNumber: String = "12가3456",
        vehicleType: VehicleType = VehicleType.SMALL,
        role: UserRole = UserRole.USER,
        status: UserStatus = UserStatus.ACTIVE
    ): User {
        return userRepository.save(
            User(
                email = email,
                password = requireNotNull(passwordEncoder.encode(rawPassword)) {
                    "Password encoding failed."
                },
                name = name,
                plateNumber = plateNumber,
                vehicleType = vehicleType,
                role = role,
                status = status
            )
        )
    }
}
