// PaymentAdminRespDto.kt
package com.example.parking.domain.payment.dto

import com.example.parking.domain.payment.entity.Payment
import java.time.LocalDateTime

data class PaymentAdminRespDto(
    val paymentId: Long,
    val reservationId: Long,
    val userId: Long,
    val userName: String,
    val amount: Int,
    val status: String,
    val createdAt: LocalDateTime?
) {
    companion object {
        fun from(payment: Payment) = PaymentAdminRespDto(
            paymentId = payment.id,
            reservationId = payment.reservation.id!!,
            userId = payment.reservation.user.id!!,
            userName = payment.reservation.user.name,
            amount = payment.amount,
            status = payment.status.name,
            createdAt = payment.createdAt
        )
    }
}