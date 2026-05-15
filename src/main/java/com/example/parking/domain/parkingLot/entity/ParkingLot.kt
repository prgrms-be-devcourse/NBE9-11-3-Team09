package com.example.parking.domain.parkingLot.entity

import com.example.parking.domain.parkingspot.entity.ParkingSpot
import jakarta.persistence.*
import java.time.LocalTime

@Entity
@Table(name = "parking_lots")
class ParkingLot protected constructor(

    @Column(nullable = false, unique = true)
    val externalId: String,

    @Column(name = "parking_lot_name", nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 255)
    var address: String,

    @Column(nullable = false)
    var totalSpot: Int,

    @Column(nullable = false)
    var price: Int = DEFAULT_PRICE,

    @Column(name = "operation_start_time", nullable = false)
    var operationStartTime: LocalTime = DEFAULT_OPERATION_START_TIME,

    @Column(name = "operation_end_time", nullable = false)
    var operationEndTime: LocalTime = DEFAULT_OPERATION_END_TIME
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "parking_lot_id")
    var id: Long? = null
        protected set

    @OneToMany(mappedBy = "parkingLot", cascade = [CascadeType.ALL])
    val spots: MutableList<ParkingSpot> = mutableListOf()

    // [CUS-01] 외부 데이터 변경 시 업데이트
    fun updateInfo(
        name: String,
        address: String,
        totalSpot: Int
    ) {
        this.name = name
        this.address = address
        this.totalSpot = totalSpot
    }

    companion object {
        private const val DEFAULT_PRICE = 1000
        private val DEFAULT_OPERATION_START_TIME: LocalTime = LocalTime.MIN
        private val DEFAULT_OPERATION_END_TIME: LocalTime = LocalTime.of(23, 59)

        // [CUS-01] 외부 API 데이터 기반 ParkingLot 생성
        fun of(
            externalId: String,
            name: String,
            address: String,
            totalSpot: Int
        ) = ParkingLot(
            externalId = externalId,
            name = name,
            address = address,
            totalSpot = totalSpot
        )
    }
}