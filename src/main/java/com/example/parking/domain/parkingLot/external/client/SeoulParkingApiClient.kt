package com.example.parking.domain.parkingLot.external.client

import com.example.parking.domain.parkingLot.external.dto.ParkingApiDto
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

/*
 * Open API 호출 명세를 정의한 Http Interface
 *
 * - RestClient + Http Interface 기반으로 동작
 * - 실제 구현 없이 인터페이스만으로 HTTP 요청을 정의
 * - ParkingApiClientConfig에서 프록시 객체로 생성됨
 */
@HttpExchange
interface SeoulParkingApiClient {

    @GetExchange("/{apiKey}/json/GetParkInfo/{start}/{end}/{addr}")
    fun fetchParkingLots(
        @PathVariable apiKey: String, // 인증키
        @PathVariable start: Int,     // 조회 시작 위치
        @PathVariable end: Int,       // 조회 끝 위치
        @PathVariable addr: String    // 조회 지역
    ): ParkingApiDto.Response
}
