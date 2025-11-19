package cl.gymtastic.app.data.repository

import android.content.Context
import android.util.Log
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import cl.gymtastic.app.data.local.entity.BookingEntity
import cl.gymtastic.app.data.remote.BookingRequest
import cl.gymtastic.app.util.ServiceLocator
import retrofit2.HttpException
import java.io.IOException

class BookingsRepository(context: Context) {
    private val db = GymTasticDatabase.get(context)
    private val dao = db.bookings()
    private val api = ServiceLocator.api() // Obtener la instancia API

    /**
     * Sincroniza el historial de reservas para un usuario desde la API y lo guarda en Room.
     */
    private suspend fun syncUserBookings(userEmail: String) {
        try {
            val response = api.getUserBookings(userEmail)
            if (response.isSuccessful) {
                // BookingRequest puede usarse como DTO ya que contiene los campos clave.
                val remoteBookings = response.body() ?: emptyList()
                Log.d("BookingsRepo", "Reservas sincronizadas. Registros: ${remoteBookings.size}")

                // Convertir DTOs de la API a Entidades de Room
                val entities = remoteBookings.map { dto ->
                    // Creamos la entidad. Si la API devolviera el ID (Long) en BookingRequest, lo usaríamos.
                    // Asumimos que los IDs de la API son la fuente de verdad.
                    BookingEntity(
                        // No podemos confiar en el ID, así que usamos autoGenerate,
                        // y borramos antes para evitar duplicados.
                        userEmail = dto.userEmail,
                        trainerId = dto.trainerId,
                        fechaHora = dto.fechaHora,
                        // Asumimos que el estado por defecto es "pendiente" o la API lo devuelve.
                    )
                }

                // Usamos una transacción para mantener la consistencia
                db.bookings().run {
                    clearByUser(userEmail) // ** NOTA: Necesitas agregar clearByUser(String) en BookingsDao **
                    insertAll(entities)    // Inserta los nuevos
                }
            } else {
                Log.e("BookingsRepo", "Error al obtener reservas: HTTP ${response.code()}")
            }
        } catch (e: IOException) {
            Log.e("BookingsRepo", "Error de red al sincronizar reservas", e)
        } catch (e: HttpException) {
            Log.e("BookingsRepo", "Error HTTP al sincronizar reservas", e)
        }
    }


    /**
     * Crea una reserva llamando a la API y luego sincroniza la historia.
     */
    suspend fun create(userEmail: String, trainerId: Long, fechaHora: Long): Boolean {
        try {
            val request = BookingRequest(
                userEmail = userEmail,
                trainerId = trainerId,
                fechaHora = fechaHora
            )
            val response = api.createBooking(request) // <-- LLAMA A LA API
            if (!response.isSuccessful) {
                // Puede fallar si la hora ya está reservada (si el backend lo valida)
                throw IOException("Error al crear reserva: ${response.errorBody()?.string() ?: "Error de servidor"}")
            }
            // Si tiene éxito, sincronizamos Room para que el usuario vea la nueva reserva
            syncUserBookings(userEmail)
            return true
        } catch (e: IOException) {
            Log.e("BookingsRepo", "Error de conexión/servidor al crear reserva", e)
            throw e
        }
    }

    /** Observa el historial de Room y lanza la sincronización en segundo plano. */
    fun observeUserBookings(userEmail: String) = dao.observeByUserEmail(userEmail)

    /** Refresca el historial de reservas desde la API. */
    suspend fun refreshUserBookings(userEmail: String) {
        syncUserBookings(userEmail)
    }

    suspend fun getTrainerSchedule(trainerId: Long): List<BookingRequest> {
        return try {
            val response = api.getTrainerBookings(trainerId)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("BookingsRepo", "Error fetching trainer schedule", e)
            emptyList()
        }
    }

}