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
import cl.gymtastic.app.ui.auth.LoginScreen
import cl.gymtastic.app.ui.theme.GymTasticTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginValidationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    private fun launchLoginScreen() {
        composeTestRule.setContent {
            GymTasticTheme {
                val navController = TestNavHostController(LocalContext.current)
                val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(360.dp, 640.dp))
                LoginScreen(nav = navController, windowSizeClass = windowSizeClass)
            }
        }
    }

    @Test
    fun show_error_when_email_format_is_invalid() {
        launchLoginScreen()

        // WHEN: Escribimos un email sin arroba
        composeTestRule.onNodeWithText("Email").performTextInput("emailinvalido")
        composeTestRule.waitForIdle()

        // THEN: Debe aparecer el mensaje de error específico
        composeTestRule.onNodeWithText("Ingresa un email válido").assertIsDisplayed()
    }

    @Test
    fun show_error_when_password_is_too_short() {
        launchLoginScreen()

        // WHEN: Escribimos una clave de 3 dígitos (el mínimo es 6)
        composeTestRule.onNodeWithText("Contraseña").performTextInput("123")
        composeTestRule.waitForIdle()

        // THEN: Debe aparecer el mensaje de longitud mínima
        composeTestRule.onNodeWithText("Mínimo 6 caracteres").assertIsDisplayed()
    }

    @Test
    fun password_visibility_toggles_work() {
        launchLoginScreen()

        // Escribimos algo para tener contenido
        composeTestRule.onNodeWithText("Contraseña").performTextInput("password123")

        // 1. Buscar el ícono por su descripción "Mostrar" y hacer click
        composeTestRule.onNodeWithContentDescription("Mostrar").performClick()

        // 2. Ahora debería cambiar a "Ocultar" (significa que el estado visual cambió)
        composeTestRule.onNodeWithContentDescription("Ocultar").assertIsDisplayed()
    }
}