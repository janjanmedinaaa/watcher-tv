package com.medina.juanantonio.watcher.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
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

    override suspend fun putBoolean(key: String, value: Boolean) {
        val preferencesKey = booleanPreferencesKey(key)
        context.dataStore.edit {
            it[preferencesKey] = value
        }
    }

    override suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val preferencesKey = booleanPreferencesKey(key)
        val booleanFlow: Flow<Boolean> = context.dataStore.data.map {
            it[preferencesKey] ?: defaultValue
        }

        return booleanFlow.firstOrNull() ?: defaultValue
    }

    override suspend fun putInt(key: String, value: Int) {
        val preferencesKey = intPreferencesKey(key)
        context.dataStore.edit {
            it[preferencesKey] = value
        }
    }

    override suspend fun getInt(key: String, defaultValue: Int): Int {
        val preferencesKey = intPreferencesKey(key)
        val intFlow: Flow<Int> = context.dataStore.data.map {
            it[preferencesKey] ?: defaultValue
        }

        return intFlow.firstOrNull() ?: defaultValue
    }

    override suspend fun putLong(key: String, value: Long) {
        val preferencesKey = longPreferencesKey(key)
        context.dataStore.edit {
            it[preferencesKey] = value
        }
    }

    override suspend fun getLong(key: String, defaultValue: Long): Long {
        val preferencesKey = longPreferencesKey(key)
        val longFlow: Flow<Long> = context.dataStore.data.map {
            it[preferencesKey] ?: defaultValue
        }

        return longFlow.firstOrNull() ?: defaultValue
    }
}

interface IDataStoreManager {
    suspend fun putString(key: String, value: String)
    suspend fun getString(key: String, defaultValue: String = ""): String
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    suspend fun putInt(key: String, value: Int)
    suspend fun getInt(key: String, defaultValue: Int = -1): Int
    suspend fun putLong(key: String, value: Long)
    suspend fun getLong(key: String, defaultValue: Long = -1L): Long
}