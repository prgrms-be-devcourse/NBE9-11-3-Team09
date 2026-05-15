package com.example.parking.global.security

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
class JwtFilter(
    private val jwtUtil: JwtUtil
) : OncePerRequestFilter() {

    // [CUS-08] JWT 인증 필터
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = resolveToken(request)

        try {
            token?.let {
                jwtUtil.validateToken(it)

                // refresh token은 인증에 사용하지 않고 access token만 인증에 사용
                if (jwtUtil.getTokenType(it) == "access") {
                    val userId = jwtUtil.getUserId(it)
                    val userEmail = jwtUtil.getUserEmail(it)
                    val role = jwtUtil.getRole(it)

                    val userDetails = CustomUserDetails(
                        userId,
                        userEmail,
                        role
                    )

                    val authentication = UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.authorities
                    )

                    SecurityContextHolder.getContext().authentication = authentication
                }
            }

            filterChain.doFilter(request, response)

        } catch (e: ExpiredJwtException) {
            writeErrorResponse(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "401-1",
                "Access token이 만료되었습니다."
            )

        } catch (e: JwtException) {
            writeErrorResponse(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "401-2",
                "유효하지 않은 토큰입니다."
            )

        } catch (e: IllegalArgumentException) {
            writeErrorResponse(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "401-2",
                "유효하지 않은 토큰입니다."
            )
        }
    }

    private fun resolveToken(request: HttpServletRequest): String? {

        // 1. Authorization 헤더 확인
        val bearerToken = request.getHeader("Authorization")

        if (
            StringUtils.hasText(bearerToken) &&
            bearerToken.startsWith("Bearer ")
        ) {
            return bearerToken.substring(7)
        }

        // 2. SSE 지원용 query parameter 확인
        val queryToken = request.getParameter("token")

        if (StringUtils.hasText(queryToken)) {
            return queryToken
        }

        return null
    }

    // JWT 예외 발생 시 JSON 형태의 401 응답 반환
    @Throws(IOException::class)
    private fun writeErrorResponse(
        response: HttpServletResponse,
        status: Int,
        resultCode: String,
        message: String
    ) {
        response.status = status
        response.characterEncoding = "UTF-8"
        response.contentType = "application/json;charset=UTF-8"

        val body = """
            {
              "msg": "$message",
              "resultCode": "$resultCode",
              "data": null
            }
        """.trimIndent()

        response.writer.write(body)
    }
}
