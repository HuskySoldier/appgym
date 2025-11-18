package cl.gymtastic.app.data.local.dao

import androidx.room.*
import cl.gymtastic.app.data.local.entity.BookingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingsDao {
    @Insert
    suspend fun insert(b: BookingEntity): Long

    // --- NUEVO: Insertar múltiples (para sincronización) ---
    @Insert
    suspend fun insertAll(list: List<BookingEntity>)

    // --- NUEVO: Limpiar por usuario (para sincronización) ---
    @Query("DELETE FROM bookings WHERE userEmail = :userEmail")
    suspend fun clearByUser(userEmail: String)

    // Observa todas las reservas de un usuario específico, ordenadas por fecha descendente.
    //@param userEmail El email del usuario cuyas reservas se quieren observar.
    @Query("SELECT * FROM bookings WHERE userEmail = :userEmail ORDER BY fechaHora DESC")
    fun observeByUserEmail(userEmail: String): Flow<List<BookingEntity>>

    @Update
    suspend fun update(b: BookingEntity)

    @Delete
    suspend fun delete(b: BookingEntity)

    @Query("SELECT * FROM bookings WHERE id = :bookingId LIMIT 1")
    suspend fun findById(bookingId: Long): BookingEntity?
}