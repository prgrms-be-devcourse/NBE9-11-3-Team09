// TossConfirmResDto.kt
package com.example.parking.domain.payment.dto




data class TossConfirmResDto(
        val paymentKey: String? = null,    // 토스 결제 키
        val orderId: String? = null,       // 주문 ID
        val orderName: String? = null,     // 주문명
        val totalAmount: Int? = null,      // 결제 금액
        val status: String? = null,        // 결제 상태 (DONE, CANCELED 등)
        val requestedAt: String? = null,   // 결제 요청 시간
        val approvedAt: String? = null,    // 결제 승인 시간
        val method: String? = null         // 결제 수단
)