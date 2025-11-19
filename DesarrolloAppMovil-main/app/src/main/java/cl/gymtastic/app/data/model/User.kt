package cl.gymtastic.app.data.model

import java.io.Serializable

data class User(
    val email: String,
    val nombre: String,
    val rol: String,
    // Datos de suscripción
    val planEndMillis: Long? = null,
    val sedeId: Int? = null,
    val sedeName: String? = null,
    val sedeLat: Double? = null,
    val sedeLng: Double? = null,
    // Perfil
    val avatarUri: String? = null,
    val fono: String? = null,
    val bio: String? = null
) : Serializable {
    val hasActivePlan: Boolean
        get() {
            val end = planEndMillis ?: return false
            return end > System.currentTimeMillis()
        }
}