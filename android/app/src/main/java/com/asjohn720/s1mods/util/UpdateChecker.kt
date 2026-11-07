package com.asjohn720.s1mods.util

import com.asjohn720.s1mods.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppRelease(
    val tag: String,        // e.g. "v1.3"
    val pageUrl: String,    // release html page
    val apkUrl: String?     // direct .apk asset, if attached
)

/**
 * Checks the project's latest GitHub Release and compares it to this build.
 * Uses the public API (no token); one call on app start. Network only.
 */
object UpdateChecker {

    suspend fun fetchLatest(): AppRelease? = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/${BuildConfig.GIT_OWNER}/${BuildConfig.GIT_REPO}/releases/latest"
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "S1Mods")
            }
            if (conn.responseCode !in 200..299) return@withContext null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })

            val tag = json.optString("tag_name").ifBlank { return@withContext null }
            val pageUrl = json.optString("html_url")
            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val name = assets.getJSONObject(i).optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = assets.getJSONObject(i).optString("browser_download_url")
                        break
                    }
                }
            }
            AppRelease(tag, pageUrl, apkUrl)
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    /** True if [tag] names a version higher than the running build. */
    fun isNewer(tag: String, current: String = BuildConfig.VERSION_NAME): Boolean {
        val a = numbers(tag)
        val b = numbers(current)
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    // "v1.3.0-beta" -> [1, 3, 0]; drops a leading 'v' and any -suffix.
    private fun numbers(version: String): List<Int> =
        version.trim().removePrefix("v").removePrefix("V")
            .substringBefore("-")
            .split(".")
            .mapNotNull { it.trim().toIntOrNull() }
}
