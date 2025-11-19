package cl.gymtastic.app.data.repository

import android.content.Context
import android.util.Log
import cl.gymtastic.app.data.model.Trainer // <-- Usamos el nuevo Modelo
import cl.gymtastic.app.util.ServiceLocator

class TrainersRepository(context: Context) {
    private val api = ServiceLocator.api()

    // Ya no usamos observeAll() con Flow de Room.
    // Usamos una función suspendida para obtener los datos frescos.
    suspend fun getTrainers(): List<Trainer> {
        return try {
            val response = api.getTrainers()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                Log.e("TrainersRepo", "Error API: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("TrainersRepo", "Excepción obteniendo trainers", e)
            emptyList()
        }
    }

    // Métodos de Admin (directo a API)
    suspend fun save(trainer: Trainer) {
        if (trainer.id == 0L) api.createTrainer(trainer)
        else api.updateTrainer(trainer.id, trainer)
    }

    suspend fun delete(id: Long) {
        api.deleteTrainer(id)
    }
}