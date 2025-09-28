package com.protopie.config.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange { exchanges ->
                exchanges
                    .anyExchange().permitAll()
            }
            .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint { exchange, _ ->
                        val response = exchange.response
                        response.statusCode = HttpStatus.UNAUTHORIZED
                        response.headers.add("Content-Type", "application/json")
                        val body = """{"error":"UNAUTHORIZED","message":"Unauthorized"}"""
                        val dataBuffer = response.bufferFactory().wrap(body.toByteArray())
                        response.writeWith(Mono.just(dataBuffer))
                    }
                    .accessDeniedHandler { exchange, _ ->
                        val response = exchange.response
                        response.statusCode = HttpStatus.FORBIDDEN
                        response.headers.add("Content-Type", "application/json")
                        val body = """{"error":"FORBIDDEN","message":"Forbidden"}"""
                        val dataBuffer = response.bufferFactory().wrap(body.toByteArray())
                        response.writeWith(Mono.just(dataBuffer))
                    }
            }
            .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("*")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600L
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}