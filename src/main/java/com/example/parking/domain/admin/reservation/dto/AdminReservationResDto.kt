package com.example.parking.domain.admin.reservation.dto

import com.example.parking.domain.reservation.entity.Reservation
import com.example.parking.domain.reservation.entity.ReservationStatus
import java.time.LocalDateTime

data class AdminReservationResDto(
    val reservationId: Long?,
    val userId: Long?,
    val userName: String,
    val userEmail: String,
    val parkingSpotId: Long?,
    val parkingLotName: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val status: ReservationStatus,
    val createdTime: LocalDateTime?
) {
    companion object {
        fun from(reservation: Reservation) = AdminReservationResDto(
            reservationId = reservation.id,
            userId = reservation.user.id,
            userName = reservation.user.name,
            userEmail = reservation.user.email,
            parkingSpotId = reservation.parkingSpot.id,
            parkingLotName = reservation.parkingLot.name,
            startTime = reservation.startTime,
            endTime = reservation.endTime,
            status = reservation.status,
            createdTime = reservation.createdAt
        )
    }
}