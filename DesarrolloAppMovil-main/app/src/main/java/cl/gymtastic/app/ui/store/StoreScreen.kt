package cl.gymtastic.app.ui.store

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import cl.gymtastic.app.data.model.Product
import cl.gymtastic.app.ui.navigation.Screen
import cl.gymtastic.app.util.ServiceLocator
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    nav: NavController,
    windowSizeClass: WindowSizeClass
) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val bg = Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.22f), cs.surface))

    // Repositorio de productos
    val productsRepo = remember { ServiceLocator.products(ctx) }
    val merchFlow = remember { productsRepo.observeMerch() }

    // Lista de productos (Modelo Product)
    val merch: List<Product> by merchFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // Mapa de stock actualizado
    val stockMap by produceState(initialValue = emptyMap<Long, Int>(), merch) {
        val ids = merch.map { it.id.toLong() }
        if (ids.isNotEmpty()) {
            val stocks = productsRepo.getStockByIds(ids)
            value = stocks.toMap()
        }
    }

    val money = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")).apply { maximumFractionDigits = 0 } }
    val widthSizeClass = windowSizeClass.widthSizeClass
    val gridColumns = if (widthSizeClass == WindowWidthSizeClass.Compact) GridCells.Fixed(2) else GridCells.Adaptive(minSize = 180.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tienda", color = cs.onBackground) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Volver", tint = cs.onBackground) } },
                actions = { IconButton(onClick = { nav.navigate(Screen.Cart.route) }) { Icon(Icons.Filled.ShoppingCart, "Carrito", tint = cs.onBackground) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background, titleContentColor = cs.onBackground)
            )
        },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { nav.navigate(Screen.Cart.route) }, icon = { Icon(Icons.Filled.ShoppingCart, null) }, text = { Text("Ver carrito") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().background(bg).padding(innerPadding).padding(16.dp)) {
            if (merch.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Cargando productos...", color = cs.onSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(columns = gridColumns, verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
                    items(merch, key = { it.id }) { p ->
                        val stock = stockMap[p.id.toLong()] ?: p.stock
                        val canAdd = (stock ?: 0) > 0

                        ProductCard(
                            product = p,
                            priceText = money.format(p.precio),
                            stock = stock,
                            // --- AHORA RECIBE LA CANTIDAD ---
                            onAdd = { qtyToAdd ->
                                scope.launch {
                                    val currentQty = ServiceLocator.cart(ctx).getQtyFor(p.id.toLong())
                                    val max = (stock ?: 0)

                                    // Validamos: (lo que tengo en carro + lo que quiero agregar) <= stock total
                                    if (currentQty + qtyToAdd > max) {
                                        val available = (max - currentQty).coerceAtLeast(0)
                                        Toast.makeText(ctx, "Stock insuficiente. Solo puedes llevar $available más.", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    ServiceLocator.cart(ctx).add(p.id.toLong(), qtyToAdd, p.precio.toInt())
                                    Toast.makeText(ctx, "Agregado: $qtyToAdd x \"${p.nombre}\"", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = canAdd
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    priceText: String,
    stock: Int?,
    onAdd: (Int) -> Unit, // <-- Cambiado para recibir la cantidad
    enabled: Boolean
) {
    val cs = MaterialTheme.colorScheme

    // Estado local para el contador de cantidad
    var quantity by remember { mutableIntStateOf(1) }

    ElevatedCard(elevation = CardDefaults.elevatedCardElevation(6.dp), colors = CardDefaults.elevatedCardColors(containerColor = cs.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(0.dp)) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(product.img).crossfade(true).build(),
                contentDescription = product.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(120.dp),
                loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(strokeWidth = 2.dp) } },
                error = { Box(Modifier.fillMaxSize().background(cs.surfaceVariant), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Category, null, tint = cs.onSurfaceVariant.copy(0.5f)) } },
                success = { SubcomposeAsyncImageContent() }
            )
            Column(Modifier.padding(14.dp)) {
                Text(product.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                if (stock != null) { AssistChip(onClick = {}, label = { Text("Stock: $stock") }); Spacer(Modifier.height(6.dp)) }
                Text(priceText, style = MaterialTheme.typography.titleLarge, color = cs.primary)
                Spacer(Modifier.height(12.dp))

                // --- SELECTOR DE CANTIDAD ---
                if (enabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = cs.surfaceVariant)
                        ) {
                            Icon(Icons.Filled.Remove, null)
                        }

                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        FilledIconButton(
                            onClick = {
                                // Limitamos el contador al stock disponible (o 99 si no hay límite estricto)
                                val max = stock ?: 99
                                if (quantity < max) quantity++
                            },
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = cs.primaryContainer)
                        ) {
                            Icon(Icons.Filled.Add, null)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Botón Agregar
                Button(
                    onClick = {
                        onAdd(quantity)
                        quantity = 1 // Resetear contador
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled
                ) {
                    Text(if (enabled) "Agregar al carrito" else "Sin stock")
                }
            }
        }
    }
}