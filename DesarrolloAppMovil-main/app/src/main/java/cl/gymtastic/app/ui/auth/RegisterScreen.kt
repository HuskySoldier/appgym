package cl.gymtastic.app.ui.auth

import cl.gymtastic.app.util.ServiceLocator
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.gymtastic.app.ui.navigation.NavRoutes
import kotlinx.coroutines.launch

@SuppressLint("UnrememberedMutableState")
@Composable
fun RegisterScreen(
    nav: NavController,
    windowSizeClass: WindowSizeClass // <-- PARÁMETRO AÑADIDO
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val cs = MaterialTheme.colorScheme

    // -----------------------------
    // Estado de UI / campos
    // -----------------------------
    var loading by remember { mutableStateOf(false) }
    var bannerMsg by remember { mutableStateOf<String?>(null) }
    var bannerIsError by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var pass2Visible by remember { mutableStateOf(false) }
    var acceptTerms by remember { mutableStateOf(false) }

    // -----------------------------
    // Animación de entrada + shake en error
    // -----------------------------
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }

    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(bannerMsg, bannerIsError) {
        if (bannerMsg != null && bannerIsError) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 500
                    (-14f) at 50 using LinearEasing
                    (14f) at 100 using LinearEasing
                    (-10f) at 150 using LinearEasing
                    (10f) at 200 using LinearEasing
                    (-6f) at 250 using LinearEasing
                    (6f) at 300 using LinearEasing
                    (-3f) at 350 using LinearEasing
                    (3f) at 400 using LinearEasing
                    (0f) at 500 using LinearEasing
                }
            )
        }
    }

    // -----------------------------
    // Reacciona al tamaño de pantalla
    // -----------------------------
    val widthSizeClass = windowSizeClass.widthSizeClass


    // -----------------------------
    // Fondo: negro sólido o gradiente
    // -----------------------------
    val usePureBlack = false
    val bg =
        if (usePureBlack) null
        else Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.30f), cs.surface))

    // Scroll interno por si el teclado tapa algo
    val scroll = rememberScrollState()

    // -----------------------------
    // Validaciones “en vivo”
    // -----------------------------
    val isEmailValid by derivedStateOf {
        email.contains("@") && email.contains(".") && email.length >= 6
    }
    val isNameValid by derivedStateOf { name.trim().length >= 2 }
    val passStrength by derivedStateOf { calcPasswordStrength(pass) } // 0..4
    val isPassValid by derivedStateOf { pass.length >= 6 }
    val passwordsMatch by derivedStateOf { pass.isNotEmpty() && pass == pass2 }

    // Botón habilitado solo si todo ok
    val canSubmit by derivedStateOf {
        !loading && isEmailValid && isNameValid && isPassValid && passwordsMatch
    }

    fun showError(msg: String) {
        bannerIsError = true
        bannerMsg = msg
    }

    // Acción unificada para registrar
    fun doRegister() {
        keyboard?.hide()
        // Validación final (por si acaso)
        when {
            !isEmailValid -> showError("Ingresa un email válido")
            !isNameValid -> showError("Ingresa tu nombre")
            !isPassValid -> showError("La contraseña debe tener al menos 6 caracteres")
            !passwordsMatch -> showError("Las contraseñas no coinciden")
            else -> {
                loading = true
                bannerMsg = null
                scope.launch {
                    val ok = ServiceLocator.auth(ctx).register(email.trim(), pass, name.trim())
                    loading = false
                    if (ok) {
                        bannerIsError = false
                        bannerMsg = "✅ Cuenta creada: ahora puedes iniciar sesión"
                        // nav.popBackStack(); nav.navigate(NavRoutes.LOGIN)
                    } else {
                        showError("Este email ya existe")
                    }
                }
            }
        }
    }

    // -----------------------------
    // Layout
    // -----------------------------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (bg == null) Modifier.background(Color.Black) else Modifier.background(bg)
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = show,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
        ) {

            // --- Modificador dinámico de la Card
            val cardModifier = if (widthSizeClass == WindowWidthSizeClass.Compact) {
                Modifier
                    .offset(x = shakeOffset.value.dp)
                    .fillMaxWidth(0.92f) // Ancho para teléfonos
                    .shadow(10.dp, RoundedCornerShape(28.dp))
            } else {
                Modifier
                    .offset(x = shakeOffset.value.dp)
                    .width(480.dp) // Ancho fijo para tablets/landscape
                    .shadow(10.dp, RoundedCornerShape(28.dp))
            }

            Card(
                modifier = cardModifier, // <-- APLICAMOS EL MODIFICADOR DINÁMICO
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp)
                        .verticalScroll(scroll),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // ---------- Título ----------
                    Text(
                        "Crear cuenta",
                        style = MaterialTheme.typography.headlineMedium,
                        color = cs.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Regístrate para empezar a usar GymTastic",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(22.dp))

                    // ---------- Email ----------
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        isError = email.isNotBlank() && !isEmailValid,
                        supportingText = {
                            if (email.isNotBlank() && !isEmailValid) Text("Ingresa un email válido")
                        },
                        modifier = Modifier.fillMaxWidth(0.94f)
                    )

                    Spacer(Modifier.height(12.dp))

                    // ---------- Nombre ----------
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        isError = name.isNotBlank() && !isNameValid,
                        supportingText = {
                            if (name.isNotBlank() && !isNameValid) Text("Mínimo 2 caracteres")
                        },
                        modifier = Modifier.fillMaxWidth(0.94f)
                    )

                    Spacer(Modifier.height(12.dp))

                    // ---------- Password (con barra de fuerza visible) ----------
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Icon(
                                    imageVector = if (passVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        isError = pass.isNotBlank() && !isPassValid,
                        supportingText = {
                            //  Más espacio y contraste: barra + etiqueta + error, apilados
                            val (strengthColor, strengthLabel) = strengthColorAndLabel(passStrength)
                            Column(Modifier.fillMaxWidth()) {
                                AnimatedVisibility(visible = pass.isNotBlank()) {
                                    Column(Modifier.fillMaxWidth()) {
                                        PasswordStrengthBar(
                                            level = passStrength,
                                            barHeightDp = 8,          // más alto = más visible
                                            activeColor = strengthColor
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            strengthLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = strengthColor
                                        )
                                    }
                                }
                                if (pass.isNotBlank() && !isPassValid) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Mínimo 6 caracteres",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.error
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.94f)
                    )

                    Spacer(Modifier.height(12.dp))

                    // ---------- Confirmar Password ----------
                    OutlinedTextField(
                        value = pass2,
                        onValueChange = { pass2 = it },
                        label = { Text("Confirmar contraseña") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { pass2Visible = !pass2Visible }) {
                                Icon(
                                    imageVector = if (pass2Visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (pass2Visible) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        visualTransformation = if (pass2Visible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (canSubmit) doRegister() }
                        ),
                        isError = pass2.isNotBlank() && !passwordsMatch,
                        supportingText = {
                            if (pass2.isNotBlank() && !passwordsMatch) Text("Las contraseñas no coinciden")
                        },
                        modifier = Modifier.fillMaxWidth(0.94f)
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Al registrarte aceptas los terminos y condiciones",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))

                    // ---------- Botón Registrar ----------
                    Button(
                        onClick = { doRegister() },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canSubmit) cs.primary else cs.primary.copy(alpha = 0.6f),
                            contentColor = cs.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.94f)
                            .height(48.dp)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 8.dp),
                                color = cs.onPrimary
                            )
                        }
                        Text(if (loading) "Guardando..." else "Crear cuenta")
                    }

                    // ---------- Link Volver / Ir a Login ----------
                    TextButton(
                        onClick = {
                            nav.popBackStack()
                            nav.navigate(NavRoutes.LOGIN)
                        },
                        modifier = Modifier.fillMaxWidth(0.94f)
                    ) {
                        Text("¿Ya tienes cuenta? Inicia sesión")
                    }

                    // ---------- Banner de mensajes ----------
                    AnimatedVisibility(
                        visible = bannerMsg != null,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 })
                    ) {
                        bannerMsg?.let { text ->
                            val color = if (bannerIsError) cs.error else cs.primary
                            Text(
                                text = text,
                                color = color,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Barra de fuerza de contraseña (0..4) configurable.
 */
@Composable
private fun PasswordStrengthBar(
    level: Int,                 // 0..4
    barHeightDp: Int = 8,       // alto de la barra
    activeColor: Color = Color(0xFF81C784) // color activo (verde por defecto)
) {
    val inactive = Color(0xFFE0E0E0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeightDp.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(4) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (i < level) activeColor else inactive,
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

/**
 * Devuelve (color, etiqueta) para el nivel de fuerza 0..4.
 */
@Composable
private fun strengthColorAndLabel(level: Int): Pair<Color, String> {
    val lvl = level.coerceIn(0, 4)
    val color = when (lvl) {
        0 -> Color(0xFFB0BEC5)           // gris
        1 -> Color(0xFFE57373)           // rojo
        2 -> Color(0xFFFFB74D)           // naranjo
        3 -> Color(0xFF64B5F6)           // azul
        else -> Color(0xFF81C784)        // verde
    }
    val label = when (lvl) {
        0 -> "Muy débil"
        1 -> "Débil"
        2 -> "Media"
        3 -> "Buena"
        else -> "Fuerte"
    }
    return color to label
}

/**
 * Calcula la fuerza de la contraseña en una escala 0..4.
 * +1 por cada criterio: largo >= 8, mayúsculas, dígitos, símbolos.
 * Si el largo >= 12, agrega un punto extra (máximo 4).
 */
private fun calcPasswordStrength(pw: String): Int {
    if (pw.isBlank()) return 0
    var score = 0
    if (pw.length >= 8) score++
    if (pw.any { it.isUpperCase() }) score++
    if (pw.any { it.isDigit() }) score++
    if (pw.any { !it.isLetterOrDigit() }) score++
    if (pw.length >= 12) score++   // bonus
    return score.coerceAtMost(4)
}
