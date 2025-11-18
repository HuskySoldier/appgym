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

// ===== Rutas (Opción A: todo hijo directo del NavHost raíz) =====
sealed class Screen(val route: String) {
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

    data object Admin : Screen("admin")


    // payment_success con query opcional ?plan=
    data object PaymentSuccess : Screen("payment_success") {
        fun withPlan(plan: Boolean) = "payment_success?plan=$plan"
        const val routeWithArg = "payment_success?plan={plan}"
    }

    // Booking con query trainerId
    data object Booking : Screen("booking?trainerId={trainerId}") {
        fun routeWith(trainerId: Long?) =
            if (trainerId != null) "booking?trainerId=$trainerId" else "booking?trainerId=-1"
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavGraph(
    startDestination: String = Screen.Login.route,
    windowSizeClass: WindowSizeClass // <-- PARÁMETRO AÑADIDO
) {
    val navController = rememberAnimatedNavController()

    val enterRight = { slideInHorizontally(animationSpec = tween(300)) { it } }
    val exitLeft = { slideOutHorizontally(animationSpec = tween(300)) { -it } }
    val enterLeft = { slideInHorizontally(animationSpec = tween(300)) { -it } }
    val exitRight = { slideOutHorizontally(animationSpec = tween(300)) { it } }

    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination,
        route = "root" // opcional, ayuda a estructurar popUpTo
    ) {
        // ======= Hijos directos del NavHost raíz =======

        composable(
            route = Screen.Login.route,
            enterTransition = { enterRight() },
            exitTransition = { exitLeft() },
            popEnterTransition = { enterLeft() },
            popExitTransition = { exitRight() }
        ) {
            // Ahora puedes pasar windowSizeClass si LoginScreen lo necesita
            // cl.gymtastic.app.ui.auth.LoginScreen(navController, windowSizeClass)
            cl.gymtastic.app.ui.auth.LoginScreen(navController, windowSizeClass)
        }

        composable(
            route = Screen.Register.route,
            enterTransition = { enterRight() },
            exitTransition = { exitLeft() },
            popEnterTransition = { enterLeft() },
            popExitTransition = { exitRight() }
        ) {
            cl.gymtastic.app.ui.auth.RegisterScreen(navController, windowSizeClass)
        }

        composable(
            route = Screen.Home.route,
            enterTransition = { enterRight() },
            exitTransition = { exitLeft() },
            popEnterTransition = { enterLeft() },
            popExitTransition = { exitRight() }
        ) {
            cl.gymtastic.app.ui.home.HomeScreen(navController, windowSizeClass)
        }

        composable(
            route = Screen.Planes.route,
            enterTransition = { enterRight() },
            exitTransition = { exitLeft() },
            popEnterTransition = { enterLeft() },
            popExitTransition = { exitRight() }
        ) {
            cl.gymtastic.app.ui.planes.PlanesScreen(navController, windowSizeClass)
        }

        composable(
            route = Screen.Payment.route,
            enterTransition = { enterRight() },
            exitTransition = { exitLeft() },
            popEnterTransition = { enterLeft() },
            popExitTransition = { exitRight() }
        ) {
            cl.gymtastic.app.ui.payment.PaymentScreen(navController, windowSizeClass)
        }

        composable(
            route = Screen.Store.route,
            enterTransition = { enterRight() },
            exitTransition = { exitLeft() },
            popEnterTransition = { enterLeft() },
            popExitTransition = { exitRight() }
        ) {
            cl.gymtastic.app.ui.store.StoreScreen(navController, windowSizeClass)
        }

        composable(
            route = Screen.Cart.route,
            enterTransition = { enterRight() },
            exitTransition = { exitLeft() },
            popEnterTransition = { enterLeft() },
            popExitTransition = { exitRight() }
        ) {
            cl.gymtastic.app.ui.cart.CartScreen(navController, windowSizeClass)
        }

        composable(
            route = Screen.CheckIn.route,
            enterTransition = { enterRight() },
            exitTransition = { exitLeft() },
            popEnterTransition = { enterLeft() },
            popExitTransition = { exitRight() }
        ) {
            cl.gymtastic.app.ui.checkin.CheckInScreen(navController, windowSizeClass)
        }

        composable(
            route = Screen.Trainers.route,
            enterTransition = { enterRight() },
            exitTransition = { exitLeft() },
            popEnterTransition = { enterLeft() },
            popExitTransition = { exitRight() }
        ) {
            cl.gymtastic.app.ui.trainers.TrainersScreen(navController, windowSizeClass)
        }

        composable(
            route = Screen.Booking.route,
            arguments = listOf(
                navArgument("trainerId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            ),
            enterTransition = { enterRight() },
            exitTransition = { exitLeft() },
            popEnterTransition = { enterLeft() },
            popExitTransition = { exitRight() }
        ) {
            it.arguments?.getLong("trainerId") ?: -1L
            cl.gymtastic.app.ui.booking.BookingScreen(
                navController,
                windowSizeClass /*, trainerId*/
            )
        }

        // ÚNICA definición de payment_success con arg opcional plan
        composable(
            route = Screen.PaymentSuccess.routeWithArg,
            arguments = listOf(
                navArgument("plan") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            ),
            enterTransition = { enterRight() },
            exitTransition = { exitLeft() },
            popEnterTransition = { enterLeft() },
            popExitTransition = { exitRight() }
        ) { backStackEntry ->
            val planActivated = backStackEntry.arguments?.getBoolean("plan") ?: false
            cl.gymtastic.app.ui.payment.PaymentSuccessScreen(
                nav = navController,
                planActivated = planActivated,windowSizeClass
            )
        }

        composable(
            route = Screen.Profile.route,
            enterTransition = { enterRight() },
            exitTransition = { exitLeft() },
            popEnterTransition = { enterLeft() },
            popExitTransition = { exitRight() }
        ) {
            cl.gymtastic.app.ui.profile.ProfileScreen(navController, windowSizeClass)
        }

        composable(
            route = Screen.Admin.route,
            enterTransition = { enterRight() },
            exitTransition = { exitLeft() },
            popEnterTransition = { enterLeft() },
            popExitTransition = { exitRight() }
        ) {
            cl.gymtastic.app.ui.admin.AdminScreen(navController, windowSizeClass)


        }
    }

}
