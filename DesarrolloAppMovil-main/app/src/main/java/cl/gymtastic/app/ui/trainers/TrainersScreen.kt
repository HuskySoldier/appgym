package cl.gymtastic.app.ui.trainers

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.gymtastic.app.data.model.Trainer // <-- CAMBIO: Usar el modelo
import cl.gymtastic.app.ui.navigation.Screen
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import cl.gymtastic.app.util.ServiceLocator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainersScreen(
    nav: NavController,
    windowSizeClass: WindowSizeClass
) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme

    // Repositorio
    val trainersRepo = remember { ServiceLocator.trainers(ctx) }

    // --- 1. Cargar Trainers desde API (Backend-Only) ---
    // Usamos produceState en lugar de collectAsStateWithLifecycle sobre un Flow de Room
    val list by produceState<List<Trainer>>(initialValue = emptyList()) {
        value = trainersRepo.getTrainers()
    }
    // ---------------------------------------------------

    val bg = Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.22f), cs.surface))

    // Reacciona al tamaño de pantalla
    val widthSizeClass = windowSizeClass.widthSizeClass
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact

    // Modificador de contenido para centrar y limitar el ancho en tablets
    val contentModifier = if (isCompact) {
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    } else {
        Modifier
            .width(600.dp) // Ancho máximo en tablets
            .padding(vertical = 12.dp)
    }
    // El Box contenedor centrará esta columna en tablets
    val boxAlignment = if (isCompact) Alignment.TopStart else Alignment.TopCenter

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trainers", color = cs.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = cs.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.background,
                    titleContentColor = cs.onBackground,
                    navigationIconContentColor = cs.onBackground
                )
            )
        }
    ) { padding ->
        // Usamos un Box para centrar el contenido en tablets
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(padding),
            contentAlignment = boxAlignment
        ) {
            Column(
                modifier = contentModifier
            ) {
                Text(
                    "Conoce a nuestro equipo",
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                if (list.isEmpty()) {
                    // Estado de carga o vacío
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(list.size) { i ->
                            val t = list[i]
                            TrainerCard(
                                trainer = t,
                                onCall = {
                                    safeStart(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${t.fono}")))
                                },
                                onEmail = {
                                    safeStart(ctx, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${t.email}")))
                                },
                                onBook = {
                                    nav.navigate(Screen.Booking.routeWith(t.id))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainerCard(
    trainer: Trainer, // <-- CAMBIO: Usar el modelo
    onCall: () -> Unit,
    onEmail: () -> Unit,
    onBook: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                SubcomposeAsyncImage(
                    model = trainer.img,
                    contentDescription = trainer.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(cs.primary.copy(alpha = 0.15f)),
                    loading = {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.padding(16.dp)
                        )
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initialsFor(trainer.nombre),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = cs.primary
                            )
                        }
                    },
                    success = { SubcomposeAsyncImageContent() }
                )

                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        trainer.nombre,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text(trainer.especialidad) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = cs.secondaryContainer,
                            labelColor = cs.onSecondaryContainer
                        )
                    )

                }
            }

            Spacer(Modifier.height(12.dp))

            // Acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCall,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = null)
                    Spacer(Modifier.width(2.dp))
                    Text("Llamar", maxLines = 1)
                }


                OutlinedButton(
                    onClick = onEmail,
                    modifier = Modifier.weight(1f),
                    contentPadding= PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Email, contentDescription = null)
                    Spacer(Modifier.width(2.dp))
                    Text("Email",maxLines = 1)
                }
                Button(
                    onClick = onBook,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null)
                    Spacer(Modifier.width(2.dp))
                    Text("Agendar", maxLines = 1)
                }
            }
        }
    }
}

private fun initialsFor(name: String): String {
    val parts = name.trim().split(Regex("\\s+"))
    return when {
        parts.size >= 2 -> "${parts[0].firstOrNull() ?: ' '}${parts[1].firstOrNull() ?: ' '}".uppercase()
        parts.isNotEmpty() -> "${parts[0].firstOrNull() ?: 'G'}".uppercase()
        else -> "G"
    }
}

private fun safeStart(ctx: android.content.Context, intent: Intent) {
    try {
        ctx.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}