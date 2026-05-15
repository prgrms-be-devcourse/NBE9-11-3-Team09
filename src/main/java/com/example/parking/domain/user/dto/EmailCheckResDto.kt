package com.example.parking.domain.user.dto

data class EmailCheckResDto(
    val available: Boolean,
    val message: String
)