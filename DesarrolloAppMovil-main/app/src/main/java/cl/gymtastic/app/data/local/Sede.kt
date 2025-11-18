package cl.gymtastic.app.data.local


data class Sede(
    val id: Int,
    val nombre: String,
    val direccion: String,
    val lat: Double,
    val lng: Double
)

object SedesRepo {
    val sedes = listOf(
        Sede(1, "Sede Centro", "Alameda 123", -33.447, -70.653),
        Sede(2, "Sede Ñuñoa", "Irarrazaval 456", -33.456, -70.595),
        Sede(3, "Sede Providencia", "Providencia 789", -33.426, -70.615)
    )
}
