package cl.gymtastic.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect // Mantener para permiso
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import cl.gymtastic.app.ui.navigation.NavGraph
import cl.gymtastic.app.ui.navigation.Screen
import cl.gymtastic.app.ui.theme.GymTasticTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController


class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val context = LocalContext.current

            // --- LANZADOR PARA SOLICITAR PERMISO DE NOTIFICACIONES ---
            // (Se mantiene aquí porque necesita el resultado en la Activity/Composable
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted: Boolean ->
                    if (isGranted) {
                        Toast.makeText(context, "Permiso de notificaciones concedido", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // --- LaunchedEffect AHORA SOLO PARA SOLICITAR PERMISO ---
            LaunchedEffect(key1 = Unit) {
                // --- SOLICITUD DE PERMISO (Android 13+) ---
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    when {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            // Permiso ya concedido, no hacer nada extra aquí
                        }
                        else -> {
                            // Solicitar permiso
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
            }

            GymTasticTheme {
                val systemUiController = rememberSystemUiController()
                val darkIcons = false // Íconos blancos para barra de estado negra

                // Configura color de la barra de estado
                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = Color.Black, // Barra de estado negra
                        darkIcons = darkIcons // Íconos claros
                    )
                }

                // Carga el grafo de navegación principal
                NavGraph(
                    startDestination = Screen.Login.route, // Empieza en Login
                    windowSizeClass = windowSizeClass // Pasa la clase de tamaño
                )
            }
        }
    }
}

