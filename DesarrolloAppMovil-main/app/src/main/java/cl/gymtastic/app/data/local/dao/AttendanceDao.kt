package cl.gymtastic.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.gymtastic.app.data.local.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {


    //el DAO es la capa que traduce las peticiones del Repository en acciones concretas sobre la base de datos
     //Inserta un nuevo registro de asistencia.

    @Insert
    suspend fun insert(reg: AttendanceEntity): Long


    //* Observa (Flow) todos los registros de asistencia para un usuario específico,
    //* ordenados por fecha descendente (más recientes primero).
    //* @param userEmail El email del usuario cuyos registros se quieren observar.
    // @return Un Flow que emite la lista de AttendanceEntity para ese usuario.

    @Query("SELECT * FROM attendance WHERE userEmail = :userEmail ORDER BY timestamp DESC")
    fun observeByUser(userEmail: String): Flow<List<AttendanceEntity>>


    // * Busca el ID del último registro de check-in que AÚN NO tiene check-out (checkOutTimestamp IS NULL)
    // * para un usuario específico.
    // * @param userEmail El email del usuario.
    // * @return El ID (Long) del último registro abierto, o null si no hay ninguno abierto.

    @Query("SELECT id FROM attendance WHERE userEmail = :userEmail AND checkOutTimestamp IS NULL ORDER BY timestamp DESC LIMIT 1")
    suspend fun findLastOpenAttendanceId(userEmail: String): Long?


    // * Actualiza el timestamp de check-out para un registro de asistencia específico, identificado por su ID.
    // * @param attendanceId El ID del registro de asistencia a actualizar.
    //* @param checkOut El timestamp (en milisegundos) del momento del check-out.

    @Query("UPDATE attendance SET checkOutTimestamp = :checkOut WHERE id = :attendanceId")
    suspend fun updateCheckOutById(attendanceId: Long, checkOut: Long)
}
