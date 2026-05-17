package com.example.parking.domain.parkingspot.service

import com.example.parking.domain.parkingLot.entity.ParkingLot
import com.example.parking.domain.parkingLot.repository.ParkingLotRepository
import com.example.parking.domain.parkingspot.dto.ParkingSpotDto
import com.example.parking.domain.parkingspot.entity.ParkingSpot
import com.example.parking.domain.parkingspot.entity.SpotStatus
import com.example.parking.domain.parkingspot.entity.SpotType
import com.example.parking.domain.parkingspot.repository.ParkingSpotRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.LocalDateTime
import java.util.*

@SpringBootTest
@Transactional
class ParkingSpotServiceTest {

    @Autowired
    private lateinit var parkingSpotRepository: ParkingSpotRepository
    @Autowired
    private lateinit var parkingLotRepository: ParkingLotRepository
    @Autowired
    private lateinit var parkingSpotService: ParkingSpotService

    private lateinit var savedSpot: ParkingSpot
    private lateinit var savedLot: ParkingLot

    @BeforeEach
    fun setUp() {
        val parkingLot = ParkingLot.of(
            externalId = "test-lot-${UUID.randomUUID()}",
            name = "테스트 주차장",
            address = "서울시 테스트구",
            totalSpot = 10
        )
        savedLot = parkingLotRepository.save(parkingLot)
        savedSpot = parkingSpotRepository.save(ParkingSpot(savedLot, "A-01", SpotType.SMALL))
    }

    @Test
    @DisplayName("AVAILABLE 자리에 tryReserve 호출 시 반환값이 1이고 상태가 OCCUPIED로 바뀐다")
    fun tryReserve_success() {
        // when
        val result = parkingSpotRepository.tryReserve(savedSpot.id, LocalDateTime.now())

        // then
        assertThat(result).isEqualTo(1)

        val updated = parkingSpotRepository.findById(savedSpot.id).get()
        assertThat(updated.status).isEqualTo(SpotStatus.OCCUPIED)
        assertThat(updated.reservedAt).isNotNull()
    }

    @Test
    @DisplayName("이미 OCCUPIED인 자리에 tryReserve 호출 시 반환값이 0이고 상태가 변하지 않는다")
    fun tryReserve_fail_alreadyOccupied() {
        // given - 먼저 한 번 선점
        parkingSpotRepository.tryReserve(savedSpot.id, LocalDateTime.now())

        // when - 이미 점유된 자리에 다시 시도
        val result = parkingSpotRepository.tryReserve(savedSpot.id, LocalDateTime.now())

        // then
        assertThat(result).isEqualTo(0)

        val unchanged = parkingSpotRepository.findById(savedSpot.id).get()
        assertThat(unchanged.status).isEqualTo(SpotStatus.OCCUPIED)
    }

    @Test
    @DisplayName("특정 주차장의 AVAILABLE 자리만 반환한다")
    fun findAvailableSpots_onlyAvailable() {
        // given - AVAILABLE 1개, OCCUPIED 1개 저장
        val available = ParkingSpot(savedLot, "A-03", SpotType.SMALL)
        val occupied  = ParkingSpot(savedLot, "A-04", SpotType.SMALL).also { it.reserve() }
        parkingSpotRepository.saveAll(listOf(available, occupied))

        // when
        val result: List<ParkingSpotDto> = parkingSpotService.findAvailableSpots(checkNotNull(savedLot.id))

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].status).isEqualTo(SpotStatus.AVAILABLE)
    }

    @Test
    @DisplayName("totalSpot 개수만큼 자리가 생성된다")
    fun createSpots_totalCount() {
        // given
        parkingSpotRepository.delete(savedSpot)

        // when
        parkingSpotService.createSpots(savedLot, 10)

        // then
        val spots = parkingSpotRepository.findAll()
            .filter { it.parkingLot.id == savedLot.id }
        assertThat(spots).hasSize(10)
    }

    @Test
    @DisplayName("자리 타입이 SMALL 80%, LARGE 10%, ELECTRIC 10% 비율로 생성된다")
    fun createSpots_typeRatio() {
        // given
        parkingSpotRepository.delete(savedSpot)

        // when
        parkingSpotService.createSpots(savedLot, 10)

        // then
        val spots = parkingSpotRepository.findAll()
            .filter { it.parkingLot.id == savedLot.id }

        assertThat(spots.count { it.type == SpotType.SMALL }.toLong()).isEqualTo(8)
        assertThat(spots.count { it.type == SpotType.LARGE }.toLong()).isEqualTo(1)
        assertThat(spots.count { it.type == SpotType.ELECTRIC }.toLong()).isEqualTo(1)
    }

    @Test
    @DisplayName("관리자가 자리 상태를 원하는 값으로 변경할 수 있다")
    fun updateSpotStatusByAdmin_success() {
        // given
        val saved = parkingSpotRepository.save(ParkingSpot(savedLot, "A-01", SpotType.SMALL))

        // when
        parkingSpotService.updateSpotStatusByAdmin(saved.id, SpotStatus.OCCUPIED)

        // then
        val updated = parkingSpotRepository.findById(saved.id).get()
        assertThat(updated.status).isEqualTo(SpotStatus.OCCUPIED)
    }

    @Test
    @DisplayName("존재하지 않는 spotId로 요청하면 예외가 발생한다")
    fun updateSpotStatusByAdmin_notFound() {
        assertThatThrownBy {
            parkingSpotService.updateSpotStatusByAdmin(999999L, SpotStatus.OCCUPIED)
        }.isInstanceOf(Exception::class.java)
    }

    @Test
    @DisplayName("subscribe 호출 시 SseEmitter가 반환된다")
    fun subscribe_returnsSseEmitter() {
        // when
        val emitter: SseEmitter = parkingSpotService.subscribe(checkNotNull(savedLot.id))

        // then
        assertThat(emitter).isNotNull()
    }
}