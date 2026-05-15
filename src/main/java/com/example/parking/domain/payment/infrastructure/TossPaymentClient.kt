// TossPaymentClient.kt
package com.example.parking.domain.payment.infrastructure

import com.example.parking.domain.payment.config.TossPaymentConfig
import com.example.parking.domain.payment.dto.TossConfirmReqDto
import com.example.parking.domain.payment.dto.TossConfirmResDto
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.Base64

@Component
class TossPaymentClient(
        private val tossPaymentConfig: TossPaymentConfig
) {
    private val log = LoggerFactory.getLogger(TossPaymentClient::class.java)

    fun confirm(request: TossConfirmReqDto): TossConfirmResDto {
        val encodedSecretKey = Base64.getEncoder()
                .encodeToString("${tossPaymentConfig.secretKey}:".toByteArray())

        val restClient = RestClient.create()

        return restClient.post()
                .uri(tossPaymentConfig.url)
                .header(HttpHeaders.AUTHORIZATION, "Basic $encodedSecretKey")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TossConfirmResDto::class.java)!!
    }
}