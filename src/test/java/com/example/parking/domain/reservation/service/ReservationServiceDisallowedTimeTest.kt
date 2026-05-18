package com.example.parking.domain.reservation.service

import com.example.parking.domain.parkingLot.entity.ParkingLot
import com.example.parking.domain.parkingLot.repository.ParkingLotRepository
import com.example.parking.domain.parkingspot.entity.ParkingSpot
import com.example.parking.domain.parkingspot.entity.SpotType
import com.example.parking.domain.parkingspot.repository.ParkingSpotRepository
import com.example.parking.domain.reservation.entity.Reservation
import com.example.parking.domain.reservation.repository.ReservationRepository
import com.example.parking.domain.user.entity.User
import com.example.parking.domain.user.entity.UserRole
import com.example.parking.domain.user.entity.UserStatus
import com.example.parking.domain.user.entity.VehicleType
import com.example.parking.domain.user.repository.UserRepository
import com.example.parking.global.security.JwtUtil
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
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
@Import(ReservationServiceDisallowedTimeTest.DisallowedTimeClockConfig::class)
class ReservationServiceDisallowedTimeTest @Autowired constructor(
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
                totalSpot = 100,
                location = null
            )
        )

        savedSpot = parkingSpotRepository.save(
            ParkingSpot(parkingLot = savedLot, number = "A-01", type = SpotType.SMALL)
        )

        reservationRepository.save(
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
    @DisplayName("[CUS-03] 예약 생성 - 허용 시간(22시~24시) 외에는 예약할 수 없다")
    fun createReservation_disallowedTime_fail() {
        val newSpot = parkingSpotRepository.save(
            ParkingSpot(parkingLot = savedLot, number = "A-02", type = SpotType.SMALL)
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
    class DisallowedTimeClockConfig {
        @Bean
        @Primary
        fun clock(): Clock = Clock.fixed(
            LocalDateTime.now().withHour(10).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        )
    }
}