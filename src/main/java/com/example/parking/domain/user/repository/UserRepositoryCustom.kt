package com.example.parking.domain.user.repository

import com.example.parking.domain.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserRepositoryCustom {

    fun searchAdminUsers(keyword: String?, pageable: Pageable): Page<User>
}