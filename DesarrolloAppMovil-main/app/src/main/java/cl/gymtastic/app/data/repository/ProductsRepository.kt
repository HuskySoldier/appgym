package cl.gymtastic.app.data.local

import android.content.Context
import androidx.room.withTransaction
import cl.gymtastic.app.data.local.dao.ProductStockProjection
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import cl.gymtastic.app.data.local.entity.CartItemEntity
import cl.gymtastic.app.data.local.entity.ProductEntity //  Importación añadida

// Excepción para informar faltantes sin tocar el carrito
class InsufficientStockException(
    val shortages: List<Pair<Long /*productId*/, Int /*requested*/>>
) : Exception("Stock insuficiente para algunos productos")

class ProductsRepository(context: Context) {
    private val db = GymTasticDatabase.get(context)
    private val dao = db.products()

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

    // Flujos
    fun observePlanes() = dao.observePlanes()
    fun observeMerch() = dao.observeMerch()

    // (opcional) obtener todo
    suspend fun getAll() = dao.getAll()

    //  Stock: lectura directa (útil si quieres mostrar disponibilidades)
    // Acepta Long (del carrito), llama al DAO con Int
    suspend fun getStockByIds(ids: List<Long>): List<ProductStockProjection> {
        if (ids.isEmpty()) return emptyList()
        val intIds = ids.map { it.toInt() } // Convertir a Int
        // Asumiendo que dao.getStockByIds(List<Int>)
        return dao.getStockByIds(intIds)
    }

    // --- Funciones de Admin (Añadidas) ---


    //* Guarda (inserta o actualiza) un producto.

    suspend fun save(product: ProductEntity) {
        dao.save(product) // Asumiendo que dao.save() existe
    }


    //* Elimina un producto.
    //* Asume que tu DAO tiene un método @Delete

    suspend fun delete(product: ProductEntity) {
        dao.delete(product) // Asumiendo que dao.delete() existe
    }

    // --- Fin Funciones de Admin ---



    //* Reserva y descuenta stock de productos "merch" en una transacción.
    // * - Si alguno no tiene stock suficiente, lanza InsufficientStockException y NO descuenta nada.
    //* - Los items "plan" se ignoran aquí (no usan stock).

    suspend fun reserveAndDecrementMerchStock(
        items: List<CartItemEntity>,
        // Si ya tienes un map de tipos, puedes pasarlo para evitar tocar la DB de nuevo.
        typesById: Map<Long, String>? = null
    ) {
        if (items.isEmpty()) return

        // Filtra solo merch; si no te pasan tipos, los resuelve con la DB
        val resolvedTypes = typesById ?: run {
            val ids = items.map { it.productId }.distinct() // Esto es List<Long>
            getTypesById(ids) // Llama a la versión que acepta List<Long>
        }
        val merchItems = items.filter { resolvedTypes[it.productId] == "merch" }
        if (merchItems.isEmpty()) return

        db.withTransaction {
            // (Opcional) Lectura previa — no estrictamente necesaria, `tryDecrementStock` ya valida.
            // La mantenemos para poder construir un mensaje mejor si falla.
            val ids = merchItems.map { it.productId }.distinct() // Esto es List<Long>

            // getStockByIds (internamente) convierte Long a Int para llamar al DAO
            // Asumiendo que ProductStockProjection.id es Int
            val stockMap = getStockByIds(ids).associate { it.id.toLong() to (it.stock ?: Int.MAX_VALUE) }

            val shortages = mutableListOf<Pair<Long, Int>>()

            merchItems.forEach { ci ->
                // Si no tiene stock definido, tratamos como “sin control” (no bloquea)
                val current = stockMap[ci.productId]
                if (current != null) {
                    // LA CORRECCIÓN: ci.productId (Long) se convierte a Int
                    val updated = dao.tryDecrementStock(ci.productId.toInt(), ci.qty)
                    if (updated == 0) {
                        shortages += (ci.productId to ci.qty)
                    }
                }
            }

            if (shortages.isNotEmpty()) {
                // Cualquier fallo aborta la transacción
                throw InsufficientStockException(shortages)
            }
        }
    }
}

