package com.example.parking.domain.parkingLot.service

import com.example.parking.domain.parkingLot.external.client.KakaoGeocodingClient
import com.example.parking.domain.parkingLot.external.client.ParkingOpenApiClient
import com.example.parking.domain.parkingLot.external.dto.ParkingApiDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.eq

@ExtendWith(MockitoExtension::class)
internal class ParkingLotSyncServiceTest {

    @Mock
    lateinit var parkingOpenApiClient: ParkingOpenApiClient

    @Mock
    lateinit var parkingLotWriter: ParkingLotWriter

    @Mock
    lateinit var kakaoGeocodingClient: KakaoGeocodingClient

    private lateinit var parkingLotSyncService: ParkingLotSyncService

    @BeforeEach
    fun setUp() {
        parkingLotSyncService = ParkingLotSyncService(
            parkingOpenApiClient,
            parkingLotWriter,
            kakaoGeocodingClient,
            "test-key"
        )
    }

    @Nested
    @DisplayName("syncParkingLots")
    inner class SyncParkingLotsTest {

        @Test
        @DisplayName("externalId가 null이면 건너뛴다")
        fun skip_whenExternalIdIsNull() {
            val item = createItem(null, "역삼 공영주차장", "서울 강남구 역삼동", 10.0)

            given(parkingOpenApiClient.fetchParkingLots()).willReturn(createResponse(listOf(item)))

            parkingLotSyncService.syncParkingLots()

            verify(parkingLotWriter, never()).saveOrUpdate(
                any(),
                any(),
                any(),
                any(),
                anyOrNull()
            )
        }

        @Test
        @DisplayName("externalId가 blank이면 건너뛴다")
        fun skip_whenExternalIdIsBlank() {
            val item = createItem("   ", "삼성 공영주차장", "서울 강남구 삼성동", 20.0)

            given(parkingOpenApiClient.fetchParkingLots()).willReturn(createResponse(listOf(item)))

            parkingLotSyncService.syncParkingLots()

            verify(parkingLotWriter, never()).saveOrUpdate(
                any(),
                any(),
                any(),
                any(),
                anyOrNull()
            )
        }

        @Test
        @DisplayName("기존 주차장이 있으면 updateInfo만 수행한다")
        fun updateExistingParkingLot() {
            val externalId = "P-001"
            val item = createItem(externalId, "수정된 주차장명", "서울 강남구 대치동", 50.0)

            given(parkingOpenApiClient.fetchParkingLots()).willReturn(createResponse(listOf(item)))

            parkingLotSyncService.syncParkingLots()

            verify(parkingLotWriter).saveOrUpdate(
                externalId = eq(externalId),
                name = eq("수정된 주차장명"),
                address = eq("서울 강남구 대치동"),
                totalSpot = eq(50),
                location = anyOrNull()
            )

        }

        @Test
        @DisplayName("기존 주차장이 없으면 저장한다")
        fun saveNewParkingLot() {
            val externalId = "P-002"
            val item = createItem(externalId, "신규 주차장", "서울 강남구 청담동", 30.0)

            given(parkingOpenApiClient.fetchParkingLots()).willReturn(createResponse(listOf(item)))

            parkingLotSyncService.syncParkingLots()

            verify(parkingLotWriter).saveOrUpdate(
                externalId = eq(externalId),
                name = eq("신규 주차장"),
                address = eq("서울 강남구 청담동"),
                totalSpot = eq(30),
                location = anyOrNull()
            )

        }

        @Test
        @DisplayName("신규 주차장이고 totalSpot이 0보다 크면 자리 생성을 수행한다")
        fun createSpots_whenNewParkingLotAndTotalSpotPositive() {
            val externalId = "P-003"
            val item = createItem(externalId, "신규 주차장", "서울 강남구 논현동", 15.0)

            given(parkingOpenApiClient.fetchParkingLots()).willReturn(createResponse(listOf(item)))

            parkingLotSyncService.syncParkingLots()

            verify(parkingLotWriter).saveOrUpdate(
                externalId = eq(externalId),
                name = eq("신규 주차장"),
                address = eq("서울 강남구 논현동"),
                totalSpot = eq(15),
                location = anyOrNull()
            )

        }

        @Test
        @DisplayName("신규 주차장이어도 totalSpot이 null이면 자리 생성을 하지 않는다")
        fun doNotCreateSpots_whenTotalSpotIsNull() {
            val item = createItem("P-004", "신규 주차장", "서울 강남구 개포동", null)

            given(parkingOpenApiClient.fetchParkingLots()).willReturn(createResponse(listOf(item)))

            parkingLotSyncService.syncParkingLots()

            verify(parkingLotWriter, never()).saveOrUpdate(
                any(),
                any(),
                any(),
                any(),
                anyOrNull()
            )
        }

        @Test
        @DisplayName("신규 주차장이어도 totalSpot이 0이면 자리 생성을 하지 않는다")
        fun doNotCreateSpots_whenTotalSpotIsZero() {
            val externalId = "P-005"
            val item = createItem(externalId, "신규 주차장", "서울 강남구 수서동", 0.0)

            given(parkingOpenApiClient.fetchParkingLots()).willReturn(createResponse(listOf(item)))

            parkingLotSyncService.syncParkingLots()

            verify(parkingLotWriter).saveOrUpdate(
                externalId = eq(externalId),
                name = eq("신규 주차장"),
                address = eq("서울 강남구 수서동"),
                totalSpot = eq(0),
                location = anyOrNull()
            )

        }
    }

    private fun createResponse(items: List<ParkingApiDto.ParkingLotItem>): ParkingApiDto.Response {
        val apiResult = ParkingApiDto.ApiResult(code = "INFO-000", message = "정상 처리되었습니다.")
        val parkInfo = ParkingApiDto.ParkInfo(listTotalCount = items.size, result = apiResult, items = items)
        return ParkingApiDto.Response(parkInfo = parkInfo)
    }

    private fun createItem(
        pkltCd: String?,
        pkltNm: String?,
        addr: String?,
        tpkct: Double?,
        lat: Double? = 37.5665,
        lot: Double? = 126.9780
    ): ParkingApiDto.ParkingLotItem {
        return ParkingApiDto.ParkingLotItem(
            pkltCd = pkltCd,
            pkltNm = pkltNm,
            addr = addr,
            tpkct = tpkct,
            lat = lat,
            lot = lot
        )
    }
}
