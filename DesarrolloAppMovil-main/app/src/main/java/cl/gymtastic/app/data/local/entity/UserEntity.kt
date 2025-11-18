package cl.gymtastic.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val passHash: String,
    val nombre: String, // Este ya existía
    val rol: String,

    // --- Campos de Suscripción ---
    val planEndMillis: Long? = null,
    val sedeId: Int? = null,
    val sedeName: String? = null,
    val sedeLat: Double? = null,
    val sedeLng: Double? = null,

    // --- Campo Avatar ---
    val avatarUri: String? = null,

    // --- NUEVOS CAMPOS DE PERFIL ---
    /** Teléfono del usuario (opcional) */
    val fono: String? = null, // <-- AÑADIDO
    /** Biografía corta del usuario (opcional) */
    val bio: String? = null // <-- AÑADIDO

) : Serializable {

    /** Lógica de negocio para el plan activo */
    val hasActivePlan: Boolean
        get() {
            val end = planEndMillis ?: return false
            return end > System.currentTimeMillis()
        }
}
