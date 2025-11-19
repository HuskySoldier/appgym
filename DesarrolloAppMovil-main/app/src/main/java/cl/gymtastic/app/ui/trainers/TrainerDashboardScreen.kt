package cl.gymtastic.app.ui.trainers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.gymtastic.app.data.model.Trainer // <-- Importar Modelo
import cl.gymtastic.app.data.remote.BookingRequest
import cl.gymtastic.app.ui.navigation.NavRoutes
import cl.gymtastic.app.util.ServiceLocator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainerDashboardScreen(nav: NavController) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    // Sesión
    val authPrefs = remember { ServiceLocator.auth(ctx).prefs() }
    val authEmail by authPrefs.userEmailFlow.collectAsStateWithLifecycle("")

    // Repositorios
    val trainersRepo = remember { ServiceLocator.trainers(ctx) }
    val bookingsRepo = remember { ServiceLocator.bookings(ctx) }

    // 1. Buscar si el email logueado corresponde a un Trainer (Desde API)
    // Reemplazamos observeAll() (Room) por getTrainers() (API)
    val allTrainers by produceState<List<Trainer>>(initialValue = emptyList()) {
        value = trainersRepo.getTrainers()
    }

    // Calculamos el trainer actual buscando por email en la lista cargada
    val currentTrainer = remember(allTrainers, authEmail) {
        allTrainers.find { it.email.equals(authEmail, ignoreCase = true) }
    }

    // 2. Cargar agenda si encontramos al trainer
    var schedule by remember { mutableStateOf<List<BookingRequest>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(currentTrainer) {
        if (currentTrainer != null) {
            loading = true
            // getTrainerSchedule ya fue actualizado para consultar la API
            schedule = bookingsRepo.getTrainerSchedule(currentTrainer.id)
            loading = false
        }
    }

    val bg = Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.22f), cs.surface))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Panel de Entrenador")
                        if (currentTrainer != null) {
                            Text(currentTrainer.nombre, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            ServiceLocator.auth(ctx).logout()
                            nav.navigate(NavRoutes.LOGIN) { popUpTo(0) }
                        }
                    }) {
                        Icon(Icons.Filled.Logout, "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(padding)
                .padding(16.dp)
        ) {
            if (authEmail.isBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (currentTrainer == null) {
                // Si ya cargaron los trainers y no se encontró el email
                if (allTrainers.isNotEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No se encontró perfil de entrenador para $authEmail.\n" +
                                    "Asegúrate de estar registrado como trainer en el sistema.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    // Cargando lista de trainers...
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (schedule.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tienes sesiones agendadas próximamente.")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Text("Próximas Sesiones", style = MaterialTheme.typography.titleMedium, color = cs.primary)
                        }
                        items(schedule) { booking ->
                            BookingCard(booking)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingCard(booking: BookingRequest) {
    val cs = MaterialTheme.colorScheme
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateStr = try {
        dateFormat.format(Date(booking.fechaHora))
    } catch (e: Exception) { "Fecha inválida" }

    Card(
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(cs.primaryContainer, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CalendarToday, null, tint = cs.onPrimaryContainer)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(dateStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Person, null, modifier = Modifier.size(16.dp), tint = cs.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("Alumno: ${booking.userEmail}", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
            }
        }
    }
}