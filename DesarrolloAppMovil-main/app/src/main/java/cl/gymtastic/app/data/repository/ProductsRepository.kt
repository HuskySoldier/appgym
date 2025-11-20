package cl.gymtastic.app.data.repository

import android.content.Context
import android.util.Log
import cl.gymtastic.app.data.model.Product // <--- Nuevo Modelo
import cl.gymtastic.app.util.ServiceLocator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ProductsRepository(context: Context) {


    private val api = ServiceLocator.api()

    // --- LECTURA (API Directa) ---
    suspend fun getAll(): List<Product> {
        return try {
            val response = api.getProducts()
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            Log.e("ProductsRepo", "Error API", e)
            emptyList()
        }
    }

    // Helpers para filtrar (en memoria, ya que la API trae todo)
    private suspend fun getPlanes(): List<Product> = getAll().filter { it.tipo == "plan" }
    private suspend fun getMerch(): List<Product> = getAll().filter { it.tipo == "merch" }

    // Flujos de compatibilidad para la UI
    fun observePlanes(): Flow<List<Product>> = flow { emit(getPlanes()) }
    fun observeMerch(): Flow<List<Product>> = flow { emit(getMerch()) }

    // --- HELPERS CARRITO ---
    suspend fun getNamesById(ids: List<Long>): Map<Long, String> {
        if (ids.isEmpty()) return emptyMap()
        val all = getAll()
        return all.filter { it.id.toLong() in ids }.associate { it.id.toLong() to it.nombre }
    }


    suspend fun getTypesById(ids: List<Long>): Map<Long, String> {
        if (ids.isEmpty()) return emptyMap()
        val all = getAll()
        return all.filter { it.id.toLong() in ids }.associate { it.id.toLong() to it.tipo }
    }

    suspend fun getStockByIds(ids: List<Long>): List<Pair<Long, Int>> {
        if (ids.isEmpty()) return emptyList()
        val all = getAll()
        return all.filter { it.id.toLong() in ids }.map { it.id.toLong() to (it.stock ?: 0) }
    }

    // --- ADMIN ---
    suspend fun save(product: Product) {
        if (product.id == 0) api.createProduct(product) else api.updateProduct(product.id, product)
    }

    suspend fun delete(product: Product) {
        api.deleteProduct(product.id)
    }
}