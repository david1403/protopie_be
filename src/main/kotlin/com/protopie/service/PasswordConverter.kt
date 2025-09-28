package com.protopie.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@Component
class PasswordConverter(
    @Value("\${app.postgre.aes-key}") private val secretKey: String,
) {
    companion object {
        const val ENCRYPT_ALGORITHM = "AES"
    }

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(ENCRYPT_ALGORITHM)
        val keySpec = SecretKeySpec(secretKey.toByteArray(), ENCRYPT_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(encrypted)
    }

    fun decrypt(encrypted: String): String {
        val cipher = Cipher.getInstance(ENCRYPT_ALGORITHM)
        val keySpec = SecretKeySpec(secretKey.toByteArray(), ENCRYPT_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        val decoded = Base64.getDecoder().decode(encrypted)
        val decrypted = cipher.doFinal(decoded)
        return String(decrypted, Charsets.UTF_8)
    }
}