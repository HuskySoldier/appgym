package cl.gymtastic.app.util

import android.content.Context
import cl.gymtastic.app.data.local.ProductsRepository
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import cl.gymtastic.app.data.repository.*

object ServiceLocator {

    private var productsRepo: ProductsRepository? = null

    fun products(ctx: Context): ProductsRepository {
        return productsRepo ?: ProductsRepository(ctx).also { productsRepo = it }
    }

    fun auth(context: Context) = AuthRepository(context)
    fun cart(context: Context) = CartRepository(context)
    fun attendance(context: Context) = AttendanceRepository(context)
    fun trainers(context: Context) = TrainersRepository(context)
    fun bookings(context: Context) = BookingsRepository(context)

    fun db(ctx: Context) = GymTasticDatabase.get(ctx)

}
