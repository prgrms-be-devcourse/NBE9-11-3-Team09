package com.example.parking.domain.payment.controller

import com.example.parking.domain.payment.dto.PaymentAdminRespDto
import com.example.parking.domain.payment.dto.PaymentRespDto
import com.example.parking.domain.payment.service.PaymentService
import com.example.parking.global.response.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/payments")
@Tag(name = "관리자 - 결제", description = "관리자 결제 관련 API")
class PaymentAdminController(
    private val paymentService: PaymentService
) {
    // [ADM-03] 전체 결제 조회
    @Operation(summary = "전체 결제 조회", description = "관리자 권한으로 시스템 내 전체 결제 내역을 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 완료"),
        ApiResponse(responseCode = "500", description = "서버 내부 오류")
    )
    @GetMapping
    fun getAllPayments(): ResponseEntity<RsData<List<PaymentAdminRespDto>>> {
        val data: List<PaymentAdminRespDto> = paymentService.getAllPayments()
        return ResponseEntity.ok(RsData("전체 결제 내역 조회가 완료되었습니다.", "200-1", data))
    }

    // [ADM-04] 고객별 결제 조회
    @Operation(summary = "고객별 결제 조회", description = "특정 고객의 모든 결제 이력을 관리자 권한으로 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 완료"),
        ApiResponse(responseCode = "500", description = "서버 내부 오류")
    )
    @GetMapping("/{userId}")
    fun getPaymentsByUser(@PathVariable userId: Long): ResponseEntity<RsData<List<PaymentAdminRespDto>>> {
        val data: List<PaymentAdminRespDto> = paymentService.getPaymentsByUser(userId)
        return ResponseEntity.ok(RsData("고객별 결제 내역 조회가 완료되었습니다.", "200-2", data))
    }

    // [ADM-01] 환불 처리
    @Operation(summary = "환불 처리", description = "관리자 권한으로 특정 결제 건에 대한 강제 환불을 처리합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "환불 완료"),
        ApiResponse(responseCode = "400", description = "존재하지 않는 결제"),
        ApiResponse(responseCode = "409", description = "이미 환불됐거나 환불 불가 상태")
    )
    @PatchMapping("/{paymentId}/refund")
    fun refundPayment(@PathVariable paymentId: Long): ResponseEntity<RsData<PaymentRespDto>> {
        val data = paymentService.refundPayment(paymentId)
        return ResponseEntity.ok(RsData("환불 처리가 완료되었습니다.", "200-3", data))
    }
}