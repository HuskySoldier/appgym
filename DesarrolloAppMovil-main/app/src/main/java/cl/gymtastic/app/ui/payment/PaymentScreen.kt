package cl.gymtastic.app.ui.payment

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.gymtastic.app.R
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import kotlinx.coroutines.flow.flowOf
import cl.gymtastic.app.data.local.InsufficientStockException
import cl.gymtastic.app.data.local.SedesRepo
import cl.gymtastic.app.ui.navigation.Screen
import cl.gymtastic.app.util.ServiceLocator
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit // <-- Importación necesaria para TimeUnit

private fun daysFromNow(days: Int): Long {
    val now = System.currentTimeMillis()
    return now + days * 24L * 60L * 60L * 1000L
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    nav: NavController,
    windowSizeClass: WindowSizeClass // <-- Parámetro WSC
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    // --- Sesión y Usuario ---
    val authPrefs = remember { ServiceLocator.auth(ctx).prefs() }
    val authEmail by authPrefs.userEmailFlow.collectAsStateWithLifecycle(initialValue = "")

    // --- Observar UserEntity desde DB ---
    val usersDao = remember { GymTasticDatabase.get(ctx).users() }
    val userEntity by remember(authEmail) {
        if (authEmail.isNotBlank()) {
            usersDao.observeByEmail(authEmail)
        } else {
            flowOf(null)
        }
    }.collectAsStateWithLifecycle(initialValue = null)

    val useGoogleMap = true // Configuración del mapa

    // --- MODIFICADO: Eliminado "Efectivo" ---
    val metodos = listOf("Débito", "Crédito", "Transferencia")
    var metodo by remember { mutableStateOf(metodos.first()) }
    var metodoExpanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    // --- NUEVO: Estado para mostrar diálogo de transferencia ---
    var showTransferDialog by remember { mutableStateOf(false) }

    // Carrito
    val cartRepo = remember { ServiceLocator.cart(ctx) } // Obtener CartRepo
    val cartFlow = remember { cartRepo.observeCart() } // Usar CartRepo para observar
    val items by cartFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val total = items.sumOf { it.qty * it.unitPrice }

    // Repo de productos: types + names
    val productsRepo = remember { ServiceLocator.products(ctx) }
    var types by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var names by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }

    LaunchedEffect(items) {
        // IDs need to be Long for repository functions
        val ids = items.map { it.productId }.distinct()
        if (ids.isNotEmpty()) {
            // Assuming getTypesById and getNamesById now correctly handle Long lists
            types = productsRepo.getTypesById(ids)
            names = productsRepo.getNamesById(ids)
        } else {
            types = emptyMap()
            names = emptyMap()
        }
    }

    val hasPlanInCart = remember(items, types) { items.any { types[it.productId] == "plan" } }
    val totalPlans = remember(items, types) {
        items.filter { types[it.productId] == "plan" }.sumOf { it.qty * it.unitPrice }
    }
    val totalMerch = total - totalPlans

    // Calcular 'canBuy' basado en UserEntity
    val thresholdDays = 3
    val canBuy by remember(userEntity) {
        derivedStateOf {
            val ue = userEntity
            if (ue == null) false
            else if (!ue.hasActivePlan) true
            else {
                val end = ue.planEndMillis ?: return@derivedStateOf true
                val diff = end - System.currentTimeMillis()
                val days = if (diff <= 0) 0 else TimeUnit.MILLISECONDS.toDays(diff)
                days <= thresholdDays
            }
        }
    }

    // Sedes (solo si hay plan en carrito)
    val sedes = SedesRepo.sedes
    var sedeExpanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(0) }
    val safeIndex = selectedIndex.coerceIn(0, (sedes.size - 1).coerceAtLeast(0))
    val sede = sedes.getOrNull(safeIndex)
    val sedeLatLng = remember(safeIndex) {
        LatLng(sede?.lat ?: -33.45, sede?.lng ?: -70.67) // fallback STGO
    }

    // Mapa
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(sedeLatLng, 14f)
    }

    // Diálogo bloqueo
    var showBlocked by remember { mutableStateOf<String?>(null) }

    val bg = Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.22f), cs.surface))

    // Lógica de Ancho
    val widthSizeClass = windowSizeClass.widthSizeClass
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact
    val cardWidthModifier = if (isCompact) Modifier.fillMaxWidth(0.95f) else Modifier.width(550.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pago", color = cs.onBackground) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center // Centra la Card en tablets
        ) {
            AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = cardWidthModifier // <-- Ancho adaptativo
                        .verticalScroll(rememberScrollState()) // Scroll interno
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Pago simulado", style = MaterialTheme.typography.headlineSmall)

                        // Resumen + detalle de ítems
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Items en carrito: ${items.size}", color = cs.onSurfaceVariant)
                            items.forEach { item ->
                                val nombre = names[item.productId] ?: "Producto #${item.productId}"
                                val subtotal = item.qty * item.unitPrice
                                Text(
                                    text = "• $nombre × ${item.qty} — CLP $subtotal",
                                    color = cs.onSurfaceVariant
                                )
                            }
                            if (hasPlanInCart) Text("Subtotal planes: CLP $totalPlans", color = cs.onSurfaceVariant)
                            if (totalMerch > 0) Text("Subtotal tienda: CLP $totalMerch", color = cs.onSurfaceVariant)
                            Text(
                                "Total: CLP $total",
                                style = MaterialTheme.typography.titleMedium,
                                color = cs.primary
                            )
                        }

                        // Método de pago
                        ExposedDropdownMenuBox(
                            expanded = metodoExpanded,
                            onExpandedChange = { metodoExpanded = !metodoExpanded }
                        ) {
                            TextField(
                                value = metodo,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Método de pago") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = metodoExpanded) },
                                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = metodoExpanded,
                                onDismissRequest = { metodoExpanded = false }
                            ) {
                                metodos.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m) },
                                        onClick = {
                                            metodo = m
                                            metodoExpanded = false
                                            // --- NUEVO: Mostrar diálogo si es Transferencia ---
                                            if (m == "Transferencia") {
                                                showTransferDialog = true
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // === Sede / Mapa solo si hay plan en carrito ===
                        if (hasPlanInCart) {
                            ExposedDropdownMenuBox(
                                expanded = sedeExpanded,
                                onExpandedChange = { sedeExpanded = !sedeExpanded }
                            ) {
                                TextField(
                                    value = if (sede != null)
                                        "${sede.nombre} • ${sede.direccion}" else "Selecciona una sede",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Sede") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = sedeExpanded)
                                    },
                                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = sedeExpanded,
                                    onDismissRequest = { sedeExpanded = false }
                                ) {
                                    sedes.forEachIndexed { idx, s ->
                                        DropdownMenuItem(
                                            text = { Text("${s.nombre} • ${s.direccion}") },
                                            onClick = {
                                                selectedIndex = idx
                                                sedeExpanded = false
                                                scope.launch {
                                                    cameraState.animate(
                                                        update = CameraUpdateFactory.newLatLngZoom(
                                                            LatLng(s.lat, s.lng), 14f
                                                        )
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Mapa
                            Box(Modifier.fillMaxWidth().height(180.dp)) {
                                if (useGoogleMap) {
                                    GoogleMap(
                                        modifier = Modifier.fillMaxSize(),
                                        cameraPositionState = cameraState,
                                        uiSettings = MapUiSettings(zoomControlsEnabled = false)
                                    ) {
                                        Marker(
                                            state = MarkerState(position = sedeLatLng),
                                            title = sede?.nombre ?: "Sede",
                                            snippet = sede?.direccion ?: ""
                                        )
                                    }
                                } else {
                                    Image(
                                        painter = painterResource(R.drawable.map_placeholder),
                                        contentDescription = "Mapa sede",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        // ===== Acciones =====
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { nav.popBackStack() },
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancelar") }

                            Button(
                                onClick = {
                                    if (total <= 0 || loading || authEmail.isBlank()) return@Button
                                    loading = true
                                    showBlocked = null // Limpiar error anterior
                                    scope.launch {
                                        try {
                                            // 1. Llamar al Orquestador de Checkout
                                            val (planActivated, message) = cartRepo.processCheckout(
                                                userEmail = authEmail,
                                                items = items,
                                                sede = if (hasPlanInCart) sede else null
                                            )

                                            // 2. Éxito: Navegar
                                            nav.navigate(Screen.PaymentSuccess.withPlan(planActivated)) {
                                                launchSingleTop = true
                                                popUpTo(Screen.Payment.route) { inclusive = true }
                                            }

                                        } catch (e: IOException) {
                                            // Captura errores de stock, plan activo, o conexión
                                            showBlocked = e.message
                                        } catch (e: Exception) {
                                            showBlocked = "Ocurrió un error inesperado: ${e.message ?: "desconocido"}"
                                        } finally {
                                            loading = false
                                        }
                                    }
                                },
                                enabled = !loading && total > 0 && authEmail.isNotBlank() && (hasPlanInCart && sede != null || !hasPlanInCart),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (loading) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp).padding(end = 8.dp)
                                    )
                                }
                                Text(if (loading) "Procesando..." else "Pagar")
                            }
                        }

                        if (hasPlanInCart) {
                            Text(
                                "Tu plan se asociará a la sede seleccionada y a tu cuenta (${authEmail}).",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de bloqueo
    if (showBlocked != null) {
        AlertDialog(
            onDismissRequest = { showBlocked = null },
            confirmButton = { TextButton(onClick = { showBlocked = null }) { Text("Entendido") } },
            title = { Text("No se puede completar el pago") },
            text = { Text(showBlocked!!) }
        )
    }

    // --- NUEVO: Diálogo de Transferencia ---
    if (showTransferDialog) {
        TransferInfoDialog(onDismiss = { showTransferDialog = false })
    }
}

// --- NUEVO: Composable para el diálogo de Transferencia ---
@Composable
private fun TransferInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Datos de Transferencia") },
        text = {
            // Reemplaza con tus datos reales
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Banco: Banco Ejemplo")
                Text("Tipo de Cuenta: Cuenta Corriente")
                Text("Número: 123456789")
                Text("RUT: 11.111.111-1")
                Text("Nombre: GymTastic SpA")
                Text("Email: pagos@gymtastic.cl")
                Spacer(Modifier.height(8.dp))
                Text("Por favor, envía el comprobante al email indicado.", fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido")
            }
        }
    )
}