package com.example.parking.domain.reservation.repository

import com.example.parking.domain.reservation.entity.Reservation
import com.example.parking.domain.reservation.entity.ReservationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface ReservationRepositoryCustom {

    fun findQByIdAndUserId(reservationId: Long, userId: Long): Reservation?

    fun findQByIdWithParkingSpot(id: Long): Reservation?

    fun findQAllByUserIdWithDetails(userId: Long, status: ReservationStatus?): List<Reservation>

    fun findQAllByUserIdWithDetailsPage(userId: Long, pageable: Pageable): Page<Reservation>

    fun findQAllWithDetailsPage(pageable: Pageable): Page<Reservation>

    fun countQOverlappingReservations(spotId: Long, startTime: LocalDateTime, endTime: LocalDateTime): Long

    fun countQOverlapping(spotId: Long, start: LocalDateTime, end: LocalDateTime): Long

    fun findQToAutoCheckIn(now: LocalDateTime): List<Reservation>

    fun findQToAutoCheckOut(now: LocalDateTime): List<Reservation>

    fun findQSelectionTimeout(limit: LocalDateTime): List<Reservation>

    fun existsQByUserIdAndDateAndStatusIn(
        userId: Long,
        date: java.time.LocalDate,
        statuses: List<ReservationStatus>
    ): Boolean
}