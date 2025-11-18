package cl.gymtastic.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val precio: Double,
    val img: String? = null,
    val stock: Int? = 0,
    val tipo: String, // "plan" or "merch"
    val descripcion: String? = null
)