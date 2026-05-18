package com.example.parking.domain.user.controller

import com.example.parking.domain.user.dto.*
import com.example.parking.domain.user.service.AuthService
import com.example.parking.domain.user.service.UserService
import com.example.parking.global.response.RsData
import com.example.parking.global.security.CustomUserDetails
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "회원", description = "회원 관련 API")
class UserController(
    private val userService: UserService,
    private val authService: AuthService
) {
    @Operation(summary = "이메일 중복 확인", description = "회원가입 전 이메일 중복 여부를 확인합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "확인 완료"),
            ApiResponse(responseCode = "400", description = "이메일 형식 오류")
        ]
    )
    @GetMapping("/api/users/check-email")
    fun checkEmail(@RequestParam email: String?): ResponseEntity<EmailCheckResDto> {
        val response = userService.checkEmail(email)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "회원가입",
        description = "이메일, 비밀번호, 이름, 차량번호, 차종을 입력하여 회원가입합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "회원가입 완료"),
            ApiResponse(responseCode = "400", description = "존재하지 않는 정보"),
            ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일 또는 차량번호")
        ]
    )
    @PostMapping("/api/users/signup")
    fun signup(
        @Valid @RequestBody reqDto: SignupReqDto
    ): ResponseEntity<RsData<UserProfileResDto>> {
        val data = userService.signup(reqDto)
        val rsData = RsData("회원가입이 완료되었습니다.", "200-1", data)
        return ResponseEntity.ok(rsData)
    }

    @Operation(
        summary = "로그인",
        description = "이메일과 비밀번호로 로그인하고 JWT 토큰을 발급받습니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "로그인 완료"),
            ApiResponse(responseCode = "400", description = "존재하지 않는 이메일 또는 비밀번호 불일치"),
            ApiResponse(responseCode = "409", description = "탈퇴한 사용자")
        ]
    )
    @PostMapping("/api/users/login")
    fun login(
        @Valid @RequestBody reqDto: LoginReqDto
    ): ResponseEntity<RsData<LoginResDto>> {
        val data = authService.login(reqDto)
        val rsData = RsData("로그인이 완료되었습니다.", "200-2", data)
        return ResponseEntity.ok(rsData)
    }

    @Operation(
        summary = "토큰 재발급",
        description = "Refresh Token으로 새로운 Access Token을 발급받습니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "토큰 재발급 완료"),
            ApiResponse(responseCode = "400", description = "유효하지 않거나 저장되지 않은 Refresh Token")
        ]
    )
    @PostMapping("/api/users/refresh")
    fun refresh(
        @Valid @RequestBody reqDto: RefreshTokenReqDto
    ): ResponseEntity<RsData<LoginResDto>> {
        val data = authService.refresh(reqDto)
        val rsData = RsData("토큰 재발급이 완료되었습니다.", "200-3", data)
        return ResponseEntity.ok(rsData)
    }

    @Operation(
        summary = "내 정보 조회",
        description = "JWT로 인증된 현재 사용자의 프로필을 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 완료"),
            ApiResponse(responseCode = "400", description = "존재하지 않는 사용자"),
            ApiResponse(responseCode = "401", description = "인증 실패")
        ]
    )
    @GetMapping("/api/users/me")
    fun getMyProfile(
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<RsData<UserProfileResDto>> {
        val data = userService.getMyProfile(userDetails.userId)
        val rsData = RsData("내 정보 조회가 완료되었습니다.", "200-4", data)
        return ResponseEntity.ok(rsData)
    }

    @Operation(
        summary = "차량 정보 수정",
        description = "JWT로 인증된 현재 사용자의 차량번호와 차종을 수정합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 완료"),
            ApiResponse(responseCode = "400", description = "존재하지 않는 사용자"),
            ApiResponse(responseCode = "409", description = "이미 등록된 차량번호")
        ]
    )
    @PatchMapping("/api/users/me/vehicle")
    fun updateMyVehicle(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody reqDto: VehicleUpdateReqDto
    ): ResponseEntity<RsData<UserProfileResDto>> {
        val data = userService.updateMyVehicle(userDetails.userId, reqDto)
        val rsData = RsData("차량 정보 수정이 완료되었습니다.", "200-5", data)
        return ResponseEntity.ok(rsData)
    }

    @Operation(
        summary = "회원 탈퇴",
        description = "비밀번호를 확인한 뒤 계정을 탈퇴 처리합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "탈퇴 완료"),
            ApiResponse(responseCode = "400", description = "비밀번호 불일치"),
            ApiResponse(responseCode = "409", description = "이미 탈퇴한 사용자")
        ]
    )
    @DeleteMapping("/api/users/me")
    fun withdraw(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody reqDto: WithdrawReqDto
    ): ResponseEntity<RsData<Void>> {
        userService.withdraw(userDetails.userId, reqDto)
        val rsData = RsData<Void>("회원 탈퇴가 완료되었습니다.", "200-6")
        return ResponseEntity.ok(rsData)
    }

    @Operation(
        summary = "로그아웃",
        description = "Refresh Token을 제거하여 로그아웃 처리합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "로그아웃 완료"),
            ApiResponse(responseCode = "400", description = "존재하지 않는 사용자"),
            ApiResponse(responseCode = "409", description = "탈퇴한 사용자")
        ]
    )
    @PostMapping("/api/users/logout")
    fun logout(
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<RsData<Void>> {
        authService.logout(userDetails.userId)
        val rsData = RsData<Void>("로그아웃이 완료되었습니다.", "200-7")
        return ResponseEntity.ok(rsData)
    }
}