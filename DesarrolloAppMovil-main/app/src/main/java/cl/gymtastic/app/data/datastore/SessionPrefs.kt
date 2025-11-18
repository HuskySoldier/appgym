package cl.gymtastic.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("session_prefs")

object SessionKeys {
    val USER_ID = intPreferencesKey("user_id")
    val TOKEN = stringPreferencesKey("token")
}

class SessionPrefs(private val context: Context) {
    val userIdFlow = context.dataStore.data.map { it[SessionKeys.USER_ID] ?: -1 }
    val tokenFlow = context.dataStore.data.map { it[SessionKeys.TOKEN] ?: "" }

    suspend fun setUser(id: Int, token: String){
        context.dataStore.edit {
            it[SessionKeys.USER_ID] = id
            it[SessionKeys.TOKEN] = token
        }
    }

    suspend fun clear(){
        context.dataStore.edit { it.clear() }
    }
}