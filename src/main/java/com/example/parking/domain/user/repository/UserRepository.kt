package com.example.parking.domain.user.repository;

import com.example.parking.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.example.parking.domain.user.entity.UserRole;


interface UserRepository : JpaRepository<User, Long> {
    fun existsByEmail(email: String): Boolean

    fun existsByPlateNumber(plateNumber: String): Boolean

    fun findByEmail(email: String): Optional<User>

    fun existsByPlateNumberAndIdNot(plateNumber: String, id: Long): Boolean

    fun findByRole(role: UserRole, pageable: Pageable): Page<User>

    fun findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        nameKeyword: String,
        emailKeyword: String,
        pageable: Pageable
    ): Page<User>

    fun findByRoleAndNameContainingIgnoreCaseOrRoleAndEmailContainingIgnoreCase(
        roleForName: UserRole,
        nameKeyword: String,
        roleForEmail: UserRole,
        emailKeyword: String,
        pageable: Pageable
    ): Page<User>
}
