package cl.gymtastic.app

import android.content.Context
import cl.gymtastic.app.data.model.Product
import cl.gymtastic.app.data.remote.GymTasticApi
import cl.gymtastic.app.data.repository.ProductsRepository
import cl.gymtastic.app.util.ServiceLocator
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ProductsRepositoryTest {

    private val mockApi = mockk<GymTasticApi>()
    private val mockContext = mockk<Context>(relaxed = true)
    private lateinit var repo: ProductsRepository

    private val fakeProducts = listOf(
        Product(1, "Plan Mensual", 100.0, null, 0, "plan", "desc"),
        Product(2, "Proteína", 50.0, null, 10, "merch", "desc"),
        Product(3, "Plan Anual", 1000.0, null, 0, "plan", "desc")
    )

    @Before
    fun setup() {
        mockkObject(ServiceLocator)
        every { ServiceLocator.api() } returns mockApi

        // Simulamos que la API devuelve la lista mixta
        coEvery { mockApi.getProducts() } returns Response.success(fakeProducts)

        repo = ProductsRepository(mockContext)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun observePlanes_FiltersOnlyPlans() = runBlocking {
        // WHEN: Observamos planes
        val planes = repo.observePlanes().first()

        // THEN: Deberían ser 2 (Plan Mensual y Plan Anual)
        assertEquals(2, planes.size)
        assertEquals("plan", planes[0].tipo)
        assertEquals("plan", planes[1].tipo)
    }

    @Test
    fun observeMerch_FiltersOnlyMerch() = runBlocking {
        // WHEN: Observamos merch
        val merch = repo.observeMerch().first()

        // THEN: Debería ser 1 (Proteína)
        assertEquals(1, merch.size)
        assertEquals("merch", merch[0].tipo)
    }

    @Test
    fun getNamesById_ReturnsMap() = runBlocking {
        // WHEN
        val map = repo.getNamesById(listOf(1L, 2L))

        // THEN
        assertEquals("Plan Mensual", map[1L])
        assertEquals("Proteína", map[2L])
    }
}