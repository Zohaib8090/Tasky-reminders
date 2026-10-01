package com.example.data.backup

import android.util.Base64
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
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

    private fun chunkCipher(mode: Int, base: ByteArray, counter: Int, last: Boolean): Cipher {
        val iv = ByteBuffer.allocate(12).put(base).putInt(counter).array()
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, key, GCMParameterSpec(128, iv))
            updateAAD(byteArrayOf(if (last) 1 else 0))
        }
    }

    private fun readFully(input: InputStream, buf: ByteArray): Int {
        var total = 0
        while (total < buf.size) {
            val n = input.read(buf, total, buf.size - total)
            if (n < 0) break
            total += n
        }
        return total
    }

    private fun readInt(input: InputStream): Int {
        val b = ByteArray(4)
        return if (readFully(input, b) < 4) -1 else ByteBuffer.wrap(b).int
    }

    /** Encrypts a (possibly large) file in independently authenticated 64 KB chunks. Does not close [out]. */
    fun encryptStream(input: InputStream, out: OutputStream) {
        val base = ByteArray(8).also { SecureRandom().nextBytes(it) }
        out.write(base)
        var buf = ByteArray(CHUNK)
        var next = ByteArray(CHUNK)
        var len = readFully(input, buf)
        var counter = 0
        while (true) {
            val nextLen = if (len == CHUNK) readFully(input, next) else 0
            val last = nextLen == 0
            val ct = chunkCipher(Cipher.ENCRYPT_MODE, base, counter, last).doFinal(buf, 0, len)
            out.write(ByteBuffer.allocate(4).putInt(ct.size).array())
            out.write(ct)
            if (last) break
            val tmp = buf; buf = next; next = tmp
            len = nextLen
            counter++
        }
    }

    /** Reverse of [encryptStream]; throws if the password is wrong or the data was altered. */
    fun decryptStream(input: InputStream, out: OutputStream) {
        val base = ByteArray(8)
        if (readFully(input, base) < 8) throw IllegalArgumentException("Truncated data")
        var frameLen = readInt(input)
        var counter = 0
        while (frameLen >= 0) {
            val ct = ByteArray(frameLen)
            if (readFully(input, ct) < frameLen) throw IllegalArgumentException("Truncated data")
            val nextLen = readInt(input)
            val last = nextLen < 0
            out.write(chunkCipher(Cipher.DECRYPT_MODE, base, counter, last).doFinal(ct))
            frameLen = nextLen
            counter++
        }
    }

    companion object {
        private const val CHUNK = 64 * 1024
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
