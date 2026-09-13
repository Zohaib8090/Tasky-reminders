package com.example.util

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

object AudioHelper {

    private const val TAG = "AudioHelper"

    /**
     * Generates a 16-bit 44.1kHz PCM WAV voice memo chime file.
     * Guarantees audible, crystal clear playback even if the emulator
     * has no physical microphone or audio input hardware.
     */
    fun createSyntheticVoiceMemoWav(file: File, durationSeconds: Int = 3) {
        val sampleRate = 44100
        val totalSamples = sampleRate * durationSeconds
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * (bitsPerSample / 8)
        val dataSize = totalSamples * (bitsPerSample / 8)
        val totalFileSize = 36 + dataSize

        FileOutputStream(file).use { out ->
            // RIFF header
            out.write("RIFF".toByteArray())
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(totalFileSize).array())
            out.write("WAVE".toByteArray())

            // fmt sub-chunk
            out.write("fmt ".toByteArray())
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(16).array()) // Subchunk1Size (16 for PCM)
            out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(1).array()) // AudioFormat (1 for PCM)
            out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(numChannels.toShort()).array())
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleRate).array())
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(byteRate).array())
            out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((numChannels * (bitsPerSample / 8)).toShort()).array())
            out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(bitsPerSample.toShort()).array())

            // data sub-chunk
            out.write("data".toByteArray())
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dataSize).array())

            // Write harmonic musical tones (Voice memo chime: E4 -> G4 -> B4 -> E5 pleasant chime)
            val notes = listOf(329.63, 392.00, 493.88, 659.25)
            val samplesPerNote = totalSamples / notes.size

            val buffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until totalSamples) {
                val noteIndex = (i / samplesPerNote).coerceIn(0, notes.size - 1)
                val freq = notes[noteIndex]
                val envelope = sin(Math.PI * ((i % samplesPerNote).toDouble() / samplesPerNote))
                val wave = sin(2.0 * Math.PI * freq * (i.toDouble() / sampleRate))
                val sampleValue = (wave * envelope * 24000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                buffer.putShort(sampleValue)
            }
            out.write(buffer.array())
        }
    }

    /**
     * Attempts to start MediaRecorder. If hardware is unavailable or in emulator,
     * returns null and callers fall back gracefully to synthetic recording.
     */
    fun createMediaRecorder(context: Context, outputFile: File): MediaRecorder? {
        return try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Hardware audio recorder unavailable, using synthetic fallback: ${e.message}")
            null
        }
    }

    /**
     * Get or create a sample audio file for demo previews.
     */
    fun getOrCreateSampleAudioFile(context: Context): File {
        val file = File(context.cacheDir, "sample_voice_memo.wav")
        if (!file.exists() || file.length() == 0L) {
            createSyntheticVoiceMemoWav(file, durationSeconds = 4)
        }
        return file
    }
}
