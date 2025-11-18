package cl.gymtastic.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

//  Paleta
val RedPrimary      = Color(0xFFD32F2F) // rojo
val BlackBackground = Color(0xFF121212) // negro
val GraySurface     = Color(0xFF2C2C2C) // gris oscuro (cards / sheets)
val WhiteText       = Color(0xFFFFFFFF) // blanco
val GrayText        = Color(0xFFB0B0B0) // gris claro (texto secundario)

private val DarkScheme = darkColorScheme(
    primary = RedPrimary,
    onPrimary = WhiteText,
    background = BlackBackground,
    onBackground = WhiteText,
    surface = GraySurface,
    onSurface = WhiteText,
    secondary = RedPrimary.copy(alpha = 0.85f),
    onSecondary = WhiteText,
    surfaceVariant = Color(0xFF1C1C1C),
    onSurfaceVariant = GrayText,
    error = Color(0xFFCF6679)
)

private val LightScheme = lightColorScheme(
    primary = RedPrimary,
    onPrimary = WhiteText,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color(0xFFF5F5F5),
    onSurface = Color(0xFF111111),
    secondary = RedPrimary,
    onSecondary = WhiteText,
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF4D4D4D),
    error = Color(0xFFB00020)
)

@Composable
fun GymTasticTheme(
    darkTheme: Boolean = true, // puedes leer de DataStore si quieres permitir cambiarlo
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = Typography(),
        content = content
    )
}
