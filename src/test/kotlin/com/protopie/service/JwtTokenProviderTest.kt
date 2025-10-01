package com.protopie.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JwtTokenProviderTest {
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private val testSecretKey = "test-secret-key-for-jwt-token-generation-needs-to-be-long-enough"
    private val validityInMilliseconds = 3600000L

    @BeforeEach
    fun setUp() {
        jwtTokenProvider = JwtTokenProvider(testSecretKey, validityInMilliseconds)
    }

    @Test
    fun `createToken should generate valid JWT token`() {
        // given
        val userId = 1L

        // when
        val token = jwtTokenProvider.createToken(userId)

        // then
        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }

    @Test
    fun `validateToken should return true for valid token`() {
        // given
        val userId = 1L
        val token = jwtTokenProvider.createToken(userId)

        // when
        val isValid = jwtTokenProvider.validateToken(token)

        // then
        assertTrue(isValid)
    }

    @Test
    fun `validateToken should return false for invalid token`() {
        // given
        val invalidToken = "invalid.token.string"

        // when
        val isValid = jwtTokenProvider.validateToken(invalidToken)

        // then
        assertFalse(isValid)
    }

    @Test
    fun `validateToken should return false for empty token`() {
        // given
        val emptyToken = ""

        // when
        val isValid = jwtTokenProvider.validateToken(emptyToken)

        // then
        assertFalse(isValid)
    }

    @Test
    fun `getUserId should extract correct user id from token`() {
        // given
        val userId = 123L
        val token = jwtTokenProvider.createToken(userId)

        // when
        val extractedUserId = jwtTokenProvider.getUserId(token)

        // then
        assertNotNull(extractedUserId)
        assertEquals(userId, extractedUserId)
    }

    @Test
    fun `getUserId should return null for invalid token`() {
        // given
        val invalidToken = "invalid.token.string"

        // when & then
        assertThrows(Exception::class.java) {
            jwtTokenProvider.getUserId(invalidToken)
        }
    }

    @Test
    fun `createToken should generate different tokens for different users`() {
        // given
        val userId1 = 1L
        val userId2 = 2L

        // when
        val token1 = jwtTokenProvider.createToken(userId1)
        val token2 = jwtTokenProvider.createToken(userId2)

        // then
        assertNotEquals(token1, token2)
    }
}