package com.example.parking.domain.parkingLot.service

import com.example.parking.domain.parkingLot.entity.ParkingLot
import com.example.parking.domain.parkingLot.repository.ParkingLotRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

/**
 * 테스트 의존성 이슈로 현재는 목 기반 단위 테스트만 우선 작성.
 * 실제 동작은 실행 환경에서 별도 검증함.
 */
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
        parkingLot1 = createParkingLot(
            id = 1L,
            externalId = "EXT-001",
            name = "역삼 공영주차장",
            address = "서울 강남구 역삼동 123",
            totalSpot = 100
        )

        parkingLot2 = createParkingLot(
            id = 2L,
            externalId = "EXT-002",
            name = "삼성 공영주차장",
            address = "서울 강남구 삼성동 456",
            totalSpot = 80
        )
    }

    @Nested
    @DisplayName("전체 주차장 조회, 동 검색")
    inner class FindAllTest {

        @Test
        @DisplayName("dong이 null이면 전체 주차장 목록을 조회한다")
        fun findAll_withNullDong_returnsAllParkingLots() {
            // given
            BDDMockito.given(parkingLotRepository.findAll())
                .willReturn(listOf(parkingLot1, parkingLot2))

            // when
            val result = parkingLotService.findAll(null)

            // then
            Assertions.assertThat(result).hasSize(2)
            Assertions.assertThat(result.map { it.name })
                .containsExactly("역삼 공영주차장", "삼성 공영주차장")

            Mockito.verify(parkingLotRepository).findAll()
        }

        @Test
        @DisplayName("dong이 공백이면 전체 주차장 목록을 조회한다")
        fun findAll_withBlankDong_returnsAllParkingLots() {
            // given
            BDDMockito.given(parkingLotRepository.findAll())
                .willReturn(listOf(parkingLot1, parkingLot2))

            // when
            val result = parkingLotService.findAll("   ")

            // then
            Assertions.assertThat(result).hasSize(2)
            Assertions.assertThat(result.map { it.name })
                .containsExactly("역삼 공영주차장", "삼성 공영주차장")

            Mockito.verify(parkingLotRepository).findAll()
        }

        @Test
        @DisplayName("dong이 있으면 해당 동이 포함된 주소의 주차장 목록을 조회한다")
        fun findAll_withDong_returnsFilteredParkingLots() {
            // given
            BDDMockito.given(parkingLotRepository.findByAddressContaining("역삼동"))
                .willReturn(listOf(parkingLot1))

            // when
            val result = parkingLotService.findAll("역삼동")

            // then
            Assertions.assertThat(result).hasSize(1)
            Assertions.assertThat(result[0].name).isEqualTo("역삼 공영주차장")

            Mockito.verify(parkingLotRepository).findByAddressContaining("역삼동")
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 리스트를 반환한다")
        fun findAll_withDong_returnsEmptyList() {
            // given
            BDDMockito.given(parkingLotRepository.findByAddressContaining("청담동"))
                .willReturn(emptyList())

            // when
            val result = parkingLotService.findAll("청담동")

            // then
            Assertions.assertThat(result).isEmpty()

            Mockito.verify(parkingLotRepository).findByAddressContaining("청담동")
        }
    }

    @Nested
    @DisplayName("특정 주차장 조회")
    inner class FindByIdTest {

        @Test
        @DisplayName("id로 주차장을 조회할 수 있다")
        fun findById_success() {
            // given
            BDDMockito.given(parkingLotRepository.findById(1L))
                .willReturn(Optional.of(parkingLot1))

            // when
            val result = parkingLotService.findById(1L)

            // then
            Assertions.assertThat(result.name).isEqualTo("역삼 공영주차장")
            Mockito.verify(parkingLotRepository).findById(1L)
        }

        @Test
        @DisplayName("존재하지 않는 id로 조회하면 예외가 발생한다")
        fun findById_fail() {
            // given
            BDDMockito.given(parkingLotRepository.findById(999L))
                .willReturn(Optional.empty())

            // when & then
            Assertions.assertThatThrownBy {
                parkingLotService.findById(999L)
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("해당 주차장이 없습니다.")

            Mockito.verify(parkingLotRepository).findById(999L)
        }
    }

    private fun createParkingLot(
        id: Long,
        externalId: String,
        name: String,
        address: String,
        totalSpot: Int
    ): ParkingLot {
        val parkingLot = ParkingLot.of(
            externalId = externalId,
            name = name,
            address = address,
            totalSpot = totalSpot
        )

        setField(parkingLot, "id", id)

        return parkingLot
    }

    private fun setField(target: Any, fieldName: String, value: Any?) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }
}
