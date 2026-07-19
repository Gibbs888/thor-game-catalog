package com.gibbstech.thorgamecatalog

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenScraperVideoTest {
    @Test
    fun normalizedVideoIsPreferredOverOriginal() {
        val media = listOf(
            media("video", "https://example.com/original.mp4"),
            media("video-normalized", "https://example.com/normalized.mp4"),
        )

        assertEquals(
            "https://example.com/normalized.mp4",
            preferredScreenScraperVideoUrl(media),
        )
    }

    @Test
    fun originalVideoIsUsedAsFallback() {
        assertEquals(
            "https://example.com/video.mp4?a=1&b=2",
            preferredScreenScraperVideoUrl(
                listOf(media("video", "https://example.com/video.mp4?a=1&amp;b=2")),
            ),
        )
    }

    @Test
    fun missingVideoReturnsNull() {
        assertNull(
            preferredScreenScraperVideoUrl(
                listOf(media("ss", "https://example.com/screenshot.png")),
            ),
        )
    }

    private fun media(type: String, url: String): JSONObject = JSONObject()
        .put("type", type)
        .put("url", url)
}
