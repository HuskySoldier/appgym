package cl.gymtastic.app.util

import android.content.Context
import cl.gymtastic.app.data.remote.GymTasticApi
import cl.gymtastic.app.data.repository.AttendanceRepository
import cl.gymtastic.app.data.repository.AuthRepository
import cl.gymtastic.app.data.repository.BookingsRepository
import cl.gymtastic.app.data.repository.CartRepository
import cl.gymtastic.app.data.repository.ProductsRepository
import cl.gymtastic.app.data.repository.TrainersRepository
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ServiceLocator {
    private var productsRepo: ProductsRepository? = null
    private var gymTasticApi: GymTasticApi? = null

    fun api(): GymTasticApi {
        return gymTasticApi ?: run {
            val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            val httpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
            val gson = GsonBuilder().serializeNulls().create()
            val retrofit = Retrofit.Builder()
                .baseUrl(GymTasticApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient)
                .build()
            retrofit.create(GymTasticApi::class.java).also { gymTasticApi = it }
        }
    }

    fun products(ctx: Context): ProductsRepository {
        return productsRepo ?: ProductsRepository(ctx).also { productsRepo = it }
    }

    fun auth(context: Context) = AuthRepository(context)
    fun cart(context: Context) = CartRepository(context)
    fun attendance(context: Context) = AttendanceRepository(context)
    fun trainers(context: Context) = TrainersRepository(context)
    fun bookings(context: Context) = BookingsRepository(context)

    // --- BORRA ESTA FUNCIÓN SI AÚN EXISTE ---
    // fun db(ctx: Context) = GymTasticDatabase.Companion.get(ctx)
}