package com.example.parking.domain.payment.dto

import com.example.parking.domain.payment.entity.Payment
import java.util.UUID

data class PaymentRespDto(
    val paymentId: Long,
    val status: String,
    val receiptUuid: String
) {
    companion object {
        fun from(payment: Payment) = PaymentRespDto(
            paymentId = payment.id,
            status = payment.status.name,
            receiptUuid = UUID.randomUUID().toString()
        )
    }
}