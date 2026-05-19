package com.example.parking.domain.reservation.entity

enum class ReservationStatus {
    PENDING,    // 결제 전 예약 생성
    CONFIRMED,  // 예약 확정
    COMPLETED,  // 주차 중
    FINISHED,   // 이용 완료 (자동 출차)
    CANCELED    // 예약 취소 및 환불
}