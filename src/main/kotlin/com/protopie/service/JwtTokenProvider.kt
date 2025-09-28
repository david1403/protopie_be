package com.protopie.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*
import javax.crypto.spec.SecretKeySpec

@Component
class JwtTokenProvider(
    @Value("\${app.jwt.secret}") private val secretKey: String,
    @Value("\${app.jwt.expiration}") private val validityInMilliseconds: Long
) {
    private val key: Key = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")

    fun createToken(userId: Long): String {
        val claims = Jwts.claims().setSubject(userId.toString())

        val now = Date()
        val validity = Date(now.time + validityInMilliseconds)

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUserId(token: String): Long? {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .subject
            .toLongOrNull()
    }
}