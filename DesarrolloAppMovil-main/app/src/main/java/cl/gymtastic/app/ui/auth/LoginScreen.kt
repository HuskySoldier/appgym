package cl.gymtastic.app.ui.auth

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import cl.gymtastic.app.data.repository.AuthRepository
import cl.gymtastic.app.ui.navigation.NavRoutes
import cl.gymtastic.app.ui.navigation.Screen
import cl.gymtastic.app.util.ServiceLocator
import kotlinx.coroutines.launch

// -----------------------------
// ViewModel
// -----------------------------
class LoginViewModel(
    private val repoProvider: (context: android.content.Context) -> AuthRepository
) : ViewModel() {

    var loading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun clearError() {
        error = null
    }

    fun login(context: android.content.Context, emailRaw: String, passRaw: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            loading = true
            error = null
            val email = emailRaw.trim().lowercase()
            val pass = passRaw.trim()

            val loginSuccessful = repoProvider(context).login(email, pass)

            loading = false
            if (loginSuccessful) {
                onSuccess()
            } else {
                error = "Credenciales inválidas"
            }
        }
    }
}

// -----------------------------
// Pantalla de Login
// -----------------------------
@SuppressLint("UnrememberedMutableState")
@Composable
fun LoginScreen(
    nav: NavController,
    windowSizeClass: WindowSizeClass,
    // INYECCIÓN: Este parámetro permite que los tests pasen un VM falso.
    // 'remember' crea el VM real por defecto para la app normal.
    viewModel: LoginViewModel = remember { LoginViewModel { ServiceLocator.auth(it) } }
) {
    val ctx = LocalContext.current
    val vm = viewModel // Usamos el VM inyectado

    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var passVisible by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val keyboard = LocalSoftwareKeyboardController.current

    // Animaciones
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }

    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(vm.error) {
        if (vm.error != null) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes { durationMillis = 500; (-14f) at 50; (14f) at 100; (-10f) at 150; (10f) at 200; (-6f) at 250; (6f) at 300; (-3f) at 350; (3f) at 400; (0f) at 500 }
            )
        }
    }

    val cs = MaterialTheme.colorScheme
    val bg = Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.22f), cs.surface))
    val scroll = rememberScrollState()

    val isEmailValid by derivedStateOf { email.contains("@") && email.contains(".") && email.length >= 6 }
    val isPassValid by derivedStateOf { pass.length >= 6 }

    fun doLogin() {
        keyboard?.hide()
        val currentEmail = email.trim().lowercase()

        vm.login(ctx, currentEmail, pass) {
            scope.launch {
                val authRepo = ServiceLocator.auth(ctx)
                val userProfile = authRepo.getUserProfile(currentEmail)

                if (userProfile?.rol == "trainer") {
                    nav.navigate(Screen.TrainerDashboard.route) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    nav.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    // Layout
    Box(modifier = Modifier.fillMaxSize().background(bg).padding(16.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = show, enter = fadeIn() + slideInVertically { it / 3 }, exit = fadeOut() + slideOutVertically { it / 3 }) {
            val widthSizeClass = windowSizeClass.widthSizeClass
            val cardModifier = if (widthSizeClass == WindowWidthSizeClass.Compact) {
                Modifier.offset(x = shakeOffset.value.dp).fillMaxWidth(0.92f).shadow(10.dp, RoundedCornerShape(28.dp))
            } else {
                Modifier.offset(x = shakeOffset.value.dp).width(480.dp).shadow(10.dp, RoundedCornerShape(28.dp))
            }
            Card(modifier = cardModifier, shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 24.dp).verticalScroll(scroll), horizontalAlignment = Alignment.CenterHorizontally) {

                    Box(modifier = Modifier.fillMaxWidth().background(brush = Brush.horizontalGradient(listOf(cs.primary, cs.secondary)), shape = RoundedCornerShape(20.dp)).padding(vertical = 14.dp, horizontal = 12.dp)) {
                        Text(text = "GymTastic", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, letterSpacing = 1.5.sp), color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Bienvenido 👋\nInicia sesión para continuar", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email, onValueChange = { email = it; vm.clearError() },
                        label = { Text("Email") }, singleLine = true, leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        isError = (email.isNotBlank() && !isEmailValid) || vm.error != null,
                        supportingText = { if (email.isNotBlank() && !isEmailValid) Text("Ingresa un email válido") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cs.primary, unfocusedBorderColor = cs.onSurface.copy(alpha = 0.3f), cursorColor = cs.primary),
                        modifier = Modifier.fillMaxWidth(0.94f)
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pass, onValueChange = { pass = it; vm.clearError() },
                        label = { Text("Contraseña") }, singleLine = true, leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = { IconButton(onClick = { passVisible = !passVisible }) { Icon(imageVector = if (passVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = if (passVisible) "Ocultar" else "Mostrar") } },
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (!vm.loading && isEmailValid && isPassValid) doLogin() }),
                        isError = (pass.isNotBlank() && !isPassValid) || vm.error != null,
                        supportingText = { if (pass.isNotBlank() && !isPassValid) Text("Mínimo 6 caracteres") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cs.primary, unfocusedBorderColor = cs.onSurface.copy(alpha = 0.3f), cursorColor = cs.primary),
                        modifier = Modifier.fillMaxWidth(0.94f)
                    )
                    Spacer(Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth(0.94f), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = { nav.navigate(NavRoutes.FORGOT_PASSWORD) }) {
                            Text("¿Olvidaste tu contraseña?", style = MaterialTheme.typography.bodySmall, color = cs.primary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    AnimatedVisibility(visible = vm.error != null) {
                        Text(text = vm.error ?: "", color = cs.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(0.94f).padding(bottom = 8.dp))
                    }

                    Button(onClick = { doLogin() }, enabled = !vm.loading && isEmailValid && isPassValid, modifier = Modifier.fillMaxWidth(0.94f).height(48.dp)) {
                        if (vm.loading) { CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp).padding(end = 8.dp), color = cs.onPrimary) }
                        Text(if (vm.loading) "Ingresando..." else "Ingresar")
                    }
                    Spacer(Modifier.height(10.dp))

                    Button(onClick = { nav.navigate(NavRoutes.REGISTER) }, modifier = Modifier.fillMaxWidth(0.94f).height(48.dp)) {
                        Text("Crear cuenta")
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}