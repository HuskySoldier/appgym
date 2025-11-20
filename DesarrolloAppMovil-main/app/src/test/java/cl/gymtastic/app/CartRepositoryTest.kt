package cl.gymtastic.app

import android.content.Context
import cl.gymtastic.app.data.repository.CartRepository
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CartRepositoryTest {

    private lateinit var cartRepository: CartRepository
    private val mockContext = mockk<Context>(relaxed = true)

    @Before
    fun setup() {
        cartRepository = CartRepository(mockContext)
    }

    @Test
    fun addProduct_AddsNewItemToFlow() = runBlocking {
        // GIVEN: Carrito vacío
        cartRepository.clear()

        // WHEN: Agregamos producto ID 1
        cartRepository.add(productId = 1L, qty = 2, unitPrice = 1000)

        // THEN: El carrito debe tener 1 item con cantidad 2
        val items = cartRepository.observeCart().first()
        assertEquals(1, items.size)
        assertEquals(1L, items[0].productId)
        assertEquals(2, items[0].qty)
    }

    @Test
    fun addExistingProduct_UpdatesQuantity() = runBlocking {
        // GIVEN: Carrito con 1 unidad del producto 10
        cartRepository.add(productId = 10L, qty = 1, unitPrice = 500)

        // WHEN: Agregamos 3 unidades más del mismo producto
        cartRepository.add(productId = 10L, qty = 3, unitPrice = 500)

        // THEN: Seguimos teniendo 1 item, pero cantidad 4
        val items = cartRepository.observeCart().first()
        assertEquals(1, items.size)
        assertEquals(4, items[0].qty)
    }

    @Test
    fun removeProduct_RemovesFromList() = runBlocking {
        // GIVEN
        cartRepository.add(productId = 5L, qty = 1, unitPrice = 100)
        val item = cartRepository.observeCart().first()[0]

        // WHEN
        cartRepository.remove(item)

        // THEN
        val items = cartRepository.observeCart().first()
        assertTrue(items.isEmpty())
    }

    @Test
    fun clear_EmptiesCart() = runBlocking {
        // GIVEN
        cartRepository.add(1L, 1, 100)
        cartRepository.add(2L, 1, 200)

        // WHEN
        cartRepository.clear()

        // THEN
        val items = cartRepository.observeCart().first()
        assertTrue(items.isEmpty())
    }

    @Test
    fun getQtyFor_ReturnsCorrectValue() = runBlocking {
        cartRepository.add(99L, 5, 100)

        val qty = cartRepository.getQtyFor(99L)
        val qtyZero = cartRepository.getQtyFor(1L) // No existe

        assertEquals(5, qty)
        assertEquals(0, qtyZero)
    }
}