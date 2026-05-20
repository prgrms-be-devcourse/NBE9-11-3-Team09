package com.example.parking.domain.admin.reservation.service

import com.example.parking.domain.parkingLot.entity.ParkingLot
import com.example.parking.domain.parkingLot.repository.ParkingLotRepository
import com.example.parking.domain.parkingspot.entity.ParkingSpot
import com.example.parking.domain.parkingspot.entity.SpotType
import com.example.parking.domain.parkingspot.repository.ParkingSpotRepository
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
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AdminReservationServiceTest @Autowired constructor(
    private val mvc: MockMvc,
    private val userRepository: UserRepository,
    private val parkingLotRepository: ParkingLotRepository,
    private val parkingSpotRepository: ParkingSpotRepository,
    private val reservationRepository: ReservationRepository,
    private val jwtUtil: JwtUtil
) {
    private lateinit var adminUser: User
    private lateinit var normalUser: User
    private lateinit var savedLot: ParkingLot
    private lateinit var savedSpot: ParkingSpot
    private lateinit var savedReservation: Reservation
    private lateinit var adminToken: String
    private lateinit var userToken: String

    @BeforeEach
    fun setUp() {
        reservationRepository.deleteAll()
        parkingSpotRepository.deleteAll()
        parkingLotRepository.deleteAll()
        userRepository.deleteAll()
        adminUser = userRepository.save(
            User(
                email = "admin@test.com",
                password = "1234",
                name = "관리자",
                plateNumber = "00가0000",
                vehicleType = VehicleType.SMALL,
                role = UserRole.ADMIN,
                status = UserStatus.ACTIVE
            )
        )
        adminToken = jwtUtil.createAccessToken(adminUser)

        normalUser = userRepository.save(
            User(
                email = "user@test.com",
                password = "1234",
                name = "일반유저",
                plateNumber = "12가3456",
                vehicleType = VehicleType.SMALL,
                role = UserRole.USER,
                status = UserStatus.ACTIVE
            )
        )
        userToken = jwtUtil.createAccessToken(normalUser)

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

        savedReservation = reservationRepository.save(
            Reservation.of(
                user = normalUser,
                parkingLot = savedLot,
                parkingSpot = savedSpot,
                startTime = LocalDateTime.now().plusHours(2),
                endTime = LocalDateTime.now().plusHours(4)
            )
        )
        // 관리자 강제 취소는 CONFIRMED 상태만 가능
        savedReservation.confirm()
        savedReservation = reservationRepository.save(savedReservation)
    }

    @Test
    @DisplayName("[ADM] 관리자 예약 목록 전체 조회 - 정상 조회되어야 한다")
    fun getAdminReservations_all_success() {
        mvc.perform(
            get("/api/admin/reservations")
                .header("Authorization", "Bearer $adminToken")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].parkingLotName").value("강남역 공영 주차장"))
    }

    @Test
    @DisplayName("[ADM] 관리자 예약 목록 유저 필터링 조회 - 정상 조회되어야 한다")
    fun getAdminReservations_filterByUser_success() {
        mvc.perform(
            get("/api/admin/reservations")
                .header("Authorization", "Bearer $adminToken")
                .param("userId", normalUser.id.toString())
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].userName").value("일반유저"))
    }

    @Test
    @DisplayName("[ADM] 관리자 예약 목록 유저 필터링 조회 - 없는 유저 ID면 빈 목록이어야 한다")
    fun getAdminReservations_filterByUser_empty() {
        mvc.perform(
            get("/api/admin/reservations")
                .header("Authorization", "Bearer $adminToken")
                .param("userId", "99999")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(0))
    }

    @Test
    @DisplayName("[ADM] 관리자 예약 목록 조회 - 일반 유저는 접근할 수 없다")
    fun getAdminReservations_normalUser_forbidden() {
        mvc.perform(
            get("/api/admin/reservations")
                .header("Authorization", "Bearer $userToken")
        )
            .andDo(print())
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("[ADM] 관리자 강제 취소 - CONFIRMED 예약은 정상 취소되어야 한다")
    fun cancelByAdmin_confirmed_success() {
        mvc.perform(
            patch("/api/admin/reservations/${savedReservation.id}/cancel")
                .header("Authorization", "Bearer $adminToken")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-5"))

        val canceled = reservationRepository.findById(savedReservation.id!!).get()
        assertThat(canceled.status).isEqualTo(ReservationStatus.CANCELED)
    }

    @Test
    @DisplayName("[ADM] 관리자 강제 취소 - PENDING 예약은 취소할 수 없다")
    fun cancelByAdmin_pending_fail() {
        // savedReservation deactivate 후 새 예약 생성
        savedReservation.deactivate()
        reservationRepository.save(savedReservation)

        val pendingReservation = reservationRepository.save(
            Reservation.of(
                user = normalUser,
                parkingLot = savedLot,
                parkingSpot = parkingSpotRepository.save(
                    ParkingSpot(parkingLot = savedLot, number = "A-02", type = SpotType.SMALL)
                ),
                startTime = LocalDateTime.now().plusHours(2),
                endTime = LocalDateTime.now().plusHours(4)
            )
        ) // status 기본값 PENDING

        mvc.perform(
            patch("/api/admin/reservations/${pendingReservation.id}/cancel")
                .header("Authorization", "Bearer $adminToken")
        )
            .andDo(print())
            .andExpect(status().isConflict)
    }

    @Test
    @DisplayName("[ADM] 관리자 강제 취소 - 존재하지 않는 예약은 실패해야 한다")
    fun cancelByAdmin_notFound_fail() {
        mvc.perform(
            patch("/api/admin/reservations/99999/cancel")
                .header("Authorization", "Bearer $adminToken")
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("[ADM] 관리자 강제 취소 - 일반 유저는 접근할 수 없다")
    fun cancelByAdmin_normalUser_forbidden() {
        mvc.perform(
            patch("/api/admin/reservations/${savedReservation.id}/cancel")
                .header("Authorization", "Bearer $userToken")
        )
            .andDo(print())
            .andExpect(status().isForbidden)
    }
}