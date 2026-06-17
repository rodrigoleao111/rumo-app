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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rodrigoleao.gramado2026.data.db.TravelDatabase
import com.rodrigoleao.gramado2026.data.model.BoardingPass
import com.rodrigoleao.gramado2026.data.model.Contact
import com.rodrigoleao.gramado2026.data.model.TravelDay
import com.rodrigoleao.gramado2026.data.model.Voucher
import com.rodrigoleao.gramado2026.data.repository.TripRepository
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
import kotlinx.coroutines.launch

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
}

private val TAB_ICONS  = listOf(Icons.Default.Home, Icons.Default.ConfirmationNumber, Icons.Default.FlightTakeoff, Icons.Default.Call)
private val TAB_LABELS = listOf("Início", "Vouchers", "Embarque", "Contatos")
private const val ANIM_DURATION = 320

// ── NAVEGAÇÃO PRINCIPAL ───────────────────────────────────────────────────────

@Composable
fun AppNavigation(initialImportUri: android.net.Uri? = null) {
    val navController = rememberNavController()
    val context       = LocalContext.current
    val db            = remember { TravelDatabase.getInstance(context) }
    val repo          = remember { TripRepository(db) }
    val scope         = rememberCoroutineScope()

    val startDestination = when {
        initialImportUri != null -> Screen.ImportTrip.route
        else                     -> Screen.Splash.route
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
            val vm: TripsListViewModel = viewModel(factory = TripsListViewModel.Factory(repo))
            TripsListScreen(
                viewModel      = vm,
                onTripClick    = { tripId -> navController.navigate(Screen.TripMain.createRoute(tripId)) },
                onNewTripClick = { navController.navigate(Screen.CreateTrip.route) },
                onTripEdit     = { tripId -> navController.navigate(Screen.EditTrip.createRoute(tripId)) },
                onTripShare    = { tripId -> navController.navigate(Screen.ShareTrip.createRoute(tripId)) },
                onImportTrip   = { navController.navigate(Screen.ImportTrip.route) }
            )
        }

        // ── Importar viagem ──────────────────────────────────────────────────
        composable(Screen.ImportTrip.route) {
            val ctx = LocalContext.current
            val vm: ImportTripViewModel = viewModel(factory = ImportTripViewModel.Factory(repo, ctx))
            ImportTripScreen(
                viewModel   = vm,
                initialUri  = initialImportUri,
                onImported  = { tripId ->
                    navController.navigate(Screen.TripMain.createRoute(tripId)) {
                        popUpTo(Screen.TripsList.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Compartilhar viagem ──────────────────────────────────────────────
        composable(
            route     = Screen.ShareTrip.route,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) { entry ->
            val tripId = entry.arguments!!.getLong("tripId")
            val ctx    = LocalContext.current
            val vm: ShareTripViewModel = viewModel(factory = ShareTripViewModel.Factory(repo, tripId, ctx))
            ShareTripScreen(
                viewModel = vm,
                onBack    = { navController.popBackStack() }
            )
        }

        // ── Wizard de criação de viagem ──────────────────────────────────────
        composable(Screen.CreateTrip.route) {
            val vm: CreateTripViewModel = viewModel(factory = CreateTripViewModel.Factory(repo))
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
            val vm: TripViewModel = viewModel(factory = TripViewModel.Factory(repo, tripId))
            val tripData by vm.tripData.collectAsStateWithLifecycle()

            val refreshKey by entry.savedStateHandle
                .getStateFlow("refresh", 0L)
                .collectAsStateWithLifecycle()
            LaunchedEffect(refreshKey) { if (refreshKey > 0L) vm.refresh() }

            val tripEntity = tripData?.trip
            MainPagerScreen(
                tripId         = tripId,
                tripName       = tripEntity?.name ?: "",
                days           = tripData?.days           ?: emptyList(),
                contacts       = tripData?.contacts       ?: emptyList(),
                vouchers       = tripData?.vouchers       ?: emptyList(),
                boardingPasses = tripData?.boardingPasses ?: emptyList(),
                hotelName      = tripEntity?.hotelName    ?: "",
                hotelAddress   = tripEntity?.hotelAddress ?: "",
                hotelPhone     = tripEntity?.hotelPhone   ?: "",
                tripLat        = tripEntity?.latitude,
                tripLon        = tripEntity?.longitude,
                tripStartDate  = tripEntity?.startDate,
                tripEndDate    = tripEntity?.endDate,
                refreshKey     = refreshKey,
                onDayClick        = { dayId -> navController.navigate(Screen.DayDetail.createRoute(tripId, dayId)) },
                onBustourMapClick = { navController.navigate(Screen.BustourMap.route) },
                onShareTrip       = { navController.navigate(Screen.ShareTrip.createRoute(tripId)) },
                onEditTrip        = { navController.navigate(Screen.EditTrip.createRoute(tripId)) },
                onAddContact   = { navController.navigate(Screen.EditContact.createRoute(tripId, 0L)) },
                onAddVoucher   = { navController.navigate(Screen.EditVoucher.createRoute(tripId, 0L)) },
                onAddBoardingPass = { navController.navigate(Screen.EditBoardingPass.createRoute(tripId, 0L)) },
                onEditContact  = { cId -> navController.navigate(Screen.EditContact.createRoute(tripId, cId)) },
                onEditVoucher  = { vId -> navController.navigate(Screen.EditVoucher.createRoute(tripId, vId)) },
                onEditBoardingPass = { pId -> navController.navigate(Screen.EditBoardingPass.createRoute(tripId, pId)) },
                savedVoucherSortMode = tripEntity?.voucherSortMode ?: "BY_CATEGORY",
                onReorderVouchers    = { ordered -> vm.reorderVouchers(ordered) },
                onDeleteVoucher      = { vId -> vm.deleteVoucher(vId) },
                onVoucherSortMode    = { mode -> vm.setVoucherSortMode(mode) },
                onToggleVoucherUsed  = { vId, used -> vm.toggleVoucherUsed(vId, used) }
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
            val vm: TripViewModel = viewModel(factory = TripViewModel.Factory(repo, tripId))
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
                    onDeleteActivity  = { actId ->
                        scope.launch {
                            repo.deleteActivity(actId)
                            vm.refresh()
                        }
                    },
                    onAddActivity     = { navController.navigate(Screen.EditActivity.createRoute(tripId, dayNumber, 0L)) },
                    onMoveActivity    = { from, to ->
                        val acts = day.activities
                        if (from in acts.indices && to in acts.indices) {
                            scope.launch {
                                repo.swapActivityPositions(acts[from].id, from, acts[to].id, to)
                                vm.refresh()
                            }
                        }
                    }
                )
            }
        }

        // ── Editar viagem ────────────────────────────────────────────────────
        composable(
            route     = Screen.EditTrip.route,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) { entry ->
            val tripId = entry.arguments!!.getLong("tripId")
            val vm: EditTripViewModel = viewModel(factory = EditTripViewModel.Factory(repo, tripId))
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
        ) { entry ->
            val tripId    = entry.arguments!!.getLong("tripId")
            val dayNumber = entry.arguments!!.getInt("dayNumber")
            val vm: EditDayViewModel = viewModel(factory = EditDayViewModel.Factory(repo, tripId, dayNumber))
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
        ) { entry ->
            val tripId     = entry.arguments!!.getLong("tripId")
            val dayNumber  = entry.arguments!!.getInt("dayNumber")
            val activityId = entry.arguments!!.getLong("activityId")
            val vm: EditActivityViewModel = viewModel(factory = EditActivityViewModel.Factory(repo, tripId, dayNumber, activityId))
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
        ) { entry ->
            val tripId    = entry.arguments!!.getLong("tripId")
            val contactId = entry.arguments!!.getLong("contactId")
            val vm: EditContactViewModel = viewModel(factory = EditContactViewModel.Factory(repo, tripId, contactId))
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
        ) { entry ->
            val tripId    = entry.arguments!!.getLong("tripId")
            val voucherId = entry.arguments!!.getLong("voucherId")
            val vm: EditVoucherViewModel = viewModel(factory = EditVoucherViewModel.Factory(repo, tripId, voucherId))
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
        ) { entry ->
            val tripId = entry.arguments!!.getLong("tripId")
            val passId = entry.arguments!!.getLong("passId")
            val vm: EditBoardingPassViewModel = viewModel(factory = EditBoardingPassViewModel.Factory(repo, tripId, passId))
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

// ── PAGER DA VIAGEM ───────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MainPagerScreen(
    tripId: Long,
    tripName: String = "",
    days: List<TravelDay>,
    contacts: List<Contact>,
    vouchers: List<Voucher>,
    boardingPasses: List<BoardingPass>,
    hotelName: String = "",
    hotelAddress: String = "",
    hotelPhone: String = "",
    tripLat: Double? = null,
    tripLon: Double? = null,
    tripStartDate: String? = null,
    tripEndDate: String? = null,
    refreshKey: Long = 0L,
    onDayClick: (Int) -> Unit,
    onBustourMapClick: () -> Unit,
    onShareTrip: () -> Unit = {},
    onEditTrip: () -> Unit = {},
    onAddContact: () -> Unit = {},
    onAddVoucher: () -> Unit = {},
    onAddBoardingPass: () -> Unit = {},
    onEditContact: (Long) -> Unit = {},
    onEditVoucher: (Long) -> Unit = {},
    onEditBoardingPass: (Long) -> Unit = {},
    savedVoucherSortMode: String = "BY_CATEGORY",
    onReorderVouchers: (List<Voucher>) -> Unit = {},
    onDeleteVoucher: (Long) -> Unit = {},
    onVoucherSortMode: (VoucherSortMode) -> Unit = {},
    onToggleVoucherUsed: (Long, Boolean) -> Unit = { _, _ -> }
) {
    val pagerState        = rememberPagerState(pageCount = { TAB_ICONS.size })
    val coroutineScope    = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior    = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var voucherSortMode   by remember(savedVoucherSortMode) {
        mutableStateOf(
            VoucherSortMode.entries.find { it.name == savedVoucherSortMode } ?: VoucherSortMode.BY_CATEGORY
        )
    }
    var showSortMenu      by remember { mutableStateOf(false) }

    LaunchedEffect(refreshKey) {
        if (refreshKey > 0L) snackbarHostState.showSnackbar("Alterações salvas ✓")
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = tripName,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        color      = Color.White
                    )
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
                                    onClick  = { voucherSortMode = VoucherSortMode.BY_CATEGORY; onVoucherSortMode(VoucherSortMode.BY_CATEGORY); showSortMenu = false }
                                )
                                SortMenuItem(
                                    label    = "Por pessoa",
                                    selected = voucherSortMode == VoucherSortMode.BY_PERSON,
                                    onClick  = { voucherSortMode = VoucherSortMode.BY_PERSON; onVoucherSortMode(VoucherSortMode.BY_PERSON); showSortMenu = false }
                                )
                                SortMenuItem(
                                    label    = "Por dia da viagem",
                                    selected = voucherSortMode == VoucherSortMode.BY_DAY,
                                    onClick  = { voucherSortMode = VoucherSortMode.BY_DAY; onVoucherSortMode(VoucherSortMode.BY_DAY); showSortMenu = false }
                                )
                            }
                        }
                    }
                    IconButton(onClick = onShareTrip) {
                        Icon(Icons.Default.Share, contentDescription = "Compartilhar viagem")
                    }
                    IconButton(onClick = onEditTrip) {
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
                1    -> onAddVoucher
                2    -> onAddBoardingPass
                3    -> onAddContact
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
                0 -> HomeScreen(days = days, hotelName = hotelName, hotelAddress = hotelAddress, hotelPhone = hotelPhone, tripLat = tripLat, tripLon = tripLon, tripStartDate = tripStartDate, tripEndDate = tripEndDate, contentPadding = innerPadding, onDayClick = onDayClick)
                1 -> VouchersScreen(vouchers = vouchers, contentPadding = innerPadding, sortMode = voucherSortMode, onEditVoucher = onEditVoucher, onReorderVouchers = onReorderVouchers, onDeleteVoucher = onDeleteVoucher, onToggleUsed = onToggleVoucherUsed)
                2 -> BoardingPassScreen(passes = boardingPasses, contentPadding = innerPadding, onEditBoardingPass = onEditBoardingPass)
                3 -> ContactsScreen(contacts = contacts, contentPadding = innerPadding, onEditContact = onEditContact)
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
