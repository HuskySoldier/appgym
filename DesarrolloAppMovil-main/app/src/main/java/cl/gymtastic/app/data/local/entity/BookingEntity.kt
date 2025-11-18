package cl.gymtastic.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userEmail: String, // Identificador del usuario
    val trainerId: Long, // Identificador del trainer (asegúrate que sea Long si TrainerEntity.id es Long)
    val fechaHora: Long, // Fecha y hora en milisegundos
    val estado: String = "pendiente" // Ejemplo: pendiente, confirmada, cancelada
)