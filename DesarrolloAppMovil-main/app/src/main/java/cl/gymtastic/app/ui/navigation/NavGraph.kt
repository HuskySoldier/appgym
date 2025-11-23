package cl.gymtastic.app.ui.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

// Importaciones de tus pantallas
import cl.gymtastic.app.ui.orders.OrderHistoryScreen
import cl.gymtastic.app.ui.splash.SplashScreen // <--- Importar Splash

// ===== Rutas =====
sealed class Screen(val route: String) {
    data object Splash   : Screen("splash") // <--- 1. Nueva Ruta
    data object Login    : Screen("login")
    data object Register : Screen("register")
    data object Home     : Screen("home")
    data object Planes   : Screen("planes")
    data object Payment  : Screen("payment")
    data object Store    : Screen("store")
    data object Cart     : Screen("cart")
    data object CheckIn  : Screen("checkin")
    data object Trainers : Screen("trainers")
    data object Profile  : Screen("profile")
    data object TrainerDashboard : Screen("trainer_dashboard")
    data object Admin : Screen("admin")
    data object ForgotPassword : Screen("forgot_password")
    data object OrderHistory : Screen("order_history")

    // Rutas con argumentos
    data object PaymentSuccess : Screen("payment_success") {
        fun withPlan(plan: Boolean) = "payment_success?plan=$plan"
        const val routeWithArg = "payment_success?plan={plan}"
    }

    data object Booking : Screen("booking?trainerId={trainerId}") {
        fun routeWith(trainerId: Long?) =
            if (trainerId != null) "booking?trainerId=$trainerId" else "booking?trainerId=-1"
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavGraph(
    // 2. Cambiamos el destino inicial a Splash
    startDestination: String = Screen.Splash.route,
    windowSizeClass: WindowSizeClass
) {
    val navController = rememberAnimatedNavController()

    // Definición de transiciones (para no repetir código)
    val enterRight = { slideInHorizontally(animationSpec = tween(300)) { it } }
    val exitLeft = { slideOutHorizontally(animationSpec = tween(300)) { -it } }
    val enterLeft = { slideInHorizontally(animationSpec = tween(300)) { -it } }
    val exitRight = { slideOutHorizontally(animationSpec = tween(300)) { it } }

    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination,
        route = "root"
    ) {
        // --- 3. PANTALLA DE SPLASH (NUEVA) ---
        composable(
            route = Screen.Splash.route,
            // El splash suele no tener animación de entrada, o una simple desvanecimiento
            exitTransition = { exitLeft() }
        ) {
            SplashScreen(nav = navController)
        }

        // --- PANTALLAS EXISTENTES ---

        composable(
            route = Screen.Login.route,
            enterTransition = { enterRight() },
            exitTransition = { exitLeft() },
            popEnterTransition = { enterLeft() },
            popExitTransition = { exitRight() }
        ) {
            cl.gymtastic.app.ui.auth.LoginScreen(navController, windowSizeClass)
        }

        composable(route = Screen.Register.route, enterTransition = { enterRight() }, exitTransition = { exitLeft() }) {
            cl.gymtastic.app.ui.auth.RegisterScreen(navController, windowSizeClass)
        }
        composable(route = Screen.Home.route, enterTransition = { enterRight() }, exitTransition = { exitLeft() }) {
            cl.gymtastic.app.ui.home.HomeScreen(navController, windowSizeClass)
        }
        composable(route = Screen.Planes.route, enterTransition = { enterRight() }, exitTransition = { exitLeft() }) {
            cl.gymtastic.app.ui.planes.PlanesScreen(navController, windowSizeClass)
        }
        composable(route = Screen.Payment.route, enterTransition = { enterRight() }, exitTransition = { exitLeft() }) {
            cl.gymtastic.app.ui.payment.PaymentScreen(navController, windowSizeClass)
        }
        composable(route = Screen.Store.route, enterTransition = { enterRight() }, exitTransition = { exitLeft() }) {
            cl.gymtastic.app.ui.store.StoreScreen(navController, windowSizeClass)
        }
        composable(route = Screen.Cart.route, enterTransition = { enterRight() }, exitTransition = { exitLeft() }) {
            cl.gymtastic.app.ui.cart.CartScreen(navController, windowSizeClass)
        }
        composable(route = Screen.CheckIn.route, enterTransition = { enterRight() }, exitTransition = { exitLeft() }) {
            cl.gymtastic.app.ui.checkin.CheckInScreen(navController, windowSizeClass)
        }
        composable(route = Screen.Trainers.route, enterTransition = { enterRight() }, exitTransition = { exitLeft() }) {
            cl.gymtastic.app.ui.trainers.TrainersScreen(navController, windowSizeClass)
        }
        composable(
            route = Screen.Booking.route,
            arguments = listOf(navArgument("trainerId") { type = NavType.LongType; defaultValue = -1L }),
            enterTransition = { enterRight() }, exitTransition = { exitLeft() }
        ) {
            cl.gymtastic.app.ui.booking.BookingScreen(navController, windowSizeClass)
        }
        composable(
            route = Screen.PaymentSuccess.routeWithArg,
            arguments = listOf(navArgument("plan") { type = NavType.BoolType; defaultValue = false }),
            enterTransition = { enterRight() }, exitTransition = { exitLeft() }
        ) { backStackEntry ->
            val planActivated = backStackEntry.arguments?.getBoolean("plan") ?: false
            cl.gymtastic.app.ui.payment.PaymentSuccessScreen(nav = navController, planActivated = planActivated, windowSizeClass)
        }
        composable(route = Screen.Profile.route, enterTransition = { enterRight() }, exitTransition = { exitLeft() }) {
            cl.gymtastic.app.ui.profile.ProfileScreen(navController, windowSizeClass)
        }
        composable(route = Screen.Admin.route, enterTransition = { enterRight() }, exitTransition = { exitLeft() }) {
            cl.gymtastic.app.ui.admin.AdminScreen(navController, windowSizeClass)
        }
        composable(route = Screen.ForgotPassword.route, enterTransition = { enterRight() }, exitTransition = { exitLeft() }) {
            cl.gymtastic.app.ui.auth.ForgotPasswordScreen(navController)
        }
        composable(route = Screen.TrainerDashboard.route, enterTransition = { enterRight() }, exitTransition = { exitLeft() }) {
            cl.gymtastic.app.ui.trainers.TrainerDashboardScreen(navController)
        }

        // --- RUTA DE HISTORIAL ---
        composable(
            route = Screen.OrderHistory.route,
            enterTransition = { enterRight() },
            exitTransition = { exitLeft() },
            popEnterTransition = { enterLeft() },
            popExitTransition = { exitRight() }
        ) {
            OrderHistoryScreen(nav = navController)
        }
    }
}