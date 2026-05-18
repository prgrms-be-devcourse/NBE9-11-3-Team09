package com.example.parking.domain.payment.service

import com.example.parking.domain.parkingLot.entity.ParkingLot
import com.example.parking.domain.parkingspot.entity.ParkingSpot
import com.example.parking.domain.parkingspot.entity.SpotType
import com.example.parking.domain.parkingspot.repository.ParkingSpotRepository
import com.example.parking.domain.payment.dto.PaymentReqDto
import com.example.parking.domain.payment.dto.TossConfirmReqDto
import com.example.parking.domain.payment.dto.TossConfirmResDto
import com.example.parking.domain.payment.entity.Payment
import com.example.parking.domain.payment.entity.PaymentStatus
import com.example.parking.domain.payment.infrastructure.TossPaymentClient
import com.example.parking.domain.payment.repository.PaymentRepository
import com.example.parking.domain.reservation.entity.Reservation
import com.example.parking.domain.reservation.entity.ReservationStatus
import com.example.parking.domain.reservation.repository.ReservationRepository
import com.example.parking.domain.reservation.service.ReservationService
import com.example.parking.domain.user.entity.User
import com.example.parking.domain.user.entity.UserRole
import com.example.parking.domain.user.entity.UserStatus
import com.example.parking.domain.user.entity.VehicleType
import com.example.parking.global.sse.SseEmitterManager
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PaymentServiceTest {

    @InjectMocks
    private lateinit var paymentService: PaymentService

    @Mock private lateinit var paymentRepository: PaymentRepository
    @Mock private lateinit var reservationRepository: ReservationRepository
    @Mock private lateinit var parkingSpotRepository: ParkingSpotRepository
    @Mock private lateinit var entityManager: EntityManager
    @Mock private lateinit var reservationService: ReservationService
    @Mock private lateinit var tossPaymentClient: TossPaymentClient
    @Mock private lateinit var sseEmitterManager: SseEmitterManager

    private lateinit var user: User
    private lateinit var parkingLot: ParkingLot
    private lateinit var parkingSpot: ParkingSpot
    private lateinit var reservation: Reservation
    private lateinit var payment: Payment

    @BeforeEach
    fun setUp() {
        user = User(
            email = "test@test.com",
            password = "1234",
            name = "테스트",
            plateNumber = "12가3456",
            vehicleType = VehicleType.SMALL,
            role = UserRole.USER,
            status = UserStatus.ACTIVE
        )
        ReflectionTestUtils.setField(user, "id", 1L)

        parkingLot = ParkingLot.of(
            externalId = "TEST_001",
            name = "테스트 주차장",
            address = "서울시",
            totalSpot = 10,
            location = null
        )
        ReflectionTestUtils.setField(parkingLot, "id", 1L)

        parkingSpot = ParkingSpot(
            parkingLot = parkingLot,
            number = "A-01",
            type = SpotType.SMALL
        )
        ReflectionTestUtils.setField(parkingSpot, "id", 1L)

        reservation = Reservation.of(
            user = user,
            parkingLot = parkingLot,
            parkingSpot = parkingSpot,
            startTime = LocalDateTime.now().minusHours(1),
            endTime = LocalDateTime.now().plusHours(1)
        )
        ReflectionTestUtils.setField(reservation, "id", 1L)

        payment = Payment(
            reservation = reservation,
            amount = 12000
        )
        ReflectionTestUtils.setField(payment, "id", 1L)
    }

    private fun createRequest(reservationId: Long, amount: Int): PaymentReqDto =
        PaymentReqDto(reservationId = reservationId, amount = amount)

    // ==================== startPayment 테스트 ====================

    @Test
    @DisplayName("결제 시작 - 정상")
    fun startPayment_success() {
        val userId = 1L
        val reservationId = 1L
        val request = createRequest(reservationId, 12000)

        ReflectionTestUtils.setField(user, "id", userId)
        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation))
        given(paymentRepository.existsByReservationId(reservationId)).willReturn(false)
        given(parkingSpotRepository.startPayment(any())).willReturn(1)
        given(parkingSpotRepository.findById(any())).willReturn(Optional.of(parkingSpot))
        given(paymentRepository.save(any<Payment>())).willReturn(payment)

        val result = paymentService.startPayment(request, userId)

        assertThat(result).isNotNull()
        verify(parkingSpotRepository).startPayment(any())
        verify(reservationService).startPaymentProcess(any())
    }

    @Test
    @DisplayName("결제 시작 실패 - 존재하지 않는 예약")
    fun startPayment_reservationNotFound() {
        val userId = 1L
        val request = createRequest(999L, 1000)

        given(reservationRepository.findById(999L)).willReturn(Optional.empty())

        assertThatThrownBy { paymentService.startPayment(request, userId) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("존재하지 않는 예약입니다.")
    }

    @Test
    @DisplayName("결제 시작 실패 - 본인 예약 아님")
    fun startPayment_notOwner() {
        val userId = 99L
        val reservationId = 1L
        val request = createRequest(reservationId, 1000)

        ReflectionTestUtils.setField(user, "id", 1L)
        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation))

        assertThatThrownBy { paymentService.startPayment(request, userId) }
            .isInstanceOf(SecurityException::class.java)
            .hasMessage("본인의 예약만 결제할 수 있습니다.")
    }

    @Test
    @DisplayName("결제 시작 실패 - 이미 결제 진행 중인 예약")
    fun startPayment_alreadyConfirmed() {
        val userId = 1L
        val reservationId = 1L
        val request = createRequest(reservationId, 1000)

        ReflectionTestUtils.setField(user, "id", userId)
        ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED)
        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation))

        assertThatThrownBy { paymentService.startPayment(request, userId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("이미 결제 진행 중인 예약입니다.")
    }

    @Test
    @DisplayName("결제 시작 실패 - 중복 결제")
    fun startPayment_duplicatePayment() {
        val userId = 1L
        val reservationId = 1L
        val request = createRequest(reservationId, 1000)

        ReflectionTestUtils.setField(user, "id", userId)
        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation))
        given(paymentRepository.existsByReservationId(reservationId)).willReturn(true)

        assertThatThrownBy { paymentService.startPayment(request, userId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("이미 결제된 예약입니다.")
    }

    @Test
    @DisplayName("결제 시작 실패 - 금액 불일치")
    fun startPayment_amountMismatch() {
        val userId = 1L
        val reservationId = 1L
        val request = createRequest(reservationId, 9999)

        ReflectionTestUtils.setField(user, "id", userId)
        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation))
        given(paymentRepository.existsByReservationId(reservationId)).willReturn(false)

        assertThatThrownBy { paymentService.startPayment(request, userId) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("결제 금액이 올바르지 않습니다.")
    }

    @Test
    @DisplayName("결제 시작 실패 - 주차자리 상태 변경 실패")
    fun startPayment_spotStatusChangeFailed() {
        val userId = 1L
        val reservationId = 1L
        val request = createRequest(reservationId, 12000)

        ReflectionTestUtils.setField(user, "id", userId)
        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation))
        given(paymentRepository.existsByReservationId(reservationId)).willReturn(false)
        given(parkingSpotRepository.startPayment(any())).willReturn(0)

        assertThatThrownBy { paymentService.startPayment(request, userId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("결제를 시작할 수 없는 상태입니다.")
    }

    // ==================== approvePayment 테스트 ====================

    @Test
    @DisplayName("결제 승인 - 정상")
    fun approvePayment_success() {
        val paymentId = 1L
        val userId = 1L
        val tossRequest = TossConfirmReqDto("paymentKey", "orderId", 1000)
        val tossResponse = TossConfirmResDto(paymentKey = "paymentKey", status = "DONE")

        ReflectionTestUtils.setField(user, "id", userId)
        ReflectionTestUtils.setField(payment, "status", PaymentStatus.PROCESSING)
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment))
        given(tossPaymentClient.confirm(any(), any())).willReturn(tossResponse)
        given(parkingSpotRepository.findById(any())).willReturn(Optional.of(parkingSpot))

        val result = paymentService.approvePayment(paymentId, userId, tossRequest)

        assertThat(result).isNotNull()
        verify(tossPaymentClient).confirm(any(), any())
        verify(reservationService).completePayment(any())
    }

    @Test
    @DisplayName("결제 승인 실패 - 존재하지 않는 결제")
    fun approvePayment_paymentNotFound() {
        val paymentId = 999L
        val userId = 1L
        val tossRequest = TossConfirmReqDto("paymentKey", "orderId", 1000)

        given(paymentRepository.findById(paymentId)).willReturn(Optional.empty())

        assertThatThrownBy { paymentService.approvePayment(paymentId, userId, tossRequest) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("존재하지 않는 결제입니다.")
    }

    @Test
    @DisplayName("결제 승인 실패 - 본인 결제 아님")
    fun approvePayment_notOwner() {
        val paymentId = 1L
        val userId = 99L
        val tossRequest = TossConfirmReqDto("paymentKey", "orderId", 1000)

        ReflectionTestUtils.setField(user, "id", 1L)
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment))

        assertThatThrownBy { paymentService.approvePayment(paymentId, userId, tossRequest) }
            .isInstanceOf(SecurityException::class.java)
            .hasMessage("본인의 결제만 승인할 수 있습니다.")
    }

    @Test
    @DisplayName("결제 승인 실패 - PROCESSING 상태 아님")
    fun approvePayment_notProcessing() {
        val paymentId = 1L
        val userId = 1L
        val tossRequest = TossConfirmReqDto("paymentKey", "orderId", 1000)

        ReflectionTestUtils.setField(user, "id", userId)
        ReflectionTestUtils.setField(payment, "status", PaymentStatus.COMPLETE)
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment))

        assertThatThrownBy { paymentService.approvePayment(paymentId, userId, tossRequest) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("결제 진행 중인 상태만 승인할 수 있습니다.")
    }

    // ==================== refundPayment 테스트 ====================

    @Test
    @DisplayName("환불 - 정상")
    fun refundPayment_success() {
        val paymentId = 1L

        ReflectionTestUtils.setField(payment, "status", PaymentStatus.COMPLETE)
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment))
        given(parkingSpotRepository.completePayment(any())).willReturn(1)
        given(parkingSpotRepository.findById(any())).willReturn(Optional.of(parkingSpot))

        val result = paymentService.refundPayment(paymentId)

        assertThat(result).isNotNull()
        verify(parkingSpotRepository).completePayment(any())
    }

    @Test
    @DisplayName("환불 실패 - 존재하지 않는 결제")
    fun refundPayment_paymentNotFound() {
        val paymentId = 999L

        given(paymentRepository.findById(paymentId)).willReturn(Optional.empty())

        assertThatThrownBy { paymentService.refundPayment(paymentId) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("존재하지 않는 결제입니다.")
    }

    @Test
    @DisplayName("환불 실패 - 이미 환불된 결제")
    fun refundPayment_alreadyRefunded() {
        val paymentId = 1L

        ReflectionTestUtils.setField(payment, "status", PaymentStatus.REFUND)
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment))

        assertThatThrownBy { paymentService.refundPayment(paymentId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("이미 환불된 결제입니다.")
    }

    @Test
    @DisplayName("환불 실패 - COMPLETE 상태 아님")
    fun refundPayment_notComplete() {
        val paymentId = 1L

        ReflectionTestUtils.setField(payment, "status", PaymentStatus.PROCESSING)
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment))

        assertThatThrownBy { paymentService.refundPayment(paymentId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("환불 가능한 상태가 아닙니다.")
    }
}