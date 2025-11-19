package cl.gymtastic.app.data.repository

import android.content.Context
import android.util.Log
import cl.gymtastic.app.data.remote.BookingRequest
import cl.gymtastic.app.util.ServiceLocator
import java.io.IOException

class BookingsRepository(context: Context) {
    private val api = ServiceLocator.api()

    // Crear reserva directo en el backend
    suspend fun create(userEmail: String, trainerId: Long, fechaHora: Long) {
        val request = BookingRequest(
            userEmail = userEmail,
            trainerId = trainerId,
            fechaHora = fechaHora
        )
        try {
            val response = api.createBooking(request)
            if (!response.isSuccessful) {
                throw IOException("Error al agendar: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("BookingsRepo", "Error creando reserva", e)
            throw e // Re-lanzamos para manejarla en la UI
        }
    }

    // Obtener agenda del trainer (para el dashboard o validaciones)
    suspend fun getTrainerSchedule(trainerId: Long): List<BookingRequest> {
        return try {
            val response = api.getTrainerBookings(trainerId)
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Obtener reservas del usuario
    suspend fun getUserBookings(email: String): List<BookingRequest> {
        return try {
            val response = api.getUserBookings(email)
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}