package cl.gymtastic.app.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.* // Importa longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionStore by preferencesDataStore("session_prefs")

class SessionPrefs(private val context: Context) {

    companion object {
        // --- CAMBIO 1: Cambiar a longPreferencesKey ---

        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_EMAIL = stringPreferencesKey("user_email")
    }

    // --- CAMBIO 2: Aceptar Long en setUser (si aún lo usas) ---
    // Si ya no usas setUser con ID numérico (porque usas email), puedes ignorar esta función.
    // Si la usas, cambia el parámetro 'id' a Long.
    suspend fun setUser(id: Long, token: String) { // <-- Cambiado a Long
        context.sessionStore.edit {

            it[KEY_TOKEN] = token
        }
    }

    suspend fun setUserEmail(email: String) {
        context.sessionStore.edit {
            it[KEY_EMAIL] = email
        }
    }

    suspend fun setToken(token: String) {
        context.sessionStore.edit {
            it[KEY_TOKEN] = token
        }
    }

    suspend fun clear() {
        context.sessionStore.edit { it.clear() }
    }

    val userEmailFlow: Flow<String> = context.sessionStore.data.map {
        it[KEY_EMAIL] ?: ""
    }

    val tokenFlow: Flow<String> = context.sessionStore.data.map {
        it[KEY_TOKEN] ?: ""
    }
}