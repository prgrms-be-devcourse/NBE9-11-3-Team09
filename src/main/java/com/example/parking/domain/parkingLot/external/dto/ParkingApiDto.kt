package com.example.parking.domain.parkingLot.external.dto

import com.fasterxml.jackson.annotation.JsonProperty

// [CUS-01] 서울시 주차장 API 응답 DTO 묶음
class ParkingApiDto {

    // 최상위 응답
    data class Response(
        @JsonProperty("GetParkInfo")
        val parkInfo: ParkInfo?
    )

    // 실제 데이터 영역
    data class ParkInfo(
        @JsonProperty("list_total_count")
        val listTotalCount: Int?,

        @JsonProperty("RESULT")
        val result: ApiResult?,

        @JsonProperty("row")
        val items: List<ParkingLotItem> = emptyList()
    )

    // 결과 코드
    data class ApiResult(
        @JsonProperty("CODE")
        val code: String?,

        @JsonProperty("MESSAGE")
        val message: String?
    )

    // 개별 주차장 데이터
    data class ParkingLotItem(
        @JsonProperty("PKLT_CD")
        val pkltCd: String?,

        @JsonProperty("PKLT_NM")
        val pkltNm: String?,

        @JsonProperty("ADDR")
        val addr: String?,

        @JsonProperty("TPKCT")
        val tpkct: Double?,

        @JsonProperty("LAT")
        val lat: Double?,

        @JsonProperty("LOT")
        val lot: Double?
    )
}
