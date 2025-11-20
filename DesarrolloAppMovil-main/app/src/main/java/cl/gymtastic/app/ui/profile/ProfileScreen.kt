package cl.gymtastic.app.ui.profile

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History // Importar ícono
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import cl.gymtastic.app.R
import cl.gymtastic.app.data.model.User
import cl.gymtastic.app.data.remote.ProfileUpdateRequest
import cl.gymtastic.app.ui.navigation.NavRoutes
import cl.gymtastic.app.util.ImageUriUtils
import cl.gymtastic.app.util.ServiceLocator
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    nav: NavController,
    windowSizeClass: WindowSizeClass
) {
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val authRepo = remember { ServiceLocator.auth(ctx) }
    val api = remember { ServiceLocator.api() }
    val session = remember { authRepo.prefs() }

    val authEmail by session.userEmailFlow.collectAsStateWithLifecycle(initialValue = "")
    var refreshTrigger by remember { mutableStateOf(0) }

    val user by produceState<User?>(initialValue = null, key1 = authEmail, key2 = refreshTrigger) {
        if (authEmail.isNotBlank()) {
            val dto = authRepo.getUserProfile(authEmail)
            value = dto?.let {
                User(it.email, it.nombre, it.rol, it.planEndMillis, it.sedeId, it.sedeName, it.sedeLat, it.sedeLng, it.avatarUri, it.fono, it.bio)
            }
        }
    }

    val displayEmail = user?.email ?: authEmail ?: "Cargando..."
    var name by rememberSaveable(user) { mutableStateOf(user?.nombre ?: "") }
    var phone by rememberSaveable(user) { mutableStateOf(user?.fono ?: "") }
    var bio by rememberSaveable(user) { mutableStateOf(user?.bio ?: "") }

    var avatarUriInternal by remember { mutableStateOf<Uri?>(null) }
    var avatarUriString by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user?.avatarUri) {
        val remoteUri = user?.avatarUri
        if (remoteUri != avatarUriString) {
            avatarUriString = remoteUri
            avatarUriInternal = remoteUri?.let { try { Uri.parse(it) } catch (e: Exception) { null } }
        }
    }

    var phoneError by remember { mutableStateOf<String?>(null) }
    val maxBioChars = 240
    val bioCount by remember(bio) { derivedStateOf { bio.length } }
    val isPhoneValid by remember(phone) {
        derivedStateOf {
            if (phone.isBlank()) return@derivedStateOf true
            val digits = phone.filter { it.isDigit() }
            digits.length in 8..12
        }
    }
    LaunchedEffect(phone) { phoneError = if (phone.isNotBlank() && !isPhoneValid) "Teléfono inválido" else null }

    var saving by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }

    val bg = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), MaterialTheme.colorScheme.surface))

    fun saveProfile(newName: String, newPhone: String, newBio: String, newAvatarUri: String?, oldAvatarUri: String?) {
        if (authEmail.isBlank()) return
        scope.launch {
            saving = true
            try {
                val request = ProfileUpdateRequest(
                    nombre = newName.trim(),
                    fono = newPhone.trim().ifBlank { null },
                    bio = newBio.trim().ifBlank { null },
                    avatarUri = newAvatarUri
                )
                val response = api.updateProfile(authEmail, request)

                if (response.isSuccessful) {
                    if (oldAvatarUri != null && oldAvatarUri != newAvatarUri) {
                        withContext(Dispatchers.IO) { ImageUriUtils.deleteFileFromInternalStorage(oldAvatarUri) }
                    }
                    snackbar.showSnackbar("Perfil actualizado correctamente ✅")
                    refreshTrigger++
                } else {
                    snackbar.showSnackbar("Error al guardar: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ProfileScreen", "Error guardando perfil", e)
                snackbar.showSnackbar("Error de conexión")
            } finally {
                saving = false
            }
        }
    }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && pendingCameraUri != null) {
            val newUri = pendingCameraUri.toString()
            saveProfile(name, phone, bio, newUri, avatarUriString)
        }
        pendingCameraUri = null
    }
    val requestCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            try {
                val u = ImageUriUtils.createTempImageUri(ctx).also { pendingCameraUri = it }
                takePictureLauncher.launch(u)
            } catch (e: Exception) { scope.launch { snackbar.showSnackbar("Error al iniciar cámara") } }
        } else { scope.launch { snackbar.showSnackbar("Permiso denegado") } }
    }
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val internalUri = withContext(Dispatchers.IO) { ImageUriUtils.copyUriToInternalStorage(ctx, uri, "avatar_${authEmail.replace("@", "_")}") }
                if (internalUri != null) saveProfile(name, phone, bio, internalUri, avatarUriString) else snackbar.showSnackbar("Error al procesar imagen")
            }
        }
    }

    val widthSizeClass = windowSizeClass.widthSizeClass
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact
    val cardWidthModifier = if (isCompact) Modifier.fillMaxWidth(0.95f) else Modifier.width(550.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onBackground) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.onBackground, navigationIconContentColor = MaterialTheme.colorScheme.onBackground)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(bg).padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
            AnimatedVisibility(visible = show, enter = fadeIn() + slideInVertically { it / 3 }, exit = fadeOut() + slideOutVertically { it / 3 }) {
                Card(modifier = cardWidthModifier.shadow(8.dp, RoundedCornerShape(24.dp)), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                        // Avatar
                        Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(110.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                if (avatarUriInternal != null) {
                                    SubcomposeAsyncImage(model = ImageRequest.Builder(ctx).data(avatarUriInternal).crossfade(true).build(), contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape), loading = { CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp)) }, error = { Image(painter = painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(72.dp)) }, success = { SubcomposeAsyncImageContent() })
                                } else { Image(painter = painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(72.dp)) }
                            }
                            if (avatarUriInternal != null) {
                                IconButton(onClick = { showRemoveDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).offset(x = 8.dp, y = 8.dp).size(32.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape).shadow(1.dp, CircleShape)) { Icon(Icons.Default.Clear, "Quitar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        // Botones Foto
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ElevatedButton(onClick = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Galería") }
                            ElevatedButton(onClick = { requestCameraPermission.launch(Manifest.permission.CAMERA) }) { Text("Cámara") }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(displayEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))

                        // === NUEVO: Botón de Historial de Compras ===
                        OutlinedButton(
                            onClick = { nav.navigate(NavRoutes.ORDER_HISTORY) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.History, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Ver mis compras")
                        }

                        Spacer(Modifier.height(20.dp))

                        // Campos Editables
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true, keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") }, singleLine = true, isError = phoneError != null, supportingText = { phoneError?.let { Text(it) } }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = bio, onValueChange = { bio = if (it.length <= maxBioChars) it else it.take(maxBioChars) }, label = { Text("Bio") }, singleLine = false, minLines = 3, supportingText = { Text("${bioCount}/${maxBioChars}") }, keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done), modifier = Modifier.fillMaxWidth())

                        Spacer(Modifier.height(20.dp))
                        Button(onClick = { saveProfile(name, phone, bio, avatarUriString, null) }, enabled = !saving && phoneError == null && authEmail.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                            if (saving) { CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp)); Text("Guardando...") } else { Text("Guardar Datos") }
                        }
                        Button(onClick = { scope.launch { authRepo.logout(); nav.navigate(NavRoutes.LOGIN) { popUpTo(0) } } }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Cerrar sesión") }
                    }
                }
            }
        }
    }
    if (showRemoveDialog) {
        AlertDialog(onDismissRequest = { showRemoveDialog = false }, title = { Text("Quitar foto") }, text = { Text("¿Volver al avatar por defecto?") }, confirmButton = { TextButton(onClick = { showRemoveDialog = false; saveProfile(name, phone, bio, null, avatarUriString) }) { Text("Quitar") } }, dismissButton = { TextButton(onClick = { showRemoveDialog = false }) { Text("Cancelar") } })
    }
}