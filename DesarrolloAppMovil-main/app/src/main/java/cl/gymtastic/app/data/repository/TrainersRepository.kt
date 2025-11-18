package cl.gymtastic.app.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import cl.gymtastic.app.data.local.entity.TrainerEntity
import cl.gymtastic.app.util.ServiceLocator
import java.io.IOException
import retrofit2.HttpException

class TrainersRepository(context: Context) {
    private val db = GymTasticDatabase.get(context)
    private val dao = db.trainers()
    private val api = ServiceLocator.api() // <-- Obtener la API

    // Función para refrescar los datos desde la API
    suspend fun refreshTrainersFromApi() {
        try {
            Log.d("TrainersRepo", "Iniciando refresh de trainers desde la API")
            val response = api.getTrainers()
            if (response.isSuccessful) {
                val newTrainers = response.body() ?: emptyList()
                Log.d("TrainersRepo", "API OK. Trainers recibidos: ${newTrainers.size}")

                db.withTransaction {
                    // Borramos y reemplazamos todos los trainers para tener la lista fresca
                    // (Esto es simple, pero asume que TrainerEntity.id de la API es la fuente de verdad)
                    dao.insertAll(newTrainers)
                    Log.d("TrainersRepo", "Trainers actualizados en Room.")
                }
            } else {
                Log.e("TrainersRepo", "Error al obtener trainers: HTTP ${response.code()}")
            }
        } catch (e: IOException) {
            Log.e("TrainersRepo", "Error de red al obtener trainers", e)
        } catch (e: HttpException) {
            Log.e("TrainersRepo", "Error HTTP al obtener trainers", e)
        }
    }

    // El flujo sigue leyendo desde Room (Cache-First)
    fun observeAll() = dao.observeAll()

    // --- Funciones de Admin (Guardar/Eliminar, ahora solo usan Room, pero el admin real es el backend) ---

    /** Guarda (inserta o actualiza) un trainer. */
    suspend fun save(trainer: TrainerEntity) {
        // En un escenario real, llamarías a la API POST/PUT aquí.
        // ** NOTA: DEBES IMPLEMENTAR LA LLAMADA PUT/POST EN EL BACKEND REAL (trainersservice:8085)
        // Por ahora, solo guardamos localmente
        dao.save(trainer)
    }

    /** Elimina un trainer. */
    suspend fun delete(trainer: TrainerEntity) {
        // En un escenario real, llamarías a la API DELETE aquí.
        // ** NOTA: DEBES IMPLEMENTAR LA LLAMADA DELETE EN EL BACKEND REAL (trainersservice:8085)
        // Por ahora, solo eliminamos localmente
        dao.delete(trainer)
    }
}