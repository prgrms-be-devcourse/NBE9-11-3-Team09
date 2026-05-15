package com.example.parking.domain.parkingLot.external.client

import com.example.parking.domain.parkingLot.external.dto.ParkingApiDto
import com.example.parking.domain.parkingLot.external.exception.ExternalApiException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ParkingOpenApiClient(
    private val seoulParkingApiClient: SeoulParkingApiClient,

    @Value("\${openapi.seoul.key}")
    private val apiKey: String
) {

    // 외부 API 호출
    fun fetchParkingLots(): ParkingApiDto.Response {
        val response = seoulParkingApiClient.fetchParkingLots(
            apiKey,
            START,
            END,
            DEFAULT_ADDR
        )

        validateResponse(response)

        return response
    }

    // 외부 API 응답 검증
    private fun validateResponse(response: ParkingApiDto.Response) {
        val parkInfo = checkNotNull(response.parkInfo) {
            "서울시 주차장 API 응답에 parkInfo가 없습니다."
        }

        val result = checkNotNull(parkInfo.result) {
            "서울시 주차장 API 응답에 RESULT가 없습니다."
        }

        if (result.code != "INFO-000") {
            throw ExternalApiException(
                "서울시 주차장 API 비정상 응답: ${result.code} / ${result.message}"
            )
        }
    }

    companion object {
        private const val START = 1
        private const val END = 100
        private const val DEFAULT_ADDR = "강남"
    }
}
