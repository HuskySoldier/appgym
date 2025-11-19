package cl.gymtastic.app.data.remote

import cl.gymtastic.app.data.local.entity.ProductEntity
import cl.gymtastic.app.data.local.entity.TrainerEntity
import retrofit2.Response
import retrofit2.http.*

interface GymTasticApi {
    companion object {
        // Usamos el host del emulador para acceder al localhost de tu PC
        private const val BASE_IP = "http://10.0.2.2"
        // Esta URL base se usa por defecto, pero las anotaciones @Url o rutas absolutas la sobrescriben
        const val BASE_URL = "$BASE_IP/"
    }

    // --- 1. LOGIN SERVICE (Puerto 8083) ---
    @POST("$BASE_IP:8083/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("$BASE_IP:8083/login/request-reset")
    suspend fun requestReset(@Query("email") email: String): Response<Unit>

    @POST("$BASE_IP:8083/login/confirm-reset")
    suspend fun confirmReset(@Body request: ResetPasswordRequestDto): Response<Unit>


    // --- 2. REGISTER SERVICE (Puerto 8084) ---
    @POST("$BASE_IP:8084/register")
    suspend fun register(@Body request: RegisterRequest): Response<Map<String, String>>


    // --- 3. USER SERVICE (Puerto 8082) ---
    @GET("$BASE_IP:8082/users/{email}")
    suspend fun getUserProfile(@Path("email") email: String): Response<UserProfileDto>

    // Nuevo: Obtener todos los usuarios (Admin)
    @GET("$BASE_IP:8082/users")
    suspend fun getAllUsers(): Response<List<UserProfileDto>>

    @PUT("$BASE_IP:8082/users/{email}/profile")
    suspend fun updateProfile(@Path("email") email: String, @Body request: ProfileUpdateRequest): Response<UserProfileDto>

    @PUT("$BASE_IP:8082/users/{email}/role")
    suspend fun updateUserRole(@Path("email") email: String, @Body request: AdminRoleUpdateRequest): Response<UserProfileDto>

    @DELETE("$BASE_IP:8082/users/{email}")
    suspend fun deleteUser(@Path("email") email: String): Response<Unit>


    // --- 4. PRODUCT SERVICE (Puerto 8081) ---
    @GET("$BASE_IP:8081/products")
    suspend fun getProducts(): Response<List<ProductEntity>>

    @POST("$BASE_IP:8081/products")
    suspend fun createProduct(@Body product: ProductEntity): Response<ProductEntity>

    @PUT("$BASE_IP:8081/products/{id}")
    suspend fun updateProduct(@Path("id") id: Int, @Body product: ProductEntity): Response<ProductEntity>

    @DELETE("$BASE_IP:8081/products/{id}")
    suspend fun deleteProduct(@Path("id") id: Int): Response<Unit>


    // --- 5. TRAINERS SERVICE (Puerto 8085) ---
    @GET("$BASE_IP:8085/trainers")
    suspend fun getTrainers(): Response<List<TrainerEntity>>

    @POST("$BASE_IP:8085/trainers")
    suspend fun createTrainer(@Body trainer: TrainerEntity): Response<TrainerEntity>

    @PUT("$BASE_IP:8085/trainers/{id}")
    suspend fun updateTrainer(@Path("id") id: Long, @Body trainer: TrainerEntity): Response<TrainerEntity>

    @DELETE("$BASE_IP:8085/trainers/{id}")
    suspend fun deleteTrainer(@Path("id") id: Long): Response<Unit>


    // --- 6. CHECKOUT SERVICE (Puerto 8086) ---
    @POST("$BASE_IP:8086/checkout")
    suspend fun processCheckout(@Body request: CheckoutRequest): Response<Map<String, Any>>


    // --- 7. ATTENDANCE SERVICE (Puerto 8087) ---
    @POST("$BASE_IP:8087/attendance/check-in")
    suspend fun checkIn(@Query("email") email: String): Response<AttendanceHistoryResponse>

    @POST("$BASE_IP:8087/attendance/check-out")
    suspend fun checkOut(@Query("email") email: String): Response<AttendanceHistoryResponse>

    @GET("$BASE_IP:8087/attendance/history/{email}")
    suspend fun getAttendanceHistory(@Path("email") email: String): Response<List<AttendanceHistoryResponse>>


    // --- 8. BOOKINGS SERVICE (Puerto 8088) ---
    @POST("$BASE_IP:8088/bookings")
    suspend fun createBooking(@Body request: BookingRequest): Response<Any>

    @GET("$BASE_IP:8088/bookings/user/{email}")
    suspend fun getUserBookings(@Path("email") email: String): Response<List<BookingRequest>>

    @GET("$BASE_IP:8088/bookings/trainer/{id}")
    suspend fun getTrainerBookings(@Path("id") trainerId: Long): Response<List<BookingRequest>>
}