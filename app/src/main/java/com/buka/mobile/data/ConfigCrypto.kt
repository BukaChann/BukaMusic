package com.buka.mobile.data

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object ConfigCrypto {
    private const val PREFIX = "BUKAENC1\n"
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun isEncrypted(raw: String): Boolean = raw.startsWith(PREFIX)

    fun encrypt(plain: String, password: String): String {
        require(password.isNotBlank()) { "密码不能为空" }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val key = derive(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val payload = salt + iv + encrypted
        return PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(raw: String, password: String): String {
        require(isEncrypted(raw)) { "配置文件未加密" }
        require(password.isNotBlank()) { "该配置需要密码" }
        val payload = Base64.decode(raw.removePrefix(PREFIX).trim(), Base64.DEFAULT)
        require(payload.size > 28) { "加密配置损坏" }
        val salt = payload.copyOfRange(0, 16)
        val iv = payload.copyOfRange(16, 28)
        val encrypted = payload.copyOfRange(28, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, derive(password, salt), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private fun derive(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }
}
