package cl.gymtastic.app.ui.admin

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
// --- IMPORTS CORREGIDOS ---
import cl.gymtastic.app.data.model.Product
import cl.gymtastic.app.data.model.Trainer
import cl.gymtastic.app.data.remote.UserProfileDto
// --------------------------
import cl.gymtastic.app.util.ImageUriUtils
import cl.gymtastic.app.util.ServiceLocator
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

// --- Pantalla Principal de Admin ---

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AdminScreen(
    nav: NavController,
    windowSizeClass: WindowSizeClass
) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val tabTitles = listOf("Productos", "Trainers", "Usuarios")
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val bg = Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.22f), cs.surface))
    val widthSizeClass = windowSizeClass.widthSizeClass
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact
    val pagerModifier = if (isCompact) Modifier.fillMaxSize() else Modifier.fillMaxSize().padding(horizontal = 100.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Administración", color = cs.onBackground) },
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
        Column(modifier = Modifier.fillMaxSize().background(bg).padding(padding)) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top
            ) { page ->
                Box(modifier = pagerModifier.padding(16.dp), contentAlignment = Alignment.TopCenter) {
                    when (page) {
                        0 -> AdminProductsTab()
                        1 -> AdminTrainersTab()
                        2 -> AdminUsersTab()
                    }
                }
            }
        }
    }
}

// --- Pestaña de Productos ---
@Composable
fun AdminProductsTab() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ServiceLocator.products(ctx) }
    val money = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")).apply { maximumFractionDigits = 0 } }

    // Estado local en lugar de Flow de base de datos
    var merch by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    // Función para recargar datos desde la API
    fun loadProducts() {
        scope.launch {
            loading = true
            // Asegúrate de que ProductsRepository tenga un método 'getAll()' que llame a la API y devuelva List<Product>
            // Si no lo tienes, usa directamente: ServiceLocator.api().getProducts().body() ?: emptyList()
            try {
                // Opción directa si no has actualizado el repo:
                val response = ServiceLocator.api().getProducts()
                if (response.isSuccessful) {
                    // Filtramos merch solo si es necesario, o mostramos todo
                    merch = response.body()?.filter { it.tipo == "merch" } ?: emptyList()
                }
            } catch (e: Exception) {
                Toast.makeText(ctx, "Error cargando productos", Toast.LENGTH_SHORT).show()
            }
            loading = false
        }
    }

    // Cargar al inicio
    LaunchedEffect(Unit) { loadProducts() }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Product?>(null) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Nuevo Producto") },
                icon = { Icon(Icons.Default.Add, null) },
                onClick = { editingProduct = null; showEditDialog = true }
            )
        }
    ) { padding ->
        if (loading && merch.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(merch, key = { it.id }) { product ->
                    ProductAdminCard(
                        product = product,
                        priceText = money.format(product.precio),
                        onEdit = { editingProduct = product; showEditDialog = true },
                        onDelete = { showDeleteDialog = product }
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        ProductEditDialog(
            product = editingProduct,
            onDismiss = { showEditDialog = false },
            onSave = { productToSave, oldImageUri ->
                scope.launch {
                    try {
                        // Aquí usamos la API directa para guardar (o el método save del repo actualizado)
                        val api = ServiceLocator.api()
                        // Mapeamos Product a ProductEntity si tu API aún usa ese nombre, o Product si ya lo cambiaste
                        // Asumiremos que tu API espera el mismo objeto JSON
                        if (productToSave.id == 0) {
                            api.createProduct(productToSave) // Crear
                        } else {
                            api.updateProduct(productToSave.id, productToSave) // Editar
                        }

                        // Manejo de imagen local (opcional si subieras la imagen al server real)
                        if (oldImageUri != null && oldImageUri != productToSave.img) {
                            withContext(Dispatchers.IO) { ImageUriUtils.deleteFileFromInternalStorage(oldImageUri) }
                        }

                        showEditDialog = false
                        Toast.makeText(ctx, "Producto guardado", Toast.LENGTH_SHORT).show()
                        loadProducts() // Recargar lista
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    showDeleteDialog?.let { productToDelete ->
        DeleteConfirmDialog(
            itemName = productToDelete.nombre,
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                scope.launch {
                    try {
                        ServiceLocator.api().deleteProduct(productToDelete.id)
                        withContext(Dispatchers.IO) { ImageUriUtils.deleteFileFromInternalStorage(productToDelete.img) }
                        Toast.makeText(ctx, "Producto eliminado", Toast.LENGTH_SHORT).show()
                        loadProducts() // Recargar lista
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    showDeleteDialog = null
                }
            }
        )
    }
}

@Composable
fun ProductAdminCard(product: Product, priceText: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SubcomposeAsyncImage(
                model = product.img, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                loading = { CircularProgressIndicator(Modifier.size(24.dp))},
                error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Category, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } },
                success = { SubcomposeAsyncImageContent() }
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(product.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Stock: ${product.stock ?: "N/A"}  •  Precio: $priceText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun ProductEditDialog(product: Product?, onDismiss: () -> Unit, onSave: (Product, String?) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val oldImageUri = product?.img
    var name by remember { mutableStateOf(product?.nombre ?: "") }
    var price by remember { mutableStateOf(product?.precio?.toInt()?.toString() ?: "") }
    var stock by remember { mutableStateOf(product?.stock?.toString() ?: "") }
    var desc by remember { mutableStateOf(product?.descripcion ?: "") }
    var imgUriString by remember { mutableStateOf(product?.img ?: "") }
    val isFormValid by derivedStateOf { name.isNotBlank() && price.toIntOrNull() != null && stock.toIntOrNull() != null }
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val internalUri = ImageUriUtils.copyUriToInternalStorage(ctx, uri, "prod_${product?.id ?: "new"}")
                withContext(Dispatchers.Main) {
                    if (internalUri != null) {
                        if (oldImageUri != null && oldImageUri != internalUri) {
                            withContext(Dispatchers.IO){ ImageUriUtils.deleteFileFromInternalStorage(oldImageUri) }
                        }
                        imgUriString = internalUri
                    } else { Toast.makeText(ctx, "Error al copiar la imagen", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Nuevo Producto" else "Editar Producto") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).clickable { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center) {
                    SubcomposeAsyncImage(model = ImageRequest.Builder(ctx).data(imgUriString.ifBlank { null }).crossfade(true).build(),
                        contentDescription = "Imagen del producto", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                        loading = { CircularProgressIndicator(Modifier.size(32.dp)) },
                        error = { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Text("Seleccionar Imagen", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                        success = { SubcomposeAsyncImageContent() })
                    if (imgUriString.isNotBlank()) {
                        IconButton(onClick = { scope.launch(Dispatchers.IO){ ImageUriUtils.deleteFileFromInternalStorage(imgUriString) }; imgUriString = "" },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f), CircleShape)) {
                            Icon(Icons.Default.Clear, "Quitar imagen", tint = Color.White) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Precio (CLP)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción (opcional)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onSave(Product(id = product?.id ?: 0, nombre = name.trim(), descripcion = desc.trim(), precio = price.toDoubleOrNull() ?: 0.0, stock = stock.toIntOrNull(), tipo = "merch", img = imgUriString.trim().ifBlank { null }), oldImageUri) }, enabled = isFormValid) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// --- Pestaña de Trainers ---
@Composable
fun AdminTrainersTab() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estado local
    var trainers by remember { mutableStateOf<List<Trainer>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    fun loadTrainers() {
        scope.launch {
            loading = true
            try {
                val response = ServiceLocator.api().getTrainers()
                if (response.isSuccessful) {
                    trainers = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Toast.makeText(ctx, "Error cargando trainers", Toast.LENGTH_SHORT).show()
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { loadTrainers() }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Trainer?>(null) }
    var editingTrainer by remember { mutableStateOf<Trainer?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Nuevo Trainer") },
                icon = { Icon(Icons.Default.Add, null) },
                onClick = { editingTrainer = null; showEditDialog = true }
            )
        }
    ) { padding ->
        if (loading && trainers.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(trainers, key = { it.id }) { trainer ->
                    TrainerAdminCard(
                        trainer = trainer,
                        onEdit = { editingTrainer = trainer; showEditDialog = true },
                        onDelete = { showDeleteDialog = trainer }
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        TrainerEditDialog(
            trainer = editingTrainer,
            onDismiss = { showEditDialog = false },
            onSave = { trainerToSave, oldImageUri ->
                scope.launch {
                    try {
                        val api = ServiceLocator.api()
                        if (trainerToSave.id == 0L) {
                            api.createTrainer(trainerToSave)
                        } else {
                            api.updateTrainer(trainerToSave.id, trainerToSave)
                        }

                        if (oldImageUri != null && oldImageUri != trainerToSave.img) {
                            withContext(Dispatchers.IO) { ImageUriUtils.deleteFileFromInternalStorage(oldImageUri) }
                        }
                        showEditDialog = false
                        Toast.makeText(ctx, "Trainer guardado", Toast.LENGTH_SHORT).show()
                        loadTrainers()
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    showDeleteDialog?.let { trainerToDelete ->
        DeleteConfirmDialog(
            itemName = trainerToDelete.nombre,
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                scope.launch {
                    try {
                        ServiceLocator.api().deleteTrainer(trainerToDelete.id)
                        withContext(Dispatchers.IO) { ImageUriUtils.deleteFileFromInternalStorage(trainerToDelete.img) }
                        Toast.makeText(ctx, "Trainer eliminado", Toast.LENGTH_SHORT).show()
                        loadTrainers()
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    showDeleteDialog = null
                }
            }
        )
    }
}

@Composable
fun TrainerAdminCard(trainer: Trainer, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SubcomposeAsyncImage(model = trainer.img, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                loading = { CircularProgressIndicator(Modifier.size(24.dp)) },
                error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } },
                success = { SubcomposeAsyncImageContent() })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(trainer.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(trainer.especialidad, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun TrainerEditDialog(trainer: Trainer?, onDismiss: () -> Unit, onSave: (Trainer, String?) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val oldImageUri = trainer?.img
    var name by remember { mutableStateOf(trainer?.nombre ?: "") }
    var especialidad by remember { mutableStateOf(trainer?.especialidad ?: "") }
    var fono by remember { mutableStateOf(trainer?.fono ?: "") }
    var email by remember { mutableStateOf(trainer?.email ?: "") }
    var imgUriString by remember { mutableStateOf(trainer?.img ?: "") }
    val isFormValid by derivedStateOf { name.isNotBlank() && especialidad.isNotBlank() && email.contains("@") && fono.isNotBlank() }
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val internalUri = ImageUriUtils.copyUriToInternalStorage(ctx, uri, "trainer_${trainer?.id ?: "new"}")
                withContext(Dispatchers.Main) {
                    if (internalUri != null) {
                        if (oldImageUri != null && oldImageUri != internalUri) {
                            withContext(Dispatchers.IO){ ImageUriUtils.deleteFileFromInternalStorage(oldImageUri) }
                        }
                        imgUriString = internalUri
                    } else { Toast.makeText(ctx, "Error al copiar la imagen", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (trainer == null) "Nuevo Trainer" else "Editar Trainer") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(150.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape).clickable { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center) {
                        SubcomposeAsyncImage(model = ImageRequest.Builder(ctx).data(imgUriString.ifBlank { null }).crossfade(true).build(),
                            contentDescription = "Foto del Trainer", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                            loading = { CircularProgressIndicator(Modifier.size(32.dp))},
                            error = { Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Text("Seleccionar Foto", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                            success = { SubcomposeAsyncImageContent() })
                    }
                    if (imgUriString.isNotBlank()) {
                        IconButton(onClick = { scope.launch(Dispatchers.IO){ ImageUriUtils.deleteFileFromInternalStorage(imgUriString) }; imgUriString = "" },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f), CircleShape)) {
                            Icon(Icons.Default.Clear, "Quitar imagen", tint = Color.White) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre Completo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = especialidad, onValueChange = { especialidad = it }, label = { Text("Especialidad") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = fono, onValueChange = { fono = it }, label = { Text("Fono (ej: +569...)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onSave(Trainer(id = trainer?.id ?: 0, nombre = name.trim(), especialidad = especialidad.trim(), fono = fono.trim(), email = email.trim(), img = imgUriString.trim().ifBlank { null }), oldImageUri) }, enabled = isFormValid) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}


// --- Pestaña de Usuarios (ACTUALIZADA PARA BACKEND) ---

@Composable
fun AdminUsersTab() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    // Eliminar UsersDao, ahora usamos AuthRepository directo
    val authRepo = remember { ServiceLocator.auth(ctx) }

    // Estado local para la lista de usuarios
    var users by remember { mutableStateOf<List<UserProfileDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<UserProfileDto?>(null) }

    // Función para refrescar la lista desde el Backend
    fun refreshUsers() {
        scope.launch {
            isLoading = true
            users = authRepo.getAllUsers() // Llamada directa a la API
            isLoading = false
        }
    }

    // Cargar usuarios al iniciar
    LaunchedEffect(Unit) {
        refreshUsers()
    }

    // --- UI ---
    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Nuevo Usuario") },
                icon = { Icon(Icons.Default.Add, null) },
                onClick = { showAddDialog = true }
            )
        }
    ) { padding ->
        if (isLoading && users.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(users, key = { it.email }) { user ->
                    UserAdminCard(
                        user = user,
                        onDelete = { showDeleteDialog = user }
                    )
                }
            }
        }
    }

    // --- Diálogo Añadir Usuario ---
    if (showAddDialog) {
        UserAddDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { email, name, password, rol ->
                scope.launch {
                    val normalizedEmail = email.trim().lowercase()
                    val normalizedName = name.trim()

                    // 1. Registrar
                    val registerSuccess = authRepo.register(normalizedEmail, password, normalizedName)

                    if (registerSuccess) {
                        // 2. Si el rol es distinto a 'user', actualizar
                        if (rol != "user") {
                            authRepo.updateRole(normalizedEmail, rol)
                        }
                        Toast.makeText(ctx, "Usuario creado", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                        refreshUsers() // Recargar lista
                    } else {
                        Toast.makeText(ctx, "Error al crear usuario (¿Email existe?)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // --- Diálogo Eliminar Usuario ---
    showDeleteDialog?.let { userToDelete ->
        DeleteConfirmDialog(
            itemName = userToDelete.email,
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                scope.launch {
                    val success = authRepo.deleteUser(userToDelete.email)
                    if (success) {
                        Toast.makeText(ctx, "Usuario eliminado", Toast.LENGTH_SHORT).show()
                        refreshUsers() // Recargar lista
                    } else {
                        Toast.makeText(ctx, "Error al eliminar usuario", Toast.LENGTH_SHORT).show()
                    }
                    showDeleteDialog = null
                }
            }
        )
    }
}

@Composable
fun UserAdminCard(
    user: UserProfileDto,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(user.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Mostrar el rol
                Text("Rol: ${user.rol}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar Usuario", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

// --- Diálogo Añadir Usuario ---
@SuppressLint("UnrememberedMutableState")
@Composable
fun UserAddDialog(
    onDismiss: () -> Unit,
    onAdd: (email: String, name: String, password: String, rol: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Estado para seleccionar rol
    val roles = listOf("user", "admin", "trainer")
    var selectedRol by remember { mutableStateOf(roles[0]) }

    val isFormValid by derivedStateOf {
        email.contains("@") && name.isNotBlank() && password.length >= 6
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Usuario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), singleLine = true)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next), singleLine = true)
                OutlinedTextField(
                    value = password, onValueChange = { password = it }, label = { Text("Contraseña Inicial") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if(isFormValid) onAdd(email, name, password, selectedRol) }),
                    trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null) } },
                    singleLine = true, supportingText = { Text("Mínimo 6 caracteres") }
                )

                Spacer(Modifier.height(8.dp))
                Text("Rol:", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth()) {
                    roles.forEach { rolOption ->
                        Row(
                            Modifier
                                .weight(1f)
                                .selectable(
                                    selected = (rolOption == selectedRol),
                                    onClick = { selectedRol = rolOption },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (rolOption == selectedRol),
                                onClick = null
                            )
                            Text(
                                text = rolOption.replaceFirstChar { it.titlecase(Locale.getDefault()) },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(email.trim().lowercase(), name.trim(), password, selectedRol) },
                enabled = isFormValid
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// --- Diálogo Genérico de Confirmación ---
@Composable
fun DeleteConfirmDialog(itemName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar Eliminación") },
        text = {
            Text("¿Estás seguro de que quieres eliminar \"$itemName\"? Esta acción no se puede deshacer.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}