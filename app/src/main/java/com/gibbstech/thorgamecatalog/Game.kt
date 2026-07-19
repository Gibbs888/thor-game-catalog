package com.gibbstech.thorgamecatalog

enum class Platform(
    val id: String,
    val label: String,
    val shortLabel: String,
    val thumbnailRepository: String,
    val igdbId: Int,
    val screenScraperId: Int,
) {
    PS1(
        id = "ps1",
        label = "PlayStation 1",
        shortLabel = "PS1",
        thumbnailRepository = "Sony_-_PlayStation",
        igdbId = 7,
        screenScraperId = 57,
    ),
    PS2(
        id = "ps2",
        label = "PlayStation 2",
        shortLabel = "PS2",
        thumbnailRepository = "Sony_-_PlayStation_2",
        igdbId = 8,
        screenScraperId = 58,
    ),
    PSP(
        id = "psp",
        label = "PlayStation Portable",
        shortLabel = "PSP",
        thumbnailRepository = "Sony_-_PlayStation_Portable",
        igdbId = 38,
        screenScraperId = 61,
    ),
    GAMECUBE(
        id = "gamecube",
        label = "Nintendo GameCube",
        shortLabel = "GC",
        thumbnailRepository = "Nintendo_-_GameCube",
        igdbId = 21,
        screenScraperId = 13,
    ),
    WII(
        id = "wii",
        label = "Nintendo Wii",
        shortLabel = "Wii",
        thumbnailRepository = "Nintendo_-_Wii",
        igdbId = 5,
        screenScraperId = 16,
    ),
    DREAMCAST(
        id = "dreamcast",
        label = "SEGA Dreamcast",
        shortLabel = "DC",
        thumbnailRepository = "Sega_-_Dreamcast",
        igdbId = 23,
        screenScraperId = 23,
    ),
    NDS(
        id = "nds",
        label = "Nintendo DS",
        shortLabel = "DS",
        thumbnailRepository = "Nintendo_-_Nintendo_DS",
        igdbId = 20,
        screenScraperId = 15,
    ),
    N3DS(
        id = "n3ds",
        label = "Nintendo 3DS",
        shortLabel = "3DS",
        thumbnailRepository = "Nintendo_-_Nintendo_3DS",
        igdbId = 37,
        screenScraperId = 17,
    );

    companion object {
        fun fromId(id: String): Platform = entries.firstOrNull { it.id == id } ?: PS1

        fun fromIgdbIds(ids: List<Int>, preferred: Platform?): Platform? {
            if (preferred != null && preferred.igdbId in ids) return preferred
            return entries.firstOrNull { it.igdbId in ids }
        }
    }
}

enum class GameSort(
    val id: String,
    val label: String,
) {
    NAME_ASC("name_asc", "A – Z"),
    NAME_DESC("name_desc", "Z – A"),
    POPULARITY("popularity", "Najpopulárnejšie"),
    RATING("rating", "Najlepšie hodnotené"),
    RATING_COUNT("rating_count", "Najviac hodnotené"),
    NEWEST("newest", "Najnovšie"),
    OLDEST("oldest", "Najstaršie");

    companion object {
        fun fromId(id: String): GameSort = entries.firstOrNull { it.id == id } ?: POPULARITY
    }
}

data class Game(
    val id: String,
    val igdbId: Int,
    val title: String,
    val platform: Platform,
    val year: Int?,
    val description: String,
    val coverUrl: String?,
    val screenshotUrls: List<String> = emptyList(),
    val previewVideoUrl: String? = null,
    val youtubeVideoId: String? = null,
    val genres: List<String> = emptyList(),
    val developer: String? = null,
    val publisher: String? = null,
    val rating: Double? = null,
    val ratingCount: Int = 0,
    val region: String = "EU",
)
