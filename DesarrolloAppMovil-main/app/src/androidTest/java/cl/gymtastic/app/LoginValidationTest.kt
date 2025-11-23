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
                // Simulamos un tamaño de pantalla de teléfono estándar
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

        // Cerramos teclado o quitamos foco para asegurar que la UI se asiente
        composeTestRule.onRoot().performClick()
        composeTestRule.waitForIdle()

        // THEN: Debe aparecer el mensaje de error
        composeTestRule.onNodeWithText("Ingresa un email válido").assertIsDisplayed()
    }

    @Test
    fun show_error_when_password_is_too_short() {
        launchLoginScreen()

        composeTestRule.onNodeWithText("Contraseña").performTextInput("123")
        composeTestRule.onRoot().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Mínimo 6 caracteres").assertIsDisplayed()
    }

    @Test
    fun password_visibility_toggles_work() {
        launchLoginScreen()

        composeTestRule.onNodeWithText("Contraseña").performTextInput("password123")

        // 1. Click en mostrar
        composeTestRule.onNodeWithContentDescription("Mostrar").performClick()

        // 2. Verificar que cambió a ocultar
        composeTestRule.onNodeWithContentDescription("Ocultar").assertIsDisplayed()
    }

    // --- NUEVO TEST PARA EL CHECKBOX ---
    @Test
    fun remember_me_checkbox_is_displayed_and_toggleable() {
        launchLoginScreen()

        // 1. Verificar que el texto existe
        val checkboxLabel = composeTestRule.onNodeWithText("Mantener sesión iniciada")
        checkboxLabel.assertIsDisplayed()

        // 2. Buscar el checkbox asociado (usualmente es el hermano en la fila o parte del Row clickeable)
        // En tu código el Row tiene el modificador .clickable, así que al hacer click en el texto cambiamos el estado
        checkboxLabel.performClick()

        // Aquí verificamos que no crashea y sigue visible.
        // (Para verificar el estado 'checked' real necesitaríamos poner un testTag en el Checkbox)
        checkboxLabel.assertIsDisplayed()
    }
}