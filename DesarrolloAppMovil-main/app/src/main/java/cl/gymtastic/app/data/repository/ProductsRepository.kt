package cl.gymtastic.app.data.local

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import cl.gymtastic.app.data.local.dao.ProductStockProjection
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import cl.gymtastic.app.data.local.entity.CartItemEntity
import cl.gymtastic.app.data.local.entity.ProductEntity
import cl.gymtastic.app.util.ServiceLocator
import retrofit2.HttpException
import java.io.IOException

// Excepción para informar faltantes sin tocar el carrito
class InsufficientStockException(
    val shortages: List<Pair<Long /*productId*/, Int /*requested*/>>
) : Exception("Stock insuficiente para algunos productos")

class ProductsRepository(context: Context) {
    private val db = GymTasticDatabase.get(context)
    private val dao = db.products()
    private val api = ServiceLocator.api()

    // Función para refrescar los datos desde la API
    suspend fun refreshProductsFromApi() {
        try {
            Log.d("ProductsRepo", "Iniciando refresh de productos desde la API")
            val response = api.getProducts()
            if (response.isSuccessful) {
                val newProducts = response.body() ?: emptyList()
                Log.d("ProductsRepo", "API OK. Productos recibidos: ${newProducts.size}")

                db.withTransaction {
                    // dao.clearAll() // Opcional: limpiar antes si quieres borrado completo
                    dao.insertAll(newProducts)
                    Log.d("ProductsRepo", "Productos actualizados en Room.")
                }
            } else {
                Log.e("ProductsRepo", "Error al obtener productos: HTTP ${response.code()}")
            }
        } catch (e: IOException) {
            Log.e("ProductsRepo", "Error de red al obtener productos", e)
        } catch (e: HttpException) {
            Log.e("ProductsRepo", "Error HTTP al obtener productos", e)
        }
    }

    // --- ADMIN: Crear/Actualizar Producto ---
    suspend fun save(product: ProductEntity) {
        try {
            val response = if (product.id == 0) {
                api.createProduct(product) // ID 0 = Crear
            } else {
                api.updateProduct(product.id, product) // ID > 0 = Editar
            }

            if (response.isSuccessful && response.body() != null) {
                // Guardamos en local lo que respondió el servidor (que tendrá el ID real)
                dao.save(response.body()!!)
                Log.d("ProductsRepo", "Producto guardado en backend y local")
            } else {
                Log.e("ProductsRepo", "Error guardando producto: ${response.code()}")
                throw Exception("Error del servidor al guardar producto")
            }
        } catch (e: Exception) {
            Log.e("ProductsRepo", "Excepción guardando producto", e)
            throw e
        }
    }

    // --- ADMIN: Eliminar Producto ---
    suspend fun delete(product: ProductEntity) {
        try {
            val response = api.deleteProduct(product.id)
            if (response.isSuccessful) {
                dao.delete(product)
                Log.d("ProductsRepo", "Producto eliminado en backend y local")
            } else {
                Log.e("ProductsRepo", "Error eliminando producto: ${response.code()}")
                throw Exception("Error del servidor al eliminar producto")
            }
        } catch (e: Exception) {
            Log.e("ProductsRepo", "Excepción eliminando producto", e)
            throw e
        }
    }

    // ⚡ Tipos por id (plan/merch)
    suspend fun getTypesById(ids: List<Long>): Map<Long, String> {
        if (ids.isEmpty()) return emptyMap()
        val intIds = ids.map { it.toInt() }
        return dao.getByIds(intIds).associate { it.id.toLong() to it.tipo }
    }

    // ⚡ Nombres por id
    suspend fun getNamesById(ids: List<Long>): Map<Long, String> {
        if (ids.isEmpty()) return emptyMap()
        val intIds = ids.map { it.toInt() }
        return dao.getNamesByIds(intIds).associate { it.id.toLong() to it.nombre }
    }

    // Flujos (LEEN DE ROOM)
    fun observePlanes() = dao.observePlanes()
    fun observeMerch() = dao.observeMerch()

    suspend fun getAll() = dao.getAll()

    suspend fun getStockByIds(ids: List<Long>): List<ProductStockProjection> {
        if (ids.isEmpty()) return emptyList()
        val intIds = ids.map { it.toInt() }
        return dao.getStockByIds(intIds)
    }

    // Placeholder para lógica antigua
    suspend fun reserveAndDecrementMerchStock(
        items: List<CartItemEntity>,
        typesById: Map<Long, String>? = null
    ) {
        // Lógica movida al backend (CheckoutService)
    }
}