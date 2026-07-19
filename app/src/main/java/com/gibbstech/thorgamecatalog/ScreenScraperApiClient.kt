package com.gibbstech.thorgamecatalog

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer

class ScreenScraperApiClient {
    suspend fun testConnection(config: ApiConfig) = withContext(Dispatchers.IO) {
        require(config.isScreenScraperReady) {
            "Najprv nastav ScreenScraper Developer ID a Developer Password."
        }
        val response = request(apiUrl("ssinfraInfos.php", config, emptyMap()))
        if (response.code !in 200..299) {
            throw ApiException("ScreenScraper chyba ${response.code}: ${response.body.take(160)}")
        }
        if (response.body.contains("Erreur", ignoreCase = true) ||
            response.body.contains("error", ignoreCase = true)
        ) {
            throw ApiException("ScreenScraper odmietol prihlasovacie údaje.")
        }
    }

    suspend fun enrichGame(config: ApiConfig, game: Game): Game = withContext(Dispatchers.IO) {
        if (!config.isScreenScraperReady) return@withContext game

        val response = request(
            apiUrl(
                endpoint = "jeuRecherche.php",
                config = config,
                extra = mapOf(
                    "systemeid" to game.platform.screenScraperId.toString(),
                    "recherche" to game.title,
                ),
            ),
        )
        if (response.code !in 200..299) return@withContext game

        runCatching { parseEnrichment(JSONObject(response.body), game) }.getOrDefault(game)
    }

    private fun parseEnrichment(root: JSONObject, original: Game): Game {
        val response = root.optJSONObject("response") ?: root
        val games = response.optJSONArray("jeux") ?: return original
        val match = bestMatch(games, original.title) ?: return original
        val media = match.optJSONArray("medias").toObjectList()

        val screenScraperCover = media.firstOrNull {
            it.optString("type").lowercase().startsWith("box-2d") &&
                preferredRegion(it.optString("region"))
        }?.optString("url")?.takeIf(String::isNotBlank)
            ?: media.firstOrNull { it.optString("type").lowercase().startsWith("box-2d") }
                ?.optString("url")?.takeIf(String::isNotBlank)

        val screenshots = media.filter {
            it.optString("type").lowercase() in setOf("ss", "sstitle")
        }.sortedByDescending { preferredRegion(it.optString("region")) }
            .mapNotNull { it.optString("url").takeIf(String::isNotBlank) }
            .distinct()
            .take(8)

        val videoUrl = media.firstOrNull {
            it.optString("type").lowercase().startsWith("video")
        }?.optString("url")?.takeIf(String::isNotBlank)

        val synopsis = localizedText(match.optJSONArray("synopsis"), listOf("en", "fr", "de"))
        val developer = match.optString("developpeur").takeIf(String::isNotBlank)
        val publisher = match.optString("editeur").takeIf(String::isNotBlank)

        return original.copy(
            description = synopsis ?: original.description,
            coverUrl = screenScraperCover ?: original.coverUrl,
            screenshotUrls = (screenshots + original.screenshotUrls).distinct().take(8),
            previewVideoUrl = videoUrl ?: original.previewVideoUrl,
            developer = developer ?: original.developer,
            publisher = publisher ?: original.publisher,
        )
    }

    private fun bestMatch(games: JSONArray, title: String): JSONObject? {
        val wanted = normalize(title)
        return games.toObjectList().maxByOrNull { game ->
            val names = game.optJSONArray("noms").toObjectList()
                .map { normalize(it.optString("text")) }
            when {
                names.any { it == wanted } -> 3
                names.any { it.contains(wanted) || wanted.contains(it) } -> 2
                else -> 1
            }
        }
    }

    private fun localizedText(items: JSONArray?, languages: List<String>): String? {
        val values = items.toObjectList()
        languages.forEach { language ->
            values.firstOrNull { it.optString("langue").equals(language, true) }
                ?.optString("text")?.takeIf(String::isNotBlank)?.let { return it }
        }
        return values.firstNotNullOfOrNull { it.optString("text").takeIf(String::isNotBlank) }
    }

    private fun apiUrl(
        endpoint: String,
        config: ApiConfig,
        extra: Map<String, String>,
    ): String {
        val parameters = linkedMapOf(
            "devid" to config.screenScraperDeveloperId.trim(),
            "devpassword" to config.screenScraperDeveloperPassword,
            "softname" to config.screenScraperSoftName.ifBlank { "ThorGameCatalog" },
            "output" to "json",
        )
        if (config.screenScraperUsername.isNotBlank()) {
            parameters["ssid"] = config.screenScraperUsername.trim()
            parameters["sspassword"] = config.screenScraperPassword
        }
        parameters.putAll(extra)
        return "https://api.screenscraper.fr/api2/$endpoint?" +
            parameters.entries.joinToString("&") { (key, value) -> "$key=${encode(value)}" }
    }

    private fun preferredRegion(region: String): Boolean =
        region.lowercase() in setOf("eu", "wor", "world", "ss")

    private fun normalize(value: String): String = Normalizer.normalize(
        value.lowercase(),
        Normalizer.Form.NFD,
    ).replace("\\p{Mn}+".toRegex(), "")
        .replace("[^a-z0-9]".toRegex(), "")
}
