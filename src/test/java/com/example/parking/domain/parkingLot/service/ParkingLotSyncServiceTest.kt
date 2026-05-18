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
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
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
            val item = createItem(null, "역삼 공영주차장", "서울 강남구 역삼동", 10.0)

            given(parkingOpenApiClient.fetchParkingLots()).willReturn(createResponse(listOf(item)))

            parkingLotSyncService.syncParkingLots()

            verify(parkingLotRepository, never()).findByExternalId(any())
            verify(parkingLotRepository, never()).save(any())
            verify(parkingSpotService, never()).createSpots(any(), any())
        }

        @Test
        @DisplayName("externalId가 blank이면 건너뛴다")
        fun skip_whenExternalIdIsBlank() {
            val item = createItem("   ", "삼성 공영주차장", "서울 강남구 삼성동", 20.0)

            given(parkingOpenApiClient.fetchParkingLots()).willReturn(createResponse(listOf(item)))

            parkingLotSyncService.syncParkingLots()

            verify(parkingLotRepository, never()).findByExternalId(any())
            verify(parkingLotRepository, never()).save(any())
            verify(parkingSpotService, never()).createSpots(any(), any())
        }

        @Test
        @DisplayName("기존 주차장이 있으면 updateInfo만 수행한다")
        fun updateExistingParkingLot() {
            val externalId = "P-001"
            val item = createItem(externalId, "수정된 주차장명", "서울 강남구 대치동", 50.0)

            val existingParkingLot = ParkingLot.of(
                externalId = "P-001",
                name = "기존 주차장명",
                address = "서울 강남구 역삼동",
                totalSpot = 10
            )

            given(parkingOpenApiClient.fetchParkingLots()).willReturn(createResponse(listOf(item)))
            given(parkingLotRepository.findByExternalId(externalId)).willReturn(Optional.of(existingParkingLot))

            parkingLotSyncService.syncParkingLots()

            verify(parkingLotRepository).findByExternalId(externalId)

            Assertions.assertThat(existingParkingLot.name).isEqualTo("수정된 주차장명")
            Assertions.assertThat(existingParkingLot.address).isEqualTo("서울 강남구 대치동")
            Assertions.assertThat(existingParkingLot.totalSpot).isEqualTo(50)

            verify(parkingLotRepository, never()).save(any())
            verify(parkingSpotService, never()).createSpots(any(), any())
        }

        @Test
        @DisplayName("기존 주차장이 없으면 저장한다")
        fun saveNewParkingLot() {
            val externalId = "P-002"
            val item = createItem(externalId, "신규 주차장", "서울 강남구 청담동", 30.0)

            val savedParkingLot = ParkingLot.of(externalId, "신규 주차장", "서울 강남구 청담동", 30)

            given(parkingOpenApiClient.fetchParkingLots()).willReturn(createResponse(listOf(item)))
            given(parkingLotRepository.findByExternalId(externalId)).willReturn(Optional.empty())
            given(parkingLotRepository.save(any<ParkingLot>())).willReturn(savedParkingLot)

            parkingLotSyncService.syncParkingLots()

            val captor = argumentCaptor<ParkingLot>()
            verify(parkingLotRepository).save(captor.capture())

            val savedArg = captor.firstValue
            Assertions.assertThat(savedArg.externalId).isEqualTo("P-002")
            Assertions.assertThat(savedArg.name).isEqualTo("신규 주차장")
            Assertions.assertThat(savedArg.address).isEqualTo("서울 강남구 청담동")
            Assertions.assertThat(savedArg.totalSpot).isEqualTo(30)
        }

        @Test
        @DisplayName("신규 주차장이고 totalSpot이 0보다 크면 자리 생성을 수행한다")
        fun createSpots_whenNewParkingLotAndTotalSpotPositive() {
            val externalId = "P-003"
            val item = createItem(externalId, "신규 주차장", "서울 강남구 논현동", 15.0)

            val savedParkingLot = ParkingLot.of(externalId, "신규 주차장", "서울 강남구 논현동", 15)

            given(parkingOpenApiClient.fetchParkingLots()).willReturn(createResponse(listOf(item)))
            given(parkingLotRepository.findByExternalId(externalId)).willReturn(Optional.empty())
            given(parkingLotRepository.save(any<ParkingLot>())).willReturn(savedParkingLot)

            parkingLotSyncService.syncParkingLots()

            verify(parkingSpotService).createSpots(savedParkingLot, 15)
        }

        @Test
        @DisplayName("신규 주차장이어도 totalSpot이 null이면 자리 생성을 하지 않는다")
        fun doNotCreateSpots_whenTotalSpotIsNull() {
            val item = createItem("P-004", "신규 주차장", "서울 강남구 개포동", null)

            given(parkingOpenApiClient.fetchParkingLots()).willReturn(createResponse(listOf(item)))

            parkingLotSyncService.syncParkingLots()

            verify(parkingLotRepository, never()).findByExternalId(any())
            verify(parkingLotRepository, never()).save(any())
            verify(parkingSpotService, never()).createSpots(any(), any())
        }

        @Test
        @DisplayName("신규 주차장이어도 totalSpot이 0이면 자리 생성을 하지 않는다")
        fun doNotCreateSpots_whenTotalSpotIsZero() {
            val externalId = "P-005"
            val item = createItem(externalId, "신규 주차장", "서울 강남구 수서동", 0.0)

            val savedParkingLot = ParkingLot.of(externalId, "신규 주차장", "서울 강남구 수서동", 0)

            given(parkingOpenApiClient.fetchParkingLots()).willReturn(createResponse(listOf(item)))
            given(parkingLotRepository.findByExternalId(externalId)).willReturn(Optional.empty())
            given(parkingLotRepository.save(any<ParkingLot>())).willReturn(savedParkingLot)

            parkingLotSyncService.syncParkingLots()

            verify(parkingSpotService, never()).createSpots(any(), any())
        }
    }

    private fun createResponse(items: List<ParkingApiDto.ParkingLotItem>): ParkingApiDto.Response {
        val apiResult = ParkingApiDto.ApiResult(code = "INFO-000", message = "정상 처리되었습니다.")
        val parkInfo = ParkingApiDto.ParkInfo(listTotalCount = items.size, result = apiResult, items = items)
        return ParkingApiDto.Response(parkInfo = parkInfo)
    }

    private fun createItem(pkltCd: String?, pkltNm: String?, addr: String?, tpkct: Double?): ParkingApiDto.ParkingLotItem {
        return ParkingApiDto.ParkingLotItem(pkltCd = pkltCd, pkltNm = pkltNm, addr = addr, tpkct = tpkct)
    }
}