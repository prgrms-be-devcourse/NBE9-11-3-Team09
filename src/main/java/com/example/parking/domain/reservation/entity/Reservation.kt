package com.example.parking.domain.reservation.entity

import com.example.parking.domain.parkingLot.entity.ParkingLot
import com.example.parking.domain.parkingspot.entity.ParkingSpot
import com.example.parking.domain.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "reservations")
@EntityListeners(AuditingEntityListener::class)
class Reservation protected constructor(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_lot_id", nullable = false)
    val parkingLot: ParkingLot,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_spot_id", nullable = false)
    val parkingSpot: ParkingSpot,

    @Column(name = "parking_start_time", nullable = false)
    val startTime: LocalDateTime,

    @Column(name = "parking_end_time", nullable = false)
    val endTime: LocalDateTime,

    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    var id: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ReservationStatus = ReservationStatus.PENDING
        protected set

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    @Column(name = "canceled_at")
    var canceledAt: LocalDateTime? = null
        protected set

    @Column(name = "payment_requested_at")
    var paymentRequestedAt: LocalDateTime? = null
        protected set

    fun startPayment() {
        this.paymentRequestedAt = LocalDateTime.now()
    }

    fun cancel() {
        this.status = ReservationStatus.CANCELED
        this.canceledAt = LocalDateTime.now()
    }

    fun confirm() {
        this.status = ReservationStatus.CONFIRMED
    }

    fun complete() {
        this.status = ReservationStatus.COMPLETED
    }

    fun finish() {
        this.status = ReservationStatus.FINISHED
    }

    fun pending() {
        this.status = ReservationStatus.PENDING
    }

    companion object {
        fun of(
            user: User,
            parkingLot: ParkingLot,
            parkingSpot: ParkingSpot,
            startTime: LocalDateTime,
            endTime: LocalDateTime
        ) = Reservation(
            user = user,
            parkingLot = parkingLot,
            parkingSpot = parkingSpot,
            startTime = startTime,
            endTime = endTime
        )
    }
}