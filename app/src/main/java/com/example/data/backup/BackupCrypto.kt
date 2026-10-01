package com.example.data.backup

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-based sealing for locked items inside an exported backup.
 * Device Keystore keys can't leave the phone, so exports use a key derived from
 * a password the user chooses at export time.
 */
class BackupSealer private constructor(
    val salt: ByteArray,
    val iterations: Int,
    val kdf: String,
    private val key: SecretKeySpec
) {
    fun seal(plain: String): String {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        return Base64.encodeToString(iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    /** Returns null when the password is wrong or the data was tampered with. */
    fun open(blob: String): String? = try {
        val raw = Base64.decode(blob, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, raw, 0, 12))
        String(cipher.doFinal(raw, 12, raw.size - 12), Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }

    companion object {
        private const val ITERATIONS = 200_000
        private const val KDF_SHA256 = "PBKDF2WithHmacSHA256"
        private const val KDF_SHA1 = "PBKDF2WithHmacSHA1"

        fun create(password: CharArray): BackupSealer {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            // SHA-256 needs API 26+; older phones fall back (recorded in the backup)
            val kdf = try {
                SecretKeyFactory.getInstance(KDF_SHA256)
                KDF_SHA256
            } catch (e: Exception) {
                KDF_SHA1
            }
            return derive(password, salt, ITERATIONS, kdf)
        }

        fun derive(password: CharArray, salt: ByteArray, iterations: Int, kdf: String): BackupSealer {
            val spec = PBEKeySpec(password, salt, iterations, 256)
            val bytes = SecretKeyFactory.getInstance(kdf).generateSecret(spec).encoded
            return BackupSealer(salt, iterations, kdf, SecretKeySpec(bytes, "AES"))
        }
    }
}
