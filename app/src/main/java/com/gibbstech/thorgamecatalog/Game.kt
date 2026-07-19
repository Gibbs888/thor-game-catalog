package com.gibbstech.thorgamecatalog

import android.net.Uri

enum class Platform(
    val id: String,
    val label: String,
    val shortLabel: String,
    val thumbnailRepository: String,
) {
    PS1(
        id = "ps1",
        label = "PlayStation 1",
        shortLabel = "PS1",
        thumbnailRepository = "Sony_-_PlayStation",
    ),
    PS2(
        id = "ps2",
        label = "PlayStation 2",
        shortLabel = "PS2",
        thumbnailRepository = "Sony_-_PlayStation_2",
    ),
    PSP(
        id = "psp",
        label = "PlayStation Portable",
        shortLabel = "PSP",
        thumbnailRepository = "Sony_-_PlayStation_Portable",
    ),
    GAMECUBE(
        id = "gamecube",
        label = "Nintendo GameCube",
        shortLabel = "GC",
        thumbnailRepository = "Nintendo_-_GameCube",
    ),
    WII(
        id = "wii",
        label = "Nintendo Wii",
        shortLabel = "Wii",
        thumbnailRepository = "Nintendo_-_Wii",
    ),
    DREAMCAST(
        id = "dreamcast",
        label = "SEGA Dreamcast",
        shortLabel = "DC",
        thumbnailRepository = "Sega_-_Dreamcast",
    ),
    NDS(
        id = "nds",
        label = "Nintendo DS",
        shortLabel = "DS",
        thumbnailRepository = "Nintendo_-_Nintendo_DS",
    ),
    N3DS(
        id = "n3ds",
        label = "Nintendo 3DS",
        shortLabel = "3DS",
        thumbnailRepository = "Nintendo_-_Nintendo_3DS",
    );

    companion object {
        fun fromId(id: String): Platform = entries.firstOrNull { it.id == id } ?: PS1
    }
}

data class Game(
    val id: String,
    val title: String,
    val platform: Platform,
    val year: Int,
    val description: String,
    val thumbnailName: String,
    val region: String = "EU",
) {
    val coverUrl: String
        get() {
            val encodedFileName = Uri.encode("$thumbnailName.png")
            return "https://raw.githubusercontent.com/libretro-thumbnails/" +
                "${platform.thumbnailRepository}/master/Named_Boxarts/$encodedFileName"
        }
}
