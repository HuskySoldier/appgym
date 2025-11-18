package cl.gymtastic.app.data.remote

import cl.gymtastic.app.data.local.Sede
import cl.gymtastic.app.data.local.entity.ProductEntity
import cl.gymtastic.app.data.local.entity.TrainerEntity
import retrofit2.Response
import retrofit2.http.*

// ====================================================================
// 1. DTOs (Data Transfer Objects) de la Aplicación
//    Deben coincidir con los DTOs y Entities de tu backend Spring Boot
// ====================================================================

// DTO para Login (Request)
data class LoginRequest(
    val email: String,
    val password: String
)

// DTO para Login (Response) - Coincide con LoginResponse.java de loginservice
data class LoginResponse(
    val success: Boolean,
    val token: String?,
    val message: String?,
    val user: UserProfileDto? // Perfil sin el passHash
)

// DTO para Registro (Request) - Coincide con RegisterRequest.java de registerservice
data class RegisterRequest(
    val email: String,
    val password: String,
    val nombre: String // Coincide con 'nombre' en el backend
)

// DTO para el perfil de usuario (sin passHash)
// Coincide con UserProfileResponse.java de user-service
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

// DTO para actualizar el perfil
data class ProfileUpdateRequest(
    val nombre: String,
    val fono: String?,
    val bio: String?,
    val avatarUri: String? // Nota: solo usamos esto para guardar la URI local
)

// DTO para decrementar stock (checkoutservice -> product-service)
data class CartItemDto(
    val productId: Int,
    val qty: Int,
    val tipo: String // Necesario para la lógica del CheckoutService
)

data class StockDecreaseRequest(
    val items: List<CartItemDto>
)

data class CheckoutRequest(
    val userEmail: String,
    val items: List<CartItemDto>,
    val sede: Sede? // Usaremos la clase Sede local si es necesaria
)

data class SubscriptionUpdateRequest(
    val planEndMillis: Long?,
    val sedeId: Int?,
    val sedeName: String?,
    val sedeLat: Double?,
    val sedeLng: Double?
)

// DTO para Check-in/Check-out (solo se usa en la App temporalmente para el history,
// pero la API solo necesita el email)
data class AttendanceHistoryResponse(
    val id: Long,
    val userEmail: String,
    val timestamp: Long,
    val checkOutTimestamp: Long?
)

data class BookingRequest(
    val userEmail: String,
    val trainerId: Long,
    val fechaHora: Long
)


// ====================================================================
// 2. Interfaz API (Retrofit)
// ====================================================================
