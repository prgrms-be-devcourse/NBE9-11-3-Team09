package com.example.parking.domain.parkingLot.service

import com.example.parking.domain.parkingLot.entity.ParkingLot
import com.example.parking.domain.parkingLot.external.client.ParkingOpenApiClient
import com.example.parking.domain.parkingLot.repository.ParkingLotRepository
import com.example.parking.domain.parkingspot.service.ParkingSpotService
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/*
주차장 외부 공공데이터를 우리 DB와 동기화하는 서비스 클래스

- 외부 API로부터 주차장 목록 조회
- 외부 API의 고유 식별자(externalId)로 기존 데이터 조회
- 기존 데이터가 없으면 신규 생성
- 기존 데이터가 있으면 최신 정보로 수정
- 최종적으로 DB에 반영
*/
@Service
@Transactional
class ParkingLotSyncService(
    private val parkingOpenApiClient: ParkingOpenApiClient,
    private val parkingLotRepository: ParkingLotRepository,
    private val parkingSpotService: ParkingSpotService
) {

    // [CUS-01] 외부 주차장 데이터를 우리 DB와 동기화
    @CacheEvict(value = ["parkingLots", "parkingLot"], allEntries = true)
    fun syncParkingLots() {
        val response = parkingOpenApiClient.fetchParkingLots()
        val parkInfo = checkNotNull(response.parkInfo) {
            "서울시 주차장 API 응답에 parkInfo가 없습니다."
        }

        parkInfo.items.forEach { item ->
            val externalId = item.pkltCd
            val name = item.pkltNm
            val address = item.addr
            val totalSpot = item.tpkct?.toInt()

            if (
                externalId.isNullOrBlank() ||
                name.isNullOrBlank() ||
                address.isNullOrBlank() ||
                totalSpot == null
            ) {
                return@forEach
            }

            parkingLotRepository.findByExternalId(externalId)
                .ifPresentOrElse(
                    { parkingLot ->
                        parkingLot.updateInfo(
                            name = name,
                            address = address,
                            totalSpot = totalSpot
                        )
                    },
                    {
                        val saved = parkingLotRepository.save(
                            ParkingLot.of(
                                externalId = externalId,
                                name = name,
                                address = address,
                                totalSpot = totalSpot
                            )
                        )

                        if (totalSpot > 0) {
                            parkingSpotService.createSpots(saved, totalSpot)
                        }
                    }
                )
        }
    }
}