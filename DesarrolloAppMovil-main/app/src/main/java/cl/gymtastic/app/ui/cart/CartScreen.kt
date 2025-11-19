package cl.gymtastic.app.ui.cart

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.gymtastic.app.data.local.entity.CartItemEntity
import cl.gymtastic.app.ui.navigation.Screen
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import cl.gymtastic.app.util.ServiceLocator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    nav: NavController,
    windowSizeClass: WindowSizeClass // <-- PARÁMETRO AÑADIDO
) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // Fondo como el resto
    val bg = Brush.verticalGradient(
        listOf(cs.primary.copy(alpha = 0.22f), cs.surface)
    )

    // Carrito
    val cartFlow = remember { ServiceLocator.cart(ctx).observeCart() }
    val items: List<CartItemEntity> by cartFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // Nombres de productos
    val productsRepo = remember { ServiceLocator.products(ctx) }
    var names by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    LaunchedEffect(items) {
        val ids = items.map { it.productId }.distinct()
        names = productsRepo.getNamesById(ids) // ← ya implementado en tu repo/DAO
    }

    val money = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "CL")).apply { maximumFractionDigits = 0 }
    }
    val total = items.sumOf { it.qty * it.unitPrice }

    // Reacciona al tamaño de pantalla
    val widthSizeClass = windowSizeClass.widthSizeClass
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact
    val contentModifier = if (isCompact) {
        Modifier.fillMaxSize()
    } else {
        Modifier.fillMaxSize().width(600.dp) // Ancho fijo para tablets
    }
    val horizontalPadding = if (isCompact) 16.dp else 0.dp // Sin padding lateral en tablets

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carrito", color = cs.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = cs.onBackground)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                ServiceLocator.cart(ctx).clear()
                                Toast.makeText(ctx, "Carrito vacío", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = items.isNotEmpty()
                    ) {
                        Text("Vaciar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.background,
                    titleContentColor = cs.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(padding)
                // Padding vertical general, el horizontal se aplica dinámicamente
                .padding(vertical = 16.dp, horizontal = horizontalPadding),
            contentAlignment = Alignment.TopCenter // Centra el contenido en tablets
        ) {
            if (items.isEmpty()) {
                // Estado vacío elegante
                Column(
                    modifier = contentModifier, // <-- APLICAMOS MODIFICADOR
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Tu carrito está vacío",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { nav.navigate(Screen.Store.route) }) {
                        Text("Ir a la Tienda")
                    }
                }
            } else {
                Column(modifier = contentModifier) { // <-- APLICAMOS MODIFICADOR
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items.size) { idx ->
                            val it = items[idx]
                            val nombre = names[it.productId] ?: "Producto #${it.productId}"
                            val unit = money.format(it.unitPrice)
                            val subtotal = money.format(it.unitPrice * it.qty)

                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Contenido
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            nombre,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Cant: ${it.qty}  •  Unit: $unit",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = cs.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            "Subtotal: $subtotal",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = cs.primary
                                        )
                                    }

                                    // Eliminar
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                ServiceLocator.cart(ctx).remove(it)   // ⬅️ usa tu método existente
                                                Toast
                                                    .makeText(ctx, "Eliminado del carrito", Toast.LENGTH_SHORT)
                                                    .show()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                                    }

                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Total + acciones
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Total: ${money.format(total)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = cs.primary
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { nav.navigate(Screen.Home.route) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Home") }

                                Button(
                                    onClick = { nav.navigate(Screen.Payment.route) },
                                    enabled = total > 0,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Pagar") }
                            }
                        }
                    }
                }
            }
        }
    }
}
