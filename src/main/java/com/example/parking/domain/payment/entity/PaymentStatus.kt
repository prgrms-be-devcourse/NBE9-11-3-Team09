package com.example.parking.domain.payment.entity

enum class PaymentStatus {
    PROCESSING,  // 결제 진행 중
    COMPLETE,    // 결제 완료
    FAILED,      // 결제 실패
    REFUND       // 환불
}