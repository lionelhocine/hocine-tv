package com.tv.streamapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tvDataStore: DataStore<Preferences> by preferencesDataStore(name = "hocine_tv_prefs")

class UserPreferences(private val context: Context) {

    private object Keys {
        val FAVORITES = stringSetPreferencesKey("favorites")
        val LAST_CHANNEL = stringPreferencesKey("last_channel")
        val AUTO_RESUME = booleanPreferencesKey("auto_resume")
    }

    val favorites: Flow<Set<String>> = context.tvDataStore.data.map { it[Keys.FAVORITES] ?: emptySet() }
    val lastChannelId: Flow<String?> = context.tvDataStore.data.map { it[Keys.LAST_CHANNEL] }
    val autoResume: Flow<Boolean> = context.tvDataStore.data.map { it[Keys.AUTO_RESUME] ?: false }

    suspend fun toggleFavorite(id: String) {
        context.tvDataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES] ?: emptySet()
            prefs[Keys.FAVORITES] = if (id in current) current - id else current + id
        }
    }

    suspend fun setLastChannel(id: String) {
        context.tvDataStore.edit { it[Keys.LAST_CHANNEL] = id }
    }

    suspend fun setAutoResume(enabled: Boolean) {
        context.tvDataStore.edit { it[Keys.AUTO_RESUME] = enabled }
    }
}
