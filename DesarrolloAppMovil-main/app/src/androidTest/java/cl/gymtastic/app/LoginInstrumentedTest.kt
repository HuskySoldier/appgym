package cl.gymtastic.app

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
class LoginInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun loginScreen_showsTitleAndFields() {
        // GIVEN: Iniciamos la UI de LoginScreen
        composeTestRule.setContent {
            GymTasticTheme {
                val context = LocalContext.current
                // Creamos un NavController de prueba
                val navController = TestNavHostController(context)

                // Creamos una WindowSizeClass ficticia (Compact para teléfonos)
                val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(360.dp, 640.dp))

                LoginScreen(nav = navController, windowSizeClass = windowSizeClass)
            }
        }

        // WHEN: La pantalla carga (automático)

        // THEN: Verificamos que los elementos clave están visibles

        // 1. Verifica que el título "GymTastic" esté visible
        composeTestRule.onNodeWithText("GymTastic").assertIsDisplayed()

        // 2. Verifica que el campo "Email" exista
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()

        // 3. Verifica que el botón "Ingresar" exista
        composeTestRule.onNodeWithText("Ingresar").assertIsDisplayed()
    }
}