package com.example.parking.domain.reservation.service

import com.example.parking.domain.parkingLot.entity.ParkingLot
import com.example.parking.domain.parkingLot.repository.ParkingLotRepository
import com.example.parking.domain.parkingspot.dto.ParkingSpotDto
import com.example.parking.domain.parkingspot.entity.ParkingSpot
import com.example.parking.domain.parkingspot.entity.SpotStatus
import com.example.parking.domain.parkingspot.repository.ParkingSpotRepository
import com.example.parking.domain.parkingspot.service.ParkingSpotService
import com.example.parking.domain.payment.repository.PaymentRepository
import com.example.parking.domain.reservation.dto.ReservationReqDto
import com.example.parking.domain.reservation.dto.ReservationResDto
import com.example.parking.domain.reservation.entity.Reservation
import com.example.parking.domain.reservation.entity.ReservationStatus
import com.example.parking.domain.reservation.repository.ReservationRepository
import com.example.parking.domain.user.entity.User
import com.example.parking.domain.user.repository.UserRepository
import com.example.parking.global.sse.SseEmitterManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.time.LocalDateTime
import java.time.Clock

@Service
@Transactional(readOnly = true)
class ReservationService(
    private val userRepository: UserRepository,
    private val parkingLotRepository: ParkingLotRepository,
    private val parkingSpotRepository: ParkingSpotRepository,
    private val parkingSpotService: ParkingSpotService,
    private val reservationRepository: ReservationRepository,
    private val taskScheduler: TaskScheduler,
    private val reservationServiceProvider: ObjectProvider<ReservationService>,
    private val paymentRepository: PaymentRepository,
    private val sseEmitterManager: SseEmitterManager,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    private val log = LoggerFactory.getLogger(ReservationService::class.java)

    // [CUS-04] 내 예약 목록 조회
    fun getMyReservations(userId: Long, status: ReservationStatus?): List<ReservationResDto> =
        reservationRepository.findQAllByUserIdWithDetails(userId, status)
            .map { ReservationResDto.from(it) }

    // [CUS-04] 내 특정 예약 상세 조회
    fun getReservationDetail(reservationId: Long, userId: Long): ReservationResDto {
        val reservation = reservationRepository.findQByIdAndUserId(reservationId, userId)
            ?: throw IllegalArgumentException("존재하지 않거나 권한이 없는 예약입니다.")
        return ReservationResDto.from(reservation)
    }

    // [CUS-04] 예약 취소
    @Transactional
    fun cancelReservation(reservationId: Long, userId: Long?, isForced: Boolean) {
        val reservation = reservationRepository.findQByIdWithParkingSpot(reservationId)
            ?: throw IllegalArgumentException("존재하지 않는 예약입니다.")

        if (reservation.status == ReservationStatus.CANCELED) {
            throw IllegalStateException("이미 취소 처리된 예약입니다.")
        }

        if (!isForced) {
            if (userId == null || reservation.user.id != userId) {
                throw IllegalArgumentException("해당 예약을 취소할 권한이 없습니다.")
            }
            if (LocalDateTime.now(clock).isAfter(reservation.startTime.minusMinutes(30))) {
                throw IllegalStateException("입차 30분 전까지만 취소가 가능합니다.")
            }
        }

        if (reservation.status == ReservationStatus.CONFIRMED) {
            paymentRepository.findByReservationId(reservationId).ifPresent { payment ->
                payment.refund()
                log.info("[환불 처리] 예약 ID: {} - 취소 정책에 따른 환불이 완료되었습니다.", reservationId)
            }
        }

        reservation.cancel()

        val spot = reservation.parkingSpot
        if (spot.status != SpotStatus.AVAILABLE && reservation.startTime.isBefore(LocalDateTime.now(clock))) {
            spot.release()
            val lotId = checkNotNull(spot.parkingLot.id)
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    sseEmitterManager.notify(lotId, ParkingSpotDto(spot))
                }
            })
        }
    }

    // [CUS-03] 예약 생성
    @Transactional
    fun createReservation(userId: Long, reqDto: ReservationReqDto): ReservationResDto {
        val start = reqDto.getParsedStartTime()
        val end = reqDto.getParsedEndTime()

        validateTime(start, end)
        validateReservationOpenTime()

        val user = findUser(userId)
        val parkingLot = findParkingLot(reqDto.parkingLotId)
        val parkingSpot = findAndValidateSpot(reqDto, user)

        val spot = reserveSpot(parkingSpot, reqDto.parkingSpotId)

        validateNoOverlap(spot, start, end)
        validateNoActiveReservation(userId, reqDto.parkingLotId)

        val savedReservation = reservationRepository.save(
            Reservation.of(user, parkingLot, spot, start, end)
        )
        val reservationId = checkNotNull(savedReservation.id)
        val lotId = checkNotNull(spot.parkingLot.id)

        registerAfterCommit(reservationId, lotId, spot)

        return ReservationResDto.from(savedReservation)
    }

// ==================== private 메서드 ====================

    private fun validateTime(start: LocalDateTime, end: LocalDateTime) {
        if (start.isBefore(LocalDateTime.now(clock))) {
            throw IllegalArgumentException("과거 시간으로 예약할 수 없습니다.")
        }
        if (end.isBefore(start)) {
            throw IllegalArgumentException("종료 시간이 시작 시간보다 앞설 수 없습니다.")
        }

        val today = LocalDateTime.now(clock).toLocalDate()
        val startDate = start.toLocalDate()
        val daysFromToday = java.time.temporal.ChronoUnit.DAYS.between(today, startDate)

        // 내일(+1일), +4일~+6일만 허용 / +2일, +3일은 차단
        if (daysFromToday == 2L || daysFromToday == 3L) {
            throw IllegalStateException("해당 날짜는 아직 예약이 오픈되지 않았습니다.")
        }

        // +7일 이상은 차단
        if (daysFromToday > 6L) {
            throw IllegalArgumentException("예약은 최대 6일 후까지만 가능합니다.")
        }
    }

    private fun validateReservationOpenTime() {
//        val hour = LocalDateTime.now(clock).hour
//        if (hour < 22) {
//            throw IllegalStateException("예약은 매일 22시부터 24시까지만 가능합니다.")
//        }
    }

    private fun findUser(userId: Long): User =
        userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 유저입니다.") }

    private fun findParkingLot(parkingLotId: Long): ParkingLot =
        parkingLotRepository.findById(parkingLotId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 주차장입니다.") }

    private fun findAndValidateSpot(reqDto: ReservationReqDto, user: User): ParkingSpot {
        val parkingSpot = parkingSpotRepository.findById(reqDto.parkingSpotId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 주차 자리입니다.") }

        if (parkingSpot.status == SpotStatus.OCCUPIED) {
            throw IllegalStateException("현재 다른 사용자가 결제 진행 중인 자리입니다. 잠시 후 다시 시도해주세요.")
        }

        if (parkingSpot.parkingLot.id != reqDto.parkingLotId) {
            throw IllegalArgumentException(
                "선택하신 주차장(ID: ${reqDto.parkingLotId})에 해당 주차 자리(ID: ${reqDto.parkingSpotId})가 존재하지 않습니다."
            )
        }

        val vehicleType = user.vehicleType
            ?: throw IllegalStateException("차량 정보가 등록되지 않았습니다. 차종을 먼저 등록해주세요.")
        if (parkingSpot.type.name != vehicleType.name) {
            throw IllegalStateException(
                "해당 자리는 ${parkingSpot.type} 전용입니다. 고객님의 차종(${vehicleType})은 이용할 수 없습니다."
            )
        }

        return parkingSpot
    }

    private fun reserveSpot(parkingSpot: ParkingSpot, spotId: Long): ParkingSpot {
        val updated = parkingSpotRepository.tryReserve(
            checkNotNull(parkingSpot.id), LocalDateTime.now(clock)
        )
        if (updated == 0) {
            throw IllegalStateException("방금 다른 사용자가 선점했습니다. 다른 자리를 선택해주세요.")
        }
        return parkingSpotRepository.findById(spotId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 주차 자리입니다.") }
    }

    private fun validateNoOverlap(spot: ParkingSpot, start: LocalDateTime, end: LocalDateTime) {
        val overlapCount = reservationRepository.countQOverlappingReservations(
            checkNotNull(spot.id), start, end
        )
        if (overlapCount > 0) {
            throw IllegalStateException("해당 시간에 이미 예약된 자리입니다.")
        }
    }

    private fun validateNoActiveReservation(userId: Long, parkingLotId: Long) {
        val activeStatuses = listOf(
            ReservationStatus.PENDING,
            ReservationStatus.CONFIRMED,
            ReservationStatus.COMPLETED
        )
        if (reservationRepository.existsQByUserIdAndParkingLotIdAndStatusIn(userId, parkingLotId, activeStatuses)) {
            throw IllegalStateException("이미 이 주차장에 진행 중인 예약이 존재합니다. 1주차장 당 1자리만 이용 가능합니다.")
        }
    }

    private fun registerAfterCommit(reservationId: Long, lotId: Long, spot: ParkingSpot) {
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                sseEmitterManager.notify(lotId, ParkingSpotDto(spot))
                taskScheduler.schedule({
                    val self = reservationServiceProvider.getObject()
                    self.cancelIfUnpaid(reservationId)
                }, Instant.now().plusSeconds(300))
                log.info("[예약 생성] 5분 타이머 작동 시작 - 예약 ID: {}", reservationId)
            }
        })
    }

    @Transactional
    fun cancelIfUnpaid(reservationId: Long) {
        reservationRepository.findQByIdWithParkingSpot(reservationId)?.let { res ->
            if (res.status == ReservationStatus.PENDING && res.paymentRequestedAt == null) {
                res.cancel()
                if (res.parkingSpot.status == SpotStatus.OCCUPIED) {
                    res.parkingSpot.release()
                }
                log.info("[1차 선점 취소] 예약 ID: {} - 결제 미진입으로 인한 만료", reservationId)
            } else {
                log.info("[1차 타이머 종료] 결제 프로세스 확인됨. 예약 ID: {}", reservationId)
            }
        }
    }

    @Transactional
    fun startPaymentProcess(reservationId: Long) {
        val res = reservationRepository.findQByIdWithParkingSpot(reservationId)
            ?: throw IllegalArgumentException("존재하지 않는 예약입니다.")
        res.startPayment()
        res.parkingSpot.updateStatus(SpotStatus.PAYING)
        log.info("[결제 시작] 예약 ID: {}, 자리 상태: PAYING", reservationId)
    }

    @Transactional
    fun completePayment(reservationId: Long) {
        val res = reservationRepository.findQByIdWithParkingSpot(reservationId)
            ?: throw IllegalArgumentException("존재하지 않는 예약입니다.")
        res.confirm()
        if (res.parkingSpot.status == SpotStatus.PAYING) {
            res.parkingSpot.release()
            log.info("[성공] 주차자리 해제 완료: spotId {}", res.parkingSpot.id)
        } else {
            log.info("[스킵] 주차자리가 이미 AVAILABLE 상태입니다: spotId {}", res.parkingSpot.id)
        }
    }
}