package cl.gymtastic.app.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Instancia única del DataStore para preferencias de sesión
private val Context.sessionStore by preferencesDataStore("session_prefs")

class SessionPrefs(private val context: Context) {

    companion object {
        // Claves para guardar los datos
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_EMAIL = stringPreferencesKey("user_email")
        // 1. NUEVA LLAVE: Para guardar la decisión del Checkbox
        private val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
    }

    // --- MÉTODOS PARA GUARDAR ---

    // Guarda el Email del usuario logueado
    suspend fun saveUserEmail(email: String) {
        context.sessionStore.edit { preferences ->
            preferences[KEY_EMAIL] = email
        }
    }

    // Guarda el Token de autenticación
    suspend fun saveAuthToken(token: String) {
        context.sessionStore.edit { preferences ->
            preferences[KEY_TOKEN] = token
        }
    }

    // 2. NUEVO MÉTODO: Guardar si el usuario quiere ser recordado
    suspend fun saveRememberMe(remember: Boolean) {
        context.sessionStore.edit { preferences ->
            preferences[KEY_REMEMBER_ME] = remember
        }
    }

    // Borra toda la sesión (Logout)
    suspend fun clearSession() {
        context.sessionStore.edit { preferences ->
            preferences.clear()
        }
    }

    // --- MÉTODOS PARA LEER (Flows) ---

    // Flujo para observar el email actual (usado en Splash y Home)
    val userEmailFlow: Flow<String> = context.sessionStore.data.map { preferences ->
        preferences[KEY_EMAIL] ?: ""
    }

    // Flujo para observar el token (usado en interceptores o repositorios)
    val tokenFlow: Flow<String> = context.sessionStore.data.map { preferences ->
        preferences[KEY_TOKEN] ?: ""
    }

    // 3. NUEVO FLOW: Leer la preferencia "Remember Me"
    // Por defecto devolvemos 'true' para que usuarios antiguos no pierdan su sesión por error
    val rememberMeFlow: Flow<Boolean> = context.sessionStore.data.map { preferences ->
        preferences[KEY_REMEMBER_ME] ?: true
    }

    // --- MÉTODOS LEGACY (Compatibilidad) ---
    suspend fun setUserEmail(email: String) = saveUserEmail(email)
    suspend fun setToken(token: String) = saveAuthToken(token)
    suspend fun clear() = clearSession()
}