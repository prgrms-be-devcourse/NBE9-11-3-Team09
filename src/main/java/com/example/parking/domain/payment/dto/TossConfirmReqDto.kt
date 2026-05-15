// TossConfirmReqDto.kt
package com.example.parking.domain.payment.dto


data class TossConfirmReqDto(
        val paymentKey: String,  // 토스에서 발급한 결제 키
        val orderId: String,     // 주문 ID
        val amount: Int          // 결제 금액
)