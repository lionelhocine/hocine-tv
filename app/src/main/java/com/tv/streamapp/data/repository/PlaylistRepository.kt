package com.tv.streamapp.data.repository

import android.content.Context
import android.util.Log
import com.tv.streamapp.Config
import com.tv.streamapp.data.model.BrandingConfig
import com.tv.streamapp.data.model.CategoryGroup
import com.tv.streamapp.data.model.PlaylistResponse
import com.tv.streamapp.data.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class PlaylistRepository(private val context: Context) {
    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(Config.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(Config.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val cacheFile: File by lazy { File(context.filesDir, Config.CACHE_PLAYLIST_FILE) }

    suspend fun loadPlaylist(forceRemote: Boolean = false): Result<PlaylistResult> = withContext(Dispatchers.IO) {
        // 1. Remote download
        try {
            val responseText = fetchRemoteString(Config.PLAYLIST_URL)
            if (responseText.isNotBlank()) {
                val parsed = parsePlaylistContent(responseText)
                if (parsed.categories.isNotEmpty()) {
                    saveToDiskCache(responseText)
                    return@withContext Result.success(PlaylistResult(parsed.branding, parsed.branding?.welcomeMessage ?: parsed.message, parsed.categories, DataSource.REMOTE))
                }
            }
        } catch (e: Exception) {
            Log.w("PlaylistRepository", "Remote failed: ${e.message}. Using cache...")
        }

        // 2. Disk cache fallback
        try {
            if (cacheFile.exists() && cacheFile.length() > 0) {
                val cached = cacheFile.readText(Charsets.UTF_8)
                val parsed = parsePlaylistContent(cached)
                if (parsed.categories.isNotEmpty()) {
                    return@withContext Result.success(PlaylistResult(parsed.branding, parsed.branding?.welcomeMessage ?: parsed.message ?: "Offline Cache Mode", parsed.categories, DataSource.CACHE))
                }
            }
        } catch (e: Exception) {}

        // 3. Bundled asset fallback
        try {
            context.assets.open("channels.json").use { stream ->
                val asset = InputStreamReader(stream, Charsets.UTF_8).readText()
                val parsed = parsePlaylistContent(asset)
                return@withContext Result.success(PlaylistResult(parsed.branding, parsed.branding?.welcomeMessage ?: parsed.message, parsed.categories, DataSource.ASSET_FALLBACK))
            }
        } catch (e: Exception) {}

        Result.failure(Exception("Could not load channels playlist from network or cache"))
    }

    private fun fetchRemoteString(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", Config.PLAYER_USER_AGENT).build()
        okHttpClient.newCall(request).execute().use { return it.body?.string() ?: "" }
    }

    private fun parsePlaylistContent(content: String): PlaylistResponse {
        val trimmed = content.trim()
        return if (M3uParser.isM3u(trimmed)) M3uParser.parse(trimmed)
        else jsonParser.decodeFromString(PlaylistResponse.serializer(), trimmed)
    }

    private fun saveToDiskCache(content: String) {
        try { cacheFile.writeText(content, Charsets.UTF_8) } catch (e: Exception) {}
    }
}

enum class DataSource { REMOTE, CACHE, ASSET_FALLBACK }
data class PlaylistResult(val branding: BrandingConfig?, val announcement: String?, val categories: List<CategoryGroup>, val source: DataSource)