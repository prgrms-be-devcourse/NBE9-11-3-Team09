package com.example.parking.domain.parkingLot.service

import com.example.parking.domain.parkingLot.dto.ParkingLotResDto
import com.example.parking.domain.parkingLot.repository.ParkingLotRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class ParkingLotService(
    private val parkingLotRepository: ParkingLotRepository
) {

    // [CUS-01] 전체 주차장 조회, 동 검색
    @Cacheable(value = ["parkingLots"], key = "#dong == null || #dong.isBlank() ? 'all' : #dong")
    fun findAll(dong: String?): List<ParkingLotResDto> {
        val parkingLots = if (dong.isNullOrBlank()) {
            parkingLotRepository.findAll()
        } else {
            parkingLotRepository.findByAddressContaining(dong)
        }

        return parkingLots.map { ParkingLotResDto.from(it) }
    }

    // [CUS-01] 특정 주차장 조회
    @Cacheable(value = ["parkingLot"], key = "#id")
    fun findById(id: Long): ParkingLotResDto {
        val parkingLot = parkingLotRepository.findById(id)
            .orElseThrow { IllegalArgumentException("해당 주차장이 없습니다.") }

        return ParkingLotResDto.from(parkingLot)
    }
}
