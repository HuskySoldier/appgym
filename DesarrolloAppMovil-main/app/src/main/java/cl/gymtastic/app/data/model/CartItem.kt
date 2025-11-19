package cl.gymtastic.app.data.model

data class CartItem(
    val id: Long = System.currentTimeMillis(), // ID temporal único
    val productId: Long,
    val qty: Int,
    val unitPrice: Int
)