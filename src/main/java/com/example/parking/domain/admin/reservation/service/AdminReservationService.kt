package com.example.parking.domain.admin.reservation.service

import com.example.parking.domain.admin.reservation.dto.AdminReservationResDto
import com.example.parking.domain.reservation.entity.ReservationStatus
import com.example.parking.domain.reservation.repository.ReservationRepository
import com.example.parking.domain.reservation.service.ReservationService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AdminReservationService(
    private val reservationRepository: ReservationRepository,
    private val reservationService: ReservationService
) {
    private val log = LoggerFactory.getLogger(AdminReservationService::class.java)

    fun getAdminReservations(userId: Long?, pageable: Pageable): Page<AdminReservationResDto> {
        return if (userId != null) {
            reservationRepository.findAllByUserIdWithDetailsPage(userId, pageable)
                .map { AdminReservationResDto.from(it) }
        } else {
            reservationRepository.findAllWithDetailsPage(pageable)
                .map { AdminReservationResDto.from(it) }
        }
    }

    @Transactional
    fun cancelReservationByAdmin(reservationId: Long) {
        val reservation = reservationRepository.findById(reservationId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 예약입니다.") }

        if (reservation.status != ReservationStatus.CONFIRMED) {
            throw IllegalStateException("결제가 완료되지 않은 예약은 관리자가 취소할 수 없습니다.")
        }

        if (LocalDateTime.now().isAfter(reservation.startTime)) {
            throw IllegalStateException("이미 입차 시간이 지난 예약은 관리자가 취소할 수 없습니다.")
        }

        reservationService.cancelReservation(reservationId, null, true)
        log.info("[관리자 강제 취소 성공] 예약 ID: {} 가 취소 및 환불 처리되었습니다.", reservationId)
    }
}