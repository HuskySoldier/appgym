package cl.gymtastic.app.ui.home


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.gymtastic.app.R
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import cl.gymtastic.app.data.local.entity.UserEntity
import cl.gymtastic.app.data.local.entity.AttendanceEntity
import cl.gymtastic.app.data.datastore.CheckCounts // Mantenemos la data class
import kotlinx.coroutines.flow.flowOf
import cl.gymtastic.app.ui.navigation.NavRoutes
import cl.gymtastic.app.ui.navigation.Screen
import cl.gymtastic.app.util.ServiceLocator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil // Import para ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    nav: NavController,
    windowSizeClass: WindowSizeClass
) {
    // --- Estado General y Contexto ---
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current

    // Ruta actual
    val currentRoute = nav.currentBackStackEntryFlow
        .collectAsState(initial = nav.currentBackStackEntry)
        .value?.destination?.route

    // --- Sesión de Autenticación y Datos del Usuario ---
    val session = remember { ServiceLocator.auth(ctx).prefs() }
    val authEmail by session.userEmailFlow.collectAsStateWithLifecycle(initialValue = "")
    // --- ELIMINADO isAdmin basado en email ---
    // val isAdmin = authEmail.equals("admin@gymtastic.cl", ignoreCase = true)

    // --- Obtener DAOs ---
    val usersDao = remember { GymTasticDatabase.get(ctx).users() }
    val attendanceDao = remember { GymTasticDatabase.get(ctx).attendance() } // <-- DAO de Asistencia

    // --- Observar UserEntity (para estado del plan y rol) ---
    val userEntity by remember(authEmail) {
        if (authEmail.isNotBlank()) usersDao.observeByEmail(authEmail) else flowOf(null)
    }.collectAsStateWithLifecycle(initialValue = null)
    val hasPlan = userEntity?.hasActivePlan ?: false

    // --- MODIFICADO: isAdmin ahora depende del rol en UserEntity ---
    val isAdmin by remember(userEntity) {
        // Usa derivedStateOf para asegurar que solo se recalcule si userEntity cambia
        derivedStateOf { userEntity?.rol == "admin" }
    }
    // --- FIN MODIFICACIÓN ---

    // --- Observar Lista de Asistencia desde Room ---
    val attendanceList by remember(authEmail) {
        if (authEmail.isNotBlank()) {
            attendanceDao.observeByUser(authEmail) // Observa la lista de AttendanceEntity
        } else {
            flowOf(emptyList()) // Lista vacía si no hay email
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // --- Calcular CheckCounts a partir de la lista de asistencia ---
    val calculatedCounts by remember(attendanceList) {
        derivedStateOf { // Se recalcula solo si attendanceList cambia
            val totalIn = attendanceList.size // Cada registro es un check-in
            // --- CORRECCIÓN: Tipo explícito ---
            val totalOut = attendanceList.count { attendance: AttendanceEntity -> attendance.checkOutTimestamp != null }
            val lastIn = attendanceList.firstOrNull() // El primero es el más reciente (ORDER BY DESC)
            // --- CORRECCIÓN: Tipo explícito ---
            val lastOut = attendanceList.firstOrNull { attendance: AttendanceEntity -> attendance.checkOutTimestamp != null }

            CheckCounts(
                totalIn = totalIn,
                totalOut = totalOut,
                lastInTs = lastIn?.timestamp,
                lastOutTs = lastOut?.checkOutTimestamp
            )
        }
    }


    val snackbar = remember { SnackbarHostState() }

    // --- Items del Drawer ---
    // Define baseItems and gatedItems if they are not already defined globally or passed as parameters
    val baseItems = listOf(
        "Home" to Screen.Home.route,
        "Planes" to Screen.Planes.route,
        "Tienda" to Screen.Store.route,
        "Carrito" to Screen.Cart.route
    )
    val gatedItems = listOf( // Items que requieren plan activo
        "Check-In" to Screen.CheckIn.route,
        "Trainers" to Screen.Trainers.route
    )
    // Recalcula items basado en hasPlan (derivado de userEntity) y ruta actual
    val drawerItems = remember(hasPlan, currentRoute) { // isAdmin ya no es necesario aquí
        buildList {
            val base = if (hasPlan) baseItems + gatedItems else baseItems
            addAll(base.filter { (_, route) -> route != currentRoute }) // Use placeholder _ for unused label
            add("Cerrar sesión" to "logout")
        }
    }


    // --- Lógica de Ancho ---
    val widthSizeClass = windowSizeClass.widthSizeClass
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact

    // --- Selección del tipo de Drawer ---
    if (isCompact) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    DrawerContent(
                        nav = nav,
                        drawerItems = drawerItems,
                        onClose = { scope.launch { drawerState.close() } }
                    )
                }
            }
        ) {
            HomeScreenScaffold(
                nav = nav,
                userEntity = userEntity,
                windowSizeClass = windowSizeClass,
                // --- Pasamos isAdmin derivado ---
                isAdmin = isAdmin,
                checkCounts = calculatedCounts,
                onOpenDrawer = { scope.launch { drawerState.open() } }
            )
        }
    } else {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(Modifier.width(280.dp)) {
                    DrawerContent(
                        nav = nav,
                        drawerItems = drawerItems,
                        onClose = {}
                    )
                }
            }
        ) {
            HomeScreenScaffold(
                nav = nav,
                userEntity = userEntity,
                windowSizeClass = windowSizeClass,
                // --- Pasamos isAdmin derivado ---
                isAdmin = isAdmin,
                checkCounts = calculatedCounts,
                onOpenDrawer = null
            )
        }
    }
}

/** Contenido del Drawer */
@Composable
private fun DrawerContent(nav: NavController, drawerItems: List<Pair<String, String>>, onClose: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = cs.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "GymTastic",
                style = MaterialTheme.typography.titleLarge,
                color = cs.primary,
                modifier = Modifier.padding(bottom = 20.dp, top = 8.dp)
            )

            drawerItems.forEach { (label, route) ->
                val isLogout = route == "logout"
                val color = if (isLogout) cs.error else cs.onSurface

                NavigationDrawerItem(
                    label = { Text(label, color = color) },
                    selected = false,
                    onClick = {
                        onClose()
                        if (isLogout) {
                            scope.launch {
                                ServiceLocator.auth(ctx).logout()
                                nav.navigate(NavRoutes.LOGIN) { popUpTo(0) }
                            }
                        } else {
                            nav.navigate(route) { launchSingleTop = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(cs.surfaceVariant)
                        .padding(horizontal = 8.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = cs.primary.copy(alpha = 0.20f),
                        unselectedContainerColor = Color.Transparent,
                    )
                )
            }
        }
    }
}


/** Scaffold principal (MODIFICADO para recibir isAdmin) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenScaffold(
    nav: NavController,
    userEntity: UserEntity?,
    windowSizeClass: WindowSizeClass,
    // --- Recibe isAdmin derivado ---
    isAdmin: Boolean,
    checkCounts: CheckCounts,
    onOpenDrawer: (() -> Unit)?
) {
    val cs = MaterialTheme.colorScheme
    val snackbar = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home", color = cs.onBackground) },
                navigationIcon = {
                    onOpenDrawer?.let { openDrawerAction ->
                        IconButton(onClick = openDrawerAction) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir menú", tint = cs.onBackground)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { nav.navigate(Screen.Profile.route) { launchSingleTop = true } }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Perfil", tint = cs.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.background,
                    titleContentColor = cs.onBackground,
                    navigationIconContentColor = cs.onBackground,
                    actionIconContentColor = cs.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        HomeContent(
            modifier = Modifier.padding(innerPadding),
            nav = nav,
            userEntity = userEntity,
            windowSizeClass = windowSizeClass,
            // --- Pasa isAdmin derivado ---
            isAdmin = isAdmin,
            checkCounts = checkCounts
        )
    }
}


/** Contenido principal (MODIFICADO para recibir isAdmin) */
@Composable
private fun HomeContent(
    modifier: Modifier = Modifier,
    nav: NavController,
    userEntity: UserEntity?,
    windowSizeClass: WindowSizeClass,
    // --- Recibe isAdmin derivado ---
    isAdmin: Boolean,
    checkCounts: CheckCounts
) {
    val cs = MaterialTheme.colorScheme
    val hasPlan = userEntity?.hasActivePlan ?: false
    val planEnd = userEntity?.planEndMillis

    // --- ELIMINADO: Ya no calculamos isAdmin aquí, lo recibimos ---
    // val isAdmin = authEmail.equals("admin@gymtastic.cl", ignoreCase = true)

    val widthSizeClass = windowSizeClass.widthSizeClass
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact
    val columnModifier = if (isCompact) {
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    } else {
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 64.dp, vertical = 16.dp)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.gym_background),
            contentDescription = "Fondo",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)))

        Column(
            modifier = columnModifier,
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // 1. Tarjeta de Bienvenida
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.elevatedCardColors(containerColor = cs.surface.copy(alpha = 0.92f)),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
                    Text("Bienvenido a GymTastic 💪", style = MaterialTheme.typography.headlineMedium, color = cs.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text("Entrena y supera tus límites", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(24.dp))

            // 2. Sección "Tu Estado"
            SectionTitle("Tu Estado")
            MembershipCard(
                hasPlan = hasPlan,
                planEndMillis = planEnd,
                onManagePlan = { nav.navigate(Screen.Planes.route) }
            )
            Spacer(Modifier.height(16.dp))
            CheckStatsCard(
                counts = checkCounts,
                enabled = hasPlan,
                onOpenHistory = { nav.navigate(Screen.CheckIn.route) }
            )
            Spacer(Modifier.height(24.dp))

            // 3. Sección "Acciones Rápidas"
            SectionTitle("Acciones Rápidas")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Trainers",
                    icon = Icons.Default.FitnessCenter,
                    enabled = hasPlan,
                    onClick = { nav.navigate(Screen.Trainers.route) }
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Tienda",
                    icon = Icons.Default.Store,
                    enabled = true,
                    onClick = { nav.navigate(Screen.Store.route) }
                )
            }

            // 4. Botón de Panel de Administración (Usa el isAdmin recibido)
            AnimatedVisibility(visible = isAdmin) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(24.dp))
                    SectionTitle("Administración")
                    Button(
                        onClick = { nav.navigate(Screen.Admin.route) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.tertiaryContainer,
                            contentColor = cs.onTertiaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Panel de Administración")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// --- Componentes Internos (Sin cambios) ---
@Composable private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = Color.White.copy(alpha = 0.8f),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 10.dp, top = 8.dp)
    )
}
@Composable private fun ActionCard(modifier: Modifier = Modifier, title: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier.height(100.dp).clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = cs.surface.copy(alpha = 0.95f),
            disabledContainerColor = cs.surface.copy(alpha = 0.8f)
        ),
        enabled = enabled,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            val iconTint = if (enabled) cs.primary else cs.onSurface.copy(alpha = 0.4f)
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(if (enabled) cs.primary.copy(alpha = 0.15f) else cs.onSurface.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = iconTint) }
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = if (enabled) cs.onSurface else cs.onSurface.copy(alpha = 0.4f), fontWeight = FontWeight.SemiBold)
        }
    }
}
@Composable private fun MembershipCard(hasPlan: Boolean, planEndMillis: Long?, onManagePlan: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val df = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    val (daysLeft, endText) = remember(planEndMillis) {
        if (planEndMillis == null) 0L to "Sin membresía activa"
        else {
            val now = System.currentTimeMillis()
            val diffMs = planEndMillis - now
            val days = ceil(diffMs / (1000.0 * 60 * 60 * 24.0)).toLong().coerceAtLeast(0)
            days to "Vence el ${df.format(Date(planEndMillis))}"
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.elevatedCardColors(containerColor = cs.surface.copy(alpha = 0.92f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(if (hasPlan) cs.primary.copy(alpha = 0.15f) else cs.error.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Text(text = if (hasPlan) "$daysLeft" else "–", style = MaterialTheme.typography.titleMedium, color = if (hasPlan) cs.primary else cs.error) }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = if (hasPlan) "Membresía activa" else "Sin membresía", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(text = if (hasPlan) endText else "Activa un plan para habilitar funcionalidades", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
            Button(onClick = onManagePlan, shape = RoundedCornerShape(12.dp)) { Text(if (hasPlan) "Cambiar plan" else "Ver planes") }
        }
    }
}
@Composable private fun CheckStatsCard(counts: CheckCounts, enabled: Boolean, onOpenHistory: () -> Unit = {}) {
    val cs = MaterialTheme.colorScheme
    val df = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cs.surface.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        onClick = onOpenHistory,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactStatBox(
                title = "IN",
                value = counts.totalIn.toString(),
                subtitle = counts.lastInTs?.let { ts -> df.format(Date(ts)) } ?: "—", // Explicit lambda param name
                icon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = cs.primary) }
            )
            VerticalDivider(modifier = Modifier.height(30.dp).padding(horizontal = 8.dp), thickness = 1.dp, color = cs.outline.copy(alpha = 0.2f))
            CompactStatBox(
                title = "OUT",
                value = counts.totalOut.toString(),
                subtitle = counts.lastOutTs?.let { ts -> df.format(Date(ts)) } ?: "—", // Explicit lambda param name
                icon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = cs.onSurfaceVariant) }
            )
        }
    }
}
@Composable private fun CompactStatBox(title: String, value: String, subtitle: String, icon: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(cs.surfaceVariant), contentAlignment = Alignment.Center) { icon() }
        Column {
            Text("$title $value", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = cs.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        }
    }
}

