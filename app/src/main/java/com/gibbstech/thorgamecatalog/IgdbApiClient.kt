package com.gibbstech.thorgamecatalog

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneOffset

class IgdbApiClient {
    private var cachedAccessToken: String? = null
    private var accessTokenExpiresAtMillis: Long = 0

    suspend fun testConnection(config: ApiConfig) = withContext(Dispatchers.IO) {
        cachedAccessToken = null
        accessToken(config)
    }

    suspend fun fetchGames(
        config: ApiConfig,
        platform: Platform?,
        search: String,
        offset: Int,
        limit: Int = PAGE_SIZE,
    ): List<Game> = withContext(Dispatchers.IO) {
        require(config.isIgdbReady) { "Najprv nastav IGDB Client ID a Client Secret." }

        val token = accessToken(config)
        val platformIds = platform?.let { listOf(it.igdbId) }
            ?: Platform.entries.map { it.igdbId }
        val escapedSearch = search.trim()
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

        val query = buildString {
            append(
                "fields name,summary,first_release_date,platforms,genres.name," +
                    "involved_companies.company.name,involved_companies.developer," +
                    "involved_companies.publisher,cover.image_id,screenshots.image_id," +
                    "videos.video_id; ",
            )
            if (escapedSearch.isNotBlank()) append("search \"").append(escapedSearch).append("\"; ")
            append("where platforms = (")
                .append(platformIds.joinToString(","))
                .append(") & version_parent = null; ")
            if (escapedSearch.isBlank()) append("sort name asc; ")
            append("limit ").append(limit).append("; offset ").append(offset).append(";")
        }

        val response = request(
            url = GAMES_URL,
            method = "POST",
            headers = mapOf(
                "Client-ID" to config.igdbClientId.trim(),
                "Authorization" to "Bearer $token",
                "Accept" to "application/json",
                "Content-Type" to "text/plain",
            ),
            body = query,
        )

        if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            cachedAccessToken = null
            throw ApiException("IGDB prihlásenie vypršalo. Skús načítať katalóg znova.")
        }
        if (response.code !in 200..299) throw ApiException(errorMessage("IGDB", response))

        parseGames(JSONArray(response.body), platform)
    }

    private fun accessToken(config: ApiConfig): String {
        val now = System.currentTimeMillis()
        cachedAccessToken?.takeIf { now < accessTokenExpiresAtMillis }?.let { return it }

        val tokenUrl = "$TOKEN_URL?client_id=${encode(config.igdbClientId.trim())}" +
            "&client_secret=${encode(config.igdbClientSecret.trim())}" +
            "&grant_type=client_credentials"
        val response = request(tokenUrl, method = "POST")
        if (response.code !in 200..299) throw ApiException(errorMessage("Twitch/IGDB", response))

        val json = JSONObject(response.body)
        val token = json.optString("access_token")
        if (token.isBlank()) throw ApiException("IGDB nevrátil prístupový token.")
        val expiresInSeconds = json.optLong("expires_in", 3600)
        cachedAccessToken = token
        accessTokenExpiresAtMillis = now + (expiresInSeconds - 60).coerceAtLeast(60) * 1000
        return token
    }

    private fun parseGames(items: JSONArray, preferredPlatform: Platform?): List<Game> = buildList {
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val igdbId = item.optInt("id", -1)
            val title = item.optString("name")
            if (igdbId < 0 || title.isBlank()) continue

            val platformIds = item.optJSONArray("platforms").toIntList()
            val platform = Platform.fromIgdbIds(platformIds, preferredPlatform) ?: continue
            val coverId = item.optJSONObject("cover")?.optString("image_id").orEmpty()
            val screenshots = item.optJSONArray("screenshots").toObjectList()
                .mapNotNull { it.optString("image_id").takeIf(String::isNotBlank) }
                .take(8)
                .map { imageUrl(it, "t_screenshot_big") }
            val genres = item.optJSONArray("genres").toObjectList()
                .mapNotNull { it.optString("name").takeIf(String::isNotBlank) }
            val companies = item.optJSONArray("involved_companies").toObjectList()
            val developer = companies.firstOrNull { it.optBoolean("developer") }
                ?.optJSONObject("company")?.optString("name")?.takeIf(String::isNotBlank)
            val publisher = companies.firstOrNull { it.optBoolean("publisher") }
                ?.optJSONObject("company")?.optString("name")?.takeIf(String::isNotBlank)
            val youtubeVideoId = item.optJSONArray("videos").toObjectList()
                .firstNotNullOfOrNull { it.optString("video_id").takeIf(String::isNotBlank) }
            val releaseTimestamp = item.optLong("first_release_date", 0)
            val year = releaseTimestamp.takeIf { it > 0 }?.let {
                Instant.ofEpochSecond(it).atZone(ZoneOffset.UTC).year
            }

            add(
                Game(
                    id = "igdb-$igdbId-${platform.id}",
                    igdbId = igdbId,
                    title = title,
                    platform = platform,
                    year = year,
                    description = item.optString("summary").ifBlank {
                        "Pre túto hru zatiaľ nie je dostupný popis."
                    },
                    coverUrl = coverId.takeIf(String::isNotBlank)?.let {
                        imageUrl(it, "t_cover_big")
                    },
                    screenshotUrls = screenshots,
                    youtubeVideoId = youtubeVideoId,
                    genres = genres,
                    developer = developer,
                    publisher = publisher,
                ),
            )
        }
    }

    private fun imageUrl(imageId: String, size: String): String =
        "https://images.igdb.com/igdb/image/upload/$size/$imageId.jpg"

    private fun errorMessage(source: String, response: HttpResponse): String {
        val message = runCatching {
            val json = if (response.body.trim().startsWith("[")) {
                JSONArray(response.body).optJSONObject(0)
            } else {
                JSONObject(response.body)
            }
            json?.optString("message")?.takeIf(String::isNotBlank)
                ?: json?.optString("error")?.takeIf(String::isNotBlank)
        }.getOrNull()
        return "$source chyba ${response.code}${message?.let { ": $it" }.orEmpty()}"
    }

    companion object {
        const val PAGE_SIZE = 30
        private const val TOKEN_URL = "https://id.twitch.tv/oauth2/token"
        private const val GAMES_URL = "https://api.igdb.com/v4/games"
    }
}

internal data class HttpResponse(val code: Int, val body: String)

internal fun request(
    url: String,
    method: String = "GET",
    headers: Map<String, String> = emptyMap(),
    body: String? = null,
): HttpResponse {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 15_000
        readTimeout = 25_000
        useCaches = false
        headers.forEach { (name, value) -> setRequestProperty(name, value) }
        if (body != null) doOutput = true
    }
    return try {
        if (body != null) {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        HttpResponse(code, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
    } finally {
        connection.disconnect()
    }
}

internal fun encode(value: String): String =
    URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

internal class ApiException(message: String) : Exception(message)

private fun JSONArray?.toIntList(): List<Int> = buildList {
    val array = this@toIntList ?: return@buildList
    for (index in 0 until array.length()) add(array.optInt(index))
}

internal fun JSONArray?.toObjectList(): List<JSONObject> = buildList {
    val array = this@toObjectList ?: return@buildList
    for (index in 0 until array.length()) array.optJSONObject(index)?.let(::add)
}
