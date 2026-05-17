package com.example.parking.domain.parkingLot.service

import com.example.parking.domain.parkingLot.dto.ParkingLotResDto
import com.example.parking.domain.parkingLot.repository.ParkingLotRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class ParkingLotService(
    private val parkingLotRepository: ParkingLotRepository
) {

    // [CUS-01] 전체 주차장 조회, 동 검색
    @Cacheable(
        value = ["parkingLots"],
        key = "(#keyword == null || #keyword.isBlank() ? 'all' : #keyword) + '-' + #pageable.pageNumber + '-' + #pageable.pageSize"
    )
    fun findAll(keyword: String?, pageable: Pageable): Page<ParkingLotResDto> {
        val parkingLots = if (keyword.isNullOrBlank()) {
            parkingLotRepository.findAll(pageable)
        } else {
            parkingLotRepository.findByNameContainingOrAddressContaining(
                keyword,
                keyword,
                pageable
            )
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
