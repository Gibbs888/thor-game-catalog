package com.gibbstech.thorgamecatalog

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeUrlTest {
    @Test
    fun watchUrlContainsVideoId() {
        assertEquals(
            "https://www.youtube.com/watch?v=abc123",
            youtubeWatchUrl("abc123"),
        )
    }
}
