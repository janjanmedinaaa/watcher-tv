package com.medina.juanantonio.watcher.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class DataStoreManager(private val context: Context) : IDataStoreManager {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = "watcher_tv"
    )

    override suspend fun putString(key: String, value: String) {
        val preferencesKey = stringPreferencesKey(key)
        context.dataStore.edit {
            it[preferencesKey] = value
        }
    }

    override suspend fun getString(key: String, defaultValue: String): String {
        val preferencesKey = stringPreferencesKey(key)
        val stringFlow: Flow<String> = context.dataStore.data.map {
            it[preferencesKey] ?: defaultValue
        }

        return stringFlow.firstOrNull() ?: defaultValue
    }
}

interface IDataStoreManager {
    suspend fun putString(key: String, value: String)
    suspend fun getString(key: String, defaultValue: String = ""): String
}