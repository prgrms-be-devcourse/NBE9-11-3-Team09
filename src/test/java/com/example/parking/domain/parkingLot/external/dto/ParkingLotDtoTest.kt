package com.example.parking.domain.parkingLot.external.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ParkingLotDtoTest {

    @Test
    fun `ParkingLotItem을 ParkingLotDto로 변환한다`() {
        // given
        // 외부 API에서 받아온 주차장 데이터 생성
        val item = ParkingApiDto.ParkingLotItem(
            pkltCd = "P001",
            pkltNm = "강남주차장",
            addr = "서울 강남구",
            tpkct = 100.0,
            lat = 37.4979,
            lot = 127.0276
        )

        // when
        // from() 메서드로 내부 DTO 변환
        val result = ParkingLotDto.from(item)

        // then
        // 필드가 정상적으로 매핑되었는지 검증
        assertThat(result.externalId).isEqualTo("P001")
        assertThat(result.name).isEqualTo("강남주차장")
        assertThat(result.address).isEqualTo("서울 강남구")

        // Double -> Int 변환 검증
        assertThat(result.totalSpot).isEqualTo(100)
    }

    @Test
    fun `tpkct가 null이면 totalSpot도 null이다`() {
        // given
        val item = ParkingApiDto.ParkingLotItem(
            pkltCd = "P002",
            pkltNm = "테스트주차장",
            addr = "서울 송파구",
            tpkct = null,
            lat = 37.4979,
            lot = 127.0276
        )

        // when
        val result = ParkingLotDto.from(item)

        // then
        // nullable 값이 안전하게 처리되는지 검증
        assertThat(result.totalSpot).isNull()
    }
}