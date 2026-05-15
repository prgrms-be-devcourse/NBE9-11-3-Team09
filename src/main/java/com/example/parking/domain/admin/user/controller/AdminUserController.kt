package com.example.parking.domain.admin.user.service

import com.example.parking.domain.admin.user.dto.AdminUserResDto
import com.example.parking.domain.user.entity.UserRole
import com.example.parking.domain.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminUserService(
    private val userRepository: UserRepository
) {
    fun getAdminUsers(keyword: String?, pageable: Pageable): Page<AdminUserResDto> {
        if (keyword.isNullOrBlank()) {
            return userRepository.findByRole(UserRole.USER, pageable)
                .map(AdminUserResDto::from)
        }

        return userRepository.findByRoleAndNameContainingIgnoreCaseOrRoleAndEmailContainingIgnoreCase(
            UserRole.USER,
            keyword,
            UserRole.USER,
            keyword,
            pageable
        ).map(AdminUserResDto::from)
    }
}
