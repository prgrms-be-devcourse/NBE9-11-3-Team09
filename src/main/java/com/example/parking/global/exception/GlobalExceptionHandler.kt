package com.example.parking.global.exception

import com.example.parking.domain.parkingLot.external.exception.ExternalApiException
import com.example.parking.global.response.RsData
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.async.AsyncRequestNotUsableException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    // [CUS-04, CUS-08, ADM-01] 잘못된 인자 전달 시 처리
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<RsData<Void>> {
        log.warn("IllegalArgumentException: {}", e.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(RsData(e.message ?: "잘못된 요청입니다.", "400-1"))
    }

    // [CUS-03, CUS-05, ADM-01] 로직 수행 불가 상태 처리
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(e: IllegalStateException): ResponseEntity<RsData<Void>> {
        log.warn("IllegalStateException: {}", e.message)
        return ResponseEntity.status(HttpStatus.CONFLICT).body(RsData(e.message ?: "비정상적인 상태입니다.", "409-1"))
    }

    // [CUS-05] 권한 부족 처리
    @ExceptionHandler(SecurityException::class)
    fun handleSecurityException(e: SecurityException): ResponseEntity<RsData<Void>> {
        log.warn("SecurityException: {}", e.message)
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(RsData(e.message ?: "권한이 없습니다.", "403-1"))
    }

    // [CUS-01] 외부 API 호출 실패 처리
    @ExceptionHandler(ExternalApiException::class)
    fun handleExternalApiException(e: ExternalApiException): ResponseEntity<RsData<Void>> {
        log.error("ExternalApiException: {}", e.message)
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(RsData(e.message ?: "외부 API 호출에 실패했습니다.", "502-1"))
    }

    // 그 외 예상치 못한 모든 예외 처리
    @ExceptionHandler(Exception::class)
    fun handleAllException(e: Exception): ResponseEntity<RsData<Void>> {
        log.error("Unhandled Exception: ", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RsData("서버 내부 오류가 발생했습니다.", "500-1"))
    }

    // SSE 클라이언트 정상 연결 해제 - 로그만 남기고 무시
    @ExceptionHandler(AsyncRequestNotUsableException::class)
    fun handleDisconnectedClient(e: AsyncRequestNotUsableException) {
        log.debug("SSE 클라이언트 연결 끊김 (정상): {}", e.message)
    }
}