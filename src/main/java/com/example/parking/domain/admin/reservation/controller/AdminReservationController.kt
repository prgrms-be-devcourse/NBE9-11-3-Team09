package com.example.parking.domain.admin.reservation.controller

import com.example.parking.domain.admin.reservation.dto.AdminReservationResDto
import com.example.parking.domain.admin.reservation.service.AdminReservationService
import com.example.parking.global.response.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/reservations")
@Tag(name = "관리자 - 예약", description = "관리자 예약 관련 API")
class AdminReservationController(
    private val adminReservationService: AdminReservationService
) {
    @Operation(summary = "전체 예약 목록 조회", description = "관리자 권한으로 전체 또는 특정 고객의 예약 목록을 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 완료"),
        ApiResponse(responseCode = "500", description = "서버 내부 오류")
    )
    @GetMapping
    fun getAdminReservations(
        @RequestParam(required = false) userId: Long?,
        pageable: Pageable
    ): ResponseEntity<RsData<Page<AdminReservationResDto>>> {
        val data = adminReservationService.getAdminReservations(userId, pageable)
        return ResponseEntity.ok(RsData("전체 예약 목록 조회가 완료되었습니다.", "200-1", data))
    }

    @Operation(summary = "예약 강제 취소", description = "관리자 권한으로 특정 예약을 강제 취소하고 환불 처리합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "강제 취소 완료"),
        ApiResponse(responseCode = "400", description = "존재하지 않는 예약"),
        ApiResponse(responseCode = "409", description = "이미 취소된 예약")
    )
    @PatchMapping("/{reservationId}/cancel")
    fun cancelByAdmin(@PathVariable reservationId: Long): ResponseEntity<RsData<Unit>> {
        adminReservationService.cancelReservationByAdmin(reservationId)
        return ResponseEntity.ok(RsData("관리자 권한으로 예약 취소 및 환불이 완료되었습니다.", "200-5"))
    }
}