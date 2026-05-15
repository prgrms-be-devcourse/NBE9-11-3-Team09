// CustomUserDetails.kt
package com.example.parking.global.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class CustomUserDetails(
        val userId: Long,
        val userEmail: String,
        val role: String
) : UserDetails {

    // [CUS-08] 로그인 - 계정이 활성화되어 있는지 여부
    override fun getAuthorities(): Collection<GrantedAuthority> =
    listOf(SimpleGrantedAuthority("ROLE_$role"))

    override fun getPassword(): String? = null

    override fun getUsername(): String = userEmail

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}