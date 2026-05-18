package com.example.parking.domain.parkingLot.external.dto

import com.example.parking.domain.parkingLot.repository.NearbyParkingLotProjection
import java.time.LocalTime
import kotlin.math.roundToInt

data class NearbyParkingLotResDto(
    val id: Long,
    val name: String,
    val address: String,
    val totalSpot: Int,
    val price: Int,
    val operationStartTime: LocalTime,
    val operationEndTime: LocalTime,
    val latitude: Double,
    val longitude: Double,
    val distance: Int
) {
    companion object {
        fun from(p: NearbyParkingLotProjection) = NearbyParkingLotResDto(
            id = p.getId(),
            name = p.getName(),
            address = p.getAddress(),
            totalSpot = p.getTotalSpot(),
            price = p.getPrice(),
            operationStartTime = p.getOperationStartTime(),
            operationEndTime = p.getOperationEndTime(),
            latitude = p.getLatitude(),
            longitude = p.getLongitude(),
            distance = p.getDistance().roundToInt()
        )
    }
}