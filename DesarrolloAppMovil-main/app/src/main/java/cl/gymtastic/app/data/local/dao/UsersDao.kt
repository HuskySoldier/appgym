package cl.gymtastic.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cl.gymtastic.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsersDao {
    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(user: UserEntity): Long

    /** Actualiza el estado de suscripción */
    @Query("UPDATE users SET planEndMillis=:planEndMillis, sedeId=:sedeId, sedeName=:sedeName, sedeLat=:sedeLat, sedeLng=:sedeLng WHERE email = :email")
    suspend fun updateSubscription(email: String, planEndMillis: Long?, sedeId: Int?, sedeName: String?, sedeLat: Double?, sedeLng: Double?)

    /** Observa un usuario por email */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun observeByEmail(email: String): Flow<UserEntity?>

    // --- FUNCIONES PARA ADMIN ---
    @Query("SELECT * FROM users WHERE email != :excludeEmail ORDER BY email ASC")
    fun observeAllExcept(excludeEmail: String): Flow<List<UserEntity>>

    @Query("UPDATE users SET passHash = :newPassHash WHERE email = :email")
    suspend fun updatePasswordHash(email: String, newPassHash: String): Int

    @Update
    suspend fun update(user: UserEntity): Int

    @Query("DELETE FROM users WHERE email = :email")
    suspend fun deleteByEmail(email: String): Int

    @Delete
    suspend fun delete(user: UserEntity): Int

    @Query("UPDATE users SET avatarUri = :avatarUri WHERE email = :email")
    suspend fun updateAvatarUri(email: String, avatarUri: String?): Int

    @Query("UPDATE users SET rol = :newRole WHERE email = :email")
    suspend fun updateUserRole(email: String, newRole: String): Int

    // NUEVO Metodo para actualizar perfil

    //  * Actualiza nombre, fono y bio de un usuario específico.
    // * @param email Email del usuario.
    // * @param newName Nuevo nombre.
    // * @param newFono Nuevo teléfono (o null).
    // * @param newBio Nueva biografía (o null).

    @Query("UPDATE users SET nombre = :newName, fono = :newFono, bio = :newBio WHERE email = :email")
    suspend fun updateProfileDetails(email: String, newName: String, newFono: String?, newBio: String?): Int // Devuelve filas afectadas

}

