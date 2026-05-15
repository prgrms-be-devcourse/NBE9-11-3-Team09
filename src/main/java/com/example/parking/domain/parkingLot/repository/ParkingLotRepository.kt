package com.example.parking.domain.parkingLot.repository

import com.example.parking.domain.parkingLot.entity.ParkingLot
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ParkingLotRepository : JpaRepository<ParkingLot, Long> {

    // 외부 API 주차장 식별값으로 조회
    fun findByExternalId(externalId: String): Optional<ParkingLot>

    // 주소(동) 기준 주차장 검색
    fun findByAddressContaining(dong: String): List<ParkingLot>
}