package cl.gymtastic.app.ui.booking

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import cl.gymtastic.app.util.ServiceLocator
import cl.gymtastic.app.work.BookingReminderWorker
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.io.IOException // <-- Importar IOException para manejar errores de red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    nav: NavController,
    windowSizeClass: WindowSizeClass
) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // --- Repositorios ---
    val trainersRepo = remember { ServiceLocator.trainers(ctx) }
    val bookingsRepo = remember { ServiceLocator.bookings(ctx) }

    // --- Trainers desde Room (cache) ---
    val trainersFlow = remember { trainersRepo.observeAll() }
    val trainers by trainersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // --- Opciones de Fecha y Hora ---
    val dateOptions = remember {
        (0..6).map { plus ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, plus) }
            Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        }
    }
    var selectedDateIndex by remember { mutableStateOf(0) }
    val timeOptions = listOf("07:00", "09:00", "12:00", "15:00", "18:00", "20:00")
    var selectedTimeIndex by remember { mutableStateOf(2) }

    // --- Estado de UI ---
    var trainerExpanded by remember { mutableStateOf(false) }
    var dateExpanded by remember { mutableStateOf(false) }
    var timeExpanded by remember { mutableStateOf(false) }
    var selectedTrainerIndex by remember { mutableStateOf(0) } // Inicializa en 0
    var loading by remember { mutableStateOf(false) }

    // --- Obtener userEmail ---
    val authPrefs = remember { ServiceLocator.auth(ctx).prefs() }
    val authEmail by authPrefs.userEmailFlow.collectAsStateWithLifecycle("")

    val bg = Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.20f), cs.surface))

    // Lógica de Ancho
    val widthSizeClass = windowSizeClass.widthSizeClass
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact
    val cardModifier = if (isCompact) Modifier.fillMaxWidth(0.95f) else Modifier.width(550.dp)

    // --- CORRECCIÓN: Calcular índice seguro ANTES de usarlo ---
    // Asegura que el índice esté dentro de los límites válidos, incluso si la lista está vacía (0..-1 se convierte en 0..0)
    val safeTrainerIndex = remember(selectedTrainerIndex, trainers.size) {
        selectedTrainerIndex.coerceIn(0, (trainers.size - 1).coerceAtLeast(0))
    }
    // --- FIN CORRECCIÓN ---


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agendar Trainer", color = cs.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { // Usa popBackStack
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
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().background(bg).padding(padding).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = cardModifier
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Reserva tu sesión personalizada", style = MaterialTheme.typography.headlineSmall)
                    Text("Elige entrenador, fecha y horario.", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)

                    // === Trainer Dropdown ===
                    ExposedDropdownMenuBox(expanded = trainerExpanded, onExpandedChange = { trainerExpanded = !trainerExpanded }) {
                        // --- CORRECCIÓN: Usar safeTrainerIndex y manejar lista vacía ---
                        val trainerLabel = trainers.getOrNull(safeTrainerIndex)?.nombre ?: if (trainers.isEmpty()) "Cargando..." else "Selecciona"
                        TextField(
                            value = trainerLabel, onValueChange = {}, readOnly = true, label = { Text("Entrenador") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = trainerExpanded) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            // Deshabilitar si no hay trainers cargados
                            enabled = trainers.isNotEmpty()
                        )
                        // Mostrar menú solo si hay trainers
                        if (trainers.isNotEmpty()) {
                            ExposedDropdownMenu(expanded = trainerExpanded, onDismissRequest = { trainerExpanded = false }) {
                                trainers.forEachIndexed { idx, t ->
                                    DropdownMenuItem(text = { Text(t.nombre) }, onClick = { selectedTrainerIndex = idx; trainerExpanded = false })
                                }
                            }
                        }
                    }

                    // === Fecha Dropdown ===
                    ExposedDropdownMenuBox(expanded = dateExpanded, onExpandedChange = { dateExpanded = !dateExpanded }) {
                        val (y, m, d) = dateOptions.getOrElse(selectedDateIndex) { dateOptions.first() }
                        val dateLabel = "%02d/%02d/%d".format(d, (m + 1), y)
                        TextField(
                            value = dateLabel, onValueChange = {}, readOnly = true, label = { Text("Fecha") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateExpanded) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = dateExpanded, onDismissRequest = { dateExpanded = false }) {
                            dateOptions.forEachIndexed { idx, triple ->
                                val (yy, mm, dd) = triple
                                DropdownMenuItem(text = { Text("%02d/%02d/%d".format(dd, (mm + 1), yy)) }, onClick = { selectedDateIndex = idx; dateExpanded = false })
                            }
                        }
                    }

                    // === Hora Dropdown ===
                    ExposedDropdownMenuBox(expanded = timeExpanded, onExpandedChange = { timeExpanded = !timeExpanded }) {
                        val safeTimeIndex = selectedTimeIndex.coerceIn(timeOptions.indices)
                        val timeLabel = timeOptions[safeTimeIndex]
                        TextField(
                            value = timeLabel, onValueChange = {}, readOnly = true, label = { Text("Horario") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeExpanded) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = timeExpanded, onDismissRequest = { timeExpanded = false }) {
                            timeOptions.forEachIndexed { idx, t ->
                                DropdownMenuItem(text = { Text(t) }, onClick = { selectedTimeIndex = idx; timeExpanded = false })
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // === Botones ===
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { nav.popBackStack() }, modifier = Modifier.weight(1f)) { Text("Cancelar") }

                        Button(
                            onClick = {
                                // --- CORRECCIÓN: Usar safeTrainerIndex aquí también ---
                                if (trainers.isEmpty() || authEmail.isBlank()) {
                                    scope.launch { snackbar.showSnackbar("Selecciona un entrenador y asegúrate de haber iniciado sesión.") }
                                    return@Button
                                }
                                loading = true

                                // Usar el índice seguro que ya calculamos
                                val selectedTrainer = trainers[safeTrainerIndex]
                                // --- FIN CORRECCIÓN ---
                                val selectedDate = dateOptions[selectedDateIndex]
                                val safeTimeIdx = selectedTimeIndex.coerceIn(timeOptions.indices)
                                val selectedTime = timeOptions[safeTimeIdx]
                                val millis = combineDateAndTime(selectedDate, selectedTime)
                                val trainerName = selectedTrainer.nombre

                                scope.launch {
                                    try {
                                        // 1. Guardar la reserva (Llamada a la API)
                                        bookingsRepo.create( // <-- USAR EL REPO MODIFICADO
                                            userEmail = authEmail,
                                            trainerId = selectedTrainer.id,
                                            fechaHora = millis
                                        )

                                        // 2. Programar el Worker
                                        val workManager = WorkManager.getInstance(ctx.applicationContext)
                                        val now = System.currentTimeMillis()
                                        val oneHourInMillis = TimeUnit.HOURS.toMillis(1)
                                        val delay = (millis - oneHourInMillis - now).coerceAtLeast(0L)
                                        val inputData = Data.Builder()
                                            .putString(BookingReminderWorker.KEY_TRAINER_NAME, trainerName)
                                            .putLong(BookingReminderWorker.KEY_BOOKING_TIME_MILLIS, millis)
                                            .build()
                                        val bookingReminderRequest = OneTimeWorkRequestBuilder<BookingReminderWorker>()
                                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                            .setInputData(inputData)
                                            .build()
                                        workManager.enqueue(bookingReminderRequest)
                                        Log.d("BookingScreen", "Booking reminder enqueued for $trainerName at $selectedTime (Delay: ${delay}ms)")

                                        // 3. Mostrar confirmación y volver
                                        val dateLabel = "%02d/%02d/%d".format(selectedDate.third, (selectedDate.second + 1), selectedDate.first)
                                        snackbar.showSnackbar("Reserva creada con $trainerName para $selectedTime del $dateLabel. Recordatorio programado.")
                                        nav.popBackStack()

                                    } catch (e: IOException) {
                                        Log.e("BookingScreen", "Error de red/servidor al crear reserva", e)
                                        snackbar.showSnackbar("Error al crear la reserva: ${e.message ?: "desconocido"}")
                                    } catch (e: Exception) {
                                        Log.e("BookingScreen", "Error inesperado al crear reserva", e)
                                        snackbar.showSnackbar("Error al crear la reserva: ${e.message ?: "desconocido"}")
                                    } finally {
                                        loading = false
                                    }
                                }
                            },
                            // Deshabilitar si no hay trainers o está cargando
                            enabled = trainers.isNotEmpty() && !loading && authEmail.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (loading) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp).padding(end = 8.dp))
                            }
                            Text(if (loading) "Guardando..." else "Confirmar")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Recibirás un recordatorio 1 hora antes de tu sesión.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** Convierte (año, mes, día) + "HH:mm" a millis */
private fun combineDateAndTime(date: Triple<Int, Int, Int>, time: String): Long {
    val (year, month, day) = date
    val (hh, mm) = try { time.split(":").map { it.toInt() } } catch (e: Exception) { listOf(0, 0) }
    val cal = Calendar.getInstance().apply {
        clear()
        set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, hh); set(Calendar.MINUTE, mm); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}