package cl.gymtastic.app.data.repository

import android.content.Context
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import cl.gymtastic.app.data.local.entity.CartItemEntity

class CartRepository(context: Context) {
    private val db = GymTasticDatabase.get(context)
    fun observeCart() = db.cart().observeAll()
    suspend fun add(productId: Long, qty: Int, unitPrice: Int) {
        db.cart().upsert(CartItemEntity(productId = productId, qty = qty, unitPrice = unitPrice))
    }
    suspend fun clear() = db.cart().clear()
    suspend fun remove(item: CartItemEntity) = db.cart().delete(item)

    suspend fun getQtyFor(productId: Long): Int = db.cart().getQtyFor(productId)


    suspend fun removeByProductIds(ids: List<Long>) {
        if (ids.isNotEmpty()) db.cart().removeByProductIds(ids)
    }
}