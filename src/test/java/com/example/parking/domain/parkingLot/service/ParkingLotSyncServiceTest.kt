package com.example.parking.domain.parkingLot.service

import com.example.parking.domain.parkingLot.entity.ParkingLot
import com.example.parking.domain.parkingLot.external.client.ParkingOpenApiClient
import com.example.parking.domain.parkingLot.external.dto.ParkingApiDto
import com.example.parking.domain.parkingLot.repository.ParkingLotRepository
import com.example.parking.domain.parkingspot.service.ParkingSpotService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class ParkingLotSyncServiceTest {

    @Mock
    lateinit var parkingOpenApiClient: ParkingOpenApiClient

    @Mock
    lateinit var parkingLotRepository: ParkingLotRepository

    @Mock
    lateinit var parkingSpotService: ParkingSpotService

    @InjectMocks
    lateinit var parkingLotSyncService: ParkingLotSyncService

    @Nested
    @DisplayName("syncParkingLots")
    inner class SyncParkingLotsTest {

        @Test
        @DisplayName("externalId가 null이면 건너뛴다")
        fun skip_whenExternalIdIsNull() {
            // given
            val item = createItem(
                null, "역삼 공영주차장", "서울 강남구 역삼동", 10.0
            )

            BDDMockito.given(parkingOpenApiClient.fetchParkingLots())
                .willReturn(createResponse(listOf(item)))

            // when
            parkingLotSyncService.syncParkingLots()

            // then
            Mockito.verify(parkingLotRepository, Mockito.never())
                .findByExternalId(ArgumentMatchers.anyString())
            Mockito.verify(parkingLotRepository, Mockito.never())
                .save(ArgumentMatchers.any(ParkingLot::class.java))
            Mockito.verify(parkingSpotService, Mockito.never())
                .createSpots(ArgumentMatchers.any(ParkingLot::class.java), ArgumentMatchers.anyInt())
        }

        @Test
        @DisplayName("externalId가 blank이면 건너뛴다")
        fun skip_whenExternalIdIsBlank() {
            // given
            val item = createItem(
                "   ", "삼성 공영주차장", "서울 강남구 삼성동", 20.0
            )

            BDDMockito.given(parkingOpenApiClient.fetchParkingLots())
                .willReturn(createResponse(listOf(item)))

            // when
            parkingLotSyncService.syncParkingLots()

            // then
            Mockito.verify(parkingLotRepository, Mockito.never())
                .findByExternalId(ArgumentMatchers.anyString())
            Mockito.verify(parkingLotRepository, Mockito.never())
                .save(ArgumentMatchers.any(ParkingLot::class.java))
            Mockito.verify(parkingSpotService, Mockito.never())
                .createSpots(ArgumentMatchers.any(ParkingLot::class.java), ArgumentMatchers.anyInt())
        }

        @Test
        @DisplayName("기존 주차장이 있으면 updateInfo만 수행한다")
        fun updateExistingParkingLot() {
            // given
            val externalId = "P-001"
            val item = createItem(
                externalId,
                "수정된 주차장명",
                "서울 강남구 대치동",
                50.0
            )

            val existingParkingLot = ParkingLot.of(
                externalId = "P-001",
                name = "기존 주차장명",
                address = "서울 강남구 역삼동",
                totalSpot = 10
            )

            BDDMockito.given(parkingOpenApiClient.fetchParkingLots())
                .willReturn(createResponse(listOf(item)))

            BDDMockito.given(parkingLotRepository.findByExternalId(externalId))
                .willReturn(Optional.of(existingParkingLot))

            // when
            parkingLotSyncService.syncParkingLots()

            // then
            Mockito.verify(parkingLotRepository).findByExternalId(externalId)

            Assertions.assertThat(existingParkingLot.name).isEqualTo("수정된 주차장명")
            Assertions.assertThat(existingParkingLot.address).isEqualTo("서울 강남구 대치동")
            Assertions.assertThat(existingParkingLot.totalSpot).isEqualTo(50)

            Mockito.verify(parkingLotRepository, Mockito.never())
                .save(ArgumentMatchers.any(ParkingLot::class.java))
            Mockito.verify(parkingSpotService, Mockito.never())
                .createSpots(ArgumentMatchers.any(ParkingLot::class.java), ArgumentMatchers.anyInt())
        }

        @Test
        @DisplayName("기존 주차장이 없으면 저장한다")
        fun saveNewParkingLot() {
            // given
            val externalId = "P-002"
            val item = createItem(
                externalId, "신규 주차장", "서울 강남구 청담동", 30.0
            )

            val savedParkingLot = ParkingLot.of(
                externalId, "신규 주차장", "서울 강남구 청담동", 30
            )

            BDDMockito.given(parkingOpenApiClient.fetchParkingLots())
                .willReturn(createResponse(listOf(item)))
            BDDMockito.given<Optional<ParkingLot>>(parkingLotRepository.findByExternalId(externalId))
                .willReturn(Optional.empty<ParkingLot>())
            BDDMockito.given(
                parkingLotRepository.save(ArgumentMatchers.any(ParkingLot::class.java)
                )
            )
                .willReturn(savedParkingLot)

            // when
            parkingLotSyncService.syncParkingLots()

            // then
            val captor = ArgumentCaptor.forClass(ParkingLot::class.java)
            Mockito.verify(parkingLotRepository).save(captor.capture())

            val savedArg = captor.value
            Assertions.assertThat(savedArg.externalId).isEqualTo("P-002")
            Assertions.assertThat(savedArg.name).isEqualTo("신규 주차장")
            Assertions.assertThat(savedArg.address).isEqualTo("서울 강남구 청담동")
            Assertions.assertThat(savedArg.totalSpot).isEqualTo(30)
        }

        @Test
        @DisplayName("신규 주차장이고 totalSpot이 0보다 크면 자리 생성을 수행한다")
        fun createSpots_whenNewParkingLotAndTotalSpotPositive() {
            // given
            val externalId = "P-003"
            val item = createItem(
                externalId, "신규 주차장", "서울 강남구 논현동", 15.0
            )

            val savedParkingLot = ParkingLot.of(
                externalId, "신규 주차장", "서울 강남구 논현동", 15
            )

            BDDMockito.given(parkingOpenApiClient.fetchParkingLots())
                .willReturn(createResponse(listOf(item)))
            BDDMockito.given(parkingLotRepository.findByExternalId(externalId))
                .willReturn(Optional.empty<ParkingLot>())
            BDDMockito.given(
                parkingLotRepository.save(
                    ArgumentMatchers.any(
                        ParkingLot::class.java
                    )
                )
            )
                .willReturn(savedParkingLot)

            // when
            parkingLotSyncService.syncParkingLots()

            // then
            Mockito.verify(parkingSpotService).createSpots(savedParkingLot, 15)
        }

        @Test
        @DisplayName("신규 주차장이어도 totalSpot이 null이면 자리 생성을 하지 않는다")
        fun doNotCreateSpots_whenTotalSpotIsNull() {
            // given
            val item = createItem(
                "P-004",
                "신규 주차장",
                "서울 강남구 개포동",
                null
            )

            BDDMockito.given(parkingOpenApiClient.fetchParkingLots())
                .willReturn(createResponse(listOf(item)))

            // when
            parkingLotSyncService.syncParkingLots()

            // then
            Mockito.verify(parkingLotRepository, Mockito.never())
                .findByExternalId(ArgumentMatchers.anyString())
            Mockito.verify(parkingLotRepository, Mockito.never())
                .save(ArgumentMatchers.any(ParkingLot::class.java))
            Mockito.verify(parkingSpotService, Mockito.never())
                .createSpots(ArgumentMatchers.any(ParkingLot::class.java), ArgumentMatchers.anyInt())
        }

        @Test
        @DisplayName("신규 주차장이어도 totalSpot이 0이면 자리 생성을 하지 않는다")
        fun doNotCreateSpots_whenTotalSpotIsZero() {
            // given
            val externalId = "P-005"
            val item = createItem(
                externalId, "신규 주차장", "서울 강남구 수서동", 0.0
            )

            val savedParkingLot = ParkingLot.of(
                externalId, "신규 주차장", "서울 강남구 수서동", 0
            )

            BDDMockito.given(parkingOpenApiClient.fetchParkingLots())
                .willReturn(createResponse(listOf(item)))
            BDDMockito.given(parkingLotRepository.findByExternalId(externalId))
                .willReturn(Optional.empty<ParkingLot>())
            BDDMockito.given(
                parkingLotRepository.save(
                    ArgumentMatchers.any(
                        ParkingLot::class.java
                    )
                )
            )
                .willReturn(savedParkingLot)

            // when
            parkingLotSyncService.syncParkingLots()

            // then
            Mockito.verify(parkingSpotService, Mockito.never())
                .createSpots(ArgumentMatchers.any(ParkingLot::class.java), ArgumentMatchers.anyInt())
        }
    }

    private fun createResponse(
        items: List<ParkingApiDto.ParkingLotItem>
    ): ParkingApiDto.Response {

        val apiResult = ParkingApiDto.ApiResult(
            code = "INFO-000",
            message = "정상 처리되었습니다."
        )

        val parkInfo = ParkingApiDto.ParkInfo(
            listTotalCount = items.size,
            result = apiResult,
            items = items
        )

        return ParkingApiDto.Response(
            parkInfo = parkInfo
        )
    }

    private fun createItem(
        pkltCd: String?,
        pkltNm: String?,
        addr: String?,
        tpkct: Double?
    ): ParkingApiDto.ParkingLotItem {
        return ParkingApiDto.ParkingLotItem(
            pkltCd = pkltCd,
            pkltNm = pkltNm,
            addr = addr,
            tpkct = tpkct
        )
    }
}
