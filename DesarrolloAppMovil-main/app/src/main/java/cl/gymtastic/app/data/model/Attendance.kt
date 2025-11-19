package cl.gymtastic.app.data.model

data class Attendance(
    val id: Long,
    val userEmail: String,
    val timestamp: Long,
    val checkOutTimestamp: Long?
)