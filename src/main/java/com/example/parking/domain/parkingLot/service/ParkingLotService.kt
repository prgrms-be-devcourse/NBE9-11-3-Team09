package com.example.parking.domain.parkingLot.service

import com.example.parking.domain.parkingLot.external.dto.NearbyParkingLotResDto
import com.example.parking.domain.parkingLot.external.dto.ParkingLotResDto
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
    // [CUS-01] 주차장 반경 검색
    fun findNearby(lat: Double, lng: Double, radius: Int): List<NearbyParkingLotResDto> {
        validateCoordinate(lat, lng, radius)

        return parkingLotRepository.findNearby(lat, lng, radius)
            .map { NearbyParkingLotResDto.from(it) }
    }

    private fun validateCoordinate(lat: Double, lng: Double, radius: Int) {
        require(lat in -90.0..90.0) { "위도는 -90 ~ 90 사이여야 합니다." }
        require(lng in -180.0..180.0) { "경도는 -180 ~ 180 사이여야 합니다." }
        require(radius in 1..10_000) { "반경은 1m ~ 10km 사이여야 합니다." }
    }
}
