package com.example.parking.domain.reservation.scheduler

import com.example.parking.domain.parkingspot.dto.ParkingSpotDto
import com.example.parking.domain.parkingspot.entity.SpotStatus
import com.example.parking.domain.reservation.repository.ReservationRepository
import com.example.parking.global.sse.SseEmitterManager
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDateTime

@Component
@Transactional
class ReservationScheduler(
    private val reservationRepository: ReservationRepository,
    private val sseEmitterManager: SseEmitterManager
) {
    private val log = LoggerFactory.getLogger(ReservationScheduler::class.java)

    @Scheduled(cron = "0 * * * * *")
    fun handleReservationLifecycle() {
        val now = LocalDateTime.now()
        log.info("[스케줄러 실행] 주차 예약 수명 주기 관리 시작: {}", now)

        cleanupSelectionTimeout(now)
        autoCheckIn(now)
        autoCheckOut(now)
    }

    private fun cleanupSelectionTimeout(now: LocalDateTime) {
        val limit = now.minusMinutes(5)
        reservationRepository.findQSelectionTimeout(limit).forEach { res ->
            res.cancel()
            res.parkingSpot.updateStatus(SpotStatus.AVAILABLE)
            val lotId = checkNotNull(res.parkingSpot.parkingLot.id)
            val spotDto = ParkingSpotDto(res.parkingSpot)
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    sseEmitterManager.notify(lotId, spotDto)
                }
            })
            log.info("[선점 만료] 예약 ID: {} 취소 및 자리 반환", res.id)
        }
    }

    private fun autoCheckIn(now: LocalDateTime) {
        reservationRepository.findQToAutoCheckIn(now).forEach { res ->
            res.parkingSpot.updateStatus(SpotStatus.PARKED)
            res.complete()
            val lotId = checkNotNull(res.parkingSpot.parkingLot.id)
            val spotDto = ParkingSpotDto(res.parkingSpot)
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    sseEmitterManager.notify(lotId, spotDto)
                }
            })
            log.info("[자동 입차] 예약 ID: {} 시작 시간 도달 - 자리 ID: {} -> PARKED", res.id, res.parkingSpot.id)
        }
    }

    private fun autoCheckOut(now: LocalDateTime) {
        reservationRepository.findQToAutoCheckOut(now).forEach { res ->
            res.parkingSpot.updateStatus(SpotStatus.AVAILABLE)
            val lotId = checkNotNull(res.parkingSpot.parkingLot.id)
            val spotDto = ParkingSpotDto(res.parkingSpot)
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    sseEmitterManager.notify(lotId, spotDto)
                }
            })
            log.info("[자동 출차] 예약 ID: {} 종료 시간 도달 - 자리 ID: {} -> AVAILABLE", res.id, res.parkingSpot.id)
        }
    }
}