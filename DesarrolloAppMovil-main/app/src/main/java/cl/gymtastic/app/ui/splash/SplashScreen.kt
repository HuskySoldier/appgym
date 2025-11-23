package cl.gymtastic.app.ui.splash

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cl.gymtastic.app.R
import cl.gymtastic.app.ui.navigation.Screen // Usamos Screen en lugar de NavRoutes para coincidir con tu NavGraph
import cl.gymtastic.app.util.ServiceLocator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun SplashScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scale = remember { Animatable(0f) }

    val authRepo = remember { ServiceLocator.auth(ctx) }

    LaunchedEffect(key1 = true) {
        // 1. Animación
        scale.animateTo(
            targetValue = 0.7f,
            animationSpec = tween(
                durationMillis = 800,
                easing = { OvershootInterpolator(4f).getInterpolation(it) }
            )
        )

        // 2. Espera
        delay(1500)

        // 3. Verificar sesión y preferencia "Remember Me"
        val prefs = authRepo.prefs()
        val userEmail = prefs.userEmailFlow.first()
        val isRemembered = prefs.rememberMeFlow.first() // <-- Leemos el checkbox guardado

        // 4. Decisión de Navegación
        if (userEmail.isNotBlank() && isRemembered) {
            // A. Tiene usuario Y quiere ser recordado -> HOME
            // Verificamos si es trainer para mandarlo a su dashboard (Opcional, según tu lógica de Login)
            // Por simplicidad, aquí mandamos al Home genérico y que Home decida o redireccione si es necesario,
            // o puedes hacer una llamada rápida a getUserProfile aquí si quieres ser muy específico.
            // Asumiremos Home por defecto:

            nav.navigate(Screen.Home.route) { // Usamos Screen.Home.route
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        } else {
            // B. No tiene usuario O no quiso ser recordado -> LOGIN

            // Si había datos pero isRemembered es false, limpiamos por seguridad
            if (userEmail.isNotBlank() && !isRemembered) {
                authRepo.logout()
            }

            nav.navigate(Screen.Login.route) { // Usamos Screen.Login.route
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    // Diseño Visual (Sin cambios)
    val cs = MaterialTheme.colorScheme
    val bg = Brush.verticalGradient(listOf(cs.primary, cs.surface))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale.value)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "GYMTASTIC",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier.scale(scale.value)
            )
        }

        Text(
            text = "v1.0",
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}