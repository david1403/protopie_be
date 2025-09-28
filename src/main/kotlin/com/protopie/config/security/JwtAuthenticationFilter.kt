package com.protopie.config.security

import com.protopie.repository.UserRepository
import com.protopie.service.JwtTokenProvider
import com.protopie.service.UserService
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationFilter(
    val jwtTokenProvider: JwtTokenProvider,
    val userService: UserService,
): WebFilter {
    companion object {
        const val DEFAULT_ROLE_PREFIX = "ROLE_"
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return mono {
            val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
            val token = extractToken(authHeader)

            if (token != null && jwtTokenProvider.validateToken(token)) {
                try {
                    val userId = jwtTokenProvider.getUserId(token)
                    val user = userService.getUserByIdOrThrow(userId!!)
                    val authorities = listOf(SimpleGrantedAuthority(DEFAULT_ROLE_PREFIX + user.role.name))
                    val authentication = UsernamePasswordAuthenticationToken(user, token, authorities)

                    return@mono chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))

                } catch (e: Exception) {
                    chain.filter(exchange)
                }
            }
            chain.filter(exchange)
        }.flatMap { it }
    }

    fun extractToken(authHeader: String?): String? {
        return authHeader?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
    }
}