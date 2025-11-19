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
import androidx.datastore.core.IOException
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.gymtastic.app.R
// import cl.gymtastic.app.data.local.db.GymTasticDatabase // <-- ELIMINADO
import cl.gymtastic.app.data.model.User // <-- NUEVO
import cl.gymtastic.app.data.model.CartItem // <-- NUEVO
import cl.gymtastic.app.data.local.SedesRepo
import cl.gymtastic.app.ui.navigation.Screen
import cl.gymtastic.app.util.ServiceLocator
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private fun daysFromNow(days: Int): Long {
    val now = System.currentTimeMillis()
    return now + days * 24L * 60L * 60L * 1000L
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    nav: NavController,
    windowSizeClass: WindowSizeClass
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    // --- Repositorios ---
    val authRepo = remember { ServiceLocator.auth(ctx) }
    val cartRepo = remember { ServiceLocator.cart(ctx) }
    val productsRepo = remember { ServiceLocator.products(ctx) }

    // --- Sesión ---
    val authPrefs = remember { authRepo.prefs() }
    val authEmail by authPrefs.userEmailFlow.collectAsStateWithLifecycle(initialValue = "")

    // --- 1. Cargar Usuario (Desde API, Backend-Only) ---
    val userEntity by produceState<User?>(initialValue = null, key1 = authEmail) {
        if (authEmail.isNotBlank()) {
            val dto = authRepo.getUserProfile(authEmail)
            value = dto?.let {
                User(
                    email = it.email,
                    nombre = it.nombre,
                    rol = it.rol,
                    planEndMillis = it.planEndMillis,
                    sedeId = it.sedeId,
                    sedeName = it.sedeName,
                    sedeLat = it.sedeLat,
                    sedeLng = it.sedeLng,
                    avatarUri = it.avatarUri,
                    fono = it.fono,
                    bio = it.bio
                )
            }
        } else {
            value = null
        }
    }
    // ---------------------------------------------------

    val useGoogleMap = true

    // Métodos de pago
    val metodos = listOf("Débito", "Crédito", "Transferencia")
    var metodo by remember { mutableStateOf(metodos.first()) }
    var metodoExpanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    // Diálogo de transferencia
    var showTransferDialog by remember { mutableStateOf(false) }

    // Carrito (En memoria)
    val cartFlow = remember { cartRepo.observeCart() }
    val items: List<CartItem> by cartFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val total = items.sumOf { it.qty * it.unitPrice }

    // Tipos y Nombres de productos (Desde API/Repo)
    var types by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var names by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }

    LaunchedEffect(items) {
        val ids = items.map { it.productId }.distinct()
        if (ids.isNotEmpty()) {
            // Estos métodos del repo ya fueron actualizados para consultar la API
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

    // Calcular 'canBuy' basado en el modelo User
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

    // Sedes (Estático en Sede.kt, no requiere cambios)
    val sedes = SedesRepo.sedes
    var sedeExpanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(0) }
    val safeIndex = selectedIndex.coerceIn(0, (sedes.size - 1).coerceAtLeast(0))
    val sede = sedes.getOrNull(safeIndex)
    val sedeLatLng = remember(safeIndex) {
        LatLng(sede?.lat ?: -33.45, sede?.lng ?: -70.67)
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
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = cardWidthModifier
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Pago simulado", style = MaterialTheme.typography.headlineSmall)

                        // Resumen
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
                                            if (m == "Transferencia") {
                                                showTransferDialog = true
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // === Sede / Mapa ===
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
                                    showBlocked = null
                                    scope.launch {
                                        try {
                                            // Checkout usando el CartRepository (que ahora es backend-only)
                                            val (planActivated, message) = cartRepo.processCheckout(
                                                userEmail = authEmail,
                                                items = items,
                                                sede = if (hasPlanInCart) sede else null
                                            )

                                            // Éxito
                                            nav.navigate(Screen.PaymentSuccess.withPlan(planActivated)) {
                                                launchSingleTop = true
                                                popUpTo(Screen.Payment.route) { inclusive = true }
                                            }

                                        } catch (e: IOException) {
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

    // Diálogo de bloqueo / error
    if (showBlocked != null) {
        AlertDialog(
            onDismissRequest = { showBlocked = null },
            confirmButton = { TextButton(onClick = { showBlocked = null }) { Text("Entendido") } },
            title = { Text("No se puede completar el pago") },
            text = { Text(showBlocked!!) }
        )
    }

    // Diálogo de Transferencia
    if (showTransferDialog) {
        TransferInfoDialog(onDismiss = { showTransferDialog = false })
    }
}

@Composable
private fun TransferInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Datos de Transferencia") },
        text = {
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