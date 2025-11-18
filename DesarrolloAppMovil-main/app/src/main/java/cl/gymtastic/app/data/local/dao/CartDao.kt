package cl.gymtastic.app.data.local.dao

import androidx.room.*
import cl.gymtastic.app.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity): Long

    @Query("SELECT * FROM cart_items")
    fun observeAll(): Flow<List<CartItemEntity>>

    @Query("DELETE FROM cart_items")
    suspend fun clear()

    @Delete
    suspend fun delete(item: CartItemEntity)

    @Query("SELECT COALESCE(SUM(qty), 0) FROM cart_items WHERE productId = :productId")
    suspend fun getQtyFor(productId: Long): Int




    @Query("DELETE FROM cart_items WHERE productId IN (:productIds)")
    suspend fun removeByProductIds(productIds: List<Long>)
}