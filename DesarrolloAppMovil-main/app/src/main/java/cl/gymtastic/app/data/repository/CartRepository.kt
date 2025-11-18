package cl.gymtastic.app.data.repository

import android.content.Context
import android.util.Log
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import cl.gymtastic.app.data.local.entity.CartItemEntity
import cl.gymtastic.app.data.local.Sede
import cl.gymtastic.app.data.local.InsufficientStockException
import cl.gymtastic.app.data.remote.CartItemDto
import cl.gymtastic.app.data.remote.CheckoutRequest
import cl.gymtastic.app.util.ServiceLocator
import retrofit2.HttpException
import java.io.IOException

class CartRepository(context: Context) {
    private val db = GymTasticDatabase.get(context)
    private val api = ServiceLocator.api() // Obtener la instancia API

    fun observeCart() = db.cart().observeAll()

    // --- Funciones de carrito local (sin cambios) ---
    suspend fun add(productId: Long, qty: Int, unitPrice: Int) {
        db.cart().upsert(CartItemEntity(productId = productId, qty = qty, unitPrice = unitPrice))
    }
    suspend fun clear() = db.cart().clear()
    suspend fun remove(item: CartItemEntity) = db.cart().delete(item)
    suspend fun getQtyFor(productId: Long): Int = db.cart().getQtyFor(productId)
    suspend fun removeByProductIds(ids: List<Long>) {
        if (ids.isNotEmpty()) db.cart().removeByProductIds(ids)
    }
    // -----------------------------------------------

    /**
     * Procesa la compra completa llamando al microservicio orquestador (checkout-service).
     * @return Pair<Boolean, String> donde Boolean es `planActivated` y String es el mensaje.
     */
    suspend fun processCheckout(
        userEmail: String,
        items: List<CartItemEntity>,
        sede: Sede?
    ): Pair<Boolean, String> {
        if (userEmail.isBlank()) throw IOException("Email de usuario no disponible.")

        // 1. Mapear entidades locales a DTOs de la API
        val types = ServiceLocator.products(context).getTypesById(items.map { it.productId })
        val cartItemsDto = items.map { item ->
            CartItemDto(
                productId = item.productId.toInt(), // El backend usa Integer
                qty = item.qty,
                tipo = types[item.productId] ?: "merch" // Asegurarse de tener el tipo
            )
        }

        // 2. Crear la Request para el orquestador
        val request = CheckoutRequest(
            userEmail = userEmail,
            items = cartItemsDto,
            sede = if (sede != null && cartItemsDto.any { it.tipo == "plan" }) sede else null
        )

        Log.d("CartRepo", "Iniciando Checkout para $userEmail con ${cartItemsDto.size} items.")

        // 3. Llamar al Orquestador
        try {
            val response = api.processCheckout(request) // <-- LLAMA A LA API
            val body = response.body()

            if (response.isSuccessful && body != null) {
                // Éxito: Plan activado o solo mercancía comprada.
                val planActivated = body["planActivated"] as? Boolean ?: false
                val message = body["message"] as? String ?: "Compra procesada exitosamente."

                // Limpiar carrito local (ya se procesó el pago)
                clear()

                return Pair(planActivated, message)
            } else {
                // Error de servidor o lógica (HTTP 409 Conflict)
                val errorMessage = response.errorBody()?.string() ?: "Error de conexión/servidor (${response.code()})"

                // Si el backend lanza 409, puede ser stock insuficiente o plan activo.
                if (response.code() == 409) {
                    val errorMap = ServiceLocator.api().gson.fromJson(errorMessage, Map::class.java)
                    val msg = errorMap["message"] as? String ?: errorMessage
                    // Lanzar excepción con el mensaje del backend para mostrar en UI
                    throw IOException(msg)
                }

                throw IOException(errorMessage)
            }

        } catch (e: HttpException) {
            // Error de conexión o timeout
            Log.e("CartRepo", "Error HTTP/conexión en checkout", e)
            throw IOException("Error de conexión con el servidor de pago. Intenta más tarde.")
        } catch (e: Exception) {
            // Incluye InsufficientStockException si el backend no lo maneja correctamente o si hay otro error.
            Log.e("CartRepo", "Error inesperado en checkout", e)
            throw e
        }
    }
}