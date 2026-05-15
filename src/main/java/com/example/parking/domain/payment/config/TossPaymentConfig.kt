package com.example.parking.domain.payment.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class TossPaymentConfig {

    @Value("\${toss.payment.secret-key}")
    lateinit var secretKey: String

    @Value("\${toss.payment.url}")
    lateinit var url: String
}