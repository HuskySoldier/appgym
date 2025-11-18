package cl.gymtastic.app.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GymTasticApi {
    // Usaremos la IP 10.0.2.2 para que el emulador Android acceda al localhost de tu máquina.
    companion object {
        const val BASE_URL = "http://10.0.2.2" // Debe ser solo el esquema+host:puerto base
    }

    // --- Endpoints de Autenticación/Registro (Proxies) ---

    // 1. LOGIN (loginservice: 8083)
    @POST(":8083/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // 2. REGISTER (registerservice: 8084)
    @POST(":8084/register")
    suspend fun register(@Body request: RegisterRequest): Response<Map<String, String>> // Devuelve Map<String, String>

    // --- Endpoints de Productos (product-service: 8081) ---

    // 3. Obtener todos los productos
    @GET(":8081/products")
    suspend fun getProducts(): Response<List<ProductEntity>>

    // 4. Obtener trainers (trainersservice: 8085)
    @GET(":8085/trainers")
    suspend fun getTrainers(): Response<List<TrainerEntity>>

    // --- Endpoints de Perfil/Usuario (user-service: 8082) ---

    // 5. Obtener perfil completo (usado después de login)
    @GET(":8082/users/{email}")
    suspend fun getUserProfile(@Path("email") email: String): Response<UserProfileDto>

    // 6. Actualizar perfil (user-service: 8082)
    @PUT(":8082/users/{email}/profile")
    suspend fun updateProfile(@Path("email") email: String, @Body request: ProfileUpdateRequest): Response<UserProfileDto>

    // 7. Eliminar perfil (user-service: 8082)
    @DELETE(":8082/users/{email}")
    suspend fun deleteUser(@Path("email") email: String): Response<Unit>

    // --- Endpoints de Asistencia (attendanceservice: 8087) ---

    // 8. Check-In
    @POST(":8087/attendance/check-in")
    suspend fun checkIn(@Query("email") email: String): Response<AttendanceHistoryResponse>

    // 9. Check-Out
    @POST(":8087/attendance/check-out")
    suspend fun checkOut(@Query("email") email: String): Response<AttendanceHistoryResponse>

    // 10. Historial de Asistencia
    @GET(":8087/attendance/history/{email}")
    suspend fun getAttendanceHistory(@Path("email") email: String): Response<List<AttendanceHistoryResponse>>

    // --- Endpoints de Checkout (checkoutservice: 8086) ---

    // 11. Procesar Checkout (Orquestador)
    @POST(":8086/checkout")
    suspend fun processCheckout(@Body request: CheckoutRequest): Response<Map<String, Any>> // Respuesta general Map<String, Object>

    // --- Endpoints de Reserva (bookingsservice: 8088) ---

    // 12. Crear Reserva
    @POST(":8088/bookings")
    suspend fun createBooking(@Body request: BookingRequest): Response<Any>

    // 13. Obtener Reservas por usuario
    @GET(":8088/bookings/user/{email}")
    suspend fun getUserBookings(@Path("email") email: String): Response<List<BookingRequest>> // BookingRequest es suficiente para los datos
}