package com.example.parking.domain.parkingspot.service

import com.example.parking.domain.parkingspot.entity.ParkingSpot
import com.example.parking.domain.parkingLot.entity.ParkingLot
import com.example.parking.domain.parkingspot.dto.ParkingSpotDto
import com.example.parking.domain.parkingspot.entity.SpotStatus
import com.example.parking.domain.parkingspot.entity.SpotType
import com.example.parking.domain.parkingspot.repository.ParkingSpotRepository
import com.example.parking.global.sse.SseEmitterManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Service
class ParkingSpotService(
    private val parkingSpotRepository: ParkingSpotRepository,
    private val sseEmitterManager: SseEmitterManager
) {

    // [CUS-11] 이용가능한 자리 목록 반환
    fun findAvailableSpots(parkingLotId: Long): List<ParkingSpotDto> =
        parkingSpotRepository
            .findByParkingLotIdAndStatus(parkingLotId, SpotStatus.AVAILABLE)
            .map { ParkingSpotDto(it) }

    // [CUS-11] 모든 자리 반환
    fun findAllSpots(parkingLotId: Long): List<ParkingSpotDto> =
        parkingSpotRepository.findByParkingLotId(parkingLotId)
            .map { ParkingSpotDto(it) }

    // [CUS-11] 주차자리에 맞는 자리 생성
    @Transactional
    fun createSpots(parkingLot: ParkingLot, totalSpot: Int) {
        val smallCount    = (totalSpot * 0.8).toInt()  // 80%
        val largeCount    = (totalSpot * 0.1).toInt()  // 10%

        val spots = listOf(
            (1..smallCount).map { ParkingSpot(parkingLot, it.toString(), SpotType.SMALL) },
            (smallCount + 1..smallCount + largeCount).map { ParkingSpot(parkingLot, it.toString(), SpotType.LARGE) },
            (smallCount + largeCount + 1..totalSpot).map { ParkingSpot(parkingLot, it.toString(), SpotType.ELECTRIC) }
        ).flatten()

        parkingSpotRepository.saveAll(spots)
    }

    // [CUS-11] 구독. 주차장내 모든 인원에게 전파
    fun subscribe(parkingLotId: Long): SseEmitter =
        sseEmitterManager.subscribe(parkingLotId)

    // [ADM-05] 관리자의 주차자리 상태변경
    @Transactional
    fun updateSpotStatusByAdmin(spotId: Long, status: SpotStatus) {
        val spot = parkingSpotRepository.findByIdOrNull(spotId)
            ?: throw IllegalArgumentException("존재하지 않는 주차 자리입니다.")

        spot.updateStatus(status)

        sseEmitterManager.notify(
            checkNotNull(spot.parkingLot.id),
            ParkingSpotDto(spot)
        )
    }
}