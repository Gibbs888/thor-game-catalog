package com.gibbstech.thorgamecatalog

import android.content.Context
import org.json.JSONArray

object GameRepository {
    fun load(context: Context): List<Game> {
        val json = context.assets.open("games.json").bufferedReader().use { it.readText() }
        return parse(json)
    }

    internal fun parse(json: String): List<Game> {
        val games = JSONArray(json)
        return buildList(games.length()) {
            for (index in 0 until games.length()) {
                val item = games.getJSONObject(index)
                add(
                    Game(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        platform = Platform.fromId(item.getString("platform")),
                        year = item.getInt("year"),
                        description = item.getString("description"),
                        thumbnailName = item.getString("thumbnailName"),
                        region = item.optString("region", "EU"),
                    ),
                )
            }
        }
    }
}
