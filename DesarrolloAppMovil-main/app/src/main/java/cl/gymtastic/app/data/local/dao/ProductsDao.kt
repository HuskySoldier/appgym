package cl.gymtastic.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import cl.gymtastic.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

// Proyección liviana para id + nombre (ya la tenías)
data class ProductNameProjection(
    val id: Int,
    val nombre: String
)

// ✅ Proyección para leer stock
data class ProductStockProjection(
    val id: Int,
    val stock: Int?
)



@Dao
interface ProductsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Query("SELECT id, stock FROM products WHERE id IN (:ids)")
    suspend fun getStockByIds(ids: List<Int>): List<ProductStockProjection>


    @Query("SELECT * FROM products")
    suspend fun getAll(): List<ProductEntity>

    // --- NUEVO: Limpiar todo (para sincronización) ---
    @Query("DELETE FROM products")
    suspend fun clearAll()

    @Query("SELECT * FROM products WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Int>): List<ProductEntity>

    @Delete
    suspend fun delete(product: ProductEntity)

    @Upsert
    suspend fun save(product: ProductEntity)

    @Query("SELECT id, nombre FROM products WHERE id IN (:ids)")
    suspend fun getNamesByIds(ids: List<Int>): List<ProductNameProjection>

    // Flujos por tipo
    @Query("SELECT * FROM products WHERE tipo = 'plan'")
    fun observePlanes(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE tipo = 'merch'")
    fun observeMerch(): Flow<List<ProductEntity>>

    // 🔽 Decremento condicional de stock (solo merch) — devuelve 1 si actualizó, 0 si no alcanzó
    @Query("""
        UPDATE products
        SET stock = stock - :qty
        WHERE id = :id
          AND tipo = 'merch'
          AND stock IS NOT NULL
          AND stock >= :qty
    """)
    suspend fun tryDecrementStock(id: Int, qty: Int): Int
}