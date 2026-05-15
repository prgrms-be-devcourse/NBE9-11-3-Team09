package com.example.parking.domain.parkingLot.controller

import com.example.parking.domain.parkingLot.dto.ParkingLotResDto
import com.example.parking.domain.parkingLot.service.ParkingLotService
import com.example.parking.global.response.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/parking-lots")
@Tag(name = "주차장", description = "주차장 관련 API")
class ParkingLotController(
    private val parkingLotService: ParkingLotService
) {

    // [CUS-01] 주차장 목록 조회
    @Operation(
        summary = "주차장 목록 조회",
        description = "전체 주차장 목록을 조회하거나 동 이름으로 검색합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 완료"),
        ApiResponse(responseCode = "502", description = "외부 주차장 API 호출 실패")
    )
    @GetMapping
    fun getParkingLots(
        @RequestParam(required = false)
        dong: String?
    ): ResponseEntity<RsData<List<ParkingLotResDto>>> {

        val data = parkingLotService.findAll(dong)

        return ResponseEntity.ok(
            RsData(
                "주차장 목록 조회가 완료되었습니다.",
                "200-1",
                data
            )
        )
    }

    // [CUS-01] 주차장 상세 조회
    @Operation(
        summary = "주차장 상세 조회",
        description = "주차장 ID로 특정 주차장의 상세 정보를 조회합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 완료"),
        ApiResponse(responseCode = "400", description = "존재하지 않는 주차장")
    )
    @GetMapping("/{id}")
    fun getParkingLot(
        @PathVariable id: Long
    ): ResponseEntity<RsData<ParkingLotResDto>> {

        val data = parkingLotService.findById(id)

        return ResponseEntity.ok(
            RsData(
                "주차장 상세 조회가 완료되었습니다.",
                "200-2",
                data
            )
        )
    }
}