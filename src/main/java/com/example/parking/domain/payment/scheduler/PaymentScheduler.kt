// PaymentScheduler.kt
package com.example.parking.domain.payment.scheduler

import com.example.parking.domain.parkingspot.dto.ParkingSpotDto
import com.example.parking.domain.payment.entity.PaymentStatus
import com.example.parking.domain.payment.repository.PaymentRepository
import com.example.parking.global.sse.SseEmitterManager
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class PaymentScheduler(
        private val paymentRepository: PaymentRepository,
        private val sseEmitterManager: SseEmitterManager
) {
    private val log = LoggerFactory.getLogger(PaymentScheduler::class.java)

    @Scheduled(fixedDelay = 60000)
    @Transactional
    fun cancelExpiredPayments() {
        val deadline = LocalDateTime.now().minusMinutes(5)

        val expiredPayments = paymentRepository
                .findByStatusAndCreatedAtBefore(PaymentStatus.PROCESSING, deadline)

        for (payment in expiredPayments) {
            payment.fail()

            val res = payment.reservation
            res.cancel()
            res.parkingSpot.release()

            sseEmitterManager.notify(
                    res.parkingSpot.parkingLot.id,
                    ParkingSpotDto(res.parkingSpot)
            )

            log.info("[2차 결제 타임아웃] 결제 ID: {}, 예약 ID: {} 취소 완료",
                    payment.id, res.id)
        }
    }
}