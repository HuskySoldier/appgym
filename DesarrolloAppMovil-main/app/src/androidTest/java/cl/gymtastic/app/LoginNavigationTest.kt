package cl.gymtastic.app

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.gymtastic.app.ui.auth.LoginScreen
import cl.gymtastic.app.ui.theme.GymTasticTheme
import cl.gymtastic.app.ui.navigation.NavRoutes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    private fun launchLoginWithNav() {
        composeTestRule.setContent {
            GymTasticTheme {
                val context = LocalContext.current
                navController = TestNavHostController(context)
                navController.navigatorProvider.addNavigator(ComposeNavigator())

                // --- FIX: Configurar el grafo para que el NavController conozca los destinos ---
                navController.graph = navController.createGraph(startDestination = NavRoutes.LOGIN) {
                    composable(NavRoutes.LOGIN) {
                        // Pantalla de inicio (aunque aquí se renderiza manualmente abajo)
                    }
                    composable(NavRoutes.FORGOT_PASSWORD) {
                        // Destino vacío solo para validar la navegación
                    }
                    composable(NavRoutes.REGISTER) {
                        // Destino vacío solo para validar la navegación
                    }
                }
                // -----------------------------------------------------------------------------

                val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(360.dp, 640.dp))
                LoginScreen(nav = navController, windowSizeClass = windowSizeClass)
            }
        }
    }

    @Test
    fun click_forgot_password_navigates_to_recovery() {
        launchLoginWithNav()

        // Click en "¿Olvidaste tu contraseña?"
        composeTestRule.onNodeWithText("¿Olvidaste tu contraseña?").performClick()

        // Verificar ruta
        assertEquals(NavRoutes.FORGOT_PASSWORD, navController.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun click_create_account_navigates_to_register() {
        launchLoginWithNav()

        // Click en "Crear cuenta"
        composeTestRule.onNodeWithText("Crear cuenta").performClick()

        // Verificar ruta
        assertEquals(NavRoutes.REGISTER, navController.currentBackStackEntry?.destination?.route)
    }
}