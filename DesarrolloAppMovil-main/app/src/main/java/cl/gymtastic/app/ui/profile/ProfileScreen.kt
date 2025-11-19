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
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import cl.gymtastic.app.R
import cl.gymtastic.app.data.local.db.GymTasticDatabase // <-- Importar DB
import cl.gymtastic.app.ui.navigation.NavRoutes
import cl.gymtastic.app.util.ImageUriUtils
import kotlinx.coroutines.Dispatchers // <-- Importar Dispatchers
import kotlinx.coroutines.flow.flowOf // <-- Importar flowOf
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.withContext // <-- Importar withContext
import cl.gymtastic.app.util.ServiceLocator

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

    // --- Sesión y UserEntity (AHORA fuente única de datos del perfil) ---
    val session = remember { ServiceLocator.auth(ctx).prefs() }
    val authEmail by session.userEmailFlow.collectAsStateWithLifecycle(initialValue = "")
    val usersDao = remember { GymTasticDatabase.get(ctx).users() } // <-- Obtener DAO

    // Observar UserEntity desde Room
    val userEntity by remember(authEmail) {
        if (authEmail.isNotBlank()) usersDao.observeByEmail(authEmail) else flowOf(null)
    }.collectAsStateWithLifecycle(initialValue = null)

    // Email mostrado
    val displayEmail = userEntity?.email ?: authEmail ?: "Sin email registrado"

    // --- Estado Editable LOCAL (sincronizado desde UserEntity) ---
    var name by rememberSaveable(userEntity?.nombre) { mutableStateOf(userEntity?.nombre ?: "") }
    var phone by rememberSaveable(userEntity?.fono) { mutableStateOf(userEntity?.fono ?: "") }
    var bio by rememberSaveable(userEntity?.bio) { mutableStateOf(userEntity?.bio ?: "") }
    var avatarUriInternal by remember { mutableStateOf<Uri?>(null) } // Uri para Coil/UI
    var avatarUriString by remember { mutableStateOf<String?>(null) } // String actual en la BD

    // Sincronizar estado local del avatar cuando UserEntity cambia
    LaunchedEffect(userEntity?.avatarUri) { // Observa específicamente el cambio de avatarUri
        val dbUriString = userEntity?.avatarUri
        if (dbUriString != avatarUriString) { // Solo actualiza si es diferente al estado local
            avatarUriString = dbUriString
            avatarUriInternal = dbUriString?.let { uriStr ->
                try { Uri.parse(uriStr) } catch (e: Exception) { null }
            }
        }
    }
    // Sincronizar otros campos si cambian desde fuera (menos probable, pero por seguridad)
    LaunchedEffect(userEntity?.nombre) { if (userEntity?.nombre != name) name = userEntity?.nombre ?: "" }
    LaunchedEffect(userEntity?.fono) { if (userEntity?.fono != phone) phone = userEntity?.fono ?: "" }
    LaunchedEffect(userEntity?.bio) { if (userEntity?.bio != bio) bio = userEntity?.bio ?: "" }


    // Validaciones
    var phoneError by remember { mutableStateOf<String?>(null) }
    val maxBioChars = 240
    val bioCount by remember(bio) { derivedStateOf { bio.length } }
    val isPhoneValid by remember(phone) {
        derivedStateOf {
            if (phone.isBlank()) return@derivedStateOf true // Teléfono opcional
            val digits = phone.filter { it.isDigit() }
            digits.length in 8..12
        }
    }
    LaunchedEffect(phone) {
        if (phone.isNotBlank() && !isPhoneValid) { phoneError = "Teléfono inválido (8-12 dígitos)" }
        else { phoneError = null }
    }

    // Estado UI
    var saving by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var show by remember { mutableStateOf(false) } // Animación entrada
    LaunchedEffect(Unit) { show = true }

    val bg = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), MaterialTheme.colorScheme.surface))

    // ====== Pickers ======
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    // --- FUNCIÓN PARA GUARDAR NUEVA URI DE AVATAR (en Room) ---
    fun saveNewAvatarUri(newUriString: String?, oldUriString: String?) {
        if (authEmail.isBlank()) { /* ... Error ... */ return }
        scope.launch {
            val updatedRows = usersDao.updateAvatarUri(authEmail, newUriString)
            if (updatedRows > 0) {
                if (oldUriString != null && oldUriString != newUriString) {
                    Log.d("ProfileScreen", "Borrando imagen antigua: $oldUriString")
                    withContext(Dispatchers.IO) { ImageUriUtils.deleteFileFromInternalStorage(oldUriString) }
                }
                // El LaunchedEffect(userEntity?.avatarUri) actualizará el estado local
                snackbar.showSnackbar("Foto de perfil actualizada ✅")
            } else { /* ... Error handling ... */
                snackbar.showSnackbar("Error al guardar la foto de perfil")
                if (newUriString != null) {
                    Log.w("ProfileScreen", "Error al guardar en BD, borrando archivo copiado: $newUriString")
                    withContext(Dispatchers.IO) { ImageUriUtils.deleteFileFromInternalStorage(newUriString) }
                }
            }
        }
    }

    // Cámara
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && pendingCameraUri != null) {
            val newUriString = pendingCameraUri.toString()
            Log.d("ProfileScreen", "Foto tomada, guardando URI: $newUriString")
            saveNewAvatarUri(newUriString = newUriString, oldUriString = avatarUriString)
        } else { Log.d("ProfileScreen", "Foto cancelada o fallida") }
        pendingCameraUri = null
    }
    val requestCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            try {
                val u = ImageUriUtils.createTempImageUri(ctx).also { pendingCameraUri = it; Log.d("ProfileScreen", "URI temporal: $it") }
                takePictureLauncher.launch(u)
            } catch (e: Exception) { Log.e("ProfileScreen", "Error al crear URI", e); scope.launch { snackbar.showSnackbar("Error cámara") } }
        } else { scope.launch { snackbar.showSnackbar("Permiso cámara denegado") } }
    }

    // Galería (Photo Picker)
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d("ProfileScreen", "Imagen galería: $uri")
            scope.launch(Dispatchers.IO) {
                Log.d("ProfileScreen", "Copiando a interno...")
                val internalUriString = ImageUriUtils.copyUriToInternalStorage(ctx, uri, "avatar_${authEmail.replace("@", "_")}")
                withContext(Dispatchers.Main) {
                    if (internalUriString != null) {
                        Log.d("ProfileScreen", "Copia OK ($internalUriString), guardando...")
                        saveNewAvatarUri(newUriString = internalUriString, oldUriString = avatarUriString)
                    } else { Log.e("ProfileScreen", "Error al copiar"); snackbar.showSnackbar("Error al copiar imagen") }
                }
            }
        } else { Log.d("ProfileScreen", "Galería cancelada") }
    }

    // Lógica de Ancho
    val widthSizeClass = windowSizeClass.widthSizeClass
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact
    val cardWidthModifier = if (isCompact) Modifier.fillMaxWidth(0.95f) else Modifier.width(550.dp)

    Scaffold(
        topBar = { /* ... sin cambios ... */
            TopAppBar(
                title = { Text("Perfil", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().background(bg).padding(padding).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(visible = show, enter = fadeIn() + slideInVertically { it / 3 }, exit = fadeOut() + slideOutVertically { it / 3 }) {
                Card(
                    modifier = cardWidthModifier.shadow(8.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // ==== Avatar (Usa avatarUriInternal del estado local) ====
                        Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier.size(110.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarUriInternal != null) {
                                    SubcomposeAsyncImage( model = ImageRequest.Builder(ctx).data(avatarUriInternal).crossfade(true).build(), /*...*/
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        loading = { CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp)) },
                                        error = { Image(painter = painterResource(R.drawable.ic_launcher_foreground), contentDescription = "Avatar por defecto", modifier = Modifier.size(72.dp)) },
                                        success = { SubcomposeAsyncImageContent() }
                                    )
                                } else {
                                    Image(painter = painterResource(R.drawable.ic_launcher_foreground), contentDescription = "Avatar por defecto", modifier = Modifier.size(72.dp))
                                }
                            }
                            // Botón quitar
                            if (avatarUriInternal != null) {
                                IconButton( onClick = { showRemoveDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).offset(x = 8.dp, y = 8.dp).size(32.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape).shadow(1.dp, CircleShape)
                                ) { Icon(Icons.Default.Clear, "Quitar foto", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        // Botones Galería / Cámara
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ElevatedButton(onClick = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { /*...*/ Text("Galería") }
                            ElevatedButton(onClick = { requestCameraPermission.launch(Manifest.permission.CAMERA) }) { /*...*/ Text("Cámara") }
                        }
                        Spacer(Modifier.height(18.dp))

                        // ==== Email ====
                        Text(displayEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))

                        // ==== Datos editables (AHORA leen/escriben estado local 'name', 'phone', 'bio') ====
                        OutlinedTextField(
                            value = name, // Lee estado local
                            onValueChange = { name = it }, // Actualiza estado local
                            label = { Text("Nombre") }, singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = phone, // Lee estado local
                            onValueChange = { phone = it }, // Actualiza estado local
                            label = { Text("Teléfono") }, singleLine = true, isError = phoneError != null,
                            supportingText = { phoneError?.let { Text(it) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = bio, // Lee estado local
                            onValueChange = { bio = if (it.length <= maxBioChars) it else it.take(maxBioChars) }, // Actualiza estado local
                            label = { Text("Bio") }, singleLine = false, minLines = 3,
                            supportingText = { Text("${bioCount}/${maxBioChars} caracteres") },
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(20.dp))

                        // ==== Botón Guardar (AHORA guarda en Room usando DAO) ====
                        Button(
                            onClick = {
                                // --- Guardar Nombre, Fono, Bio en UserEntity (Room) ---
                                if (authEmail.isBlank()) { /* Error */ return@Button }
                                scope.launch {
                                    try {
                                        saving = true
                                        val updatedRows = usersDao.updateProfileDetails(
                                            email = authEmail,
                                            newName = name.trim(),
                                            newFono = phone.trim().ifBlank { null },
                                            newBio = bio.trim().ifBlank { null }
                                        )
                                        if (updatedRows > 0) {
                                            snackbar.showSnackbar("Datos del perfil guardados ✅")
                                        } else { snackbar.showSnackbar("No se pudieron guardar los datos") }
                                    } catch (e: Exception) {
                                        Log.e("ProfileScreen", "Error al guardar perfil en Room", e)
                                        snackbar.showSnackbar("Error al guardar: ${e.message ?: "desconocido"}")
                                    } finally { saving = false }
                                }
                            },
                            enabled = !saving && (phoneError == null) && authEmail.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (saving) { /* Indicador */
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp)); Text("Guardando…")
                            } else { Text("Guardar Datos") }
                        }

                        // Botón Cerrar Sesión
                        Button(
                            onClick = { scope.launch { ServiceLocator.auth(ctx).logout(); nav.navigate(NavRoutes.LOGIN) { popUpTo(0) } } },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) { Text("Cerrar sesión") }
                    }
                }
            }
        }
    }

    // ==== Diálogo: quitar foto (Usa saveNewAvatarUri) ====
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Quitar foto de perfil") },
            text = { Text("¿Seguro que quieres quitar tu foto y volver al avatar por defecto?") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog = false
                    Log.d("ProfileScreen", "Quitando foto, URI antigua: $avatarUriString")
                    saveNewAvatarUri(newUriString = null, oldUriString = avatarUriString) // Llama a guardar null
                }) { Text("Quitar") }
            },
            dismissButton = { TextButton(onClick = { showRemoveDialog = false }) { Text("Cancelar") } }
        )
    }
}


