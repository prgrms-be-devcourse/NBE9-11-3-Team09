package com.example.parking.domain.parkingspot.scheduler

import com.example.parking.domain.parkingspot.dto.ParkingSpotDto
import com.example.parking.domain.parkingspot.entity.SpotStatus
import com.example.parking.domain.parkingspot.repository.ParkingSpotRepository
import com.example.parking.global.sse.SseEmitterManager
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class ParkingSpotScheduler(
    private val parkingSpotRepository: ParkingSpotRepository,
    private val sseEmitterManager: SseEmitterManager
) {

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional
    fun releaseExpiredSpots() {
        val deadline = LocalDateTime.now().minusMinutes(5)

        // [CUS-11] 만료가 될수있는 후보들을 조회. OCCUPIED 인 parkingSpot을 조회함
        parkingSpotRepository
            .findByStatusAndReservedAtBefore(SpotStatus.OCCUPIED, deadline)
            .forEach { spot ->
                // [CUS-11] 원자적 업데이트 시도 (CAS 방식)
                val updatedCount = parkingSpotRepository.releaseExpiredSpot(spot.id, deadline)

                // [CUS-11] 업데이트에 성공했을 때만 알림 발송
                if (updatedCount > 0) {
                    sseEmitterManager.notify(
                        checkNotNull(spot.parkingLot.id),
                        ParkingSpotDto(spot)
                    )
                }
            }
    }
}