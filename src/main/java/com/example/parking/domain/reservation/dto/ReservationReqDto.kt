package com.example.parking.domain.reservation.dto

import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ReservationReqDto(
    @field:NotNull(message = "주차장 ID는 필수입니다.")
    val parkingLotId: Long,

    @field:NotNull(message = "주차 자리 ID는 필수입니다.")
    val parkingSpotId: Long,

    @field:NotNull(message = "시작 시간은 필수입니다.")
    val startTime: String,

    @field:NotNull(message = "종료 시간은 필수입니다.")
    val endTime: String
) {
    companion object {
        private val CUSTOM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    fun getParsedStartTime(): LocalDateTime = parseTime(startTime)
    fun getParsedEndTime(): LocalDateTime = parseTime(endTime)

    private fun parseTime(timeStr: String?): LocalDateTime {
        if (timeStr == null) return LocalDateTime.now()
        var t = timeStr
        if (t.contains(" 24:00")) {
            t = t.replace(" 24:00", " 00:00")
            return LocalDateTime.parse(t, CUSTOM_FORMATTER).plusDays(1)
        }
        return LocalDateTime.parse(t, CUSTOM_FORMATTER)
    }
}