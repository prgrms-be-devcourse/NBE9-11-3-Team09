package com.example.parking.domain.reservation.repository

import com.example.parking.domain.reservation.entity.Reservation
import org.springframework.data.jpa.repository.JpaRepository

interface ReservationRepository : JpaRepository<Reservation, Long>, ReservationRepositoryCustom