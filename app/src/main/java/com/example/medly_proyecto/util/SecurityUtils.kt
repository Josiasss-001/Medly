package com.example.medly_proyecto.util

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    // Nota: En una app real, esta clave no debería estar hardcodeada. 
    // Se debería usar Android KeyStore.
    private val key = SecretKeySpec("12345678901234567890123456789012".toByteArray(), "AES")
    private val iv = IvParameterSpec("1234567890123456".toByteArray())

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val encrypted = cipher.doFinal(value.toByteArray())
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    fun decrypt(encrypted: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        val decodedBytes = Base64.decode(encrypted, Base64.DEFAULT)
        val decrypted = cipher.doFinal(decodedBytes)
        return String(decrypted)
    }
}
