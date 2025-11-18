package cl.gymtastic.app.data.repository

import android.content.Context
import android.util.Log // Importar para logs
import cl.gymtastic.app.data.local.datastore.SessionPrefs // Asegúrate que SessionPrefs esté importado
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import cl.gymtastic.app.data.local.entity.UserEntity
import cl.gymtastic.app.data.remote.LoginRequest
import cl.gymtastic.app.data.remote.RegisterRequest
import cl.gymtastic.app.data.remote.UserProfileDto
import cl.gymtastic.app.util.ServiceLocator // Importar ServiceLocator
import retrofit2.HttpException // Importar para manejar errores HTTP

class AuthRepository(private val context: Context) {
    private val db = GymTasticDatabase.get(context)
    private val prefs = SessionPrefs(context)
    private val api = ServiceLocator.api() // Obtener la instancia API

    // funciones auxiliares (Mantenemos el hash local para ser consistentes con tu seed local)
    private fun normEmail(raw: String) = raw.trim().lowercase()
    fun hashPassword(raw: String) = raw.trim().hashCode().toString()

    /**
     * Registra un nuevo usuario llamando al microservicio de registro.
     * Si tiene éxito, guarda el usuario en la BD local (Room).
     */
    suspend fun register(email: String, password: String, nombre: String): Boolean {
        val e = normEmail(email)
        try {
            val response = api.register(RegisterRequest(e, password, nombre))
            if (response.isSuccessful) {
                // La respuesta 201 (Created) significa éxito en Spring Boot.
                // Guardar una copia local (sin passHash) en Room para uso offline.
                db.users().insert(
                    UserEntity(
                        email = e,
                        passHash = hashPassword(password), // Usamos el hash local para Room
                        nombre = nombre,
                        rol = "user" // Rol por defecto
                    )
                )
                return true
            } else if (response.code() == 400) {
                // Error 400 Bad Request, probablemente "El email ya existe"
                Log.w("AuthRepo", "Registro fallido: Email ya existe (400)")
                return false
            } else {
                Log.e("AuthRepo", "Registro fallido: HTTP ${response.code()}")
                return false
            }
        } catch (e: HttpException) {
            // Captura si el servidor está caído o hay un error de conexión
            Log.e("AuthRepo", "Error HTTP durante registro", e)
            return false
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error desconocido durante registro", e)
            return false
        }
    }


    /**
     * Inicia sesión llamando al microservicio de login.
     * Si tiene éxito, sincroniza el perfil y guarda la sesión.
     */
    suspend fun login(email: String, password: String): Boolean {
        val e = normEmail(email)
        try {
            val response = api.login(LoginRequest(e, password))
            val loginResponse = response.body()

            if (response.isSuccessful && loginResponse != null && loginResponse.success) {
                val userDto = loginResponse.user
                if (userDto != null) {
                    // 1. Guardar/actualizar la sesión y el token.
                    prefs.setUserEmail(e)
                    prefs.setToken(loginResponse.token ?: "local-token-${e.hashCode()}")

                    // 2. Sincronizar el perfil completo en Room (BD local)
                    syncUserToRoom(userDto)

                    return true
                }
            }
            Log.w("AuthRepo", "Login fallido: ${loginResponse?.message ?: "Credenciales inválidas"}")
            return false
        } catch (e: HttpException) {
            Log.e("AuthRepo", "Error HTTP durante login", e)
            return false
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error desconocido durante login", e)
            return false
        }
    }

    /**
     * Sincroniza un UserProfileDto (obtenido de la API) con la base de datos local.
     */
    private suspend fun syncUserToRoom(dto: UserProfileDto) {
        val userEntity = UserEntity(
            email = dto.email,
            // Re-hash del password para consistencia si no lo obtenemos de la API
            passHash = db.users().findByEmail(dto.email)?.passHash ?: hashPassword(""),
            nombre = dto.nombre,
            rol = dto.rol,
            planEndMillis = dto.planEndMillis,
            sedeId = dto.sedeId,
            sedeName = dto.sedeName,
            sedeLat = dto.sedeLat,
            sedeLng = dto.sedeLng,
            avatarUri = dto.avatarUri,
            fono = dto.fono,
            bio = dto.bio
        )
        // Usa update, si falla (porque no existe), usa insert. Esto es idempotente.
        if (db.users().update(userEntity) == 0) {
            db.users().insert(userEntity)
        }
    }


    suspend fun logout() {
        prefs.clear()
    }

    fun prefs() = prefs
}