package com.example.parking.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI {
        val securityScheme = SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .`in`(SecurityScheme.In.HEADER)
            .name("Authorization")

        val securityRequirement = SecurityRequirement().addList("bearerAuth")

        return OpenAPI()
            .info(
                Info()
                    .title("ParkEasy API")
                    .description("강남구 공영주차장 예약 서비스 API 문서")
                    .version("v1.0.0")
            )
            .components(Components().addSecuritySchemes("bearerAuth", securityScheme))
            .addSecurityItem(securityRequirement)
    }

    @Bean
    fun userApi(): GroupedOpenApi = GroupedOpenApi.builder()
        .group("user")
        .displayName("고객용 API")
        .pathsToMatch(
            "/api/users/**",
            "/api/parking-lots/**",
            "/api/parking-spots/**",
            "/api/reservations/**",
            "/api/payments/**"
        )
        .build()

    @Bean
    fun adminApi(): GroupedOpenApi = GroupedOpenApi.builder()
        .group("admin")
        .displayName("관리자용 API")
        .pathsToMatch("/api/admin/**")
        .build()
}