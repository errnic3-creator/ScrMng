package com.example.data.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object SecurityHelper {
    private const val ALGORITHM = "SHA-256"

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance(ALGORITHM)
        md.update(Base64.getDecoder().decode(salt))
        val hashedBytes = md.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashedBytes)
    }

    fun verifyPin(enteredPin: String, storedHash: String, storedSalt: String): Boolean {
        if (storedHash.isEmpty() || storedSalt.isEmpty()) return false
        val computed = hashPin(enteredPin, storedSalt)
        return computed == storedHash
    }
}
