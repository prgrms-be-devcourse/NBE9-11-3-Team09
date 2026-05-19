package com.example.parking.domain.user.service

import com.example.parking.domain.user.dto.LoginReqDto
import com.example.parking.domain.user.dto.RefreshTokenReqDto
import com.example.parking.domain.user.entity.RefreshToken
import com.example.parking.domain.user.entity.User
import com.example.parking.domain.user.entity.UserRole
import com.example.parking.domain.user.entity.UserStatus
import com.example.parking.domain.user.entity.VehicleType
import com.example.parking.domain.user.repository.RefreshTokenRepository
import com.example.parking.domain.user.repository.UserRepository
import com.example.parking.global.security.JwtUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.security.MessageDigest

@SpringBootTest
@Transactional
class AuthServiceTest @Autowired constructor(
    private val authService: AuthService,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil
) {
    @Test
    @DisplayName("로그인 성공 시 access token과 refresh token을 발급하고 refresh token을 저장한다")
    fun login_issuesTokens() {
        val savedUser = createUser(
            email = "auth1@test.com",
            rawPassword = "test1234",
            name = "홍길동",
            plateNumber = "12가3456",
            vehicleType = VehicleType.SMALL
        )

        val reqDto = LoginReqDto(
            userEmail = "auth1@test.com",
            password = "test1234"
        )

        val result = authService.login(reqDto)

        assertThat(result.accessToken).isNotBlank()
        assertThat(result.refreshToken).isNotBlank()
        assertThat(result.tokenType).isEqualTo("Bearer")

        val savedRefreshToken = refreshTokenRepository.findByUserId(savedUser.id!!).orElseThrow()
        assertThat(savedRefreshToken.token).isEqualTo(hashRefreshToken(result.refreshToken))
        assertThat(savedRefreshToken.token).isNotEqualTo(result.refreshToken)
        assertThat(jwtUtil.getTokenType(result.accessToken)).isEqualTo("access")
        assertThat(jwtUtil.getTokenType(result.refreshToken)).isEqualTo("refresh")
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 통합 로그인 실패 메시지를 반환한다")
    fun login_fails_whenEmailDoesNotExist() {
        val reqDto = LoginReqDto(
            userEmail = "not-found@test.com",
            password = "test1234"
        )

        assertThatThrownBy { authService.login(reqDto) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage(LOGIN_FAIL_MESSAGE)
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 통합 로그인 실패 메시지를 반환한다")
    fun login_fails_whenPasswordIsWrong() {
        createUser(
            email = "auth2@test.com",
            rawPassword = "test1234",
            name = "김철수",
            plateNumber = "23나4567",
            vehicleType = VehicleType.LARGE
        )

        val reqDto = LoginReqDto(
            userEmail = "auth2@test.com",
            password = "wrong1234"
        )

        assertThatThrownBy { authService.login(reqDto) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage(LOGIN_FAIL_MESSAGE)
    }

    @Test
    @DisplayName("탈퇴한 사용자는 로그인할 수 없다")
    fun login_fails_whenUserIsWithdrawn() {
        val withdrawnUser = createUser(
            email = "auth3@test.com",
            rawPassword = "test1234",
            name = "이영희",
            plateNumber = "34다5678",
            vehicleType = VehicleType.ELECTRIC
        )
        withdrawnUser.withdraw()

        val reqDto = LoginReqDto(
            userEmail = "auth3@test.com",
            password = "test1234"
        )

        assertThatThrownBy { authService.login(reqDto) }
            .isInstanceOf(IllegalArgumentException::class.java)

        assertThat(withdrawnUser.status).isEqualTo(UserStatus.WITHDRAW)
    }

    @Test
    @DisplayName("유효한 refresh token으로 access token을 재발급할 수 있다")
    fun refresh_reissuesAccessToken() {
        createUser(
            email = "auth4@test.com",
            rawPassword = "test1234",
            name = "박민수",
            plateNumber = "45라6789",
            vehicleType = VehicleType.SMALL
        )

        val loginResult = authService.login(
            LoginReqDto(
                userEmail = "auth4@test.com",
                password = "test1234"
            )
        )

        val refreshReqDto = RefreshTokenReqDto(
            refreshToken = loginResult.refreshToken
        )

        val refreshResult = authService.refresh(refreshReqDto)

        assertThat(refreshResult.accessToken).isNotBlank()
        assertThat(refreshResult.refreshToken).isEqualTo(loginResult.refreshToken)
        assertThat(refreshResult.tokenType).isEqualTo("Bearer")
        assertThat(jwtUtil.getTokenType(refreshResult.accessToken)).isEqualTo("access")
        assertThat(jwtUtil.getTokenType(refreshResult.refreshToken)).isEqualTo("refresh")
    }

    @Test
    @DisplayName("DB에 저장되지 않은 refresh token으로는 재발급할 수 없다")
    fun refresh_fails_whenTokenIsNotSaved() {
        val user = createUser(
            email = "auth5@test.com",
            rawPassword = "test1234",
            name = "최지우",
            plateNumber = "56마7890",
            vehicleType = VehicleType.LARGE
        )

        val unsavedRefreshToken = jwtUtil.createRefreshToken(user)
        val refreshReqDto = RefreshTokenReqDto(
            refreshToken = unsavedRefreshToken
        )

        assertThatThrownBy { authService.refresh(refreshReqDto) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("access token으로는 재발급할 수 없다")
    fun refresh_fails_whenTokenTypeIsAccess() {
        createUser(
            email = "auth6@test.com",
            rawPassword = "test1234",
            name = "정수지",
            plateNumber = "67바8901",
            vehicleType = VehicleType.SMALL
        )

        val loginResult = authService.login(
            LoginReqDto(
                userEmail = "auth6@test.com",
                password = "test1234"
            )
        )

        val refreshReqDto = RefreshTokenReqDto(
            refreshToken = loginResult.accessToken
        )

        assertThatThrownBy { authService.refresh(refreshReqDto) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("로그아웃하면 저장된 refresh token을 삭제한다")
    fun logout_deletesRefreshToken() {
        val savedUser = createUser(
            email = "auth7@test.com",
            rawPassword = "test1234",
            name = "서지민",
            plateNumber = "78사9012",
            vehicleType = VehicleType.ELECTRIC
        )

        authService.login(
            LoginReqDto(
                userEmail = "auth7@test.com",
                password = "test1234"
            )
        )

        assertThat(refreshTokenRepository.findByUserId(savedUser.id!!)).isPresent()

        authService.logout(savedUser.id!!)

        assertThat(refreshTokenRepository.findByUserId(savedUser.id!!)).isEmpty()
    }

    @Test
    @DisplayName("같은 사용자에 대한 refresh token 저장 요청이 오면 기존 토큰을 갱신한다")
    fun saveOrUpdateRefreshToken_updatesExistingToken() {
        val savedUser = createUser(
            email = "auth8@test.com",
            rawPassword = "test1234",
            name = "유재석",
            plateNumber = "89아0123",
            vehicleType = VehicleType.SMALL
        )

        val firstRefreshToken = jwtUtil.createRefreshToken(savedUser)
        authService.saveOrUpdateRefreshToken(savedUser.id!!, firstRefreshToken)

        val firstSavedToken = refreshTokenRepository.findByUserId(savedUser.id!!).orElseThrow()
        val firstTokenId = firstSavedToken.id

        val secondRefreshToken = jwtUtil.createRefreshToken(savedUser)

        authService.saveOrUpdateRefreshToken(savedUser.id!!, secondRefreshToken)

        val updatedToken = refreshTokenRepository.findByUserId(savedUser.id!!).orElseThrow()
        assertThat(updatedToken.id).isEqualTo(firstTokenId)
        assertThat(updatedToken.token).isEqualTo(hashRefreshToken(secondRefreshToken))
        assertThat(updatedToken.token).isNotEqualTo(secondRefreshToken)
    }

    private fun createUser(
        email: String,
        rawPassword: String,
        name: String,
        plateNumber: String,
        vehicleType: VehicleType
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
                role = UserRole.USER,
                status = UserStatus.ACTIVE
            )
        )
    }

    private fun hashRefreshToken(refreshToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(refreshToken.toByteArray(Charsets.UTF_8))

        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val LOGIN_FAIL_MESSAGE = "이메일 또는 비밀번호가 올바르지 않습니다."
    }
}
