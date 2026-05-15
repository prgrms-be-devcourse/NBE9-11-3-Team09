package com.example.parking.domain.parkingLot.external.config

import com.example.parking.domain.parkingLot.external.client.SeoulParkingApiClient
import com.example.parking.domain.parkingLot.external.exception.ExternalApiException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

// RestClient 설정 + Http Interface 구현체 생성
@Configuration
class ParkingApiClientConfig {

    // [1] 서울시 Open API 호출용 RestClient 등록
    @Bean
    fun seoulParkingRestClient(): RestClient =
        RestClient.builder()
            .baseUrl("http://openapi.seoul.go.kr:8088")
            .defaultStatusHandler(
                { status -> status.isError },
                { _, response ->
                    throw ExternalApiException(
                        "서울시 주차장 API 호출 실패. status=${response.statusCode}"
                    )
                }
            )
            .build()

    // [2] Http Interface 기반 구현체 생성
    @Bean
    fun seoulParkingApiClient(
        seoulParkingRestClient: RestClient
    ): SeoulParkingApiClient {
        val adapter = RestClientAdapter.create(seoulParkingRestClient)

        val factory = HttpServiceProxyFactory
            .builderFor(adapter)
            .build()

        return factory.createClient(SeoulParkingApiClient::class.java)
    }
}