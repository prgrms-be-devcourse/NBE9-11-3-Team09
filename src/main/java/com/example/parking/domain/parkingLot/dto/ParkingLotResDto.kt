package com.example.parking.domain.parkingLot.dto

import com.example.parking.domain.parkingLot.entity.ParkingLot
import java.time.LocalTime

data class ParkingLotResDto(
    val id: Long,
    val name: String,
    val address: String,
    val totalSpot: Int,
    val price: Int,
    val operationStartTime: LocalTime,
    val operationEndTime: LocalTime
) {
    companion object {
        fun from(parkingLot: ParkingLot) = ParkingLotResDto(
            id = checkNotNull(parkingLot.id) {
                "저장되지 않은 ParkingLot은 응답 DTO로 변환할 수 없습니다."
            },
            name = parkingLot.name,
            address = parkingLot.address,
            totalSpot = parkingLot.totalSpot,
            price = parkingLot.price,
            operationStartTime = parkingLot.operationStartTime,
            operationEndTime = parkingLot.operationEndTime
        )
    }
}
