package com.example.parking.domain.parkingLot.repository

import java.time.LocalTime

interface NearbyParkingLotProjection {
    fun getId(): Long
    fun getName(): String
    fun getAddress(): String
    fun getTotalSpot(): Int
    fun getPrice(): Int
    fun getOperationStartTime(): LocalTime
    fun getOperationEndTime(): LocalTime
    fun getLatitude(): Double
    fun getLongitude(): Double
    fun getDistance(): Double  // 미터 단위
}