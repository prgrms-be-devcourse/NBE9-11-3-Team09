package com.example.parking.domain.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener::class)
class User(
    @Column(name = "user_email", nullable = false, unique = true, length = 100)
    var email: String,

    @Column(name = "user_password", nullable = false)
    var password: String,

    @Column(name = "user_name", nullable = false, length = 50)
    var name: String,

    @Column(name = "plate_number", nullable = false, unique = true, length = 20)
    var plateNumber: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    var vehicleType: VehicleType,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: UserRole = UserRole.USER,

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    var status: UserStatus = UserStatus.ACTIVE
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    var id: Long? = null
        protected set

    @CreatedDate
    @Column(name = "created_time", nullable = false, updatable = false)
    var createdTime: LocalDateTime? = null
        protected set

    fun updateVehicleInfo(plateNumber: String, vehicleType: VehicleType) {
        this.plateNumber = plateNumber
        this.vehicleType = vehicleType
    }

    fun withdraw() {
        this.status = UserStatus.WITHDRAW
    }
}
