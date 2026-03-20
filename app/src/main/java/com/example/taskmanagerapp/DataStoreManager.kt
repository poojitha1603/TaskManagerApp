package com.example.taskmanagerapp

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        val USERNAME = stringPreferencesKey("username")
    }

    suspend fun saveUsername(name: String) {
        context.dataStore.edit {
            it[USERNAME] = name
        }
    }

    val getUsername: Flow<String?> = context.dataStore.data
        .map { prefs ->
            prefs[USERNAME]
        }
}