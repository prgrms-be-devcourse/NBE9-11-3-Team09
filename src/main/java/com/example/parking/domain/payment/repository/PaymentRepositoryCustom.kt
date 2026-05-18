package com.example.parking.domain.payment.repository

import com.example.parking.domain.payment.entity.Payment
import com.example.parking.domain.payment.entity.PaymentStatus

interface PaymentRepositoryCustom {
    fun findAllByStatus(status: PaymentStatus): List<Payment>
}