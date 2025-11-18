package cl.gymtastic.app.data.repository

import android.content.Context
import android.util.Log
import cl.gymtastic.app.data.local.datastore.SessionPrefs
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import cl.gymtastic.app.data.local.entity.UserEntity
import cl.gymtastic.app.data.remote.*
import cl.gymtastic.app.util.ServiceLocator
import retrofit2.HttpException

class AuthRepository(private val context: Context) {
    private val db = GymTasticDatabase.get(context)
    private val prefs = SessionPrefs(context)
    private val api = ServiceLocator.api()

    private fun normEmail(raw: String) = raw.trim().lowercase()
    fun hashPassword(raw: String) = raw.trim().hashCode().toString() // Aún se usa para passHash local dummy

    suspend fun register(email: String, password: String, nombre: String): Boolean {
        val e = normEmail(email)
        try {
            val response = api.register(RegisterRequest(e, password, nombre))
            if (response.isSuccessful) {
                db.users().insert(
                    UserEntity(
                        email = e,
                        passHash = hashPassword(password),
                        nombre = nombre,
                        rol = "user"
                    )
                )
                return true
            } else if (response.code() == 400) {
                Log.w("AuthRepo", "Registro fallido: Email ya existe (400)")
                return false
            } else {
                Log.e("AuthRepo", "Registro fallido: HTTP ${response.code()}")
                return false
            }
        } catch (e: Exception) {
            Log.e("AuthRepo", "Excepción durante registro", e)
            return false
        }
    }

    suspend fun login(email: String, password: String): Boolean {
        val e = normEmail(email)
        try {
            val response = api.login(LoginRequest(e, password))
            val loginResponse = response.body()

            if (response.isSuccessful && loginResponse != null && loginResponse.success) {
                val userDto = loginResponse.user
                if (userDto != null) {
                    prefs.setUserEmail(e)
                    prefs.setToken(loginResponse.token ?: "local-token-${e.hashCode()}")
                    syncUserToRoom(userDto)
                    return true
                }
            }
            Log.w("AuthRepo", "Login fallido: ${loginResponse?.message}")
            return false
        } catch (e: Exception) {
            Log.e("AuthRepo", "Excepción durante login", e)
            return false
        }
    }

    // --- ADMIN: Sincronizar TODOS los usuarios ---
    suspend fun syncAllUsers() {
        try {
            val response = api.getAllUsers()
            if (response.isSuccessful) {
                val remoteUsers = response.body() ?: emptyList()

                val entities = remoteUsers.map { dto ->
                    UserEntity(
                        email = dto.email,
                        passHash = "", // Dummy, backend maneja auth
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
                }

                entities.forEach { user ->
                    // Preservar passHash local si existe, solo actualizar datos
                    val existing = db.users().findByEmail(user.email)
                    val userToSave = user.copy(passHash = existing?.passHash ?: "synced")
                    if (existing == null) {
                        db.users().insert(userToSave)
                    } else {
                        db.users().update(userToSave)
                    }
                }
                Log.d("AuthRepo", "Sincronizados ${entities.size} usuarios")
            }
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error syncing all users", e)
        }
    }

    // --- ADMIN: Actualizar rol ---
    suspend fun updateRole(email: String, newRole: String): Boolean {
        return try {
            val response = api.updateUserRole(email, AdminRoleUpdateRequest(newRole))
            if (response.isSuccessful) {
                db.users().updateUserRole(email, newRole)
                true
            } else false
        } catch (e: Exception) { false }
    }

    // --- ADMIN: Eliminar usuario ---
    suspend fun deleteUser(email: String): Boolean {
        return try {
            val response = api.deleteUser(email)
            if (response.isSuccessful) {
                db.users().deleteByEmail(email)
                true
            } else false
        } catch (e: Exception) { false }
    }

    // Recuperación de contraseña
    suspend fun requestPasswordReset(email: String): Boolean {
        return try {
            val response = api.requestReset(normEmail(email))
            response.isSuccessful
        } catch (e: Exception) { false }
    }

    // Helpers privados
    private suspend fun syncUserToRoom(dto: UserProfileDto) {
        val userEntity = UserEntity(
            email = dto.email,
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
        if (db.users().update(userEntity) == 0) {
            db.users().insert(userEntity)
        }
    }

    suspend fun logout() { prefs.clear() }
    fun prefs() = prefs
}