package com.gibbstech.thorgamecatalog

import android.content.Context

class PlatformWebsitePreferences(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): Map<Platform, String> = Platform.entries.associateWith { platform ->
        preferences.getString(key(platform), "").orEmpty()
    }

    fun save(platform: Platform, template: String) {
        val normalizedTemplate = template.trim()
        preferences.edit().apply {
            if (normalizedTemplate.isBlank()) {
                remove(key(platform))
            } else {
                putString(key(platform), normalizedTemplate)
            }
        }.apply()
    }

    private fun key(platform: Platform): String = "platform_website_${platform.id}"

    private companion object {
        const val PREFERENCES_NAME = "platform_websites"
    }
}
