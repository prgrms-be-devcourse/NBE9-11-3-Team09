// Payment.kt
package com.example.parking.domain.payment.entity

import com.example.parking.domain.reservation.entity.Reservation
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener::class)
class Payment(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_reservation_id", nullable = false, unique = true)
    val reservation: Reservation,

    @Column(name = "payment_amount", nullable = false)
    val amount: Int,

    @Column(name = "payment_idempotency_key", nullable = false, unique = true)
    val idempotencyKey: String = UUID.randomUUID().toString()
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    val id: Long = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    var status: PaymentStatus = PaymentStatus.PROCESSING

    @CreatedDate
    @Column(name = "payment_created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null

    fun complete() { this.status = PaymentStatus.COMPLETE }
    fun refund() { this.status = PaymentStatus.REFUND }
    fun fail() { this.status = PaymentStatus.FAILED }
}