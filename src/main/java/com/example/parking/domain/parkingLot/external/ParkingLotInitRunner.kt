package com.example.parking.domain.parkingLot.external

import com.example.parking.domain.parkingLot.repository.ParkingLotRepository
import com.example.parking.domain.parkingLot.service.ParkingLotSyncService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class ParkingLotInitRunner(
    private val parkingLotSyncService: ParkingLotSyncService,
    private val parkingLotRepository: ParkingLotRepository
) : ApplicationRunner {

    companion object {
        private val log = LoggerFactory.getLogger(ParkingLotInitRunner::class.java)
    }

    // [CUS-01] 서버 시작 시 외부 주차장 데이터 동기화
    // 데이터가 비어 있을 때만 초기 적재
    override fun run(args: ApplicationArguments) {
        if (parkingLotRepository.count() == 0L) {
            log.info("주차장 초기 데이터 동기화를 시작합니다.")

            parkingLotSyncService.syncParkingLots()

            log.info("주차장 초기 데이터 동기화가 완료되었습니다.")
        } else {
            log.info("주차장 데이터가 이미 존재하여 초기 동기화를 생략합니다.")
        }
    }
}
