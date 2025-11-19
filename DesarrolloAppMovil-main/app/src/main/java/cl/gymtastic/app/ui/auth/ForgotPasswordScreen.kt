package cl.gymtastic.app.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.gymtastic.app.ui.navigation.NavRoutes
import cl.gymtastic.app.util.ServiceLocator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    // Estado del flujo: 1 = Ingresar Email, 2 = Ingresar Token y Nueva Clave
    var step by remember { mutableIntStateOf(1) }

    // Campos
    var email by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }

    // UI States
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val bg = Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.22f), cs.surface))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuperar Contraseña") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.surface)
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
            Card(
                modifier = Modifier.fillMaxWidth(0.95f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Animación simple entre pasos
                    if (step == 1) {
                        // --- PASO 1: SOLICITAR EMAIL ---
                        Text("¿Olvidaste tu contraseña?", style = MaterialTheme.typography.headlineSmall, color = cs.primary)
                        Spacer(Modifier.height(12.dp))
                        Text("Ingresa tu email y te enviaremos un código de verificación.", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done)
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (email.isBlank()) {
                                    message = "Error: Ingresa un email válido"
                                    return@Button
                                }
                                loading = true
                                message = null
                                scope.launch {
                                    // Llamada al Repositorio (requestReset)
                                    val success = ServiceLocator.auth(ctx).requestPasswordReset(email)
                                    loading = false
                                    if (success) {
                                        message = "Código enviado. Revisa tu correo (o consola)."
                                        step = 2 // AVANZAR AL PASO 2
                                    } else {
                                        message = "Error al enviar solicitud. Verifica tu conexión."
                                    }
                                }
                            },
                            enabled = !loading && email.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = cs.onPrimary)
                            else Text("Enviar Código")
                        }

                    } else {
                        // --- PASO 2: CONFIRMAR TOKEN ---
                        Text("Restablecer contraseña", style = MaterialTheme.typography.headlineSmall, color = cs.primary)
                        Spacer(Modifier.height(12.dp))
                        Text("Ingresa el código de 6 dígitos enviado a $email y tu nueva contraseña.", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))

                        // Campo Token
                        OutlinedTextField(
                            value = token,
                            onValueChange = { token = it },
                            label = { Text("Código (Token)") },
                            leadingIcon = { Icon(Icons.Default.Key, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                        )

                        Spacer(Modifier.height(12.dp))

                        // Campo Nueva Contraseña
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("Nueva contraseña") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            trailingIcon = {
                                IconButton(onClick = { passVisible = !passVisible }) {
                                    Icon(if (passVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                                }
                            },
                            visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (token.isBlank() || newPassword.length < 6) {
                                    message = "Error: Verifica el token y que la clave tenga min 6 caracteres"
                                    return@Button
                                }
                                loading = true
                                message = null
                                scope.launch {
                                    // Llamada al Repositorio (confirmReset)
                                    val success = ServiceLocator.auth(ctx).confirmPasswordReset(email, token, newPassword)
                                    loading = false
                                    if (success) {
                                        // Éxito: Volver al Login
                                        nav.popBackStack() // Salir de ForgotPassword
                                        nav.navigate(NavRoutes.LOGIN) { popUpTo(0) } // Ir a Login limpio
                                    } else {
                                        message = "Error: Código inválido o expirado."
                                    }
                                }
                            },
                            enabled = !loading && token.isNotBlank() && newPassword.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = cs.onPrimary)
                            else Text("Cambiar Contraseña")
                        }

                        TextButton(onClick = { step = 1 }) {
                            Text("Volver a ingresar email")
                        }
                    }

                    // Mensajes de error/estado
                    if (message != null) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = message!!,
                            color = if (message!!.startsWith("Error")) cs.error else cs.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}