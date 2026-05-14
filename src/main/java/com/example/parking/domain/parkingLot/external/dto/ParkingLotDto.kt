package com.example.parking.domain.parkingLot.external.dto

import com.example.parking.domain.parkingLot.external.dto.ParkingApiDto.ParkingLotItem

// [CUS-01] 외부 API 데이터를 내부 서비스에서 사용하는 형태로 변환한 객체
data class ParkingLotDto(
    val externalId: String?,
    val name: String?,
    val address: String?,
    val totalSpot: Int?
) {
    companion object {
        fun from(item: ParkingApiDto.ParkingLotItem) = ParkingLotDto(
            externalId = item.pkltCd,
            name = item.pkltNm,
            address = item.addr,
            totalSpot = item.tpkct?.toInt()
        )
    }
}
