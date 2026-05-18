package com.example.parking

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@EnableCaching
@EnableScheduling
@EnableJpaAuditing
@SpringBootApplication
@ComponentScan(basePackages = ["com.example.parking"])
class ParkingApplication

fun main(args: Array<String>) {
    runApplication<ParkingApplication>(*args)
}