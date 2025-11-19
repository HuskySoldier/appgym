package cl.gymtastic.app.data.repository

import android.content.Context
import cl.gymtastic.app.util.ServiceLocator
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CartRepositoryTest {

    private lateinit var cartRepo: CartRepository
    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setup() {
        // Inicializamos el repo
        cartRepo = CartRepository(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun additemincreasescartsize() = runBlocking {
        // GIVEN: Un carrito vacío
        cartRepo.clear()

        // WHEN: Agregamos un producto (ID: 1, Cantidad: 2, Precio: 5000)
        cartRepo.add(1L, 2, 5000)

        // THEN: El carrito debe tener 1 elemento
        val items = cartRepo.observeCart().first()
        assertEquals(1, items.size)
        assertEquals(1L, items[0].productId)
        assertEquals(2, items[0].qty)
    }

    @Test
    fun addexistingitemupdatesquantity() = runBlocking {
        // GIVEN: Un carrito con un producto
        cartRepo.clear()
        cartRepo.add(10L, 1, 1000)

        // WHEN: Agregamos el mismo producto otra vez
        cartRepo.add(10L, 2, 1000)

        // THEN: El tamaño sigue siendo 1, pero la cantidad suma 3
        val items = cartRepo.observeCart().first()
        assertEquals(1, items.size)
        assertEquals(3, items[0].qty)
    }
}