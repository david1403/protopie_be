package com.protopie.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PasswordConverterTest {
    private lateinit var passwordConverter: PasswordConverter
    private val testSecretKey = "1234567890123456"

    @BeforeEach
    fun setUp() {
        passwordConverter = PasswordConverter(testSecretKey)
    }

    @Test
    fun `encrypt should return encrypted string`() {
        // given
        val plainText = "password123"

        // when
        val encrypted = passwordConverter.encrypt(plainText)

        // then
        assertNotNull(encrypted)
        assertNotEquals(plainText, encrypted)
        assertTrue(encrypted.isNotEmpty())
    }

    @Test
    fun `decrypt should return original plain text`() {
        // given
        val plainText = "password123"
        val encrypted = passwordConverter.encrypt(plainText)

        // when
        val decrypted = passwordConverter.decrypt(encrypted)

        // then
        assertEquals(plainText, decrypted)
    }

    @Test
    fun `encrypt same text twice should produce same result`() {
        // given
        val plainText = "password123"

        // when
        val encrypted1 = passwordConverter.encrypt(plainText)
        val encrypted2 = passwordConverter.encrypt(plainText)

        // then
        assertEquals(encrypted1, encrypted2)
    }

    @Test
    fun `encrypt different texts should produce different results`() {
        // given
        val plainText1 = "password123"
        val plainText2 = "password456"

        // when
        val encrypted1 = passwordConverter.encrypt(plainText1)
        val encrypted2 = passwordConverter.encrypt(plainText2)

        // then
        assertNotEquals(encrypted1, encrypted2)
    }

    @Test
    fun `encrypt and decrypt should work with special characters`() {
        // given
        val plainText = "p@ssw0rd!#$%"

        // when
        val encrypted = passwordConverter.encrypt(plainText)
        val decrypted = passwordConverter.decrypt(encrypted)

        // then
        assertEquals(plainText, decrypted)
    }

    @Test
    fun `decrypt with invalid encrypted string should throw exception`() {
        // given
        val invalidEncrypted = "invalid-encrypted-string"

        // when & then
        assertThrows<Exception> {
            passwordConverter.decrypt(invalidEncrypted)
        }
    }
}