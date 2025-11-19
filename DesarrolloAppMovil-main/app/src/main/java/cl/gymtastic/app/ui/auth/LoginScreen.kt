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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import cl.gymtastic.app.data.repository.AuthRepository // Importa AuthRepository si ViewModel lo necesita
import cl.gymtastic.app.ui.navigation.NavRoutes // Asegúrate que NavRoutes esté definido
import cl.gymtastic.app.ui.navigation.Screen
import kotlinx.coroutines.launch

// -----------------------------
// ViewModel
// -----------------------------
class LoginViewModel(
    // Acepta una función que provea el AuthRepository
    private val repoProvider: (context: android.content.Context) -> AuthRepository
) : ViewModel() {
    // Estado de carga (true mientras se verifica el login)
    var loading by mutableStateOf(false)
        private set // Solo modificable desde el ViewModel

    // Estado de error (String con mensaje si falla, null si no hay error)
    var error by mutableStateOf<String?>(null)
        private set // Solo modificable desde el ViewModel

    // --- FUNCIÓN MOVIDA AQUÍ ---
    /** Limpia el mensaje de error actual. */
    fun clearError() {
        error = null
    }
    // --- FIN FUNCIÓN MOVIDA ---

    // Función llamada desde la UI para intentar iniciar sesión
    fun login(context: android.content.Context, emailRaw: String, passRaw: String, onSuccess: () -> Unit) {
        // Lanzar coroutine para operaciones asíncronas (llamada al repo)
        viewModelScope.launch {
            loading = true // Inicia estado de carga
            error = null   // Limpia errores previos
            // Normaliza email y contraseña (quitar espacios, minúsculas para email)
            val email = emailRaw.trim().lowercase()
            val pass = passRaw.trim()
            // Llama al AuthRepository (obtenido via repoProvider) para verificar credenciales
            val loginSuccessful = repoProvider(context).login(email, pass)
            loading = false // Finaliza estado de carga
            if (loginSuccessful) {
                onSuccess() // Llama a la lambda si el login fue exitoso (para navegar)
            } else {
                error = "Credenciales inválidas" // Establece mensaje de error si falló
            }
        }
    }
}

// -----------------------------
// Pantalla de Login
// -----------------------------
@SuppressLint("UnrememberedMutableState") // Justificado si el estado se maneja con ViewModel/rememberSaveable
@Composable
fun LoginScreen(
    nav: NavController,
    windowSizeClass: WindowSizeClass // Recibe WindowSizeClass para diseño adaptativo
) {
    // --- Preparativos ---
    val ctx = LocalContext.current // Contexto actual
    // Crea y recuerda la instancia del ViewModel
    val vm: LoginViewModel = remember { LoginViewModel { ServiceLocator.auth(it) } }

    // --- Estado de UI / Campos del Formulario ---
    var email by rememberSaveable { mutableStateOf("admin@gymtastic.cl") } // Valor inicial para pruebas
    var pass by rememberSaveable { mutableStateOf("admin123") } // Valor inicial para pruebas
    var passVisible by rememberSaveable { mutableStateOf(false) } // Estado para mostrar/ocultar contraseña
    val scope = rememberCoroutineScope()

    val keyboard = LocalSoftwareKeyboardController.current // Controlador para ocultar teclado

    // --- Animaciones ---
    var show by remember { mutableStateOf(false) } // Estado para animación de entrada
    LaunchedEffect(Unit) { show = true } // Inicia animación

    // Estado animable para "shake" en caso de error
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(vm.error) { // Reacciona a cambios en vm.error
        if (vm.error != null) { // Si hay error...
            shakeOffset.snapTo(0f) // Resetea
            shakeOffset.animateTo( /* ... keyframes ... */ // Anima el shake
                targetValue = 0f,
                animationSpec = keyframes { durationMillis = 500; (-14f) at 50; (14f) at 100; (-10f) at 150; (10f) at 200; (-6f) at 250; (6f) at 300; (-3f) at 350; (3f) at 400; (0f) at 500 }
            )
        }
    }

    // --- Estilo y Layout ---
    val cs = MaterialTheme.colorScheme // Colores
    val bg = Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.22f), cs.surface)) // Fondo gradiente
    val scroll = rememberScrollState() // Estado para scroll interno

    // --- Validaciones ---
    val isEmailValid by derivedStateOf { email.contains("@") && email.contains(".") && email.length >= 6 }
    val isPassValid by derivedStateOf { pass.length >= 6 }

    // --- Función de Acción ---
    // --- Función de Acción CORREGIDA ---
    fun doLogin() {
        keyboard?.hide()
        // Usamos 'email' directamente del estado del formulario
        val currentEmail = email.trim().lowercase()

        vm.login(ctx, currentEmail, pass) {
            // ÉXITO (Callback del ViewModel)

            // Usamos el 'scope' que declaramos arriba
            scope.launch {
                // Consultamos la DB directo con el email del formulario
                val dao = ServiceLocator.db(ctx).users()
                val entity = dao.findByEmail(currentEmail) // Esto es una función suspendida, por eso requiere scope.launch

                if (entity?.rol == "trainer") {
                    // Redirigir a Dashboard de Trainer
                    nav.navigate(Screen.TrainerDashboard.route) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    // Redirigir a Home normal (User o Admin)
                    nav.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    // --- Layout Principal ---
    Box(modifier = Modifier.fillMaxSize().background(bg).padding(16.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = show, enter = fadeIn() + slideInVertically { it / 3 }, exit = fadeOut() + slideOutVertically { it / 3 }) { // Tarjeta animada
            val widthSizeClass = windowSizeClass.widthSizeClass
            val cardModifier = if (widthSizeClass == WindowWidthSizeClass.Compact) { // Ancho adaptativo
                Modifier.offset(x = shakeOffset.value.dp).fillMaxWidth(0.92f).shadow(10.dp, RoundedCornerShape(28.dp))
            } else {
                Modifier.offset(x = shakeOffset.value.dp).width(480.dp).shadow(10.dp, RoundedCornerShape(28.dp))
            }
            Card(modifier = cardModifier, shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) { // Card principal
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 24.dp).verticalScroll(scroll), horizontalAlignment = Alignment.CenterHorizontally) { // Contenido scrollable
                    // --- Encabezado ---
                    Box(modifier = Modifier.fillMaxWidth().background(brush = Brush.horizontalGradient(listOf(cs.primary, cs.secondary)), shape = RoundedCornerShape(20.dp)).padding(vertical = 14.dp, horizontal = 12.dp)) {
                        Text(text = "GymTastic", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, letterSpacing = 1.5.sp), color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Bienvenido 👋\nInicia sesión para continuar", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))

                    // --- Campo Email ---
                    OutlinedTextField(
                        value = email, onValueChange = { email = it; vm.clearError() }, // Llama a clearError()
                        label = { Text("Email") }, singleLine = true, leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        isError = (email.isNotBlank() && !isEmailValid) || vm.error != null, // Muestra error si formato mal O hay error de login
                        supportingText = { if (email.isNotBlank() && !isEmailValid) Text("Ingresa un email válido") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cs.primary, unfocusedBorderColor = cs.onSurface.copy(alpha = 0.3f), cursorColor = cs.primary),
                        modifier = Modifier.fillMaxWidth(0.94f)
                    )
                    Spacer(Modifier.height(12.dp))

                    // --- Campo Contraseña ---
                    OutlinedTextField(
                        value = pass, onValueChange = { pass = it; vm.clearError() }, // Llama a clearError()
                        label = { Text("Contraseña") }, singleLine = true, leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = { IconButton(onClick = { passVisible = !passVisible }) { Icon(imageVector = if (passVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = if (passVisible) "Ocultar" else "Mostrar") } },
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (!vm.loading && isEmailValid && isPassValid) doLogin() }),
                        isError = (pass.isNotBlank() && !isPassValid) || vm.error != null, // Muestra error si largo mal O hay error de login
                        supportingText = { if (pass.isNotBlank() && !isPassValid) Text("Mínimo 6 caracteres") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cs.primary, unfocusedBorderColor = cs.onSurface.copy(alpha = 0.3f), cursorColor = cs.primary),
                        modifier = Modifier.fillMaxWidth(0.94f)
                    )
                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(0.94f),
                        contentAlignment = Alignment.CenterEnd // Alineado a la derecha
                    ) {
                        TextButton(onClick = { nav.navigate(NavRoutes.FORGOT_PASSWORD) }) {
                            Text(
                                "¿Olvidaste tu contraseña?",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))



                    // --- Mensaje de Error (Credenciales Inválidas) ---
                    AnimatedVisibility(visible = vm.error != null) { // Aparece si vm.error no es null
                        Text(
                            text = vm.error ?: "", // Muestra el mensaje del ViewModel
                            color = cs.error, // Color de error
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(0.94f).padding(bottom = 8.dp) // Padding
                        )
                    }

                    // --- Botón Ingresar ---
                    Button(onClick = { doLogin() }, enabled = !vm.loading && isEmailValid && isPassValid, /*...*/ modifier = Modifier.fillMaxWidth(0.94f).height(48.dp)) {
                        if (vm.loading) { CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp).padding(end = 8.dp), color = cs.onPrimary) }
                        Text(if (vm.loading) "Ingresando..." else "Ingresar")
                    }
                    Spacer(Modifier.height(10.dp))

                    // --- Botón Crear Cuenta ---
                    Button(onClick = { nav.navigate(NavRoutes.REGISTER) }, /*...*/ modifier = Modifier.fillMaxWidth(0.94f).height(48.dp)) {
                        Text("Crear cuenta")
                    }
                    Spacer(Modifier.height(16.dp)) // Espacio final
                }
            }
        }
    }
}

// --- FUNCIÓN DE EXTENSIÓN ELIMINADA ---
// Ya no está aquí, fue movida dentro de LoginViewModel

