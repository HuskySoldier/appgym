package cl.gymtastic.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

//los Entities definen qué datos guardas y cómo se estructuran en la base de datos.
// Los DAOs los usan para leer/escribir, y los Repositories te los entregan (o los reciben) para que los uses en la UI

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userEmail: String,
    val timestamp: Long = System.currentTimeMillis(), // check-in
    val checkOutTimestamp: Long? = null               // check-out (opcional)
)
