package cl.gymtastic.app.ui.trainers

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.gymtastic.app.data.model.Trainer // <--- Nuevo Modelo
import cl.gymtastic.app.ui.navigation.Screen
import cl.gymtastic.app.util.ServiceLocator
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainersScreen(nav: NavController, windowSizeClass: WindowSizeClass) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val trainersRepo = remember { ServiceLocator.trainers(ctx) }

    // Cargar desde API
    val list by produceState<List<Trainer>>(initialValue = emptyList()) {
        value = trainersRepo.getTrainers()
    }

    val bg = Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.22f), cs.surface))
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    val contentModifier = if (isCompact) Modifier.fillMaxSize().padding(16.dp) else Modifier.width(600.dp).padding(vertical = 12.dp)
    val boxAlignment = if (isCompact) Alignment.TopStart else Alignment.TopCenter

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trainers", color = cs.onBackground) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Volver", tint = cs.onBackground) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(bg).padding(padding), contentAlignment = boxAlignment) {
            Column(contentModifier) {
                Text("Conoce a nuestro equipo", style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                if (list.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(list.size) { i ->
                            val t = list[i]
                            TrainerCard(t,
                                { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${t.fono}"))) },
                                { ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${t.email}"))) },
                                { nav.navigate(Screen.Booking.routeWith(t.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainerCard(trainer: Trainer, onCall: () -> Unit, onEmail: () -> Unit, onBook: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = cs.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubcomposeAsyncImage(model = trainer.img, contentDescription = trainer.nombre, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(CircleShape).background(cs.primary.copy(alpha = 0.15f)), loading = { CircularProgressIndicator() }, success = { SubcomposeAsyncImageContent() })
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(trainer.nombre, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    AssistChip(onClick = {}, label = { Text(trainer.especialidad) })
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCall, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Phone, null); Text("Llamar") }
                OutlinedButton(onClick = onEmail, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Email, null); Text("Email") }
                Button(onClick = onBook, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.CalendarToday, null); Text("Agendar") }
            }
        }
    }
}