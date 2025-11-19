package cl.gymtastic.app.ui.planes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import cl.gymtastic.app.data.model.Product // <-- Nuevo Modelo
import cl.gymtastic.app.data.model.User    // <-- Nuevo Modelo
import cl.gymtastic.app.ui.navigation.Screen
import cl.gymtastic.app.util.ServiceLocator
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanesScreen(
    nav: NavController,
    windowSizeClass: WindowSizeClass
) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // --- Sesión y Repositorios ---
    val authRepo = remember { ServiceLocator.auth(ctx) }
    val productsRepo = remember { ServiceLocator.products(ctx) }

    val authPrefs = remember { authRepo.prefs() }
    val authEmail by authPrefs.userEmailFlow.collectAsStateWithLifecycle(initialValue = "")

    // --- PASO 1: Cargar Usuario desde API (Backend-Only) ---
    // Reemplaza al DAO observable
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

    // --- Flujo de Planes (Backend -> Repo -> Flow) ---
    // El repositorio ya fue refactorizado para obtener esto de la API
    val planesFlow = remember { productsRepo.observePlanes() }
    val planes: List<Product> by planesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // --- PASO 2: Calcular días restantes y 'canBuy' ---
    val thresholdDays = 3
    val remainingDays: Long? = remember(userEntity) {
        userEntity?.planEndMillis?.let { end ->
            val diff = end - System.currentTimeMillis()
            if (diff <= 0) 0L else TimeUnit.MILLISECONDS.toDays(diff)
        }
    }

    val canBuy by remember(userEntity) {
        derivedStateOf {
            val ue = userEntity
            if (ue == null) {
                false // No puede comprar sin usuario
            } else if (!ue.hasActivePlan) {
                true // Puede comprar si no tiene plan activo
            } else {
                // Si tiene plan activo, verifica si quedan pocos días
                remainingDays?.let { days -> days <= thresholdDays } ?: true
            }
        }
    }

    val bg = Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.20f), cs.surface))
    val widthSizeClass = windowSizeClass.widthSizeClass

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planes", color = cs.onBackground) },
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
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(bg)
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("Elige tu plan", style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            // --- Mostrar advertencia si hay plan activo y no puede renovar ---
            if (userEntity?.hasActivePlan == true && !canBuy) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = cs.surfaceVariant),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Plan activo", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Podrás contratar otro plan cuando falten ${thresholdDays} días o menos para que termine. " +
                                    "Días restantes: ${remainingDays ?: "—"}",
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }

            // --- LAYOUT ADAPTATIVO ---
            if (widthSizeClass == WindowWidthSizeClass.Compact) {
                // --- Layout Compacto (Teléfono) ---
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(planes.size) { idx ->
                        val p = planes[idx]
                        val unitPrice = p.precio.toInt()

                        PlanCardContent(
                            product = p,
                            unitPrice = unitPrice,
                            canBuy = canBuy,
                            remainingDays = remainingDays,
                            onAddToCart = {
                                scope.launch {
                                    ServiceLocator.cart(ctx).add(p.id.toLong(), 1, unitPrice)
                                    snackbar.showSnackbar("Agregado al carrito")
                                }
                            },
                            onBuyNow = {
                                scope.launch {
                                    ServiceLocator.cart(ctx).add(p.id.toLong(), 1, unitPrice)
                                    nav.navigate(Screen.Payment.route) { launchSingleTop = true }
                                }
                            },
                            onShowSnackbar = { msg ->
                                scope.launch { snackbar.showSnackbar(msg) }
                            }
                        )
                    }
                }
            } else {
                // --- Layout Medium/Expanded (Tablet) ---
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(planes.size) { idx ->
                        val p = planes[idx]
                        val unitPrice = p.precio.toInt()

                        PlanCardContent(
                            product = p,
                            unitPrice = unitPrice,
                            canBuy = canBuy,
                            remainingDays = remainingDays,
                            onAddToCart = {
                                scope.launch {
                                    ServiceLocator.cart(ctx).add(p.id.toLong(), 1, unitPrice)
                                    snackbar.showSnackbar("Agregado al carrito")
                                }
                            },
                            onBuyNow = {
                                scope.launch {
                                    ServiceLocator.cart(ctx).add(p.id.toLong(), 1, unitPrice)
                                    nav.navigate(Screen.Payment.route) { launchSingleTop = true }
                                }
                            },
                            onShowSnackbar = { msg ->
                                scope.launch { snackbar.showSnackbar(msg) }
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Composable interno para el contenido de la tarjeta del plan */
@Composable
private fun PlanCardContent(
    product: Product, // <-- AHORA USA EL MODELO PRODUCT
    unitPrice: Int,
    canBuy: Boolean,
    remainingDays: Long?,
    onAddToCart: () -> Unit,
    onBuyNow: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val cannotBuyMessage = "No puedes ${if (remainingDays != null) "contratar aún. Restan ${remainingDays} día(s)." else "comprar ahora."}"

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { // Click en toda la tarjeta intenta comprar ahora
                if (!canBuy) {
                    onShowSnackbar(cannotBuyMessage)
                    return@clickable
                }
                onBuyNow()
            }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(product.nombre, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text("CLP $unitPrice", style = MaterialTheme.typography.titleMedium, color = cs.primary)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (!canBuy) {
                            onShowSnackbar(cannotBuyMessage)
                            return@OutlinedButton
                        }
                        onAddToCart()
                    },
                    enabled = canBuy,
                    modifier = Modifier.weight(1f)
                ) { Text("Agregar") }

                Button(
                    onClick = {
                        if (!canBuy) {
                            onShowSnackbar(cannotBuyMessage)
                            return@Button
                        }
                        onBuyNow()
                    },
                    enabled = canBuy,
                    modifier = Modifier.weight(1f)
                ) { Text("Contratar") }
            }
        }
    }
}