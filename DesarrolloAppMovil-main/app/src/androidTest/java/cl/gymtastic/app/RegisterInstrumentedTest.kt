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
import cl.gymtastic.app.ui.auth.RegisterScreen
import cl.gymtastic.app.ui.theme.GymTasticTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegisterInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    private fun launchRegisterScreen() {
        composeTestRule.setContent {
            GymTasticTheme {
                val navController = TestNavHostController(LocalContext.current)
                val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(360.dp, 640.dp))
                RegisterScreen(nav = navController, windowSizeClass = windowSizeClass)
            }
        }
    }

    @Test
    fun create_account_button_is_disabled_initially() {
        launchRegisterScreen()

        // CORRECCIÓN: Usamos 'hasClickAction()' para diferenciar el botón del título.
        // Buscamos el nodo que tenga el texto "Crear cuenta" Y que sea clickable.
        composeTestRule.onNode(hasText("Crear cuenta") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun shows_error_when_passwords_do_not_match() {
        launchRegisterScreen()

        // Llenamos la primera contraseña
        composeTestRule.onNodeWithText("Contraseña").performTextInput("password123")

        // Llenamos la confirmación con algo distinto
        composeTestRule.onNodeWithText("Confirmar contraseña").performTextInput("otraCosa")

        // Forzamos cierre de teclado/foco
        composeTestRule.onRoot().performClick()
        composeTestRule.waitForIdle()

        // Debería aparecer el error
        composeTestRule.onNodeWithText("Las contraseñas no coinciden").assertIsDisplayed()
    }

    @Test
    fun shows_error_when_name_is_too_short() {
        launchRegisterScreen()

        // Escribimos un nombre de 1 letra (mínimo 2 según tu código)
        composeTestRule.onNodeWithText("Nombre").performTextInput("A")
        composeTestRule.waitForIdle()

        // Verificamos el mensaje de ayuda/error
        composeTestRule.onNodeWithText("Mínimo 2 caracteres").assertIsDisplayed()
    }

    @Test
    fun button_enables_when_form_is_valid() {
        launchRegisterScreen()

        // Llenamos todo correctamente
        composeTestRule.onNodeWithText("Email").performTextInput("nuevo@usuario.com")
        composeTestRule.onNodeWithText("Nombre").performTextInput("Usuario Test")
        composeTestRule.onNodeWithText("Contraseña").performTextInput("123456")
        composeTestRule.onNodeWithText("Confirmar contraseña").performTextInput("123456")

        composeTestRule.waitForIdle()

        // CORRECCIÓN: Igual que arriba, usamos el matcher específico para el botón
        composeTestRule.onNode(hasText("Crear cuenta") and hasClickAction()).assertIsEnabled()
    }
}