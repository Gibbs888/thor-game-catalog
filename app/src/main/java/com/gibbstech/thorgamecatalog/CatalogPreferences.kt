package com.gibbstech.thorgamecatalog

import android.content.Context

class CatalogPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(
        "catalog_preferences",
        Context.MODE_PRIVATE,
    )

    fun loadSort(): GameSort = GameSort.fromId(
        preferences.getString(KEY_SORT, null).orEmpty(),
    )

    fun saveSort(sort: GameSort) {
        preferences.edit().putString(KEY_SORT, sort.id).apply()
    }

    private companion object {
        const val KEY_SORT = "game_sort"
    }
}
