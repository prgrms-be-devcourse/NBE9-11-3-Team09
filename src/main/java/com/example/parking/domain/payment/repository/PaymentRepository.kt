
package com.example.parking.domain.payment.repository

import com.example.parking.domain.payment.entity.Payment
import com.example.parking.domain.payment.entity.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.Optional

interface PaymentRepository : JpaRepository<Payment, Long>, PaymentRepositoryCustom {

    fun existsByReservationId(reservationId: Long): Boolean

    @Query("SELECT p FROM Payment p JOIN FETCH p.reservation r JOIN FETCH r.user u")
    fun findAllWithReservationAndUser(): List<Payment>

    @Query("SELECT p FROM Payment p JOIN FETCH p.reservation r JOIN FETCH r.user u WHERE r.user.id = :userId")
    fun findAllByUserIdWithReservationAndUser(@Param("userId") userId: Long): List<Payment>

    fun findByStatusAndCreatedAtBefore(status: PaymentStatus, dateTime: LocalDateTime): List<Payment>

    fun findByReservationParkingSpotIdAndStatus(parkingSpotId: Long, status: PaymentStatus): Optional<Payment>

    fun findByReservationId(reservationId: Long): Optional<Payment>
}