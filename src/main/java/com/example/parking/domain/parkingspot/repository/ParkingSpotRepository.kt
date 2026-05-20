package com.example.parking.domain.parkingspot.repository

import com.example.parking.domain.parkingspot.entity.ParkingSpot
import com.example.parking.domain.parkingspot.entity.SpotStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ParkingSpotRepository : JpaRepository<ParkingSpot, Long> {

    // [CUS-11] 자리 선점 CAS: AVAILABLE → OCCUPIED
    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query("UPDATE ParkingSpot p SET p.status = 'OCCUPIED', p.reservedAt = :now WHERE p.id = :id AND p.status = 'AVAILABLE'")
    fun tryReserve(@Param("id") id: Long, @Param("now") now: LocalDateTime): Int

    // [CUS-11] OCCUPIED 인 parkingSpot을 조회
    fun findByStatusAndReservedAtBefore(status: SpotStatus, time: LocalDateTime): List<ParkingSpot>

    // [CUS-11] 주차장 아이디랑 상태로 자리 조회
    fun findByParkingLotIdAndStatus(parkingLotId: Long, status: SpotStatus): List<ParkingSpot>

    // [CUS-11] 스케줄러의 만료 처리 (CAS 방식)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ParkingSpot p SET p.status = 'AVAILABLE', p.reservedAt = null WHERE p.id = :id AND p.status = 'OCCUPIED' AND p.reservedAt < :expiredTime")
    fun releaseExpiredSpot(@Param("id") id: Long, @Param("expiredTime") expiredTime: LocalDateTime): Int

    // [CUS-05] 결제 시작: OCCUPIED → PAYING
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ParkingSpot p SET p.status = 'PAYING' WHERE p.id = :id AND p.status = 'OCCUPIED'")
    fun startPayment(@Param("id") id: Long): Int

    // [CUS-05] 결제 완료/만료/환불: PAYING → AVAILABLE
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ParkingSpot p SET p.status = 'AVAILABLE', p.reservedAt = null WHERE p.id = :id AND p.status = 'PAYING'")
    fun completePayment(@Param("id") id: Long): Int

    fun findByParkingLotId(parkingLotId: Long): List<ParkingSpot>
}