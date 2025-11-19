package cl.gymtastic.app.data.model

data class Trainer(
    val id: Long = 0,
    val nombre: String,
    val fono: String,
    val email: String,
    val especialidad: String,
    val img: String? = null
)