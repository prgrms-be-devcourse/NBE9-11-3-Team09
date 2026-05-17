
package com.example.parking.domain.payment.service

import com.example.parking.domain.parkingspot.dto.ParkingSpotDto
import com.example.parking.domain.parkingspot.repository.ParkingSpotRepository
import com.example.parking.domain.payment.dto.PaymentAdminRespDto
import com.example.parking.domain.payment.dto.PaymentReqDto
import com.example.parking.domain.payment.dto.PaymentRespDto
import com.example.parking.domain.payment.dto.TossConfirmReqDto
import com.example.parking.domain.payment.entity.Payment
import com.example.parking.domain.payment.entity.PaymentStatus
import com.example.parking.domain.payment.infrastructure.TossPaymentClient
import com.example.parking.domain.payment.repository.PaymentRepository
import com.example.parking.domain.reservation.entity.Reservation
import com.example.parking.domain.reservation.entity.ReservationStatus
import com.example.parking.domain.reservation.repository.ReservationRepository
import com.example.parking.domain.reservation.service.ReservationService
import com.example.parking.global.sse.SseEmitterManager
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

@Service
@Transactional
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val reservationRepository: ReservationRepository,
    private val parkingSpotRepository: ParkingSpotRepository,
    private val entityManager: EntityManager,
    private val reservationService: ReservationService,
    private val tossPaymentClient: TossPaymentClient,
    private val sseEmitterManager: SseEmitterManager
) {
    private val log = LoggerFactory.getLogger(PaymentService::class.java)

    fun startPayment(request: PaymentReqDto, userId: Long): PaymentRespDto {
        val reservation = findReservation(request.reservationId)
        validateOwner(reservation, userId)
        validateReservationStatus(reservation)
        validateDuplicatePayment(request.reservationId)
        validateAmount(reservation, request.amount)

        val updatedCount = parkingSpotRepository.startPayment(
            reservation.parkingSpot.id
        )

        if (updatedCount == 0) {
            log.warn("결제 시작 실패 - 주차자리 상태 변경 실패 spotId: {}", reservation.parkingSpot.id)
            throw IllegalStateException("결제를 시작할 수 없는 상태입니다.")
        }

        reservationService.startPaymentProcess(reservation.id!!)

        val payment = Payment(
            reservation = reservation,
            amount = request.amount
        )

        paymentRepository.save(payment)

        val spot = parkingSpotRepository.findById(reservation.parkingSpot.id)
            .orElseThrow()

        sseEmitterManager.notify(spot.parkingLot.id!!, ParkingSpotDto(spot))

        log.info("결제 시작 - reservationId: {}, userId: {}", request.reservationId, userId)
        return PaymentRespDto.from(payment)
    }

    fun approvePayment(paymentId: Long, userId: Long, tossRequest: TossConfirmReqDto): PaymentRespDto {
        val payment = paymentRepository.findById(paymentId)
            .orElseThrow {
                log.warn("결제 승인 실패 - 존재하지 않는 결제 paymentId: {}", paymentId)
                IllegalArgumentException("존재하지 않는 결제입니다.")
            }

        if (payment.reservation.user.id != userId) {
            log.warn("결제 승인 실패 - 본인 결제 아님 userId: {}", userId)
            throw SecurityException("본인의 결제만 승인할 수 있습니다.")
        }

        if (payment.status != PaymentStatus.PROCESSING) {
            log.warn("결제 승인 실패 - 결제 진행 중 상태가 아님 paymentId: {}", paymentId)
            throw IllegalStateException("결제 진행 중인 상태만 승인할 수 있습니다.")
        }

        val tossResponse = tossPaymentClient.confirm(tossRequest, payment.idempotencyKey)
        log.info("토스 결제 승인 완료 - paymentKey: {}, status: {}", tossResponse.paymentKey, tossResponse.status)

        payment.complete()
        reservationService.completePayment(payment.reservation.id!!)
        entityManager.flush()

        val spot = parkingSpotRepository.findById(
            payment.reservation.parkingSpot.id
        ).orElseThrow()

        sseEmitterManager.notify(spot.parkingLot.id!!, ParkingSpotDto(spot))

        log.info("결제 승인 완료 - paymentId: {}", paymentId)
        return PaymentRespDto.from(payment)
    }

    @Transactional(readOnly = true)
    fun getAllPayments(): List<PaymentAdminRespDto> =
        paymentRepository.findAllWithReservationAndUser()
            .map { PaymentAdminRespDto.from(it) }

    @Transactional(readOnly = true)
    fun getPaymentsByUser(userId: Long): List<PaymentAdminRespDto> =
        paymentRepository.findAllByUserIdWithReservationAndUser(userId)
            .map { PaymentAdminRespDto.from(it) }

    fun refundPayment(paymentId: Long): PaymentRespDto {
        val payment = paymentRepository.findById(paymentId)
            .orElseThrow {
                log.warn("환불 실패 - 존재하지 않는 결제 paymentId: {}", paymentId)
                IllegalArgumentException("존재하지 않는 결제입니다.")
            }

        validateRefundStatus(payment)

        payment.refund()
        entityManager.flush()

        val updatedCount = parkingSpotRepository.completePayment(
            payment.reservation.parkingSpot.id
        )

        if (updatedCount == 0) {
            log.warn("환불 실패 - 주차자리 상태 변경 실패 spotId: {}",
                payment.reservation.parkingSpot.id)
            throw IllegalStateException("환불을 처리할 수 없는 상태입니다.")
        }

        val spot = parkingSpotRepository.findById(
            payment.reservation.parkingSpot.id
        ).orElseThrow()

        sseEmitterManager.notify(spot.parkingLot.id!!, ParkingSpotDto(spot))

        log.info("환불 완료 - paymentId: {}", paymentId)
        return PaymentRespDto.from(payment)
    }

    // ==================== private 메서드 ====================

    private fun findReservation(reservationId: Long): Reservation =
        reservationRepository.findById(reservationId)
            .orElseThrow {
                log.warn("결제 실패 - 존재하지 않는 예약 reservationId: {}", reservationId)
                IllegalArgumentException("존재하지 않는 예약입니다.")
            }

    private fun validateOwner(reservation: Reservation, userId: Long) {
        if (reservation.user.id != userId) {
            log.warn("결제 실패 - 본인 예약 아님 userId: {}, reservationId: {}", userId, reservation.id)
            throw SecurityException("본인의 예약만 결제할 수 있습니다.")
        }
    }

    private fun validateReservationStatus(reservation: Reservation) {
        when (reservation.status) {
            ReservationStatus.PENDING -> {}
            ReservationStatus.CONFIRMED -> {
                log.warn("결제 실패 - 이미 결제 진행 중인 예약 reservationId: {}", reservation.id)
                throw IllegalStateException("이미 결제 진행 중인 예약입니다.")
            }
            ReservationStatus.COMPLETED -> {
                log.warn("결제 실패 - 이미 완료된 예약 reservationId: {}", reservation.id)
                throw IllegalStateException("이미 완료된 예약입니다.")
            }
            ReservationStatus.CANCELED -> {
                log.warn("결제 실패 - 취소된 예약 reservationId: {}", reservation.id)
                throw IllegalStateException("취소된 예약은 결제할 수 없습니다.")
            }
        }
    }

    private fun validateDuplicatePayment(reservationId: Long) {
        if (paymentRepository.existsByReservationId(reservationId)) {
            log.warn("결제 실패 - 중복 결제 시도 reservationId: {}", reservationId)
            throw IllegalStateException("이미 결제된 예약입니다.")
        }
    }

    private fun validateAmount(reservation: Reservation, amount: Int) {
        val expectedAmount = calculateExpectedAmount(reservation)
        if (amount != expectedAmount) {
            log.warn("결제 실패 - 금액 불일치 reservationId: {}", reservation.id)
            throw IllegalArgumentException("결제 금액이 올바르지 않습니다. 예상 금액: $expectedAmount")
        }
    }

    private fun validateRefundStatus(payment: Payment) {
        if (payment.status == PaymentStatus.REFUND) {
            log.warn("환불 실패 - 이미 환불된 결제 paymentId: {}", payment.id)
            throw IllegalStateException("이미 환불된 결제입니다.")
        }
        if (payment.status != PaymentStatus.COMPLETE) {
            log.warn("환불 실패 - 환불 불가 상태 paymentId: {}", payment.id)
            throw IllegalStateException("환불 가능한 상태가 아닙니다.")
        }
    }

    private fun calculateExpectedAmount(reservation: Reservation): Int {
        val minutes = ChronoUnit.MINUTES.between(
            reservation.startTime,
            reservation.endTime
        )
        val units = minutes / 10.0
        val price = reservation.parkingLot.price
        return ceil(units * price).toInt()
    }
}