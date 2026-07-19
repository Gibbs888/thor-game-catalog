package com.gibbstech.thorgamecatalog

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val SUPPORTED_TOKENS = mapOf(
    "{title}" to { game: Game -> game.title },
    "{platform}" to { game: Game -> game.platform.label },
    "{region}" to { game: Game -> game.region },
    "{year}" to { game: Game -> game.year.toString() },
)

fun buildWebsiteUrl(template: String, game: Game): String? {
    if (template.isBlank()) return null

    val resolvedUrl = SUPPORTED_TOKENS.entries.fold(template.trim()) { url, (token, value) ->
        url.replace(token, encodeUrlValue(value(game)), ignoreCase = true)
    }

    return resolvedUrl.takeIf(::isValidWebsiteTemplate)
}

fun isValidWebsiteTemplate(template: String): Boolean {
    if (template.isBlank()) return false

    val valueForValidation = SUPPORTED_TOKENS.keys.fold(template.trim()) { url, token ->
        url.replace(token, "example", ignoreCase = true)
    }

    return runCatching {
        val uri = URI(valueForValidation)
        (uri.scheme.equals("https", ignoreCase = true) ||
            uri.scheme.equals("http", ignoreCase = true)) &&
            !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}

private fun encodeUrlValue(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
