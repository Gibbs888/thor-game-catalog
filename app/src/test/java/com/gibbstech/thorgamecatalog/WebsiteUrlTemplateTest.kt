package com.gibbstech.thorgamecatalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebsiteUrlTemplateTest {
    private val game = Game(
        id = "ps1-crash-bandicoot",
        igdbId = 123,
        title = "Crash Bandicoot",
        platform = Platform.PS1,
        year = 1996,
        description = "Test",
        coverUrl = null,
        region = "EU",
    )

    @Test
    fun replacesAndEncodesSupportedTokens() {
        val url = buildWebsiteUrl(
            "https://example.com/search?q={title}&platform={platform}&region={region}&year={year}",
            game,
        )

        assertEquals(
            "https://example.com/search?q=Crash%20Bandicoot&platform=PlayStation%201&region=EU&year=1996",
            url,
        )
    }

    @Test
    fun keepsPlainWebsiteUrlUnchanged() {
        assertEquals(
            "https://example.com/ps1",
            buildWebsiteUrl("https://example.com/ps1", game),
        )
    }

    @Test
    fun rejectsUnsupportedOrMalformedUrls() {
        assertFalse(isValidWebsiteTemplate("example.com/{title}"))
        assertFalse(isValidWebsiteTemplate("javascript:alert(1)"))
        assertNull(buildWebsiteUrl("", game))
    }

    @Test
    fun acceptsHttpAndHttpsTemplates() {
        assertTrue(isValidWebsiteTemplate("https://example.com/search?q={title}"))
        assertTrue(isValidWebsiteTemplate("http://example.com/{platform}"))
    }
}
