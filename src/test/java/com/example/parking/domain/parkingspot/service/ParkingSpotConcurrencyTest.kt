package com.example.parking.domain.parkingspot.service

import com.example.parking.domain.parkingLot.entity.ParkingLot
import com.example.parking.domain.parkingLot.repository.ParkingLotRepository
import com.example.parking.domain.parkingspot.entity.ParkingSpot
import com.example.parking.domain.parkingspot.entity.SpotType
import com.example.parking.domain.parkingspot.repository.ParkingSpotRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// [CUS-11] 동시성 테스트
@SpringBootTest
class ParkingSpotConcurrencyTest {

    @Autowired
    private lateinit var parkingSpotRepository: ParkingSpotRepository
    @Autowired
    private lateinit var parkingLotRepository: ParkingLotRepository
    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    private var savedSpotId: Long? = null

    @BeforeEach
    fun setUp() {
        savedSpotId = transactionTemplate.execute {
            val parkingLot = ParkingLot.of(
                externalId = "test-lot-${UUID.randomUUID()}",
                name = "테스트 주차장",
                address = "서울시 테스트구",
                totalSpot = 10,
                location = null
            )
            val savedLot = parkingLotRepository.save(parkingLot)
            val spot = ParkingSpot(savedLot, "A-01", SpotType.SMALL)
            parkingSpotRepository.save(spot).id
        }
    }

    @Test
    @DisplayName("동시에 두 요청이 같은 자리에 tryReserve를 호출하면 하나만 성공한다")
    fun tryReserve_concurrency() {
        val threadCount = 2
        val executor = Executors.newFixedThreadPool(threadCount)
        val results = Collections.synchronizedList(mutableListOf<Int>())
        val latch = CountDownLatch(threadCount)

        repeat(threadCount) {
            executor.submit {
                try {
                    println("Thread accessing ID: $savedSpotId")
                    val result = transactionTemplate.execute {
                        parkingSpotRepository.tryReserve(savedSpotId!!, LocalDateTime.now())
                    }!!
                    results.add(result)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)

        assertThat(results)
            .`as`("결과 리스트가 비어있습니다. DB 연결이나 데이터 생성 확인 필요")
            .isNotEmpty()
        assertThat(results).containsExactlyInAnyOrder(0, 1)
    }

    @AfterEach
    fun tearDown() {
        savedSpotId?.let { id ->
            parkingSpotRepository.findById(id).ifPresent { spot ->
                val lot = spot.parkingLot
                parkingSpotRepository.delete(spot)
                parkingLotRepository.delete(lot)
            }
        }
    }

    @Test
    @DisplayName("강남구 트래픽 - 100명이 동시에 같은 자리를 선점 시도하면 단 1명만 성공한다")
    fun tryReserve_gangnamTraffic() {
        // given - 강남 주차장 자리 1개 생성
        val spotId = transactionTemplate.execute {
            val gangnamLot = ParkingLot.of(
                externalId = "gangnam-lot-${UUID.randomUUID()}",
                name = "강남역 공영주차장",
                address = "서울시 강남구 강남대로",
                totalSpot = 100,
                location = null
            )
            val savedLot = parkingLotRepository.save(gangnamLot)
            val spot = ParkingSpot(savedLot, "G-01", SpotType.SMALL)
            parkingSpotRepository.save(spot).id
        }!!

        val threadCount = 100
        val executor = Executors.newFixedThreadPool(threadCount)
        val results = Collections.synchronizedList(mutableListOf<Int>())
        val ready = CountDownLatch(threadCount)
        val start = CountDownLatch(1)
        val done  = CountDownLatch(threadCount)

        // when - 100개 스레드가 동시에 출발
        repeat(threadCount) {
            executor.submit {
                try {
                    ready.countDown()
                    start.await()
                    val result = transactionTemplate.execute {
                        parkingSpotRepository.tryReserve(spotId, LocalDateTime.now())
                    }!!
                    results.add(result)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    done.countDown()
                }
            }
        }

        ready.await()
        start.countDown()
        done.await(30, TimeUnit.SECONDS)

        // then
        val successCount = results.count { it == 1 }.toLong()
        val failCount    = results.count { it == 0 }.toLong()

        println("✅ 성공: ${successCount}건, ❌ 실패: ${failCount}건")

        assertThat(successCount).isEqualTo(1)
        assertThat(failCount).isEqualTo(99)
        assertThat(results).hasSize(100)
    }
}