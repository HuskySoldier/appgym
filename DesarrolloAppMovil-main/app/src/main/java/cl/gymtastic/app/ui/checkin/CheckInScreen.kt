package cl.gymtastic.app.ui.checkin

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.gymtastic.app.data.local.entity.AttendanceEntity
import cl.gymtastic.app.ui.navigation.NavRoutes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import cl.gymtastic.app.data.local.entity.UserEntity
// --- Importaciones para ZXing ---
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
// ---
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
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

    // --- Sesión y Usuario ---
    val authPrefs = remember { ServiceLocator.auth(ctx).prefs() }
    val authEmail by authPrefs.userEmailFlow.collectAsStateWithLifecycle(initialValue = "")
    // ---  YA NO NECESITAMOS userIdFlow NUMÉRICO ---
    // val userId by authPrefs.userIdFlow.collectAsStateWithLifecycle(initialValue = -1) // Eliminado o comentado

    // --- Observar UserEntity desde DB (sin cambios) ---
    val usersDao = remember { GymTasticDatabase.get(ctx).users() }
    val userEntity by remember(authEmail) {
        if (authEmail.isNotBlank()) {
            usersDao.observeByEmail(authEmail)
        } else {
            flowOf(null)
        }
    }.collectAsStateWithLifecycle(initialValue = null)

    // --- Flujo de Asistencia (depende de authEmail) ---
    val attendanceFlow = remember(authEmail) { // <-- Depende de authEmail ahora
        if (authEmail.isNotBlank()) { // <-- Solo observa si hay email
            // --- CAMBIO: Usar authEmail (String) ---
            ServiceLocator.attendance(ctx).observe(authEmail)
        } else {
            flowOf(emptyList()) // Flow vacío si no hay email
        }
    }
    val list: List<AttendanceEntity> by attendanceFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val hasOpen = remember(list) { list.any { it.checkOutTimestamp == null } }

    // --- Generar Código de Barras (sin cambios) ---
    val barcodeBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = authEmail) { /* ... */
        value = if (authEmail.isNotBlank()) {
            withContext(Dispatchers.IO) { // Ejecutar en hilo IO
                generateBarcodeBitmap(
                    text = authEmail,
                    format = BarcodeFormat.QR_CODE, // O BarcodeFormat.CODE_128
                    width = 512, // Ancho en píxeles
                    height = 512 // Alto en píxeles (para QR suele ser cuadrado)
                )
            }
        } else {
            null // No generar si no hay email
        }
    }

    val bg = Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.22f), cs.surface))
    val widthSizeClass = windowSizeClass.widthSizeClass
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact

    Scaffold(
        topBar = { /* ... sin cambios ... */
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
                userEntity = userEntity,
                hasOpenSession = hasOpen,
                onCheckIn = {
                    // ---  CAMBIO: Usar authEmail ---
                    Log.d("CheckInScreen", "Botón IN presionado. authEmail = $authEmail")
                    if (authEmail.isBlank()) { // <-- Validar email en lugar de userId
                        Log.w("CheckInScreen", "Check-In cancelado: authEmail vacío")
                        return@PillButtonsRow
                    }
                    scope.launch {
                        try {
                            Log.d("CheckInScreen", "Llamando a ServiceLocator.attendance.checkIn con authEmail = $authEmail")
                            ServiceLocator.attendance(ctx).checkIn(authEmail) // <-- Usar email
                            Log.d("CheckInScreen", "checkIn completado. Mostrando Snackbar.")
                            snackbar.showMessage("✅ Check-In registrado")
                        } catch (e: Exception) {
                            Log.e("CheckInScreen", "Error durante checkIn", e)
                            snackbar.showMessage("❌ Error al registrar Check-In: ${e.localizedMessage}")
                        }
                    }
                },
                onCheckOut = {
                    // ---  CAMBIO: Usar authEmail ---
                    Log.d("CheckInScreen", "Botón OUT presionado. authEmail = $authEmail")
                    if (authEmail.isBlank()) { // <-- Validar email
                        Log.w("CheckInScreen", "Check-Out cancelado: authEmail vacío")
                        return@PillButtonsRow
                    }
                    scope.launch {
                        try {
                            Log.d("CheckInScreen", "Llamando a ServiceLocator.attendance.checkOut con authEmail = $authEmail")
                            ServiceLocator.attendance(ctx).checkOut(authEmail) // <-- Usar email
                            Log.d("CheckInScreen", "checkOut completado. Mostrando Snackbar.")
                            snackbar.showMessage("✅ Check-Out registrado")
                        } catch (e: Exception) {
                            Log.e("CheckInScreen", "Error durante checkOut", e)
                            snackbar.showMessage("❌ Error al registrar Check-Out: ${e.localizedMessage}")
                        }
                    }
                }
            )
        },
        floatingActionButtonPosition = if (isCompact) FabPosition.Center else FabPosition.End
    ) { padding ->
        val contentModifier = if (isCompact) {
            Modifier.fillMaxSize().padding(16.dp)
        } else {
            Modifier.width(600.dp).padding(top = 16.dp)
        }
        val boxAlignment = if (isCompact) Alignment.TopStart else Alignment.TopCenter

        Box(
            modifier = Modifier.fillMaxSize().background(bg).padding(padding)
                .then(if (!isCompact) Modifier.padding(horizontal = 16.dp) else Modifier),
            contentAlignment = boxAlignment
        ) {
            Column(modifier = contentModifier) {
                // Card Código de Barras (sin cambios)
                AnimatedVisibility(visible = authEmail.isNotBlank()) { /* ... */
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp) // Añadido padding abajo
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally // Centrar contenido
                        ) {
                            Text(
                                "Tu Código de Acceso",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Contenedor para el código de barras o indicador de carga/error
                            Box(
                                modifier = Modifier
                                    .size(200.dp) // Tamaño fijo para el contenedor del código
                                    .background(cs.primary) // Fondo blanco para el código
                                    .padding(8.dp), // Padding interno para que no toque los bordes
                                contentAlignment = Alignment.Center
                            ) {
                                if (barcodeBitmap != null) {
                                    // Muestra el código de barras si se generó correctamente
                                    Image(
                                        bitmap = barcodeBitmap!!,
                                        contentDescription = "Código de barras para $authEmail",
                                        modifier = Modifier.fillMaxSize(), // Llenar el Box
                                        contentScale = ContentScale.Fit // Escalar para caber
                                    )
                                } else if (authEmail.isNotBlank()) {
                                    // Muestra indicador mientras se genera (si hay email)
                                    // Podría también mostrar un error si produceState devuelve null después de intentar
                                    CircularProgressIndicator()
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Muestra el email debajo del código
                            Text(
                                text = authEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                // Historial (sin cambios)
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { /* ... */
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.History, contentDescription = null, tint = cs.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Historial de asistencia", style = MaterialTheme.typography.titleMedium)
                            val total = list.size
                            Text(
                                "$total registro${if (total == 1) "" else "s"} • ${if (hasOpen) "Sesión en curso" else "Sin sesión abierta"}",
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (list.isEmpty()) { /* ... */
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Aún no tienes registros", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text("Usa el botón IN para registrar tu entrada.", color = cs.onSurfaceVariant)
                        }
                    }
                } else { /* ... */
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 96.dp) // espacio para los botones FAB
                    ) {
                        // Usar items con key si AttendanceEntity tiene un id estable
                        items(list, key = { it.id }) { e ->
                            AttendanceCard(e)
                        }
                    }
                }
            }
        }
    }
}

/* ---------- Botones “pill” (Sin cambios en su lógica interna) ---------- */
@Composable
private fun PillButtonsRow(userEntity: UserEntity?, hasOpenSession: Boolean, onCheckIn: () -> Unit, onCheckOut: () -> Unit) {
    /* ... Código sin cambios ... */
    val cs = MaterialTheme.colorScheme
    val hasActivePlan = userEntity?.hasActivePlan ?: false

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        // IN
        Button(
            onClick = onCheckIn,
            enabled = hasActivePlan && !hasOpenSession,
            shape = MaterialTheme.shapes.extraLarge,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = cs.primary,
                contentColor = cs.onPrimary,
                disabledContainerColor = cs.surfaceVariant,
                disabledContentColor = cs.onSurfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Filled.Login, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("IN")
        }

        // OUT
        Button(
            onClick = onCheckOut,
            enabled = hasActivePlan && hasOpenSession,
            shape = MaterialTheme.shapes.extraLarge,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = cs.primary,
                contentColor = cs.onPrimary,
                disabledContainerColor = cs.surfaceVariant,
                disabledContentColor = cs.onSurfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("OUT")
        }
    }
}

/* ---------- Tarjeta de asistencia (Sin cambios) ---------- */
@Composable
private fun AttendanceCard(e: AttendanceEntity) { /* ... Código sin cambios ... */
    val cs = MaterialTheme.colorScheme
    val inTxt = fmt(e.timestamp)
    val outTxt = fmt(e.checkOutTimestamp)
    val inCourse = e.checkOutTimestamp == null
    val dur = durationText(e.timestamp, e.checkOutTimestamp)

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Entrada", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                StatusPill(inCourse)
            }
            Text(inTxt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Spacer(Modifier.height(8.dp))

            Text("Salida", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            Text(outTxt, style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(8.dp))

            Text("Duración", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            Text(dur, style = MaterialTheme.typography.bodyLarge, color = cs.primary)
        }
    }
}

@Composable
private fun StatusPill(inCourse: Boolean) { /* ... Código sin cambios ... */
    val cs = MaterialTheme.colorScheme
    val label = if (inCourse) "En curso" else "Finalizado"
    val bg = if (inCourse) cs.primary.copy(alpha = 0.12f) else cs.surfaceVariant
    val fg = if (inCourse) cs.primary else cs.onSurfaceVariant

    Surface(
        color = bg,
        contentColor = fg,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/* ---------- Helpers (Sin cambios) ---------- */
private fun fmt(ts: Long?): String = /* ... Código sin cambios ... */
    ts?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it)) } ?: "—"

private fun durationText(start: Long?, end: Long?): String { /* ... Código sin cambios ... */
    val s = start ?: return "—"
    val e = end ?: System.currentTimeMillis()
    val diff = (e - s).coerceAtLeast(0L)
    val h = TimeUnit.MILLISECONDS.toHours(diff)
    val m = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
private suspend fun SnackbarHostState.showMessage(msg: String) { /* ... Código sin cambios ... */
    showSnackbar(message = msg, withDismissAction = true)
}

// --- Generador de Código de Barras (Sin cambios) ---
private fun generateBarcodeBitmap(text: String, format: BarcodeFormat, width: Int, height: Int): ImageBitmap? {
    /* ... Código sin cambios ... */
    return try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            text,
            format,
            width,
            height
        )
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        // Loggear el error si es necesario
        // Log.e("BarcodeGenerator", "Error generating barcode", e)
        null // Devolver null en caso de error
    }
}
