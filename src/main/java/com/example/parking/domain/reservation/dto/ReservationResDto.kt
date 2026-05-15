package com.example.parking.domain.reservation.dto

import com.example.parking.domain.reservation.entity.Reservation
import com.example.parking.domain.reservation.entity.ReservationStatus
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class ReservationResDto(
    val reservationId: Long?,
    val parkingLotId: Long?,
    val parkingSpotId: Long?,
    val parkingLotName: String,
    val parkingSpotNumber: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val status: ReservationStatus,
    val totalPrice: Int
) {
    companion object {
        @JvmStatic
        fun from(reservation: Reservation): ReservationResDto {
            val minutes = ChronoUnit.MINUTES.between(reservation.startTime, reservation.endTime)
            val price = (Math.ceil(minutes / 10.0) * reservation.parkingLot.price).toInt()

            return ReservationResDto(
                reservationId = reservation.id,
                parkingLotId = reservation.parkingLot.id,
                parkingSpotId = reservation.parkingSpot.id,
                parkingLotName = reservation.parkingLot.name,
                parkingSpotNumber = reservation.parkingSpot.number,
                startTime = reservation.startTime,
                endTime = reservation.endTime,
                status = reservation.status,
                totalPrice = price
            )
        }
    }
}