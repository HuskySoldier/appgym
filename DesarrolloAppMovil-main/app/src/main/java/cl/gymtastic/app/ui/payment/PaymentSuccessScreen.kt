package cl.gymtastic.app.ui.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.gymtastic.app.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSuccessScreen(
    nav: NavController,
    planActivated: Boolean,
    windowSizeClass: WindowSizeClass // <-- PARÁMETRO AÑADIDO
) {
    val cs = MaterialTheme.colorScheme
    val bg = Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.22f), cs.surface))

    // Reacciona al tamaño de pantalla
    val widthSizeClass = windowSizeClass.widthSizeClass
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact
    val cardModifier = if (isCompact) {
        Modifier.fillMaxWidth(0.92f)
    } else {
        Modifier.width(550.dp) // Ancho fijo para tablets
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pago exitoso", color = cs.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = cs.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.background,
                    titleContentColor = cs.onBackground,
                    navigationIconContentColor = cs.onBackground
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
                ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                modifier = cardModifier // <-- APLICAMOS MODIFICADOR
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (planActivated) "¡Membresía activada! 🎉" else "¡Compra completada! 🛍️",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (planActivated)
                            "Tu plan ya está activo. Ahora puedes usar Check-In y agendar con Trainers."
                        else
                            "Gracias por tu compra. Tus productos estarán disponibles según el método de retiro/envío.",
                        color = cs.onSurfaceVariant
                    )

                    Spacer(Modifier.height(20.dp))

                    // Acciones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { nav.navigate(Screen.Home.route) { popUpTo(0) } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ir al Home")
                        }

                        Button(
                            onClick = {
                                if (planActivated) {
                                    nav.navigate(Screen.CheckIn.route) { launchSingleTop = true }
                                } else {
                                    nav.navigate(Screen.Store.route) { launchSingleTop = true }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            if (planActivated) {
                                Icon(Icons.Filled.FitnessCenter, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Ir a Check-In")
                            } else {
                                Text("Seguir comprando")
                            }
                        }
                    }
                }
            }
        }
    }
}
