package com.gibbstech.thorgamecatalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SurpriseSelectionTest {
    @Test
    fun returnsThreeGamesFromDifferentPlatformsWhenPossible() {
        val games = listOf(
            game(1, Platform.PS1),
            game(2, Platform.PS1),
            game(3, Platform.PS2),
            game(4, Platform.PSP),
            game(5, Platform.NDS),
        )

        val result = selectSurpriseGames(
            candidates = games,
            recentGameIds = emptySet(),
            preferDifferentPlatforms = true,
            random = Random(7),
        )

        assertEquals(3, result.size)
        assertEquals(3, result.map { it.platform }.distinct().size)
    }

    @Test
    fun avoidsRecentlyShownGamesWhenEnoughFreshChoicesExist() {
        val games = (1..6).map { game(it, Platform.PS1) }

        val result = selectSurpriseGames(
            candidates = games,
            recentGameIds = setOf(1, 2, 3),
            preferDifferentPlatforms = false,
            random = Random(3),
        )

        assertEquals(3, result.size)
        assertTrue(result.all { it.igdbId !in setOf(1, 2, 3) })
    }

    @Test
    fun fallsBackToRecentGamesInsteadOfReturningTooFew() {
        val games = (1..3).map { game(it, Platform.PS1) }

        val result = selectSurpriseGames(
            candidates = games,
            recentGameIds = setOf(1, 2),
            preferDifferentPlatforms = false,
            random = Random(1),
        )

        assertEquals(3, result.size)
        assertEquals(3, result.map { it.igdbId }.distinct().size)
        assertTrue(result.any { it.igdbId == 3 })
    }

    private fun game(id: Int, platform: Platform): Game = Game(
        id = "game-$id",
        igdbId = id,
        title = "Game $id",
        platform = platform,
        year = 2000,
        description = "",
        coverUrl = "https://example.com/$id.jpg",
    )
}
