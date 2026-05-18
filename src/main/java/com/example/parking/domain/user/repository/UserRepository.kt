package com.example.parking.domain.user.repository

import com.example.parking.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional


interface UserRepository : JpaRepository<User, Long>, UserRepositoryCustom {
    fun existsByEmail(email: String): Boolean

    fun existsByPlateNumber(plateNumber: String): Boolean

    fun findByEmail(email: String): Optional<User>

    fun existsByPlateNumberAndIdNot(plateNumber: String, id: Long): Boolean
}
