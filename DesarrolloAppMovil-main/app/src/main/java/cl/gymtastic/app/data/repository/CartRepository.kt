package cl.gymtastic.app.data.repository

import android.content.Context
import android.util.Log
import cl.gymtastic.app.data.local.Sede
import cl.gymtastic.app.data.model.CartItem
import cl.gymtastic.app.data.remote.CartItemDto
import cl.gymtastic.app.data.remote.CheckoutRequest
import cl.gymtastic.app.data.remote.OrderDto // Importar
import cl.gymtastic.app.util.ServiceLocator
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.HttpException
import java.io.IOException

class CartRepository(private val context: Context) {
    private val api = ServiceLocator.api()

    // --- CARRITO EN MEMORIA ---
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    fun observeCart() = _cartItems.asStateFlow()

    suspend fun add(productId: Long, qty: Int, unitPrice: Int) {
        _cartItems.update { currentList ->
            val existing = currentList.find { it.productId == productId }
            if (existing != null) {
                currentList.map {
                    if (it.productId == productId) it.copy(qty = it.qty + qty) else it
                }
            } else {
                currentList + CartItem(productId = productId, qty = qty, unitPrice = unitPrice)
            }
        }
    }

    suspend fun clear() {
        _cartItems.value = emptyList()
    }

    suspend fun remove(item: CartItem) {
        _cartItems.update { list -> list.filter { it.id != item.id } }
    }

    suspend fun getQtyFor(productId: Long): Int {
        return _cartItems.value.find { it.productId == productId }?.qty ?: 0
    }

    suspend fun removeByProductIds(ids: List<Long>) {
        _cartItems.update { list -> list.filter { it.productId !in ids } }
    }

    // --- CHECKOUT ---
    suspend fun processCheckout(
        userEmail: String,
        items: List<CartItem>,
        sede: Sede?
    ): Pair<Boolean, String> {
        if (userEmail.isBlank()) throw IOException("Email de usuario no disponible.")

        val types = ServiceLocator.products(context).getTypesById(items.map { it.productId })

        val cartItemsDto = items.map { item ->
            CartItemDto(
                productId = item.productId.toInt(),
                qty = item.qty, // Asegúrate que CartItemDto usa @SerializedName("qty") o "qty" en el backend
                tipo = types[item.productId] ?: "merch",

                // --- AGREGAR ESTO ---

                // 1. PRECIO (Crucial para evitar el error 500)
                precio = item.unitPrice.toDouble(),

                // 2. NOMBRE (Para que el historial se vea bien)
                // Si tu CartItem no tiene .name, tendrás que buscarlo o poner uno temporal
                nombre = "Producto ${item.productId}" // O usa: item.name si actualizas tu modelo CartItem
            )
        }

        val request = CheckoutRequest(
            userEmail = userEmail,
            items = cartItemsDto,
            sede = if (sede != null && cartItemsDto.any { it.tipo == "plan" }) sede else null
        )

        try {
            val response = api.processCheckout(request)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                val planActivated = body["planActivated"] as? Boolean ?: false
                val message = body["message"] as? String ?: "Compra procesada exitosamente."

                clear()
                if (planActivated) {
                    ServiceLocator.auth(context).getUserProfile(userEmail)
                }
                return Pair(planActivated, message)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error ${response.code()}"
                if (response.code() == 409) {
                    try {
                        val map = Gson().fromJson(errorMsg, Map::class.java)
                        throw IOException(map["message"] as? String ?: errorMsg)
                    } catch (e: Exception) { throw IOException(errorMsg) }
                }
                throw IOException(errorMsg)
            }
        } catch (e: HttpException) {
            throw IOException("Error de conexión.")
        }
    }

    // --- NUEVO: OBTENER HISTORIAL ---
    suspend fun getOrderHistory(email: String): List<OrderDto> {
        return try {
            val response = api.getOrderHistory(email)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                Log.e("CartRepo", "Error fetching history: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("CartRepo", "Exception fetching history", e)
            emptyList() // Retorna lista vacía si hay error de red (o si el endpoint no existe aún)
        }
    }
}