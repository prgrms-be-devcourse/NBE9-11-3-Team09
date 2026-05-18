package com.example.parking.domain.parkingLot.external.dto

import com.example.parking.domain.parkingLot.entity.ParkingLot
import java.time.LocalTime

data class ParkingLotResDto(
    val id: Long,
    val name: String,
    val address: String,
    val totalSpot: Int,
    val price: Int,
    val operationStartTime: LocalTime,
    val operationEndTime: LocalTime,
    val latitude: Double?,
    val longitude: Double?
) {
    companion object {
        fun from(parkingLot: ParkingLot) = ParkingLotResDto(
            id = checkNotNull(parkingLot.id) { "..." },
            name = parkingLot.name,
            address = parkingLot.address,
            totalSpot = parkingLot.totalSpot,
            price = parkingLot.price,
            operationStartTime = parkingLot.operationStartTime,
            operationEndTime = parkingLot.operationEndTime,
            latitude = parkingLot.location?.y,
            longitude = parkingLot.location?.x
        )
    }
}