package cl.gymtastic.app.data.model

data class Product(
    val id: Int = 0,
    val nombre: String,
    val precio: Double,
    val img: String? = null,
    val stock: Int? = 0,
    val tipo: String, // "plan" o "merch"
    val descripcion: String? = null
)