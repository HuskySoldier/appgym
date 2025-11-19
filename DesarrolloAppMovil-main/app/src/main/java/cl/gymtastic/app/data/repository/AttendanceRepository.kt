package cl.gymtastic.app.data.repository

import android.content.Context
import android.util.Log
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import cl.gymtastic.app.data.local.entity.AttendanceEntity
import cl.gymtastic.app.util.ServiceLocator
import retrofit2.HttpException
import java.io.IOException

class AttendanceRepository(context: Context) {
    private val db = GymTasticDatabase.get(context)
    private val dao = db.attendance()
    private val api = ServiceLocator.api()

    // CORRECCIÓN: Quitamos 'private' para que HomeScreen pueda usarla
    suspend fun syncHistoryFromApi(userEmail: String) {
        try {
            val response = api.getAttendanceHistory(userEmail)
            if (response.isSuccessful) {
                val remoteHistory = response.body() ?: emptyList()
                val entities = remoteHistory.map { dto ->
                    AttendanceEntity(
                        id = dto.id,
                        userEmail = dto.userEmail,
                        timestamp = dto.timestamp,
                        checkOutTimestamp = dto.checkOutTimestamp
                    )
                }
                db.attendance().run {
                    clearByUser(userEmail)
                    insertAll(entities)
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

    fun observe(userEmail: String) = dao.observeByUser(userEmail)

    suspend fun checkIn(userEmail: String) {
        try {
            val response = api.checkIn(userEmail)
            if (!response.isSuccessful) {
                throw IOException("Error Check-In: ${response.errorBody()?.string() ?: "Error de servidor"}")
            }
            syncHistoryFromApi(userEmail)
        } catch (e: IOException) {
            throw IOException("Error de conexión al registrar Check-In: ${e.message}")
        }
    }

    suspend fun checkOut(userEmail: String) {
        try {
            val response = api.checkOut(userEmail)
            if (!response.isSuccessful) {
                throw IOException("Error Check-Out: ${response.errorBody()?.string() ?: "Error de servidor"}")
            }
            syncHistoryFromApi(userEmail)
        } catch (e: IOException) {
            throw IOException("Error de conexión al registrar Check-Out: ${e.message}")
        }
    }
}