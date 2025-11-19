package cl.gymtastic.app.ui.checkin

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.gymtastic.app.data.model.Attendance // <-- Usar Modelo
import cl.gymtastic.app.data.model.User       // <-- Usar Modelo
import cl.gymtastic.app.ui.navigation.NavRoutes
import cl.gymtastic.app.util.ServiceLocator
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    nav: NavController,
    windowSizeClass: WindowSizeClass
) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // Repositorios
    val authRepo = remember { ServiceLocator.auth(ctx) }
    val attendanceRepo = remember { ServiceLocator.attendance(ctx) }

    // Sesión
    val authPrefs = remember { authRepo.prefs() }
    val authEmail by authPrefs.userEmailFlow.collectAsState(initial = "")

    // --- 1. Cargar Usuario (Desde API) ---
    val user by produceState<User?>(initialValue = null, key1 = authEmail) {
        if (authEmail.isNotBlank()) {
            val dto = authRepo.getUserProfile(authEmail)
            value = dto?.let {
                User(
                    email = it.email,
                    nombre = it.nombre,
                    rol = it.rol,
                    planEndMillis = it.planEndMillis,
                    // Mapear resto de campos si es necesario para esta pantalla
                    avatarUri = it.avatarUri
                )
            }
        }
    }

    // --- 2. Cargar Historial (Desde API) ---
    // Usamos una variable de refresco para recargar tras check-in/out
    var refreshTrigger by remember { mutableStateOf(0) }

    val list by produceState<List<Attendance>>(initialValue = emptyList(), key1 = authEmail, key2 = refreshTrigger) {
        if (authEmail.isNotBlank()) {
            value = attendanceRepo.getHistory(authEmail)
        }
    }

    val hasOpen = remember(list) { list.any { it.checkOutTimestamp == null } }

    // --- 3. Generar QR ---
    val barcodeBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = authEmail) {
        value = if (authEmail.isNotBlank()) {
            withContext(Dispatchers.IO) {
                generateBarcodeBitmap(authEmail, BarcodeFormat.QR_CODE, 512, 512)
            }
        } else {
            null
        }
    }

    val bg = Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.22f), cs.surface))
    val widthSizeClass = windowSizeClass.widthSizeClass
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check-In", color = cs.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { nav.navigate(NavRoutes.HOME) { launchSingleTop = true } }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = cs.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.background,
                    titleContentColor = cs.onBackground,
                    navigationIconContentColor = cs.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            PillButtonsRow(
                user = user,
                hasOpenSession = hasOpen,
                onCheckIn = {
                    if (authEmail.isBlank()) return@PillButtonsRow
                    scope.launch {
                        try {
                            attendanceRepo.checkIn(authEmail)
                            snackbar.showMessage("✅ Check-In registrado")
                            refreshTrigger++ // Recargar lista
                        } catch (e: Exception) {
                            snackbar.showMessage("❌ Error: ${e.message}")
                        }
                    }
                },
                onCheckOut = {
                    if (authEmail.isBlank()) return@PillButtonsRow
                    scope.launch {
                        try {
                            attendanceRepo.checkOut(authEmail)
                            snackbar.showMessage("✅ Check-Out registrado")
                            refreshTrigger++ // Recargar lista
                        } catch (e: Exception) {
                            snackbar.showMessage("❌ Error: ${e.message}")
                        }
                    }
                }
            )
        },
        floatingActionButtonPosition = if (isCompact) FabPosition.Center else FabPosition.End
    ) { padding ->
        val contentModifier = if (isCompact) Modifier.fillMaxSize().padding(16.dp) else Modifier.width(600.dp).padding(top = 16.dp)
        val boxAlignment = if (isCompact) Alignment.TopStart else Alignment.TopCenter

        Box(modifier = Modifier.fillMaxSize().background(bg).padding(padding).then(if (!isCompact) Modifier.padding(horizontal = 16.dp) else Modifier), contentAlignment = boxAlignment) {
            Column(modifier = contentModifier) {
                AnimatedVisibility(visible = authEmail.isNotBlank()) {
                    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = cs.surface), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tu Código de Acceso", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                            Box(modifier = Modifier.size(200.dp).background(cs.primary).padding(8.dp), contentAlignment = Alignment.Center) {
                                if (barcodeBitmap != null) {
                                    Image(bitmap = barcodeBitmap!!, contentDescription = "QR", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                } else { CircularProgressIndicator() }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(authEmail, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                        }
                    }
                }

                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.History, null, tint = cs.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Historial de asistencia", style = MaterialTheme.typography.titleMedium)
                            Text("${list.size} registros • ${if (hasOpen) "Sesión en curso" else "Sin sesión"}", color = cs.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (list.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { Text("Aún no tienes registros") }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
                        items(list, key = { it.id }) { e -> AttendanceCard(e) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PillButtonsRow(user: User?, hasOpenSession: Boolean, onCheckIn: () -> Unit, onCheckOut: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val hasActivePlan = user?.hasActivePlan ?: false

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(end = 8.dp)) {
        Button(onClick = onCheckIn, enabled = hasActivePlan && !hasOpenSession, shape = MaterialTheme.shapes.extraLarge) {
            Icon(Icons.Filled.Login, null); Spacer(Modifier.width(8.dp)); Text("IN")
        }
        Button(onClick = onCheckOut, enabled = hasActivePlan && hasOpenSession, shape = MaterialTheme.shapes.extraLarge) {
            Icon(Icons.Filled.Logout, null); Spacer(Modifier.width(8.dp)); Text("OUT")
        }
    }
}

@Composable
private fun AttendanceCard(e: Attendance) {
    val cs = MaterialTheme.colorScheme
    val inTxt = fmt(e.timestamp)
    val outTxt = fmt(e.checkOutTimestamp)
    val dur = durationText(e.timestamp, e.checkOutTimestamp)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Entrada", style = MaterialTheme.typography.labelMedium)
                StatusPill(e.checkOutTimestamp == null)
            }
            Text(inTxt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Salida", style = MaterialTheme.typography.labelMedium)
            Text(outTxt, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(dur, style = MaterialTheme.typography.bodyLarge, color = cs.primary)
        }
    }
}

@Composable
private fun StatusPill(inCourse: Boolean) {
    val cs = MaterialTheme.colorScheme
    Surface(color = if (inCourse) cs.primary.copy(alpha = 0.12f) else cs.surfaceVariant, shape = MaterialTheme.shapes.large) {
        Text(if (inCourse) "En curso" else "Finalizado", modifier = Modifier.padding(10.dp, 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

private fun fmt(ts: Long?): String = ts?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it)) } ?: "—"
private fun durationText(start: Long?, end: Long?): String {
    val s = start ?: return "—"; val e = end ?: System.currentTimeMillis()
    val diff = (e - s).coerceAtLeast(0L)
    val h = TimeUnit.MILLISECONDS.toHours(diff); val m = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
private suspend fun SnackbarHostState.showMessage(msg: String) { showSnackbar(message = msg, withDismissAction = true) }
private fun generateBarcodeBitmap(text: String, format: BarcodeFormat, width: Int, height: Int): ImageBitmap? {
    return try {
        val bitMatrix = MultiFormatWriter().encode(text, format, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) for (y in 0 until height) bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
        bitmap.asImageBitmap()
    } catch (e: Exception) { null }
}