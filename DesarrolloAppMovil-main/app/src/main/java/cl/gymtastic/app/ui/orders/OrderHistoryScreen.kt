package cl.gymtastic.app.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.gymtastic.app.data.remote.OrderDto
import cl.gymtastic.app.util.ServiceLocator
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(nav: NavController) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val cartRepo = remember { ServiceLocator.cart(ctx) }
    val authRepo = remember { ServiceLocator.auth(ctx) }

    // Obtener email del usuario
    val authPrefs = remember { authRepo.prefs() }
    val authEmail by authPrefs.userEmailFlow.collectAsState(initial = "")

    // Cargar historial
    val orders by produceState<List<OrderDto>>(initialValue = emptyList(), key1 = authEmail) {
        if (authEmail.isNotBlank()) {
            value = cartRepo.getOrderHistory(authEmail)
        }
    }

    val bg = Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.22f), cs.surface))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Compras", color = cs.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = cs.onBackground)
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
        ) {
            if (orders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.ReceiptLong, null, Modifier.size(64.dp), tint = cs.onSurfaceVariant.copy(0.5f))
                        Spacer(Modifier.height(16.dp))
                        Text("No tienes compras registradas", color = cs.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders) { order ->
                        OrderCard(order)
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(order: OrderDto) {
    val cs = MaterialTheme.colorScheme
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val moneyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL")).apply { maximumFractionDigits = 0 }

    Card(
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pedido #${order.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(dateFormat.format(Date(order.timestamp)), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp), color = cs.outlineVariant)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ShoppingBag, null, tint = cs.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(order.summary, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            }

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${order.itemsCount} productos", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                Text(moneyFormat.format(order.total), style = MaterialTheme.typography.titleMedium, color = cs.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}