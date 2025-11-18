package cl.gymtastic.app.data.repository

import android.content.Context
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import cl.gymtastic.app.data.local.entity.BookingEntity

// Ejemplo en BookingRepository.kt
class BookingsRepository(context: Context) {
    private val db = GymTasticDatabase.get(context)
    private val dao = db.bookings() // Asumiendo que tienes 'bookings()' en tu DB

    // --- MODIFICADO: Eliminado el parámetro userId ---
    suspend fun create(userEmail: String, trainerId: Long, fechaHora: Long) {
        val newBooking = BookingEntity(
            userEmail = userEmail,
            trainerId = trainerId,
            fechaHora = fechaHora
        )
        dao.insert(newBooking) // Asume que dao.insert espera BookingEntity sin userId
    }

}