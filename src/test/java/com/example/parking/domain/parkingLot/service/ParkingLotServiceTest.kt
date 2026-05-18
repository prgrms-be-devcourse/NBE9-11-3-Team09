package com.example.parking.domain.parkingLot.service

import com.example.parking.domain.parkingLot.entity.ParkingLot
import com.example.parking.domain.parkingLot.repository.ParkingLotRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.test.util.ReflectionTestUtils
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class ParkingLotServiceTest {

    @Mock
    lateinit var parkingLotRepository: ParkingLotRepository

    @InjectMocks
    lateinit var parkingLotService: ParkingLotService

    private lateinit var parkingLot1: ParkingLot
    private lateinit var parkingLot2: ParkingLot

    @BeforeEach
    fun setUp() {
        parkingLot1 = createParkingLot(id = 1L, externalId = "EXT-001", name = "역삼 공영주차장", address = "서울 강남구 역삼동 123", totalSpot = 100)
        parkingLot2 = createParkingLot(id = 2L, externalId = "EXT-002", name = "삼성 공영주차장", address = "서울 강남구 삼성동 456", totalSpot = 80)
    }

    @Nested
    @DisplayName("전체 주차장 조회, 동 검색")
    inner class FindAllTest {

        @Test
        @DisplayName("dong이 null이면 전체 주차장 목록을 조회한다")
        fun findAll_withNullDong_returnsAllParkingLots() {
            given(parkingLotRepository.findAll()).willReturn(listOf(parkingLot1, parkingLot2))

            val result = parkingLotService.findAll(null)

            Assertions.assertThat(result).hasSize(2)
            Assertions.assertThat(result.map { it.name }).containsExactly("역삼 공영주차장", "삼성 공영주차장")
            verify(parkingLotRepository).findAll()
        }

        @Test
        @DisplayName("dong이 공백이면 전체 주차장 목록을 조회한다")
        fun findAll_withBlankDong_returnsAllParkingLots() {
            given(parkingLotRepository.findAll()).willReturn(listOf(parkingLot1, parkingLot2))

            val result = parkingLotService.findAll("   ")

            Assertions.assertThat(result).hasSize(2)
            Assertions.assertThat(result.map { it.name }).containsExactly("역삼 공영주차장", "삼성 공영주차장")
            verify(parkingLotRepository).findAll()
        }

        @Test
        @DisplayName("dong이 있으면 해당 동이 포함된 주소의 주차장 목록을 조회한다")
        fun findAll_withDong_returnsFilteredParkingLots() {
            given(parkingLotRepository.findByAddressContaining("역삼동")).willReturn(listOf(parkingLot1))

            val result = parkingLotService.findAll("역삼동")

            Assertions.assertThat(result).hasSize(1)
            Assertions.assertThat(result[0].name).isEqualTo("역삼 공영주차장")
            verify(parkingLotRepository).findByAddressContaining("역삼동")
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 리스트를 반환한다")
        fun findAll_withDong_returnsEmptyList() {
            given(parkingLotRepository.findByAddressContaining("청담동")).willReturn(emptyList())

            val result = parkingLotService.findAll("청담동")

            Assertions.assertThat(result).isEmpty()
            verify(parkingLotRepository).findByAddressContaining("청담동")
        }
    }

    @Nested
    @DisplayName("특정 주차장 조회")
    inner class FindByIdTest {

        @Test
        @DisplayName("id로 주차장을 조회할 수 있다")
        fun findById_success() {
            given(parkingLotRepository.findById(1L)).willReturn(Optional.of(parkingLot1))

            val result = parkingLotService.findById(1L)

            Assertions.assertThat(result.name).isEqualTo("역삼 공영주차장")
            verify(parkingLotRepository).findById(1L)
        }

        @Test
        @DisplayName("존재하지 않는 id로 조회하면 예외가 발생한다")
        fun findById_fail() {
            given(parkingLotRepository.findById(999L)).willReturn(Optional.empty())

            Assertions.assertThatThrownBy { parkingLotService.findById(999L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("해당 주차장이 없습니다.")

            verify(parkingLotRepository).findById(999L)
        }
    }

    private fun createParkingLot(id: Long, externalId: String, name: String, address: String, totalSpot: Int): ParkingLot {
        val parkingLot = ParkingLot.of(externalId = externalId, name = name, address = address, totalSpot = totalSpot)
        ReflectionTestUtils.setField(parkingLot, "id", id)
        return parkingLot
    }
}