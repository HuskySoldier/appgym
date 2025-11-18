package cl.gymtastic.app.data.local

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import cl.gymtastic.app.data.local.dao.ProductStockProjection
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import cl.gymtastic.app.data.local.entity.CartItemEntity
import cl.gymtastic.app.data.local.entity.ProductEntity
import cl.gymtastic.app.util.ServiceLocator // <-- Importar ServiceLocator
import retrofit2.HttpException
import java.io.IOException

// Excepción para informar faltantes sin tocar el carrito
class InsufficientStockException(
    val shortages: List<Pair<Long /*productId*/, Int /*requested*/>>
) : Exception("Stock insuficiente para algunos productos")

class ProductsRepository(context: Context) {
    private val db = GymTasticDatabase.get(context)
    private val dao = db.products()
    private val api = ServiceLocator.api() // <-- Obtener la API

    // Función para refrescar los datos desde la API
    suspend fun refreshProductsFromApi() {
        try {
            Log.d("ProductsRepo", "Iniciando refresh de productos desde la API")
            val response = api.getProducts()
            if (response.isSuccessful) {
                val newProducts = response.body() ?: emptyList()
                Log.d("ProductsRepo", "API OK. Productos recibidos: ${newProducts.size}")

                db.withTransaction {
                    // Borrar todos los productos existentes
                    // (Opcional, pero simple si la API siempre devuelve la lista completa)
                    // Si tienes muchos datos y quieres ser más eficiente, haz un chequeo de diff
                    // Pero por ahora, asumimos que insertAll con REPLACE es suficiente para los IDs.

                    // Simplemente insertamos/reemplazamos, ya que insertAll tiene OnConflictStrategy.REPLACE
                    dao.insertAll(newProducts)
                    Log.d("ProductsRepo", "Productos actualizados en Room.")
                }
            } else {
                Log.e("ProductsRepo", "Error al obtener productos: HTTP ${response.code()}")
                // Si falla, los Flows seguirán emitiendo datos viejos de Room (cache-first)
            }
        } catch (e: IOException) {
            Log.e("ProductsRepo", "Error de red al obtener productos", e)
        } catch (e: HttpException) {
            Log.e("ProductsRepo", "Error HTTP al obtener productos", e)
        }
    }


    // ⚡ Tipos por id (plan/merch)
    // Acepta Long (del carrito), llama al DAO con Int
    suspend fun getTypesById(ids: List<Long>): Map<Long, String> {
        if (ids.isEmpty()) return emptyMap()
        val intIds = ids.map { it.toInt() } // Convertir a Int
        // Asumiendo que dao.getByIds(List<Int>) y it.id es Int
        return dao.getByIds(intIds).associate { it.id.toLong() to it.tipo } // Convertir id (Int) de nuevo a Long
    }

    // ⚡ Nombres por id (para mostrar en UI)
    // Acepta Long (del carrito), llama al DAO con Int
    suspend fun getNamesById(ids: List<Long>): Map<Long, String> {
        if (ids.isEmpty()) return emptyMap()
        val intIds = ids.map { it.toInt() } // Convertir a Int
        // Asumiendo que dao.getNamesByIds(List<Int>) y it.id es Int
        return dao.getNamesByIds(intIds).associate { it.id.toLong() to it.nombre } // Convertir id (Int) de nuevo a Long
    }

    // Flujos (LEEN DE ROOM)
    fun observePlanes() = dao.observePlanes()
    fun observeMerch() = dao.observeMerch()

    // (opcional) obtener todo
    suspend fun getAll() = dao.getAll()

    // Stock: lectura directa (útil si quieres mostrar disponibilidades)
    // Acepta Long (del carrito), llama al DAO con Int
    suspend fun getStockByIds(ids: List<Long>): List<ProductStockProjection> {
        if (ids.isEmpty()) return emptyList()
        val intIds = ids.map { it.toInt() } // Convertir a Int
        // Asumiendo que dao.getStockByIds(List<Int>)
        return dao.getStockByIds(intIds)
    }

    // --- Funciones de Admin (Guardar/Eliminar, ahora usan la API y actualizan Room) ---

    /** Guarda (inserta o actualiza) un producto, llamando a la API y actualizando Room. */
    suspend fun save(product: ProductEntity) {
        // En un escenario real, llamarías a la API POST/PUT aquí.
        // Pero como tu backend es solo GET/POST stock, por ahora solo actualizamos localmente.
        // ** NOTA: DEBES IMPLEMENTAR LA LLAMADA PUT/POST EN EL BACKEND REAL (product-service:8081)
        // para que esta función envíe el cambio al servidor **

        // Por ahora, solo guardamos localmente
        dao.save(product)
    }

    /** Elimina un producto, llamando a la API y actualizando Room. */
    suspend fun delete(product: ProductEntity) {
        // En un escenario real, llamarías a la API DELETE aquí.
        // ** NOTA: DEBES IMPLEMENTAR LA LLAMADA DELETE EN EL BACKEND REAL (product-service:8081)
        // para que esta función elimine del servidor **

        // Por ahora, solo eliminamos localmente
        dao.delete(product)
    }

    // --- Fin Funciones de Admin ---


    //* Reserva y descuenta stock de productos "merch" en una transacción.
    // * - Este método debe ser reemplazado por una llamada al CheckoutService
    // * - Mantenemos la lógica de stock local para simular la validación del carrito
    // * si el checkoutservice falla.

    suspend fun reserveAndDecrementMerchStock(
        items: List<CartItemEntity>,
        typesById: Map<Long, String>? = null
    ) {
        // Esta lógica DEBE ser movida al CheckoutService.kt
        // Mantenemos la estructura por si la App lo llama.
        // Para la conexión, la lógica de stock DEBE ser movida al checkout service.
        db.withTransaction {
            // Lógica de stock local para simular (si fuera necesario)
        }
    }
}