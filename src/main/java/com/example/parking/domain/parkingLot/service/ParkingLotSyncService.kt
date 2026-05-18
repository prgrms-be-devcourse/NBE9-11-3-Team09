package com.example.parking.domain.parkingLot.service

import com.example.parking.domain.parkingLot.entity.ParkingLot
import com.example.parking.domain.parkingLot.external.client.KakaoGeocodingClient
import com.example.parking.domain.parkingLot.external.client.ParkingOpenApiClient
import com.example.parking.domain.parkingLot.repository.ParkingLotRepository
import com.example.parking.domain.parkingspot.service.ParkingSpotService
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.locationtech.jts.geom.Point

/*
주차장 외부 공공데이터를 우리 DB와 동기화하는 서비스 클래스

- 외부 API로부터 주차장 목록 조회
- 외부 API의 고유 식별자(externalId)로 기존 데이터 조회
- 기존 데이터가 없으면 신규 생성
- 기존 데이터가 있으면 최신 정보로 수정
- 최종적으로 DB에 반영
*/
@Service
class ParkingLotSyncService(
    private val parkingOpenApiClient: ParkingOpenApiClient,
    private val parkingLotWriter: ParkingLotWriter,
    private val kakaoGeocodingClient: KakaoGeocodingClient,
    @Value("\${kakao.rest-api-key}") private val kakaoApiKey: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
    // [CUS-01] 외부 주차장 데이터를 우리 DB와 동기화
    @CacheEvict(value = ["parkingLots", "parkingLot"], allEntries = true)
    fun syncParkingLots() {
        val response = parkingOpenApiClient.fetchParkingLots()
        val parkInfo = checkNotNull(response.parkInfo) {
            "서울시 주차장 API 응답에 parkInfo가 없습니다."
        }

        log.info("Kakao API key length: {}", kakaoApiKey.length)

        parkInfo.items.forEach { item ->
            val externalId = item.pkltCd
            val name = item.pkltNm
            val address = item.addr
            val totalSpot = item.tpkct?.toInt()

            if (
                externalId.isNullOrBlank() ||
                name.isNullOrBlank() ||
                address.isNullOrBlank() ||
                totalSpot == null
            ) {
                return@forEach
            }

            val location = resolveLocation(item.lat, item.lot, address)

            parkingLotWriter.saveOrUpdate(
                externalId = externalId,
                name = name,
                address = address,
                totalSpot = totalSpot,
                location = location
            )
        }
    }

    private fun resolveLocation(lat: Double?, lng: Double?, address: String): Point? {
        // 1차: API 좌표 그대로
        if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
            return createPoint(lat, lng)
        }

        log.debug("좌표 누락, Geocoding 시도: {}", address)
        return geocodeAddress(address)
    }

    private fun geocodeAddress(address: String): Point? {
        geocodeOnce(address)?.let { return it }

        val normalized = address.replace(Regex("-0$"), "")
        if (normalized != address) {
            geocodeOnce(normalized)?.let {
                log.info("정규화 후 매칭 성공: '{}' → '{}'", address, normalized)
                return it
            }
        }

        log.warn("Geocoding 실패: {}", address)
        return null
    }

    private fun geocodeOnce(query: String): Point? {
        return try {
            val response = kakaoGeocodingClient.searchAddress(
                "KakaoAK $kakaoApiKey", query
            )
            val doc = response.documents.firstOrNull() ?: return null
            val lat = doc.latitude?.toDoubleOrNull() ?: return null
            val lng = doc.longitude?.toDoubleOrNull() ?: return null
            Thread.sleep(50)
            createPoint(lat, lng)
        } catch (e: Exception) {
            log.warn("Geocoding 호출 에러: query={}, error={}", query, e.message)
            null
        }
    }

    private fun createPoint(lat: Double, lng: Double): Point {
        val point = geometryFactory.createPoint(Coordinate(lng, lat))  // (경도, 위도) 순서
        point.srid = 4326
        return point
    }
}