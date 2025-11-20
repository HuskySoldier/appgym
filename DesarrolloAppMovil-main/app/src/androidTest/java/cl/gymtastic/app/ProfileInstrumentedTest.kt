package cl.gymtastic.app

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.gymtastic.app.ui.profile.ProfileScreen
import cl.gymtastic.app.ui.theme.GymTasticTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    private fun launchProfileScreen() {
        composeTestRule.setContent {
            GymTasticTheme {
                val navController = TestNavHostController(LocalContext.current)
                // Simulamos un tamaño de pantalla normal
                val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(360.dp, 640.dp))
                ProfileScreen(nav = navController, windowSizeClass = windowSizeClass)
            }
        }
    }

    @Test
    fun shows_basic_profile_fields() {
        launchProfileScreen()

        // Verificamos que los campos principales estén ahí
        composeTestRule.onNodeWithText("Nombre").assertIsDisplayed()
        composeTestRule.onNodeWithText("Teléfono").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bio").assertIsDisplayed()
    }

    @Test
    fun shows_error_on_invalid_phone_number() {
        launchProfileScreen()

        // En tu código, el teléfono debe tener entre 8 y 12 dígitos
        // Probamos con uno muy corto
        composeTestRule.onNodeWithText("Teléfono").performTextInput("123")

        composeTestRule.waitForIdle()

        // Verificamos que aparezca el error "Teléfono inválido"
        // Nota: Este texto lo definiste en la variable `phoneError` de ProfileScreen.kt
        composeTestRule.onNodeWithText("Teléfono inválido").assertIsDisplayed()
    }

    @Test
    fun save_button_is_disabled_on_error() {
        launchProfileScreen()

        // Ingresamos un error
        composeTestRule.onNodeWithText("Teléfono").performTextInput("123") // Inválido

        composeTestRule.waitForIdle()

        // El botón "Guardar Datos" debe desactivarse si hay error en el teléfono
        composeTestRule.onNodeWithText("Guardar Datos").assertIsNotEnabled()
    }
}