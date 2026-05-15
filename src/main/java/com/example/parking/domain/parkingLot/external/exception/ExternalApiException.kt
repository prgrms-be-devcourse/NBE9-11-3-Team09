package com.example.parking.domain.parkingLot.external.exception

// [CUS-01] 외부 API 전용 예외 클래스
class ExternalApiException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
