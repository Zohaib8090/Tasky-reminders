package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UpdateInfo(val versionName: String, val notes: String, val url: String)

sealed class UpdateCheckResult {
    object UpToDate : UpdateCheckResult()
    data class Available(val info: UpdateInfo) : UpdateCheckResult()
    data class Failed(val message: String) : UpdateCheckResult()
}

/** Looks up the newest GitHub release; nothing about the user or their data is sent. */
object UpdateChecker {
    private const val REPO = "Zohaib8090/Tasky-reminders"
    private const val LATEST_URL = "https://api.github.com/repos/$REPO/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun check(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(LATEST_URL)
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code == 404) return@withContext UpdateCheckResult.UpToDate // no releases yet
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Failed("Update server returned ${response.code}")
                }
                val json = JSONObject(response.body?.string().orEmpty())
                val tag = json.optString("tag_name")
                val url = json.optString("html_url")
                if (tag.isBlank() || !url.startsWith("https://github.com/")) {
                    return@withContext UpdateCheckResult.Failed("Unexpected response from update server")
                }
                if (isNewer(tag, currentVersion)) {
                    UpdateCheckResult.Available(UpdateInfo(tag.removePrefix("v"), json.optString("body"), url))
                } else {
                    UpdateCheckResult.UpToDate
                }
            }
        } catch (e: Exception) {
            UpdateCheckResult.Failed("Couldn't check for updates. Check your connection.")
        }
    }

    /** Compares dotted version numbers such as "1.2.0" ("v" prefix and "-suffix" are ignored). */
    fun isNewer(latest: String, current: String): Boolean {
        fun parts(v: String) = v.removePrefix("v").substringBefore('-').split('.')
            .map { it.toIntOrNull() ?: 0 }
        val a = parts(latest)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
