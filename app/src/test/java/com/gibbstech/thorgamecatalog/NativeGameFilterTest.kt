package com.gibbstech.thorgamecatalog

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeGameFilterTest {
    @Test
    fun nativeFilterUsesPlatformAndReleaseCutoff() {
        val condition = nativePlatformCondition(Platform.PSP)

        assertTrue(condition.contains("platforms = 38"))
        assertTrue(condition.contains("first_release_date >="))
        assertFalse(condition.contains("game_type"))
    }

    @Test
    fun nativeFilterIncludesNintendoSwitch() {
        val filter = gameFilter(
            platformIds = Platform.entries.map { it.igdbId },
            nativeOnly = true,
        )

        assertTrue(filter.contains("platforms = 130"))
        assertTrue(filter.contains("platforms = 37"))
    }

    @Test
    fun disabledNativeFilterKeepsCompatibilityCatalog() {
        val filter = gameFilter(
            platformIds = listOf(Platform.PSP.igdbId),
            nativeOnly = false,
        )

        assertTrue(filter.contains("platforms = (38)"))
        assertFalse(filter.contains("first_release_date"))
    }
}
