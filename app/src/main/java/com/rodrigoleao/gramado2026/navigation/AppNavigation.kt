package com.rodrigoleao.gramado2026.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Sort
import com.rodrigoleao.gramado2026.ui.vouchers.VoucherSortMode
import androidx.compose.material3.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rodrigoleao.gramado2026.data.model.BoardingPass
import com.rodrigoleao.gramado2026.data.model.Contact
import com.rodrigoleao.gramado2026.data.model.TravelDay
import com.rodrigoleao.gramado2026.data.model.Voucher
import com.rodrigoleao.gramado2026.data.repository.TripData
import com.rodrigoleao.gramado2026.ui.boarding.BoardingPassScreen
import com.rodrigoleao.gramado2026.ui.contacts.ContactsScreen
import com.rodrigoleao.gramado2026.ui.day.DayDetailScreen
import com.rodrigoleao.gramado2026.ui.home.HomeScreen
import com.rodrigoleao.gramado2026.ui.map.BustourMapScreen
import com.rodrigoleao.gramado2026.ui.theme.*
import com.rodrigoleao.gramado2026.ui.edit.EditActivityScreen
import com.rodrigoleao.gramado2026.ui.edit.EditActivityViewModel
import com.rodrigoleao.gramado2026.ui.edit.EditBoardingPassScreen
import com.rodrigoleao.gramado2026.ui.edit.EditBoardingPassViewModel
import com.rodrigoleao.gramado2026.ui.edit.EditContactScreen
import com.rodrigoleao.gramado2026.ui.edit.EditContactViewModel
import com.rodrigoleao.gramado2026.ui.edit.EditDayScreen
import com.rodrigoleao.gramado2026.ui.edit.EditDayViewModel
import com.rodrigoleao.gramado2026.ui.edit.EditTripScreen
import com.rodrigoleao.gramado2026.ui.edit.EditTripViewModel
import com.rodrigoleao.gramado2026.ui.edit.EditVoucherScreen
import com.rodrigoleao.gramado2026.ui.edit.EditVoucherViewModel
import com.rodrigoleao.gramado2026.ui.import_trip.ImportTripScreen
import com.rodrigoleao.gramado2026.ui.import_trip.ImportTripViewModel
import com.rodrigoleao.gramado2026.ui.splash.SplashScreen
import com.rodrigoleao.gramado2026.ui.share_trip.ShareTripScreen
import com.rodrigoleao.gramado2026.ui.share_trip.ShareTripViewModel
import com.rodrigoleao.gramado2026.ui.trips.CreateTripScreen
import com.rodrigoleao.gramado2026.ui.trips.CreateTripViewModel
import com.rodrigoleao.gramado2026.ui.trips.TripsListScreen
import com.rodrigoleao.gramado2026.ui.trips.TripsListViewModel
import com.rodrigoleao.gramado2026.ui.trips.TripViewModel
import com.rodrigoleao.gramado2026.ui.vouchers.VouchersScreen
import com.rodrigoleao.gramado2026.ui.settings.SettingsScreen
import com.rodrigoleao.gramado2026.ui.settings.SettingsViewModel
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.launch
import java.time.LocalDate

// ── ROTAS ─────────────────────────────────────────────────────────────────────

sealed class Screen(val route: String) {
    object TripsList  : Screen("trips_list")
    object TripMain   : Screen("trip/{tripId}/main") {
        fun createRoute(tripId: Long) = "trip/$tripId/main"
    }
    object DayDetail  : Screen("trip/{tripId}/day/{dayId}") {
        fun createRoute(tripId: Long, dayId: Int) = "trip/$tripId/day/$dayId"
    }
    object BustourMap    : Screen("map/bustour")
    object CreateTrip    : Screen("trip/create")
    object EditTrip      : Screen("trip/{tripId}/edit") {
        fun createRoute(tripId: Long) = "trip/$tripId/edit"
    }
    object EditDay       : Screen("trip/{tripId}/day/{dayNumber}/edit") {
        fun createRoute(tripId: Long, dayNumber: Int) = "trip/$tripId/day/$dayNumber/edit"
    }
    object EditActivity  : Screen("trip/{tripId}/day/{dayNumber}/activity/{activityId}") {
        fun createRoute(tripId: Long, dayNumber: Int, activityId: Long) = "trip/$tripId/day/$dayNumber/activity/$activityId"
    }
    object EditContact : Screen("trip/{tripId}/contact/{contactId}") {
        fun createRoute(tripId: Long, contactId: Long) = "trip/$tripId/contact/$contactId"
    }
    object EditVoucher : Screen("trip/{tripId}/voucher/{voucherId}") {
        fun createRoute(tripId: Long, voucherId: Long) = "trip/$tripId/voucher/$voucherId"
    }
    object EditBoardingPass : Screen("trip/{tripId}/pass/{passId}") {
        fun createRoute(tripId: Long, passId: Long) = "trip/$tripId/pass/$passId"
    }
    object Splash     : Screen("splash")
    object ImportTrip : Screen("import_trip")
    object ShareTrip  : Screen("trip/{tripId}/share") {
        fun createRoute(tripId: Long) = "trip/$tripId/share"
    }
    object Settings   : Screen("settings")
}

private val TAB_ICONS  = listOf(Icons.Default.Home, Icons.Default.ConfirmationNumber, Icons.Default.FlightTakeoff, Icons.Default.Call)
private val TAB_LABELS = listOf("Início", "Vouchers", "Embarque", "Contatos")
private const val ANIM_DURATION = 320

// ── NAVEGAÇÃO PRINCIPAL ───────────────────────────────────────────────────────

@Composable
fun AppNavigation(importUriState: MutableState<android.net.Uri?> = remember { mutableStateOf(null) }) {
    val navController = rememberNavController()
    val settingsVm: SettingsViewModel = hiltViewModel()
    val showEmergencyContacts by settingsVm.showEmergencyContacts.collectAsStateWithLifecycle()

    val importUri = importUriState.value

    val startDestination = when {
        importUri != null -> Screen.ImportTrip.route
        else              -> Screen.Splash.route
    }

    // Trata onNewIntent: app já aberto, novo arquivo .travel aberto externamente
    LaunchedEffect(importUri) {
        if (importUri != null && navController.currentDestination?.route != Screen.ImportTrip.route) {
            navController.navigate(Screen.ImportTrip.route) { launchSingleTop = true }
        }
    }

    NavHost(
        navController      = navController,
        startDestination   = startDestination,
        enterTransition    = { slideInHorizontally(initialOffsetX = { it },       animationSpec = tween(ANIM_DURATION)) + fadeIn(tween(ANIM_DURATION)) },
        exitTransition     = { slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(ANIM_DURATION)) + fadeOut(tween(ANIM_DURATION)) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(ANIM_DURATION)) + fadeIn(tween(ANIM_DURATION)) },
        popExitTransition  = { slideOutHorizontally(targetOffsetX = { it },       animationSpec = tween(ANIM_DURATION)) + fadeOut(tween(ANIM_DURATION)) }
    ) {

        // ── Splash screen ────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(onFinished = {
                navController.navigate(Screen.TripsList.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        // ── Lista de viagens (entry point) ───────────────────────────────────
        composable(Screen.TripsList.route) {
            val vm: TripsListViewModel = hiltViewModel()
            val trips by vm.trips.collectAsStateWithLifecycle()
            val autoOpenEnabled by settingsVm.autoOpenActiveTrip.collectAsStateWithLifecycle()
            var autoNavigated by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(trips) {
                if (!autoNavigated && trips != null && autoOpenEnabled) {
                    val today = LocalDate.now()
                    val active = trips!!.filter { trip ->
                        val start = runCatching { LocalDate.parse(trip.startDate) }.getOrNull()
                        val end   = runCatching { LocalDate.parse(trip.endDate)   }.getOrNull()
                        start != null && end != null && !today.isBefore(start) && !today.isAfter(end)
                    }
                    if (active.size == 1) {
                        autoNavigated = true
                        navController.navigate(Screen.TripMain.createRoute(active.first().id)) {
                            popUpTo(Screen.TripsList.route)
                        }
                    } else {
                        autoNavigated = true
                    }
                }
            }

            TripsListScreen(
                viewModel      = vm,
                onTripClick    = { tripId -> navController.navigate(Screen.TripMain.createRoute(tripId)) },
                onNewTripClick = { navController.navigate(Screen.CreateTrip.route) },
                onTripEdit     = { tripId -> navController.navigate(Screen.EditTrip.createRoute(tripId)) },
                onTripShare    = { tripId -> navController.navigate(Screen.ShareTrip.createRoute(tripId)) },
                onImportTrip   = { navController.navigate(Screen.ImportTrip.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        // ── Configurações ────────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            val vm: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = vm,
                onBack    = { navController.popBackStack() }
            )
        }

        // ── Importar viagem ──────────────────────────────────────────────────
        composable(Screen.ImportTrip.route) {
            val vm: ImportTripViewModel = hiltViewModel()
            ImportTripScreen(
                viewModel   = vm,
                initialUri  = importUriState.value,
                onImported  = { tripId ->
                    importUriState.value = null
                    // Garante TripsList na backstack mesmo quando o app foi aberto via intent externo
                    // (nesse caso ImportTrip é o startDestination e TripsList nunca foi empilhada)
                    navController.navigate(Screen.TripsList.route) {
                        popUpTo(0) { inclusive = true }
                    }
                    navController.navigate(Screen.TripMain.createRoute(tripId))
                },
                onBack = {
                    importUriState.value = null
                    navController.popBackStack()
                }
            )
        }

        // ── Compartilhar viagem ──────────────────────────────────────────────
        composable(
            route     = Screen.ShareTrip.route,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) {
            val vm: ShareTripViewModel = hiltViewModel()
            ShareTripScreen(
                viewModel = vm,
                onBack    = { navController.popBackStack() }
            )
        }

        // ── Wizard de criação de viagem ──────────────────────────────────────
        composable(Screen.CreateTrip.route) {
            val vm: CreateTripViewModel = hiltViewModel()
            CreateTripScreen(
                viewModel     = vm,
                onBack        = { navController.popBackStack() },
                onTripCreated = { tripId ->
                    navController.navigate(Screen.TripMain.createRoute(tripId)) {
                        popUpTo(Screen.TripsList.route)
                    }
                }
            )
        }

        // ── Pager da viagem ──────────────────────────────────────────────────
        composable(
            route     = Screen.TripMain.route,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) { entry ->
            val tripId = entry.arguments!!.getLong("tripId")
            val vm: TripViewModel = hiltViewModel()
            val tripData by vm.tripData.collectAsStateWithLifecycle()

            val refreshKey by entry.savedStateHandle
                .getStateFlow("refresh", 0L)
                .collectAsStateWithLifecycle()
            LaunchedEffect(refreshKey) { if (refreshKey > 0L) vm.refresh() }

            MainPagerScreen(
                state = TripScreenState(
                    tripData              = tripData,
                    refreshKey            = refreshKey,
                    showEmergencyContacts = showEmergencyContacts
                ),
                actions = TripScreenActions(
                    onDayClick          = { dayId -> navController.navigate(Screen.DayDetail.createRoute(tripId, dayId)) },
                    onBustourMapClick   = { navController.navigate(Screen.BustourMap.route) },
                    onShareTrip         = { navController.navigate(Screen.ShareTrip.createRoute(tripId)) },
                    onEditTrip          = { navController.navigate(Screen.EditTrip.createRoute(tripId)) },
                    onAddContact        = { navController.navigate(Screen.EditContact.createRoute(tripId, 0L)) },
                    onAddVoucher        = { navController.navigate(Screen.EditVoucher.createRoute(tripId, 0L)) },
                    onAddBoardingPass   = { navController.navigate(Screen.EditBoardingPass.createRoute(tripId, 0L)) },
                    onEditContact       = { cId -> navController.navigate(Screen.EditContact.createRoute(tripId, cId)) },
                    onEditVoucher       = { vId -> navController.navigate(Screen.EditVoucher.createRoute(tripId, vId)) },
                    onEditBoardingPass  = { pId -> navController.navigate(Screen.EditBoardingPass.createRoute(tripId, pId)) },
                    onReorderVouchers   = { ordered -> vm.reorderVouchers(ordered) },
                    onDeleteVoucher     = { vId -> vm.deleteVoucher(vId) },
                    onVoucherSortMode   = { mode -> vm.setVoucherSortMode(mode) },
                    onToggleVoucherUsed = { vId, used -> vm.toggleVoucherUsed(vId, used) },
                    onDeleteContact         = { cId -> vm.deleteContact(cId) },
                    onReorderContacts       = { list -> vm.reorderContacts(list) },
                    onToggleFavoriteContact = { cId, fav -> vm.toggleFavoriteContact(cId, fav) },
                    onBack                  = { navController.popBackStack() }
                )
            )
        }

        // ── Detalhe de um dia ────────────────────────────────────────────────
        composable(
            route     = Screen.DayDetail.route,
            arguments = listOf(
                navArgument("tripId") { type = NavType.LongType },
                navArgument("dayId")  { type = NavType.IntType  }
            )
        ) { entry ->
            val tripId    = entry.arguments!!.getLong("tripId")
            val dayNumber = entry.arguments!!.getInt("dayId")
            val vm: TripViewModel = hiltViewModel()
            val tripData by vm.tripData.collectAsStateWithLifecycle()

            val refreshKey by entry.savedStateHandle
                .getStateFlow("refresh", 0L)
                .collectAsStateWithLifecycle()
            LaunchedEffect(refreshKey) { if (refreshKey > 0L) vm.refresh() }

            val day = tripData?.days?.find { it.id == dayNumber }

            val trip = tripData?.trip
            if (day != null) {
                DayDetailScreen(
                    day               = day,
                    refreshKey        = refreshKey,
                    tripLat           = trip?.latitude,
                    tripLon           = trip?.longitude,
                    tripStartDate     = trip?.startDate,
                    tripEndDate       = trip?.endDate,
                    onBack            = { navController.popBackStack() },
                    onBustourMapClick = { navController.navigate(Screen.BustourMap.route) },
                    onEditDay         = { navController.navigate(Screen.EditDay.createRoute(tripId, dayNumber)) },
                    onEditActivity    = { actId -> navController.navigate(Screen.EditActivity.createRoute(tripId, dayNumber, actId)) },
                    onDeleteActivity  = { actId -> vm.deleteActivity(actId) },
                    onAddActivity     = { navController.navigate(Screen.EditActivity.createRoute(tripId, dayNumber, 0L)) },
                    onMoveActivity    = { from, to ->
                        val acts = day.activities
                        if (from in acts.indices && to in acts.indices) {
                            vm.swapActivityPositions(acts[from].id, from, acts[to].id, to)
                        }
                    }
                )
            }
        }

        // ── Editar viagem ────────────────────────────────────────────────────
        composable(
            route     = Screen.EditTrip.route,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) {
            val vm: EditTripViewModel = hiltViewModel()
            EditTripScreen(
                viewModel = vm,
                onBack    = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh", System.currentTimeMillis())
                    navController.popBackStack()
                },
                onDeleted = {
                    navController.navigate(Screen.TripsList.route) {
                        popUpTo(Screen.TripsList.route) { inclusive = false }
                    }
                }
            )
        }

        // ── Editar dia ───────────────────────────────────────────────────────
        composable(
            route     = Screen.EditDay.route,
            arguments = listOf(
                navArgument("tripId")    { type = NavType.LongType },
                navArgument("dayNumber") { type = NavType.IntType  }
            )
        ) {
            val vm: EditDayViewModel = hiltViewModel()
            EditDayScreen(
                viewModel = vm,
                onBack    = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh", System.currentTimeMillis())
                    navController.popBackStack()
                }
            )
        }

        // ── Editar / criar atividade ─────────────────────────────────────────
        composable(
            route     = Screen.EditActivity.route,
            arguments = listOf(
                navArgument("tripId")     { type = NavType.LongType },
                navArgument("dayNumber")  { type = NavType.IntType  },
                navArgument("activityId") { type = NavType.LongType }
            )
        ) {
            val vm: EditActivityViewModel = hiltViewModel()
            EditActivityScreen(
                viewModel = vm,
                onBack    = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh", System.currentTimeMillis())
                    navController.popBackStack()
                }
            )
        }

        // ── Editar / criar contato ───────────────────────────────────────────
        composable(
            route     = Screen.EditContact.route,
            arguments = listOf(
                navArgument("tripId")    { type = NavType.LongType },
                navArgument("contactId") { type = NavType.LongType }
            )
        ) {
            val vm: EditContactViewModel = hiltViewModel()
            EditContactScreen(
                viewModel = vm,
                onBack    = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh", System.currentTimeMillis())
                    navController.popBackStack()
                }
            )
        }

        // ── Editar / criar voucher ───────────────────────────────────────────
        composable(
            route     = Screen.EditVoucher.route,
            arguments = listOf(
                navArgument("tripId")    { type = NavType.LongType },
                navArgument("voucherId") { type = NavType.LongType }
            )
        ) {
            val vm: EditVoucherViewModel = hiltViewModel()
            EditVoucherScreen(
                viewModel = vm,
                onBack    = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh", System.currentTimeMillis())
                    navController.popBackStack()
                }
            )
        }

        // ── Editar / criar passagem ──────────────────────────────────────────
        composable(
            route     = Screen.EditBoardingPass.route,
            arguments = listOf(
                navArgument("tripId") { type = NavType.LongType },
                navArgument("passId") { type = NavType.LongType }
            )
        ) {
            val vm: EditBoardingPassViewModel = hiltViewModel()
            EditBoardingPassScreen(
                viewModel = vm,
                onBack    = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh", System.currentTimeMillis())
                    navController.popBackStack()
                }
            )
        }

        // ── Mapa do Bustour ──────────────────────────────────────────────────
        composable(Screen.BustourMap.route) {
            BustourMapScreen(contentPadding = WindowInsets.systemBars.asPaddingValues())
        }
    }
}

// ── TRIP SCREEN CONTRACTS ─────────────────────────────────────────────────────

private data class TripScreenState(
    val tripData: TripData?,
    val refreshKey: Long,
    val showEmergencyContacts: Boolean
)

private data class TripScreenActions(
    val onDayClick: (Int) -> Unit,
    val onBustourMapClick: () -> Unit,
    val onShareTrip: () -> Unit,
    val onEditTrip: () -> Unit,
    val onAddContact: () -> Unit,
    val onAddVoucher: () -> Unit,
    val onAddBoardingPass: () -> Unit,
    val onEditContact: (Long) -> Unit,
    val onEditVoucher: (Long) -> Unit,
    val onEditBoardingPass: (Long) -> Unit,
    val onReorderVouchers: (List<Voucher>) -> Unit,
    val onDeleteVoucher: (Long) -> Unit,
    val onVoucherSortMode: (VoucherSortMode) -> Unit,
    val onToggleVoucherUsed: (Long, Boolean) -> Unit,
    val onDeleteContact: (Long) -> Unit,
    val onReorderContacts: (List<Contact>) -> Unit,
    val onToggleFavoriteContact: (Long, Boolean) -> Unit,
    val onBack: () -> Unit
)

// ── PAGER DA VIAGEM ───────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MainPagerScreen(
    state: TripScreenState,
    actions: TripScreenActions
) {
    val trip              = state.tripData?.trip
    val pagerState        = rememberPagerState(pageCount = { TAB_ICONS.size })
    val coroutineScope    = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior    = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val savedSortModeStr  = trip?.voucherSortMode ?: "BY_CATEGORY"
    var voucherSortMode   by remember(savedSortModeStr) {
        mutableStateOf(
            VoucherSortMode.entries.find { it.name == savedSortModeStr } ?: VoucherSortMode.BY_CATEGORY
        )
    }
    var showSortMenu      by remember { mutableStateOf(false) }

    LaunchedEffect(state.refreshKey) {
        if (state.refreshKey > 0L) snackbarHostState.showSnackbar("Alterações salvas ✓")
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when (pagerState.currentPage) {
                        0    -> trip?.name ?: ""
                        1    -> "${trip?.name ?: ""}  •  Vouchers"
                        2    -> "${trip?.name ?: ""}  •  Passagens"
                        3    -> "${trip?.name ?: ""}  •  Contatos"
                        else -> trip?.name ?: ""
                    }
                    Text(
                        text       = titleText,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        color      = Color.White
                    )
                },
                navigationIcon = {
                    if (pagerState.currentPage == 0) {
                        IconButton(onClick = actions.onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Minhas viagens",
                                tint = Color.White
                            )
                        }
                    }
                },
                actions = {
                    // Botão de agrupamento — só visível na aba de vouchers
                    if (pagerState.currentPage == 1) {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Agrupar vouchers",
                                    tint = if (voucherSortMode != VoucherSortMode.BY_CATEGORY)
                                               AmberPrimary else Color.White
                                )
                            }
                            DropdownMenu(
                                expanded         = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                containerColor   = SurfaceWhite
                            ) {
                                SortMenuItem(
                                    label    = "Por categoria",
                                    selected = voucherSortMode == VoucherSortMode.BY_CATEGORY,
                                    onClick  = { voucherSortMode = VoucherSortMode.BY_CATEGORY; actions.onVoucherSortMode(VoucherSortMode.BY_CATEGORY); showSortMenu = false }
                                )
                                SortMenuItem(
                                    label    = "Por pessoa",
                                    selected = voucherSortMode == VoucherSortMode.BY_PERSON,
                                    onClick  = { voucherSortMode = VoucherSortMode.BY_PERSON; actions.onVoucherSortMode(VoucherSortMode.BY_PERSON); showSortMenu = false }
                                )
                                SortMenuItem(
                                    label    = "Por dia da viagem",
                                    selected = voucherSortMode == VoucherSortMode.BY_DAY,
                                    onClick  = { voucherSortMode = VoucherSortMode.BY_DAY; actions.onVoucherSortMode(VoucherSortMode.BY_DAY); showSortMenu = false }
                                )
                            }
                        }
                    }
                    IconButton(onClick = actions.onShareTrip) {
                        Icon(Icons.Default.Share, contentDescription = "Compartilhar viagem")
                    }
                    IconButton(onClick = actions.onEditTrip) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar viagem")
                    }
                },
                colors        = TopAppBarDefaults.topAppBarColors(
                    containerColor         = GreenMoss,
                    titleContentColor      = Color.White,
                    actionIconContentColor = Color.White,
                    scrolledContainerColor = GreenMoss
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            val fabAction: (() -> Unit)? = when (pagerState.currentPage) {
                1    -> actions.onAddVoucher
                2    -> actions.onAddBoardingPass
                3    -> actions.onAddContact
                else -> null
            }
            fabAction?.let { action ->
                FloatingActionButton(
                    onClick        = action,
                    containerColor = AmberPrimary,
                    contentColor   = Color.White,
                    shape          = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar")
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = AmberPrimary,
                    contentColor   = Color.White
                )
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation    = 20.dp,
                            shape        = RoundedCornerShape(32.dp),
                            ambientColor = GreenMoss.copy(alpha = 0.25f),
                            spotColor    = GreenMoss.copy(alpha = 0.35f)
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(GreenMoss)
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TAB_ICONS.forEachIndexed { index, icon ->
                        PillNavItem(
                            icon     = icon,
                            label    = TAB_LABELS[index],
                            selected = pagerState.currentPage == index,
                            onClick  = { coroutineScope.launch { pagerState.animateScrollToPage(index) } }
                        )
                    }
                }
            }
        },
        containerColor = GreenLight
    ) { innerPadding ->
        HorizontalPager(
            state                   = pagerState,
            modifier                = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                0 -> HomeScreen(
                    days          = state.tripData?.days ?: emptyList(),
                    hotelName     = trip?.hotelName ?: "",
                    hotelAddress  = trip?.hotelAddress ?: "",
                    hotelPhone    = trip?.hotelPhone ?: "",
                    tripLat       = trip?.latitude,
                    tripLon       = trip?.longitude,
                    tripStartDate = trip?.startDate,
                    tripEndDate   = trip?.endDate,
                    contentPadding = innerPadding,
                    onDayClick    = actions.onDayClick
                )
                1 -> VouchersScreen(
                    vouchers          = state.tripData?.vouchers ?: emptyList(),
                    contentPadding    = innerPadding,
                    sortMode          = voucherSortMode,
                    onEditVoucher     = actions.onEditVoucher,
                    onReorderVouchers = actions.onReorderVouchers,
                    onDeleteVoucher   = actions.onDeleteVoucher,
                    onToggleUsed      = actions.onToggleVoucherUsed
                )
                2 -> BoardingPassScreen(
                    passes             = state.tripData?.boardingPasses ?: emptyList(),
                    contentPadding     = innerPadding,
                    onEditBoardingPass = actions.onEditBoardingPass
                )
                3 -> ContactsScreen(
                    contacts                = state.tripData?.contacts ?: emptyList(),
                    contentPadding          = innerPadding,
                    onEditContact           = actions.onEditContact,
                    onDeleteContact         = actions.onDeleteContact,
                    onReorderContacts       = actions.onReorderContacts,
                    onToggleFavoriteContact = actions.onToggleFavoriteContact,
                    showEmergencyContacts   = state.showEmergencyContacts
                )
                else -> Box(Modifier.fillMaxSize())
            }
        }
    }
}

// ── SORT MENU ITEM ────────────────────────────────────────────────────────────

@Composable
private fun SortMenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            }
        },
        onClick = onClick,
        trailingIcon = if (selected) {{
            Icon(Icons.Default.Check, contentDescription = null, tint = GreenMoss, modifier = Modifier.size(16.dp))
        }} else null
    )
}

// ── PILL NAV ITEM ─────────────────────────────────────────────────────────────

@Composable
private fun PillNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AmberPrimary.copy(alpha = 0.20f))
            )
        }
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = if (selected) AmberPrimary else Color.White.copy(alpha = 0.50f),
            modifier           = Modifier.size(22.dp)
        )
    }
}
