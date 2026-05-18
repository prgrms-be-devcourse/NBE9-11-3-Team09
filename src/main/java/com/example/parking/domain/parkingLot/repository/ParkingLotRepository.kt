package com.example.parking.domain.parkingLot.repository

import com.example.parking.domain.parkingLot.entity.ParkingLot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface ParkingLotRepository : JpaRepository<ParkingLot, Long>, ParkingLotRepositoryCustom {

    // 외부 API 주차장 식별값으로 조회
    fun findByExternalId(externalId: String): Optional<ParkingLot>

    // 주소(동) 기준 주차장 검색
//    fun findByNameContainingOrAddressContaining(
//        name: String,
//        address: String,
//        pageable: Pageable
//    ): Page<ParkingLot>

    // 반경 검색 - native query (공간 인덱스 활용)
    @Query(
        value = """
            SELECT 
                p.parking_lot_id              AS id,
                p.parking_lot_name            AS name,
                p.address                     AS address,
                p.total_spot                  AS totalSpot,
                p.price                       AS price,
                p.operation_start_time        AS operationStartTime,
                p.operation_end_time          AS operationEndTime,
                ST_Y(p.location)              AS latitude,
                ST_X(p.location)              AS longitude,
                ST_Distance_Sphere(
                    p.location,
                    ST_SRID(POINT(:lng, :lat), 4326)
                ) AS distance
            FROM parking_lots p
            WHERE p.location IS NOT NULL
              AND MBRContains(
                    ST_Buffer(
                      ST_SRID(POINT(:lng, :lat), 4326),
                      :radius
                    ),
                    p.location
                  )
              AND ST_Distance(
                    p.location,
                    ST_SRID(POINT(:lng, :lat), 4326)
                  ) <= :radius
            ORDER BY distance ASC
        """,
        nativeQuery = true
    )
    fun findNearby(
        @Param("lat") lat: Double,
        @Param("lng") lng: Double,
        @Param("radius") radiusInMeters: Int
    ): List<NearbyParkingLotProjection>
}