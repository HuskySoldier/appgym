package cl.gymtastic.app

import android.content.Context
import cl.gymtastic.app.data.repository.AuthRepository
import cl.gymtastic.app.ui.auth.LoginViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LoginViewModel
    private val authRepo = mockk<AuthRepository>() // Simulamos el Repo
    private val context = mockk<Context>() // Simulamos el Contexto

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // CORRECCIÓN: No mockeamos saveRememberMe porque no es un método público del Repo.
        // El guardado es interno en la función login, así que solo mockeamos el login.

        // Inyectamos el repo simulado
        viewModel = LoginViewModel { authRepo }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login success calls onSuccess and passes rememberMe to repository`() = runTest(testDispatcher) {
        // GIVEN: El repositorio responderá "true" (login exitoso)
        // NOTA: Configuramos el mock para aceptar el tercer parámetro booleano
        coEvery { authRepo.login("juan@test.com", "123456", true) } returns true

        var successCalled = false
        val rememberMeValue = true // Queremos recordar sesión

        // WHEN: Ejecutamos login pasando rememberMe = true
        viewModel.login(context, "juan@test.com", "123456", rememberMeValue) {
            successCalled = true
        }

        // Avanzamos el despachador de corrutinas
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN:
        assertTrue("onSuccess debió ser llamado", successCalled)
        assertFalse("Loading debe ser false al terminar", viewModel.loading)
        assertNull("No debe haber errores", viewModel.error)

        // VERIFICACIÓN CORREGIDA:
        // Aseguramos que el ViewModel pasó el valor 'true' correctamente a la función login del repositorio
        coVerify { authRepo.login("juan@test.com", "123456", true) }
    }

    @Test
    fun `login failure sets error message`() = runTest(testDispatcher) {
        // GIVEN: El repositorio responderá "false" (login fallido) para cualquier input
        coEvery { authRepo.login(any(), any(), any()) } returns false

        var successCalled = false
        val rememberMeValue = true

        // WHEN: Ejecutamos login con credenciales malas
        viewModel.login(context, "bad@test.com", "wrongpass", rememberMeValue) {
            successCalled = true
        }

        testDispatcher.scheduler.advanceUntilIdle()

        // THEN:
        assertFalse("onSuccess NO debió ser llamado", successCalled)
        assertEquals("Credenciales inválidas", viewModel.error)

        // Verificamos que se intentó llamar al login con los parámetros
        coVerify { authRepo.login("bad@test.com", "wrongpass", true) }
    }
}