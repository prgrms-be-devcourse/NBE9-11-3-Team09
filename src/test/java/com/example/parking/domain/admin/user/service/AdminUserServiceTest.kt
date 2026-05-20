package com.example.parking.domain.admin.user.service

import com.example.parking.domain.user.entity.User
import com.example.parking.domain.user.entity.UserRole
import com.example.parking.domain.user.entity.UserStatus
import com.example.parking.domain.user.entity.VehicleType
import com.example.parking.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class AdminUserServiceTest @Autowired constructor(
    private val adminUserService: AdminUserService,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    @Test
    @DisplayName("회원 목록 조회 시 USER 계정만 반환한다")
    fun getAdminUsers_returnsOnlyUserRoleAccounts() {
        createUser("tdd-user1@test.com", "TDD홍길동", "16가3356", VehicleType.SMALL, UserRole.USER)
        createUser("tdd-user2@test.com", "TDD김철수", "23나4567", VehicleType.LARGE, UserRole.USER)
        createUser("tdd-admin@test.com", "TDD관리자", "99가9999", VehicleType.SMALL, UserRole.ADMIN)

        val result = adminUserService.getAdminUsers("TDD", PageRequest.of(0, 20))

        assertThat(result.content).hasSize(2)
        assertThat(result.content.map { it.userEmail })
            .containsExactlyInAnyOrder("tdd-user1@test.com", "tdd-user2@test.com")
        assertThat(result.content.map { it.role })
            .containsOnly(UserRole.USER)
    }

    @Test
    @DisplayName("검색어가 없으면 USER 계정 목록을 조회한다")
    fun getAdminUsers_returnsUsers_whenKeywordIsBlank() {
        createUser("blank-user1@test.com", "공백검색1", "31가1111", VehicleType.SMALL, UserRole.USER)
        createUser("blank-user2@test.com", "공백검색2", "31가2222", VehicleType.LARGE, UserRole.USER)
        createUser("blank-admin@test.com", "공백관리자", "31가3333", VehicleType.SMALL, UserRole.ADMIN)

        val result = adminUserService.getAdminUsers(null, PageRequest.of(0, 20))

        assertThat(result.content.map { it.userEmail })
            .contains("blank-user1@test.com", "blank-user2@test.com")
        assertThat(result.content.map { it.userEmail })
            .doesNotContain("blank-admin@test.com")
        assertThat(result.content.map { it.role })
            .containsOnly(UserRole.USER)
    }

    @Test
    @DisplayName("이름 키워드로 회원 목록을 검색할 수 있다")
    fun getAdminUsers_filtersByNameKeyword() {
        createUser("hong1@test.com", "홍길동", "34다5678", VehicleType.SMALL, UserRole.USER)
        createUser("hong2@test.com", "홍민수", "45라6789", VehicleType.ELECTRIC, UserRole.USER)
        createUser("kim@test.com", "김영희", "56마7890", VehicleType.LARGE, UserRole.USER)
        createUser("admin-hong@test.com", "홍관리자", "00가0001", VehicleType.SMALL, UserRole.ADMIN)

        val result = adminUserService.getAdminUsers("홍", PageRequest.of(0, 20))

        assertThat(result.content).hasSize(2)
        assertThat(result.content.map { it.userName })
            .containsExactlyInAnyOrder("홍길동", "홍민수")
        assertThat(result.content.map { it.role })
            .containsOnly(UserRole.USER)
    }

    @Test
    @DisplayName("이메일 키워드로 회원 목록을 검색할 수 있다")
    fun getAdminUsers_filtersByEmailKeyword() {
        createUser("alpha.user@test.com", "사용자1", "67바8901", VehicleType.SMALL, UserRole.USER)
        createUser("beta.user@test.com", "사용자2", "78사9012", VehicleType.LARGE, UserRole.USER)
        createUser("gamma@test.com", "사용자3", "89아0123", VehicleType.ELECTRIC, UserRole.USER)

        val result = adminUserService.getAdminUsers("beta", PageRequest.of(0, 20))

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].userEmail).isEqualTo("beta.user@test.com")
        assertThat(result.content[0].role).isEqualTo(UserRole.USER)
    }

    @Test
    @DisplayName("검색어가 일치해도 ADMIN 계정은 회원 목록에서 제외된다")
    fun getAdminUsers_doesNotReturnAdminAccountsEvenWhenKeywordMatches() {
        createUser("member-admin-keyword@test.com", "관리회원", "90자1234", VehicleType.SMALL, UserRole.USER)
        createUser("admin-keyword@test.com", "관리자키워드", "00가1234", VehicleType.SMALL, UserRole.ADMIN)

        val result = adminUserService.getAdminUsers("admin", PageRequest.of(0, 20))

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].userEmail).isEqualTo("member-admin-keyword@test.com")
        assertThat(result.content[0].role).isEqualTo(UserRole.USER)
    }

    @Test
    @DisplayName("회원 목록 조회에 pageable이 적용된다")
    fun getAdminUsers_appliesPaging() {
        for (i in 1..5) {
            createUser(
                email = "tdd-paging$i@test.com",
                name = "TDD회원$i",
                plateNumber = "10가10$i",
                vehicleType = VehicleType.SMALL,
                role = UserRole.USER
            )
        }

        val result = adminUserService.getAdminUsers("TDD", PageRequest.of(0, 2))

        assertThat(result.content).hasSize(2)
        assertThat(result.size).isEqualTo(2)
        assertThat(result.totalElements).isEqualTo(5L)
        assertThat(result.totalPages).isEqualTo(3)
        assertThat(result.content.map { it.userName })
            .allMatch { it.startsWith("TDD") }
    }

    private fun createUser(
        email: String,
        name: String,
        plateNumber: String,
        vehicleType: VehicleType,
        role: UserRole
    ): User {
        return userRepository.save(
            User(
                email = email,
                password = requireNotNull(passwordEncoder.encode("test1234")) {
                    "Password encoding failed."
                },
                name = name,
                plateNumber = plateNumber,
                vehicleType = vehicleType,
                role = role,
                status = UserStatus.ACTIVE
            )
        )
    }
}
