package cl.gymtastic.app.data.repository

import android.content.Context
import android.util.Log
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import cl.gymtastic.app.data.local.entity.AttendanceEntity
import cl.gymtastic.app.data.remote.AttendanceHistoryResponse
import cl.gymtastic.app.util.ServiceLocator
import retrofit2.HttpException
import java.io.IOException

//Es el encargado de decidir cómo obtener o guardar los datos,
// comunicándose con el DAO (el experto en base de datos) para realizar las operaciones necesarias.
class AttendanceRepository(context: Context) {
    private val db = GymTasticDatabase.get(context)
    private val dao = db.attendance()
    private val api = ServiceLocator.api() // <-- Obtener la API


    /**
     * Sincroniza la historia de asistencia desde la API y la guarda en Room.
     */
    private suspend fun syncHistoryFromApi(userEmail: String) {
        try {
            val response = api.getAttendanceHistory(userEmail)
            if (response.isSuccessful) {
                val remoteHistory = response.body() ?: emptyList()
                Log.d("AttendanceRepo", "Historia de asistencia sincronizada. Registros: ${remoteHistory.size}")

                // Convertir DTOs de la API a Entidades de Room
                val entities = remoteHistory.map { dto ->
                    AttendanceEntity(
                        id = dto.id,
                        userEmail = dto.userEmail,
                        timestamp = dto.timestamp,
                        checkOutTimestamp = dto.checkOutTimestamp
                    )
                }

                // Usamos una transacción para mantener la consistencia
                db.attendance().run {
                    clearByUser(userEmail) // Borra todos los registros antiguos de este usuario
                    insertAll(entities)    // Inserta los nuevos
                }

            } else if (response.code() == 404) {
                Log.w("AttendanceRepo", "Usuario no encontrado en servicio de asistencia (404)")
            } else {
                Log.e("AttendanceRepo", "Error al obtener historial: HTTP ${response.code()}")
            }
        } catch (e: IOException) {
            Log.e("AttendanceRepo", "Error de red al sincronizar historial", e)
        } catch (e: HttpException) {
            Log.e("AttendanceRepo", "Error HTTP al sincronizar historial", e)
        }
    }

    /**
     * Devuelve el historial desde Room (Cache) y lanza la sincronización desde la API.
     */
    fun observe(userEmail: String) = dao.observeByUser(userEmail) // <-- LEE DE ROOM

    /**
     * Registra un Check-In llamando a la API y luego sincroniza la historia.
     */
    suspend fun checkIn(userEmail: String) {
        try {
            val response = api.checkIn(userEmail) // <-- LLAMA A LA API
            if (!response.isSuccessful) {
                // Captura 400 Bad Request si ya hay una sesión abierta
                throw IOException("Error Check-In: ${response.errorBody()?.string() ?: "Error de servidor"}")
            }
            // Si el check-in es exitoso, sincroniza Room con la nueva historia
            syncHistoryFromApi(userEmail)
        } catch (e: IOException) {
            throw IOException("Error de conexión al registrar Check-In: ${e.message}")
        }
    }

    /**
     * Registra un Check-Out llamando a la API y luego sincroniza la historia.
     */
    suspend fun checkOut(userEmail: String) {
        try {
            val response = api.checkOut(userEmail) // <-- LLAMA A LA API
            if (!response.isSuccessful) {
                // Captura 400 Bad Request si no hay una sesión abierta
                throw IOException("Error Check-Out: ${response.errorBody()?.string() ?: "Error de servidor"}")
            }
            // Si el check-out es exitoso, sincroniza Room con la nueva historia
            syncHistoryFromApi(userEmail)
        } catch (e: IOException) {
            throw IOException("Error de conexión al registrar Check-Out: ${e.message}")
        }
    }
}