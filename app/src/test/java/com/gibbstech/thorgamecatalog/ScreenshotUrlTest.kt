package com.gibbstech.thorgamecatalog

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenshotUrlTest {
    @Test
    fun igdbThumbnailIsUpgradedForFullscreenGallery() {
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_1080p/example.jpg",
            largeScreenshotUrl(
                "https://images.igdb.com/igdb/image/upload/t_screenshot_big/example.jpg",
            ),
        )
    }

    @Test
    fun otherMediaUrlsStayUnchanged() {
        val url = "https://screenscraper.example/media/example.png"

        assertEquals(url, largeScreenshotUrl(url))
    }
}
