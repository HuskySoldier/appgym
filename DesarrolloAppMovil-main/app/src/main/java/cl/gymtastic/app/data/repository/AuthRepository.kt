package cl.gymtastic.app.data.repository

import android.content.Context
import cl.gymtastic.app.data.local.datastore.SessionPrefs // Asegúrate que SessionPrefs esté importado
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import cl.gymtastic.app.data.local.entity.UserEntity

class AuthRepository(private val context: Context) {
    private val db = GymTasticDatabase.get(context)
    private val prefs = SessionPrefs(context)

    // funciones auxiliares
    private fun normEmail(raw: String) = raw.trim().lowercase()
    fun hashPassword(raw: String) = raw.trim().hashCode().toString()

    suspend fun register(email: String, password: String, nombre: String): Boolean {
        val e = normEmail(email)
        val existing = db.users().findByEmail(e)
        if (existing != null) return false
        // Asumiendo que insert ignora conflictos (email ya existe)
        val result = db.users().insert(
            UserEntity(
                email = e,
                passHash = hashPassword(password),
                nombre = nombre,
                rol = "user"
            )
        )
        // insert devuelve Long: -1 si falla/ignora, rowId si tiene éxito.
        // register debe devolver true solo si la inserción fue exitosa.
        return result != -1L
    }


    suspend fun login(email: String, password: String): Boolean {
        val e = normEmail(email)
        val u = db.users().findByEmail(e) ?: return false // Usuario no encontrado
        if (u.passHash == hashPassword(password)) {
            // --- CAMBIO ---
            // Solo guardamos email y token
            prefs.setUserEmail(e)
            prefs.setToken("local-token-${e.hashCode()}")
            // Ya no llamamos a prefs.setUser(userIdInt, token)
            // --- FIN CAMBIO ---
            return true
        }
        return false // Contraseña incorrecta
    }


    suspend fun logout() {
        prefs.clear()
    }

    fun prefs() = prefs
}

