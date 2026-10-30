package com.example.dispensador_iot

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.util.Base64

object SecurityUtils {

    fun hashPassword(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, 1000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    fun saltToString(salt: ByteArray): String {
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun stringToSalt(saltStr: String): ByteArray {
        return Base64.decode(saltStr, Base64.NO_WRAP)
    }
}