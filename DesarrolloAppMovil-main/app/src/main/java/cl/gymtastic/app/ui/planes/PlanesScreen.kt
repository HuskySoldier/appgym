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
import cl.gymtastic.app.data.model.Product // <--- Nuevo Modelo
import cl.gymtastic.app.data.model.User    // <--- Nuevo Modelo
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

    val authRepo = remember { ServiceLocator.auth(ctx) }
    val productsRepo = remember { ServiceLocator.products(ctx) }
    val authPrefs = remember { authRepo.prefs() }
    val authEmail by authPrefs.userEmailFlow.collectAsStateWithLifecycle(initialValue = "")

    // --- Cargar Usuario desde API ---
    val userEntity by produceState<User?>(initialValue = null, key1 = authEmail) {
        if (authEmail.isNotBlank()) {
            val dto = authRepo.getUserProfile(authEmail)
            value = dto?.let {
                User(it.email, it.nombre, it.rol, it.planEndMillis, it.sedeId, it.sedeName, it.sedeLat, it.sedeLng, it.avatarUri, it.fono, it.bio)
            }
        }
    }

    // --- Cargar Planes desde API ---
    val planesFlow = remember { productsRepo.observePlanes() }
    val planes: List<Product> by planesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // Lógica de fechas
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
            if (ue == null) false
            else if (!ue.hasActivePlan) true
            else remainingDays?.let { it <= thresholdDays } ?: true
        }
    }

    val bg = Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.20f), cs.surface))
    val widthSizeClass = windowSizeClass.widthSizeClass

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planes", color = cs.onBackground) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Volver", tint = cs.onBackground) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background, titleContentColor = cs.onBackground)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.fillMaxSize().background(bg).padding(padding).padding(16.dp)) {
            Text("Elige tu plan", style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            if (userEntity?.hasActivePlan == true && !canBuy) {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = cs.surfaceVariant), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Plan activo", style = MaterialTheme.typography.titleMedium)
                        Text("Podrás contratar otro plan en ${remainingDays ?: "-"} días.", color = cs.onSurfaceVariant)
                    }
                }
            }

            if (widthSizeClass == WindowWidthSizeClass.Compact) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(planes.size) { idx ->
                        val p = planes[idx]
                        PlanCardContent(p, p.precio.toInt(), canBuy, remainingDays,
                            { scope.launch { ServiceLocator.cart(ctx).add(p.id.toLong(), 1, p.precio.toInt()); snackbar.showSnackbar("Agregado") } },
                            { scope.launch { ServiceLocator.cart(ctx).add(p.id.toLong(), 1, p.precio.toInt()); nav.navigate(Screen.Payment.route) } },
                            { msg -> scope.launch { snackbar.showSnackbar(msg) } })
                    }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Adaptive(300.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(planes.size) { idx ->
                        val p = planes[idx]
                        PlanCardContent(p, p.precio.toInt(), canBuy, remainingDays,
                            { scope.launch { ServiceLocator.cart(ctx).add(p.id.toLong(), 1, p.precio.toInt()); snackbar.showSnackbar("Agregado") } },
                            { scope.launch { ServiceLocator.cart(ctx).add(p.id.toLong(), 1, p.precio.toInt()); nav.navigate(Screen.Payment.route) } },
                            { msg -> scope.launch { snackbar.showSnackbar(msg) } })
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCardContent(product: Product, unitPrice: Int, canBuy: Boolean, remainingDays: Long?, onAddToCart: () -> Unit, onBuyNow: () -> Unit, onShowSnackbar: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val cannotBuyMessage = "No puedes contratar aún."

    ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = cs.surface), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { if (!canBuy) onShowSnackbar(cannotBuyMessage) else onBuyNow() }) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(product.nombre, style = MaterialTheme.typography.titleLarge)
            Text("CLP $unitPrice", style = MaterialTheme.typography.titleMedium, color = cs.primary)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { if(!canBuy) onShowSnackbar(cannotBuyMessage) else onAddToCart() }, enabled = canBuy, modifier = Modifier.weight(1f)) { Text("Agregar") }
                Button(onClick = { if(!canBuy) onShowSnackbar(cannotBuyMessage) else onBuyNow() }, enabled = canBuy, modifier = Modifier.weight(1f)) { Text("Contratar") }
            }
        }
    }
}