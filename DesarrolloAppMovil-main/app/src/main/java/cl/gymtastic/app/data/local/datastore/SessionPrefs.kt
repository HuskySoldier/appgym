package cl.gymtastic.app.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionStore by preferencesDataStore("session_prefs")

class SessionPrefs(private val context: Context) {

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_EMAIL = stringPreferencesKey("user_email")
        // 1. NUEVA CLAVE
        private val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
    }

    // --- Guardar ---
    suspend fun saveUserEmail(email: String) {
        context.sessionStore.edit { it[KEY_EMAIL] = email }
    }

    suspend fun saveAuthToken(token: String) {
        context.sessionStore.edit { it[KEY_TOKEN] = token }
    }

    // 2. NUEVA FUNCIÓN PARA GUARDAR PREFERENCIA
    suspend fun saveRememberMe(remember: Boolean) {
        context.sessionStore.edit { it[KEY_REMEMBER_ME] = remember }
    }

    suspend fun clearSession() {
        context.sessionStore.edit { it.clear() }
    }

    // --- Leer ---
    val userEmailFlow: Flow<String> = context.sessionStore.data.map { it[KEY_EMAIL] ?: "" }
    val tokenFlow: Flow<String> = context.sessionStore.data.map { it[KEY_TOKEN] ?: "" }

    // 3. NUEVO FLUJO PARA LEER PREFERENCIA
    val rememberMeFlow: Flow<Boolean> = context.sessionStore.data.map {
        it[KEY_REMEMBER_ME] ?: true // Por defecto true
    }

    // Compatibilidad
    suspend fun setUserEmail(email: String) = saveUserEmail(email)
    suspend fun setToken(token: String) = saveAuthToken(token)
    suspend fun clear() = clearSession()
}