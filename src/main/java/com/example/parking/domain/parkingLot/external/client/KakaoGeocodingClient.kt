package com.example.parking.domain.parkingLot.external.client

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange
interface KakaoGeocodingClient {
    @GetExchange("/v2/local/search/address.json",
        accept = ["application/json"])
    fun searchAddress(
        @RequestHeader("Authorization") authorization: String,
        @RequestParam("query") query: String
    ): KakaoGeocodingResponse

    data class KakaoGeocodingResponse(
        val documents: List<Document> = emptyList()
    )

    data class Document(
        @JsonProperty("x") val longitude: String?,
        @JsonProperty("y") val latitude: String?
    )
}