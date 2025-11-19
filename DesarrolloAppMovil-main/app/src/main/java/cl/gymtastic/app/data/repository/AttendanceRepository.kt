package cl.gymtastic.app.data.repository

import android.content.Context
import android.util.Log
import cl.gymtastic.app.data.model.Attendance // <-- Nuevo modelo
import cl.gymtastic.app.util.ServiceLocator
import java.io.IOException

class AttendanceRepository(context: Context) {
    private val api = ServiceLocator.api()

    // Obtener historial directo de la API
    suspend fun getHistory(userEmail: String): List<Attendance> {
        return try {
            val response = api.getAttendanceHistory(userEmail)
            if (response.isSuccessful) {
                // Mapeamos del DTO al Modelo
                response.body()?.map { dto ->
                    Attendance(
                        id = dto.id,
                        userEmail = dto.userEmail,
                        timestamp = dto.timestamp,
                        checkOutTimestamp = dto.checkOutTimestamp
                    )
                } ?: emptyList()
            } else {
                Log.e("AttendanceRepo", "Error API: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("AttendanceRepo", "Excepción red", e)
            emptyList()
        }
    }

    // Check-In directo a API
    suspend fun checkIn(userEmail: String) {
        try {
            val response = api.checkIn(userEmail)
            if (!response.isSuccessful) {
                throw IOException("Error Check-In: ${response.message()}")
            }
        } catch (e: Exception) {
            throw IOException("Error de conexión al registrar Check-In", e)
        }
    }

    // Check-Out directo a API
    suspend fun checkOut(userEmail: String) {
        try {
            val response = api.checkOut(userEmail)
            if (!response.isSuccessful) {
                throw IOException("Error Check-Out: ${response.message()}")
            }
        } catch (e: Exception) {
            throw IOException("Error de conexión al registrar Check-Out", e)
        }
    }

    // Método de compatibilidad (si alguien llama a syncHistoryFromApi, no hace nada o solo loguea)
    suspend fun syncHistoryFromApi(userEmail: String) {
        // Ya no es necesario sincronizar a local, pero lo dejamos vacío para no romper HomeScreen si lo llama
        Log.d("AttendanceRepo", "syncHistoryFromApi llamado (Backend-Only mode)")
    }
}