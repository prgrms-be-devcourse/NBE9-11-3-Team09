package com.example.parking.domain.reservation.repository

import com.example.parking.domain.reservation.entity.Reservation
import com.example.parking.domain.reservation.entity.ReservationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ReservationRepository : JpaRepository<Reservation, Long> {

    @Query("SELECT r FROM Reservation r JOIN FETCH r.parkingLot JOIN FETCH r.parkingSpot " +
            "WHERE r.user.id = :userId " +
            "AND (:status IS NULL OR r.status = :status)")
    fun findAllByUserIdWithDetails(
        @Param("userId") userId: Long,
        @Param("status") status: ReservationStatus?
    ): List<Reservation>

    @Query("SELECT r FROM Reservation r JOIN FETCH r.parkingLot JOIN FETCH r.parkingSpot " +
            "WHERE r.id = :reservationId AND r.user.id = :userId")
    fun findByIdAndUserIdWithDetails(
        @Param("reservationId") reservationId: Long,
        @Param("userId") userId: Long
    ): Reservation?

    @Query("SELECT COUNT(r) FROM Reservation r " +
            "WHERE r.parkingSpot.id = :spotId " +
            "AND r.status != 'CANCELED' " +
            "AND r.startTime < :endTime AND r.endTime > :startTime")
    fun countOverlappingReservations(
        @Param("spotId") spotId: Long,
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime
    ): Long

    @Query("SELECT r FROM Reservation r JOIN FETCH r.parkingSpot WHERE r.id = :id")
    fun findByIdWithParkingSpot(@Param("id") id: Long): Reservation?

    @Query(value = "SELECT r FROM Reservation r JOIN FETCH r.user JOIN FETCH r.parkingLot JOIN FETCH r.parkingSpot WHERE r.user.id = :userId",
        countQuery = "SELECT count(r) FROM Reservation r WHERE r.user.id = :userId")
    fun findAllByUserIdWithDetailsPage(@Param("userId") userId: Long, pageable: Pageable): Page<Reservation>

    @Query(value = "SELECT r FROM Reservation r JOIN FETCH r.user JOIN FETCH r.parkingLot JOIN FETCH r.parkingSpot",
        countQuery = "SELECT count(r) FROM Reservation r")
    fun findAllWithDetailsPage(pageable: Pageable): Page<Reservation>

    @Query("SELECT r FROM Reservation r JOIN FETCH r.parkingSpot " +
            "WHERE r.status = 'CONFIRMED' AND r.startTime <= :now")
    fun findToAutoCheckIn(@Param("now") now: LocalDateTime): List<Reservation>

    @Query("SELECT r FROM Reservation r JOIN FETCH r.parkingSpot " +
            "WHERE r.status = 'COMPLETED' AND r.endTime <= :now " +
            "AND r.parkingSpot.status = 'PARKED'")
    fun findToAutoCheckOut(@Param("now") now: LocalDateTime): List<Reservation>

    @Query("SELECT r FROM Reservation r JOIN FETCH r.parkingSpot " +
            "WHERE r.status = 'PENDING' AND r.paymentRequestedAt IS NULL AND r.createdAt < :limit")
    fun findSelectionTimeout(@Param("limit") limit: LocalDateTime): List<Reservation>

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.parkingSpot.id = :spotId " +
            "AND r.status IN ('PENDING', 'CONFIRMED') " +
            "AND (:start < r.endTime AND :end > r.startTime)")
    fun countOverlapping(
        @Param("spotId") spotId: Long,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): Long

    fun existsByUserIdAndParkingLotIdAndStatusIn(
        userId: Long,
        parkingLotId: Long,
        statuses: List<ReservationStatus>
    ): Boolean
}