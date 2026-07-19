package com.gibbstech.thorgamecatalog

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.random.Random

class IgdbApiClient {
    private var cachedAccessToken: String? = null
    private var accessTokenExpiresAtMillis: Long = 0
    private val popularityIdsCache = mutableMapOf<PopularityCacheKey, List<Int>>()

    suspend fun testConnection(config: ApiConfig) = withContext(Dispatchers.IO) {
        cachedAccessToken = null
        accessToken(config)
    }

    suspend fun fetchGames(
        config: ApiConfig,
        platform: Platform?,
        search: String,
        offset: Int,
        sort: GameSort,
        nativeOnly: Boolean,
        limit: Int = PAGE_SIZE,
    ): List<Game> = withContext(Dispatchers.IO) {
        require(config.isIgdbReady) { "Najprv nastav IGDB Client ID a Client Secret." }

        val token = accessToken(config)
        val platformIds = platform?.let { listOf(it.igdbId) }
            ?: Platform.entries.map { it.igdbId }
        val escapedSearch = search.trim()
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("*", "")

        if (sort == GameSort.POPULARITY) {
            return@withContext fetchPopularGames(
                config = config,
                token = token,
                platform = platform,
                platformIds = platformIds,
                escapedSearch = escapedSearch,
                offset = offset,
                limit = limit,
                nativeOnly = nativeOnly,
            )
        }

        val query = buildString {
            append(GAME_FIELDS)
            append("where ")
                .append(gameFilter(platformIds, sort, escapedSearch, nativeOnly))
                .append("; ")
            append("sort ").append(sort.apiSort).append("; ")
            append("limit ").append(limit).append("; offset ").append(offset).append(";")
        }

        val response = igdbRequest(config, token, GAMES_URL, query)

        parseGames(JSONArray(response.body), platform)
    }

    suspend fun fetchSurpriseGames(
        config: ApiConfig,
        platform: Platform?,
        recentGameIds: Set<Int>,
        nativeOnly: Boolean,
        count: Int = SURPRISE_COUNT,
    ): List<Game> = withContext(Dispatchers.IO) {
        require(config.isIgdbReady) { "Najprv nastav IGDB Client ID a Client Secret." }

        val token = accessToken(config)
        val platformIds = platform?.let { listOf(it.igdbId) }
            ?: Platform.entries.map { it.igdbId }
        val filter = gameFilter(platformIds, nativeOnly = nativeOnly) +
            " & cover != null & total_rating_count >= $MIN_SURPRISE_RATING_COUNT"
        val countResponse = igdbRequest(
            config = config,
            token = token,
            url = GAMES_COUNT_URL,
            body = "where $filter;",
        )
        val available = JSONObject(countResponse.body).optInt("count", 0)
        if (available == 0) return@withContext emptyList()

        val poolSize = minOf(SURPRISE_POOL_SIZE, available)
        val maxOffset = (available - poolSize).coerceAtLeast(0)
        val randomOffset = if (maxOffset == 0) 0 else Random.nextInt(maxOffset + 1)
        val query = buildString {
            append(GAME_FIELDS)
            append("where ").append(filter).append("; ")
            append("sort name asc; limit ").append(poolSize)
            append("; offset ").append(randomOffset).append(";")
        }
        val response = igdbRequest(config, token, GAMES_URL, query)
        selectSurpriseGames(
            candidates = parseGames(JSONArray(response.body), platform),
            recentGameIds = recentGameIds,
            preferDifferentPlatforms = platform == null,
            count = count,
        )
    }

    private fun fetchPopularGames(
        config: ApiConfig,
        token: String,
        platform: Platform?,
        platformIds: List<Int>,
        escapedSearch: String,
        offset: Int,
        limit: Int,
        nativeOnly: Boolean,
    ): List<Game> {
        val cacheKey = PopularityCacheKey(
            clientId = config.igdbClientId.trim(),
            platformIds = platformIds,
            search = escapedSearch,
            nativeOnly = nativeOnly,
        )
        val orderedIds = popularityIdsCache[cacheKey] ?: loadPopularityIds(
            config = config,
            token = token,
            platformIds = platformIds,
            escapedSearch = escapedSearch,
            nativeOnly = nativeOnly,
        ).also { popularityIdsCache[cacheKey] = it }

        val pageIds = orderedIds.drop(offset).take(limit)
        if (pageIds.isEmpty()) return emptyList()

        val query = buildString {
            append(GAME_FIELDS)
            append("where id = (").append(pageIds.joinToString(",")).append("); ")
            append("limit ").append(pageIds.size).append(";")
        }
        val response = igdbRequest(config, token, GAMES_URL, query)
        val gamesById = parseGames(JSONArray(response.body), platform).associateBy { it.igdbId }
        return pageIds.mapNotNull(gamesById::get)
    }

    private fun loadPopularityIds(
        config: ApiConfig,
        token: String,
        platformIds: List<Int>,
        escapedSearch: String,
        nativeOnly: Boolean,
    ): List<Int> {
        val candidatesQuery = buildString {
            append("fields id,total_rating_count; ")
            append("where ").append(
                gameFilter(
                    platformIds = platformIds,
                    search = escapedSearch,
                    nativeOnly = nativeOnly,
                ),
            ).append("; ")
            append("sort total_rating_count desc; ")
            append("limit ").append(POPULARITY_CANDIDATE_LIMIT).append(";")
        }
        val candidatesResponse = igdbRequest(config, token, GAMES_URL, candidatesQuery)
        val candidates = JSONArray(candidatesResponse.body).toObjectList()
            .mapNotNull { item ->
                item.optInt("id", -1).takeIf { it >= 0 }?.let {
                    PopularityCandidate(it, item.optInt("total_rating_count", 0))
                }
            }
        if (candidates.isEmpty()) return emptyList()

        val popularityQuery = buildString {
            append("fields game_id,value; where popularity_type = 1 & game_id = (")
            append(candidates.joinToString(",") { it.gameId.toString() })
            append("); sort value desc; limit ").append(POPULARITY_CANDIDATE_LIMIT).append(";")
        }
        val popularityResponse = igdbRequest(
            config,
            token,
            POPULARITY_PRIMITIVES_URL,
            popularityQuery,
        )
        val popularityByGame = JSONArray(popularityResponse.body).toObjectList()
            .associate { it.optInt("game_id") to it.optDouble("value", 0.0) }

        return candidates.sortedWith(
            compareByDescending<PopularityCandidate> { popularityByGame[it.gameId] ?: -1.0 }
                .thenByDescending { it.ratingCount },
        ).map { it.gameId }
    }

    private fun igdbRequest(
        config: ApiConfig,
        token: String,
        url: String,
        body: String,
    ): HttpResponse {
        val response = request(
            url = url,
            method = "POST",
            headers = mapOf(
                "Client-ID" to config.igdbClientId.trim(),
                "Authorization" to "Bearer $token",
                "Accept" to "application/json",
                "Content-Type" to "text/plain",
            ),
            body = body,
        )

        if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            cachedAccessToken = null
            popularityIdsCache.clear()
            throw ApiException("IGDB prihlásenie vypršalo. Skús načítať katalóg znova.")
        }
        if (response.code !in 200..299) throw ApiException(errorMessage("IGDB", response))
        return response
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
                    rating = item.optDouble("total_rating").takeIf { !it.isNaN() && it > 0 },
                    ratingCount = item.optInt("total_rating_count", 0),
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
        const val SURPRISE_COUNT = 3
        private const val MIN_SURPRISE_RATING_COUNT = 5
        private const val SURPRISE_POOL_SIZE = 30
        private const val POPULARITY_CANDIDATE_LIMIT = 500
        private const val TOKEN_URL = "https://id.twitch.tv/oauth2/token"
        private const val GAMES_URL = "https://api.igdb.com/v4/games"
        private const val GAMES_COUNT_URL = "https://api.igdb.com/v4/games/count"
        private const val POPULARITY_PRIMITIVES_URL =
            "https://api.igdb.com/v4/popularity_primitives"
        private const val GAME_FIELDS =
            "fields name,summary,first_release_date,platforms,genres.name," +
                "involved_companies.company.name,involved_companies.developer," +
                "involved_companies.publisher,cover.image_id,screenshots.image_id," +
                "videos.video_id,total_rating,total_rating_count; "
    }
}

private data class PopularityCacheKey(
    val clientId: String,
    val platformIds: List<Int>,
    val search: String,
    val nativeOnly: Boolean,
)

private data class PopularityCandidate(
    val gameId: Int,
    val ratingCount: Int,
)

private val GameSort.apiSort: String
    get() = when (this) {
        GameSort.NAME_ASC -> "name asc"
        GameSort.NAME_DESC -> "name desc"
        GameSort.POPULARITY -> "total_rating_count desc"
        GameSort.RATING -> "total_rating desc"
        GameSort.RATING_COUNT -> "total_rating_count desc"
        GameSort.NEWEST -> "first_release_date desc"
        GameSort.OLDEST -> "first_release_date asc"
    }

internal fun gameFilter(
    platformIds: List<Int>,
    sort: GameSort? = null,
    search: String = "",
    nativeOnly: Boolean = false,
): String = buildString {
    if (nativeOnly) {
        val platforms = Platform.entries.filter { it.igdbId in platformIds }
        append(
            platforms.joinToString(prefix = "(", postfix = ")", separator = " | ") {
                nativePlatformCondition(it)
            },
        )
    } else {
        append("platforms = (").append(platformIds.joinToString(",")).append(")")
    }
    append(" & version_parent = null")
    when (sort) {
        GameSort.RATING -> append(" & total_rating_count >= ").append(10)
        GameSort.RATING_COUNT -> append(" & total_rating_count != null")
        GameSort.NEWEST, GameSort.OLDEST -> append(" & first_release_date != null")
        else -> Unit
    }
    if (search.isNotBlank()) append(" & name ~ *\"").append(search).append("\"*")
}

internal fun nativePlatformCondition(platform: Platform): String {
    val cutoff = LocalDate.of(platform.nativeCutoffYear, 1, 1)
        .atStartOfDay(ZoneOffset.UTC)
        .toEpochSecond()
    return "(platforms = ${platform.igdbId} & first_release_date >= $cutoff)"
}

internal fun selectSurpriseGames(
    candidates: List<Game>,
    recentGameIds: Set<Int>,
    preferDifferentPlatforms: Boolean,
    count: Int = IgdbApiClient.SURPRISE_COUNT,
    random: Random = Random.Default,
): List<Game> {
    if (count <= 0) return emptyList()
    val shuffled = candidates.distinctBy { it.igdbId }.shuffled(random)
    val fresh = shuffled.filterNot { it.igdbId in recentGameIds }
    val preferredPool = fresh.takeIf { it.size >= count } ?: shuffled
    if (!preferDifferentPlatforms) return preferredPool.take(count)

    val result = mutableListOf<Game>()
    val usedPlatforms = mutableSetOf<Platform>()
    preferredPool.forEach { game ->
        if (result.size < count && usedPlatforms.add(game.platform)) result += game
    }
    preferredPool.forEach { game ->
        if (result.size < count && game !in result) result += game
    }
    return result
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
