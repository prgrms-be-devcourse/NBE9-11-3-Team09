package com.example.parking.domain.reservation.service

import com.example.parking.domain.parkingLot.entity.ParkingLot
import com.example.parking.domain.parkingLot.repository.ParkingLotRepository
import com.example.parking.domain.parkingspot.entity.ParkingSpot
import com.example.parking.domain.parkingspot.entity.SpotType
import com.example.parking.domain.parkingspot.repository.ParkingSpotRepository
import com.example.parking.domain.reservation.controller.ReservationController
import com.example.parking.domain.reservation.entity.Reservation
import com.example.parking.domain.reservation.entity.ReservationStatus
import com.example.parking.domain.reservation.repository.ReservationRepository
import com.example.parking.domain.user.entity.User
import com.example.parking.domain.user.entity.UserRole
import com.example.parking.domain.user.entity.UserStatus
import com.example.parking.domain.user.entity.VehicleType
import com.example.parking.domain.user.repository.UserRepository
import com.example.parking.global.security.JwtUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(ReservationServiceTest.AllowedTimeClockConfig::class)
class ReservationServiceTest @Autowired constructor(
    private val mvc: MockMvc,
    private val userRepository: UserRepository,
    private val parkingLotRepository: ParkingLotRepository,
    private val parkingSpotRepository: ParkingSpotRepository,
    private val reservationRepository: ReservationRepository,
    private val jwtUtil: JwtUtil,
    private val clock: Clock
) {
    private lateinit var savedUser: User
    private lateinit var savedLot: ParkingLot
    private lateinit var savedSpot: ParkingSpot
    private lateinit var savedReservation: Reservation
    private lateinit var token: String

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @BeforeEach
    fun setUp() {
        savedUser = userRepository.save(
            User(
                email = "test@test.com",
                password = "1234",
                name = "테스트유저",
                plateNumber = "12가3453",
                vehicleType = VehicleType.SMALL,
                role = UserRole.USER,
                status = UserStatus.ACTIVE
            )
        )
        token = jwtUtil.createAccessToken(savedUser)

        savedLot = parkingLotRepository.save(
            ParkingLot.of(
                externalId = "TEST_001",
                name = "강남역 공영 주차장",
                address = "서울시 강남구",
                totalSpot = 100
            )
        )

        savedSpot = parkingSpotRepository.save(
            ParkingSpot(parkingLot = savedLot, number = "A-01", type = SpotType.SMALL)
        )

        savedReservation = reservationRepository.save(
            Reservation.of(
                user = savedUser,
                parkingLot = savedLot,
                parkingSpot = savedSpot,
                startTime = LocalDateTime.now(clock).plusHours(2),
                endTime = LocalDateTime.now(clock).plusHours(4)
            )
        )
    }

    @Test
    @DisplayName("[CUS-04] 내 예약 목록 조회 - 정상적으로 조회되어야 한다")
    fun getMyReservations_success() {
        mvc.perform(
            get("/api/reservations")
                .header("Authorization", "Bearer $token")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(ReservationController::class.java))
            .andExpect(handler().methodName("getList"))
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].parkingLotName").value("강남역 공영 주차장"))
            .andExpect(jsonPath("$.data[0].parkingSpotNumber").value("A-01"))
            .andExpect(jsonPath("$.data[0].status").value("PENDING"))
    }

    @Test
    @DisplayName("[CUS-04] 내 예약 목록 조회 - 상태 필터링이 동작해야 한다")
    fun getMyReservations_filterByStatus() {
        mvc.perform(
            get("/api/reservations")
                .header("Authorization", "Bearer $token")
                .param("status", "CONFIRMED")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    @Test
    @DisplayName("[CUS-04] 예약 상세 조회 - 정상적으로 조회되어야 한다")
    fun getReservationDetail_success() {
        mvc.perform(
            get("/api/reservations/${savedReservation.id}")
                .header("Authorization", "Bearer $token")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(ReservationController::class.java))
            .andExpect(handler().methodName("getDetail"))
            .andExpect(jsonPath("$.resultCode").value("200-2"))
            .andExpect(jsonPath("$.data.parkingLotName").value("강남역 공영 주차장"))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
    }

    @Test
    @DisplayName("[CUS-04] 예약 상세 조회 - 다른 유저의 예약 조회 시 실패해야 한다")
    fun getReservationDetail_otherUser_fail() {
        val otherUser = userRepository.save(
            User(
                email = "other@test.com",
                password = "1234",
                name = "다른유저",
                plateNumber = "99가9999",
                vehicleType = VehicleType.SMALL,
                role = UserRole.USER,
                status = UserStatus.ACTIVE
            )
        )
        val otherToken = jwtUtil.createAccessToken(otherUser)

        mvc.perform(
            get("/api/reservations/${savedReservation.id}")
                .header("Authorization", "Bearer $otherToken")
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("[CUS-04] 예약 취소 - 정상적으로 취소되어야 한다")
    fun cancelReservation_success() {
        mvc.perform(
            patch("/api/reservations/${savedReservation.id}/cancel")
                .header("Authorization", "Bearer $token")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(ReservationController::class.java))
            .andExpect(handler().methodName("cancel"))
            .andExpect(jsonPath("$.resultCode").value("200-3"))

        val canceled = reservationRepository.findById(savedReservation.id!!).get()
        assertThat(canceled.status).isEqualTo(ReservationStatus.CANCELED)
    }

    @Test
    @DisplayName("[CUS-04] 예약 취소 - 이미 취소된 예약은 실패해야 한다")
    fun cancelReservation_alreadyCanceled_fail() {
        mvc.perform(
            patch("/api/reservations/${savedReservation.id}/cancel")
                .header("Authorization", "Bearer $token")
        )
        mvc.perform(
            patch("/api/reservations/${savedReservation.id}/cancel")
                .header("Authorization", "Bearer $token")
        )
            .andDo(print())
            .andExpect(status().isConflict)
    }

    @Test
    @DisplayName("[CUS-04] 예약 취소 - 다른 유저의 예약 취소 시 실패해야 한다")
    fun cancelReservation_otherUser_fail() {
        val otherUser = userRepository.save(
            User(
                email = "other@test.com",
                password = "1234",
                name = "다른유저",
                plateNumber = "99가9999",
                vehicleType = VehicleType.SMALL,
                role = UserRole.USER,
                status = UserStatus.ACTIVE
            )
        )
        val otherToken = jwtUtil.createAccessToken(otherUser)

        mvc.perform(
            patch("/api/reservations/${savedReservation.id}/cancel")
                .header("Authorization", "Bearer $otherToken")
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("[CUS-04] 예약 취소 - 입차 30분 전 이후에는 취소할 수 없다")
    fun cancelReservation_after30Minutes_fail() {
        val newSpot = parkingSpotRepository.save(
            ParkingSpot(parkingLot = savedLot, number = "A-04", type = SpotType.SMALL)
        )
        val soonReservation = reservationRepository.save(
            Reservation.of(
                user = savedUser,
                parkingLot = savedLot,
                parkingSpot = newSpot,
                startTime = LocalDateTime.now(clock).plusMinutes(10),
                endTime = LocalDateTime.now(clock).plusHours(2)
            )
        )

        mvc.perform(
            patch("/api/reservations/${soonReservation.id}/cancel")
                .header("Authorization", "Bearer $token")
        )
            .andDo(print())
            .andExpect(status().isConflict)
    }

    @Test
    @DisplayName("[CUS-03] 예약 생성 - 정상적으로 생성되어야 한다")
    fun createReservation_success() {
        val newUser = userRepository.save(
            User(
                email = "new@test.com",
                password = "1234",
                name = "새유저",
                plateNumber = "34나5678",
                vehicleType = VehicleType.SMALL,
                role = UserRole.USER,
                status = UserStatus.ACTIVE
            )
        )
        val newToken = jwtUtil.createAccessToken(newUser)
        val newSpot = parkingSpotRepository.save(
            ParkingSpot(parkingLot = savedLot, number = "A-02", type = SpotType.SMALL)
        )

        mvc.perform(
            post("/api/reservations")
                .header("Authorization", "Bearer $newToken")
                .content("""
                    {
                        "parkingLotId": ${savedLot.id},
                        "parkingSpotId": ${newSpot.id},
                        "startTime": "${LocalDateTime.now(clock).plusHours(5).format(formatter)}",
                        "endTime": "${LocalDateTime.now(clock).plusHours(7).format(formatter)}"
                    }
                """.trimIndent())
                .contentType(MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(ReservationController::class.java))
            .andExpect(handler().methodName("create"))
            .andExpect(jsonPath("$.resultCode").value("201-1"))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
    }

    @Test
    @DisplayName("[CUS-03] 예약 생성 - 과거 시간으로 예약 시 실패해야 한다")
    fun createReservation_pastTime_fail() {
        mvc.perform(
            post("/api/reservations")
                .header("Authorization", "Bearer $token")
                .content("""
                    {
                        "parkingLotId": ${savedLot.id},
                        "parkingSpotId": ${savedSpot.id},
                        "startTime": "${LocalDateTime.now(clock).minusHours(2).format(formatter)}",
                        "endTime": "${LocalDateTime.now(clock).minusHours(1).format(formatter)}"
                    }
                """.trimIndent())
                .contentType(MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("[CUS-03] 예약 생성 - 종료 시간이 시작 시간보다 앞서면 실패해야 한다")
    fun createReservation_endBeforeStart_fail() {
        mvc.perform(
            post("/api/reservations")
                .header("Authorization", "Bearer $token")
                .content("""
                    {
                        "parkingLotId": ${savedLot.id},
                        "parkingSpotId": ${savedSpot.id},
                        "startTime": "${LocalDateTime.now(clock).plusHours(5).format(formatter)}",
                        "endTime": "${LocalDateTime.now(clock).plusHours(3).format(formatter)}"
                    }
                """.trimIndent())
                .contentType(MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("[CUS-03] 예약 생성 - 차종 불일치 시 실패해야 한다")
    fun createReservation_vehicleTypeMismatch_fail() {
        val largeSpot = parkingSpotRepository.save(
            ParkingSpot(parkingLot = savedLot, number = "B-01", type = SpotType.LARGE)
        )

        mvc.perform(
            post("/api/reservations")
                .header("Authorization", "Bearer $token")
                .content("""
                    {
                        "parkingLotId": ${savedLot.id},
                        "parkingSpotId": ${largeSpot.id},
                        "startTime": "${LocalDateTime.now(clock).plusHours(5).format(formatter)}",
                        "endTime": "${LocalDateTime.now(clock).plusHours(7).format(formatter)}"
                    }
                """.trimIndent())
                .contentType(MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
        )
            .andDo(print())
            .andExpect(status().isConflict)
    }

    @Test
    @DisplayName("[CUS-03] 예약 생성 - 같은 주차장에 이미 진행 중인 예약이 있으면 실패해야 한다")
    fun createReservation_sameUserSameLot_fail() {
        val newSpot = parkingSpotRepository.save(
            ParkingSpot(parkingLot = savedLot, number = "A-06", type = SpotType.SMALL)
        )

        mvc.perform(
            post("/api/reservations")
                .header("Authorization", "Bearer $token")
                .content("""
                    {
                        "parkingLotId": ${savedLot.id},
                        "parkingSpotId": ${newSpot.id},
                        "startTime": "${LocalDateTime.now(clock).plusHours(5).format(formatter)}",
                        "endTime": "${LocalDateTime.now(clock).plusHours(7).format(formatter)}"
                    }
                """.trimIndent())
                .contentType(MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
        )
            .andDo(print())
            .andExpect(status().isConflict)
    }

    @TestConfiguration
    class AllowedTimeClockConfig {
        @Bean
        @Primary
        fun clock(): Clock = Clock.fixed(
            LocalDateTime.now().withHour(22).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        )
    }
}