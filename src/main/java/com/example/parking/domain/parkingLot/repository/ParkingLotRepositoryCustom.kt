package com.example.parking.domain.parkingLot.repository

import com.example.parking.domain.parkingLot.entity.ParkingLot
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ParkingLotRepositoryCustom {

    fun search(
        keyword: String?,
        pageable: Pageable
    ): Page<ParkingLot>
}