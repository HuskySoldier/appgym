package cl.gymtastic.app.data.repository

import android.content.Context
import cl.gymtastic.app.data.local.db.GymTasticDatabase
// No necesitas importar ProductEntity aquí
import cl.gymtastic.app.data.local.entity.TrainerEntity

class TrainersRepository(context: Context) {
    private val db = GymTasticDatabase.get(context)
    private val dao = db.trainers()

    // --- ERROR CORREGIDO ---
    // Solo necesitas una de estas líneas, la otra era un duplicado.
    fun observeAll() = dao.observeAll()
    // fun observeAll() = db.trainers().observeAll() // <- Esta línea sobraba y causaba el conflicto


    //* Guarda (inserta o actualiza) un trainer.
    //* (Parámetro renombrado de 'product' a 'trainer' para claridad)

    suspend fun save(trainer: TrainerEntity) {
        dao.save(trainer)
    }


    //* Elimina un trainer.
    //* (Parámetro renombrado de 'product' a 'trainer' y comentario actualizado)

    suspend fun delete(trainer: TrainerEntity) {
        dao.delete(trainer)
    }
}
