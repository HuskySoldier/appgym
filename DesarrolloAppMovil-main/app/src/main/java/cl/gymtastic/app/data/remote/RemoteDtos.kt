package cl.gymtastic.app.data.remote

import cl.gymtastic.app.data.local.Sede
import com.google.gson.annotations.SerializedName


// ====================================================================
// 1. DTOs (Data Transfer Objects)
// ====================================================================

// --- Auth & Users ---

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val token: String?,
    val message: String?,
    val user: UserProfileDto?
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val nombre: String
)

// Nuevo DTO para confirmar reseteo de contraseña
data class ResetPasswordRequestDto(
    val email: String,
    val token: String,
    val newPassword: String
)

// Nuevo DTO para que el Admin cambie roles
data class AdminRoleUpdateRequest(
    val rol: String
)

// DTO para perfil (lectura)
data class UserProfileDto(
    val email: String,
    val nombre: String,
    val rol: String,
    val planEndMillis: Long? = null,
    val sedeId: Int? = null,
    val sedeName: String? = null,
    val sedeLat: Double? = null,
    val sedeLng: Double? = null,
    val avatarUri: String? = null,
    val fono: String? = null,
    val bio: String? = null
)

// DTO para que el usuario actualice su propio perfil
data class ProfileUpdateRequest(
    val nombre: String,
    val fono: String?,
    val bio: String?,
    val avatarUri: String?
)

// --- Shop & Checkout ---

data class CartItemDto(
    val productId: Int,
    val qty: Int,
    val tipo: String,
    val nombre: String,
    val precio: Double // <--- AGREGA ESTA LÍNEA
)

data class StockDecreaseRequest(
    val items: List<CartItemDto>
)

data class CheckoutRequest(
    val userEmail: String,
    val items: List<CartItemDto>,
    val sede: Sede?
)

data class SubscriptionUpdateRequest(
    val planEndMillis: Long?,
    val sedeId: Int?,
    val sedeName: String?,
    val sedeLat: Double?,
    val sedeLng: Double?
)

// --- NUEVO: DTO para Historial de Compras ---
data class OrderDto(
    val id: Long,

    // 1. El backend envía "date" (String tipo fecha), no un Long timestamp
    @SerializedName("date")
    val date: String?,

    // 2. El backend envía "totalAmount" (Double), no "total" (Int)
    @SerializedName("totalAmount")
    val total: Double,

    // 3. "itemsCount" NO existe en el backend.
    // Si lo necesitas, debes calcularlo o pedirle al backend que lo envíe.
    // Por ahora, dales un valor por defecto para que no falle.
    val itemsCount: Int = 0,

    // 4. CRÍTICO: El backend envía "description".
    // Debe ser String? (nullable) para que no se cierre la app si viene vacío.
    @SerializedName("description")
    val summary: String?
)
// --- Attendance ---

data class AttendanceHistoryResponse(
    val id: Long,
    val userEmail: String,
    val timestamp: Long,
    val checkOutTimestamp: Long?
)

// --- Bookings ---

data class BookingRequest(
    val userEmail: String,
    val trainerId: Long,
    val fechaHora: Long
)