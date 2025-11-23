package cl.gymtastic.app.data.repository

import android.content.Context
import android.util.Log
import cl.gymtastic.app.data.local.datastore.SessionPrefs
import cl.gymtastic.app.data.remote.*
import cl.gymtastic.app.util.ServiceLocator

// Se agrega 'open' para permitir herencia en los tests (FakeAuthRepository)
open class AuthRepository(context: Context) {
    // Solo dependemos de DataStore (para el token) y la API
    private val prefs = SessionPrefs(context)
    private val api = ServiceLocator.api()

    private fun normEmail(raw: String) = raw.trim().lowercase()

    // --- AUTHENTICATION ---

    suspend fun register(email: String, password: String, nombre: String): Boolean {
        val e = normEmail(email)
        return try {
            val response = api.register(RegisterRequest(e, password, nombre))
            if (!response.isSuccessful && response.code() == 400) {
                Log.w("AuthRepo", "Registro fallido: Email ya existe")
            }
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("AuthRepo", "Excepción durante registro", e)
            false
        }
    }

    // CORRECCIÓN AQUÍ: Agregamos el parámetro rememberMe
    open suspend fun login(email: String, password: String, rememberMe: Boolean): Boolean {
        val e = normEmail(email)
        return try {
            val response = api.login(LoginRequest(e, password))
            val loginResponse = response.body()

            if (response.isSuccessful && loginResponse != null && loginResponse.success) {
                // 1. Obtenemos los datos (token y email seguro)
                val token = loginResponse.token ?: ""
                val userEmail = loginResponse.user?.email ?: e

                // 2. Guardamos los datos de sesión
                prefs.saveAuthToken(token)
                prefs.saveUserEmail(userEmail)

                // 3. IMPORTANTE: Guardamos la preferencia del usuario
                prefs.saveRememberMe(rememberMe)

                true
            } else {
                Log.w("AuthRepo", "Login fallido: ${loginResponse?.message}")
                false
            }
        } catch (e: Exception) {
            Log.e("AuthRepo", "Excepción durante login", e)
            false
        }
    }

    suspend fun logout() {
        // Borra los datos de sesión al salir
        prefs.clearSession()
    }

    fun prefs() = prefs

    // --- USER DATA (Backend Directo) ---

    open suspend fun getUserProfile(email: String): UserProfileDto? {
        return try {
            val response = api.getUserProfile(email)
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e("AuthRepo", "Error obteniendo perfil: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("AuthRepo", "Excepción obteniendo perfil", e)
            null
        }
    }

    // --- ADMIN FEATURES ---

    suspend fun getAllUsers(): List<UserProfileDto> {
        return try {
            val response = api.getAllUsers()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                Log.e("AuthRepo", "Error obteniendo usuarios: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("AuthRepo", "Excepción obteniendo usuarios", e)
            emptyList()
        }
    }

    suspend fun updateRole(email: String, newRole: String): Boolean {
        return try {
            val response = api.updateUserRole(email, AdminRoleUpdateRequest(newRole))
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error actualizando rol", e)
            false
        }
    }

    suspend fun deleteUser(email: String): Boolean {
        return try {
            val response = api.deleteUser(email)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error eliminando usuario", e)
            false
        }
    }

    // --- PASSWORD RESET ---

    suspend fun requestPasswordReset(email: String): Boolean {
        return try {
            val response = api.requestReset(normEmail(email))
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun confirmPasswordReset(email: String, token: String, newPass: String): Boolean {
        return try {
            val response = api.confirmReset(
                ResetPasswordRequestDto(
                    email = normEmail(email),
                    token = token,
                    newPassword = newPass
                )
            )
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}