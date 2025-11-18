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
    private val api = ServiceLocator.api()

    // Función para refrescar los datos desde la API
    suspend fun refreshTrainersFromApi() {
        try {
            Log.d("TrainersRepo", "Iniciando refresh de trainers desde la API")
            val response = api.getTrainers()
            if (response.isSuccessful) {
                val newTrainers = response.body() ?: emptyList()
                Log.d("TrainersRepo", "API OK. Trainers recibidos: ${newTrainers.size}")

                db.withTransaction {
                    // dao.clearAll()
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

    // --- ADMIN: Guardar Trainer ---
    suspend fun save(trainer: TrainerEntity) {
        try {
            val response = if (trainer.id == 0L) {
                api.createTrainer(trainer)
            } else {
                api.updateTrainer(trainer.id, trainer)
            }

            if (response.isSuccessful && response.body() != null) {
                dao.save(response.body()!!)
                Log.d("TrainersRepo", "Trainer guardado en backend y local")
            } else {
                throw Exception("Error al guardar trainer: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("TrainersRepo", "Excepción guardando trainer", e)
            throw e
        }
    }

    // --- ADMIN: Eliminar Trainer ---
    suspend fun delete(trainer: TrainerEntity) {
        try {
            val response = api.deleteTrainer(trainer.id)
            if (response.isSuccessful) {
                dao.delete(trainer)
                Log.d("TrainersRepo", "Trainer eliminado en backend y local")
            } else {
                throw Exception("Error al eliminar trainer: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("TrainersRepo", "Excepción eliminando trainer", e)
            throw e
        }
    }
}