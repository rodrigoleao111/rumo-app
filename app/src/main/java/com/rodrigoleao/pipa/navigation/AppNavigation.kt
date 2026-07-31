package com.rodrigoleao.pipa.navigation

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
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Sort
import com.rodrigoleao.pipa.ui.vouchers.VoucherSortMode
import androidx.compose.material3.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.graphics.BlurMaskFilter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rodrigoleao.pipa.data.model.BoardingPass
import com.rodrigoleao.pipa.data.model.Contact
import com.rodrigoleao.pipa.data.model.Note
import com.rodrigoleao.pipa.data.model.TravelDay
import com.rodrigoleao.pipa.data.model.Voucher
import com.rodrigoleao.pipa.data.repository.TripData
import com.rodrigoleao.pipa.ui.boarding.BoardingPassScreen
import com.rodrigoleao.pipa.ui.contacts.ContactsScreen
import com.rodrigoleao.pipa.ui.day.DayDetailScreen
import com.rodrigoleao.pipa.ui.home.HomeScreen
import com.rodrigoleao.pipa.ui.theme.*
import com.rodrigoleao.pipa.ui.edit.EditActivityScreen
import com.rodrigoleao.pipa.ui.edit.EditActivityViewModel
import com.rodrigoleao.pipa.ui.edit.EditBoardingPassScreen
import com.rodrigoleao.pipa.ui.edit.EditBoardingPassViewModel
import com.rodrigoleao.pipa.ui.edit.EditContactScreen
import com.rodrigoleao.pipa.ui.edit.EditContactViewModel
import com.rodrigoleao.pipa.ui.edit.EditDayScreen
import com.rodrigoleao.pipa.ui.edit.EditDayViewModel
import com.rodrigoleao.pipa.ui.edit.EditTripScreen
import com.rodrigoleao.pipa.ui.edit.EditTripViewModel
import com.rodrigoleao.pipa.ui.edit.EditVoucherScreen
import com.rodrigoleao.pipa.ui.edit.EditVoucherViewModel
import com.rodrigoleao.pipa.ui.import_trip.ImportTripScreen
import com.rodrigoleao.pipa.ui.import_trip.ImportTripViewModel
import com.rodrigoleao.pipa.ui.notes.DayNotesScreen
import com.rodrigoleao.pipa.ui.notes.NoteEditorScreen
import com.rodrigoleao.pipa.ui.notes.NotesListContent
import com.rodrigoleao.pipa.ui.notes.NoteEditorViewModel
import com.rodrigoleao.pipa.ui.notes.NotesListViewModel
import com.rodrigoleao.pipa.ui.splash.SplashScreen
import com.rodrigoleao.pipa.ui.share_trip.ShareTripScreen
import com.rodrigoleao.pipa.ui.share_trip.ShareTripViewModel
import com.rodrigoleao.pipa.ui.trips.CreateTripScreen
import com.rodrigoleao.pipa.ui.trips.CreateTripViewModel
import com.rodrigoleao.pipa.ui.trips.TripsListScreen
import com.rodrigoleao.pipa.ui.trips.TripsListViewModel
import com.rodrigoleao.pipa.ui.trips.TripViewModel
import com.rodrigoleao.pipa.ui.vouchers.VouchersScreen
import com.rodrigoleao.pipa.ui.settings.SettingsScreen
import com.rodrigoleao.pipa.ui.settings.SettingsViewModel
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.stringResource
import com.rodrigoleao.pipa.R

// ── ROTAS ─────────────────────────────────────────────────────────────────────

sealed class Screen(val route: String) {
    object TripsList  : Screen("trips_list")
    object TripMain   : Screen("trip/{tripId}/main") {
        fun createRoute(tripId: Long) = "trip/$tripId/main"
    }
    object DayDetail  : Screen("trip/{tripId}/day/{dayId}") {
        fun createRoute(tripId: Long, dayId: Int) = "trip/$tripId/day/$dayId"
    }
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
    object NoteEditor : Screen("trip/{tripId}/note/{noteId}") {
        fun createRoute(tripId: Long, noteId: Long) = "trip/$tripId/note/$noteId"
    }
    object DayNotes : Screen("trip/{tripId}/day/{dayNumber}/notes") {
        fun createRoute(tripId: Long, dayNumber: Int) = "trip/$tripId/day/$dayNumber/notes"
    }
    object Splash     : Screen("splash")
    object ImportTrip : Screen("import_trip")
    object ShareTrip  : Screen("trip/{tripId}/share") {
        fun createRoute(tripId: Long) = "trip/$tripId/share"
    }
    object Settings   : Screen("settings")
}

private val TAB_ICON_RES = listOf(R.drawable.ic_home, R.drawable.ic_ticket, R.drawable.ic_boarding, R.drawable.ic_contacts, R.drawable.ic_notes_nav)
private val TAB_LABEL_RES = listOf(R.string.nav_tab_home, R.string.nav_tab_vouchers, R.string.nav_tab_boarding, R.string.nav_tab_contacts, R.string.nav_tab_notes)
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
            val generalNotes by vm.generalNotes.collectAsStateWithLifecycle()

            val refreshKey by entry.savedStateHandle
                .getStateFlow("refresh", 0L)
                .collectAsStateWithLifecycle()
            LaunchedEffect(refreshKey) { if (refreshKey > 0L) vm.refresh() }

            MainPagerScreen(
                state = TripScreenState(
                    tripData              = tripData,
                    refreshKey            = refreshKey,
                    showEmergencyContacts = showEmergencyContacts,
                    generalNotes          = generalNotes
                ),
                actions = TripScreenActions(
                    onDayClick          = { dayId -> navController.navigate(Screen.DayDetail.createRoute(tripId, dayId)) },
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
                    onAddNote               = { vm.createGeneralNote { noteId -> navController.navigate(Screen.NoteEditor.createRoute(tripId, noteId)) } },
                    onOpenNote              = { noteId -> navController.navigate(Screen.NoteEditor.createRoute(tripId, noteId)) },
                    onDeleteNote            = { noteId -> vm.deleteNote(noteId) },
                    onReorderNotes          = { list -> vm.reorderNotes(list) },
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
            val dayNoteCounts by vm.dayNoteCounts.collectAsStateWithLifecycle()

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
                    onEditDay         = { navController.navigate(Screen.EditDay.createRoute(tripId, dayNumber)) },
                    onEditActivity    = { actId -> navController.navigate(Screen.EditActivity.createRoute(tripId, dayNumber, actId)) },
                    onDeleteActivity  = { actId -> vm.deleteActivity(actId) },
                    onAddActivity     = { navController.navigate(Screen.EditActivity.createRoute(tripId, dayNumber, 0L)) },
                    onMoveActivity    = { from, to ->
                        val acts = day.activities
                        if (from in acts.indices && to in acts.indices) {
                            vm.swapActivityPositions(acts[from].id, from, acts[to].id, to)
                        }
                    },
                    onOpenDayNotes    = { navController.navigate(Screen.DayNotes.createRoute(tripId, dayNumber)) },
                    dayNotesCount     = dayNoteCounts[dayNumber] ?: 0
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

        // ── Editor de nota (F4) ──────────────────────────────────────────────
        composable(
            route     = Screen.NoteEditor.route,
            arguments = listOf(
                navArgument("tripId") { type = NavType.LongType },
                navArgument("noteId") { type = NavType.LongType }
            )
        ) {
            val vm: NoteEditorViewModel = hiltViewModel()
            NoteEditorScreen(
                viewModel = vm,
                onBack    = {
                    // avisa a lista (aba geral ou notas do dia) para recarregar
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh", System.currentTimeMillis())
                    navController.popBackStack()
                }
            )
        }

        // ── Notas de um dia (F4) ─────────────────────────────────────────────
        composable(
            route     = Screen.DayNotes.route,
            arguments = listOf(
                navArgument("tripId")    { type = NavType.LongType },
                navArgument("dayNumber") { type = NavType.IntType }
            )
        ) { entry ->
            val tripId    = entry.arguments!!.getLong("tripId")
            val dayNumber = entry.arguments!!.getInt("dayNumber")
            val vm: NotesListViewModel = hiltViewModel()

            val refreshKey by entry.savedStateHandle
                .getStateFlow("refresh", 0L)
                .collectAsStateWithLifecycle()
            LaunchedEffect(refreshKey) { if (refreshKey > 0L) vm.refresh() }

            DayNotesScreen(
                viewModel  = vm,
                dayLabel   = stringResource(R.string.nav_day_label, dayNumber),
                onOpenNote = { noteId -> navController.navigate(Screen.NoteEditor.createRoute(tripId, noteId)) },
                onBack     = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh", System.currentTimeMillis())
                    navController.popBackStack()
                }
            )
        }

    }
}

// ── TRIP SCREEN CONTRACTS ─────────────────────────────────────────────────────

private data class TripScreenState(
    val tripData: TripData?,
    val refreshKey: Long,
    val showEmergencyContacts: Boolean,
    val generalNotes: List<Note>
)

private data class TripScreenActions(
    val onDayClick: (Int) -> Unit,
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
    val onAddNote: () -> Unit,
    val onOpenNote: (Long) -> Unit,
    val onDeleteNote: (Long) -> Unit,
    val onReorderNotes: (List<Note>) -> Unit,
    val onBack: () -> Unit
)

/**
 * Sombra suave desenhada manualmente. O `Modifier.shadow` padrão, com cor
 * tingida e alpha baixo, quase some sobre fundos claros; este desenha um halo
 * borrado (BlurMaskFilter) atrás do elemento, com cor e alcance controláveis.
 */
private fun Modifier.softDropShadow(
    color: Color,
    cornerRadius: Dp,
    blurRadius: Dp,
    offsetY: Dp,
) = this.drawBehind {
    val argb    = color.toArgb()
    val blurPx  = blurRadius.toPx()
    val cornerPx = cornerRadius.toPx()
    val dy      = offsetY.toPx()
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            this.color  = argb
            maskFilter  = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.nativeCanvas.drawRoundRect(
            0f,
            dy,
            size.width,
            size.height + dy,
            cornerPx,
            cornerPx,
            paint,
        )
    }
}

// ── PAGER DA VIAGEM ───────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MainPagerScreen(
    state: TripScreenState,
    actions: TripScreenActions
) {
    val trip              = state.tripData?.trip
    val pagerState        = rememberPagerState(pageCount = { TAB_ICON_RES.size })
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

    val changesSavedMsg = stringResource(R.string.nav_changes_saved)
    LaunchedEffect(state.refreshKey) {
        if (state.refreshKey > 0L) snackbarHostState.showSnackbar(changesSavedMsg)
    }

    Scaffold(
        // A barra verde (com colapso via scrollBehavior) só existe fora da Home;
        // conectar o nestedScroll na Home faria a app bar fantasma "engolir" a rolagem.
        modifier = if (pagerState.currentPage != 0)
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        else
            Modifier,
        topBar = {
            // Página 0 (home) tem cabeçalho de capa próprio — sem a barra verde.
            if (pagerState.currentPage != 0) {
            TopAppBar(
                title = {
                    val titleText = when (pagerState.currentPage) {
                        0    -> trip?.name ?: ""
                        1    -> "${trip?.name ?: ""}  •  ${stringResource(R.string.nav_section_vouchers)}"
                        2    -> "${trip?.name ?: ""}  •  ${stringResource(R.string.nav_section_passes)}"
                        3    -> "${trip?.name ?: ""}  •  ${stringResource(R.string.nav_section_contacts)}"
                        4    -> "${trip?.name ?: ""}  •  ${stringResource(R.string.nav_section_notes)}"
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
                                ImageVector.vectorResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.nav_my_trips),
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
                                    ImageVector.vectorResource(R.drawable.ic_sort),
                                    contentDescription = stringResource(R.string.nav_group_vouchers),
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
                                    label    = stringResource(R.string.nav_sort_by_category),
                                    selected = voucherSortMode == VoucherSortMode.BY_CATEGORY,
                                    onClick  = { voucherSortMode = VoucherSortMode.BY_CATEGORY; actions.onVoucherSortMode(VoucherSortMode.BY_CATEGORY); showSortMenu = false }
                                )
                                SortMenuItem(
                                    label    = stringResource(R.string.nav_sort_by_person),
                                    selected = voucherSortMode == VoucherSortMode.BY_PERSON,
                                    onClick  = { voucherSortMode = VoucherSortMode.BY_PERSON; actions.onVoucherSortMode(VoucherSortMode.BY_PERSON); showSortMenu = false }
                                )
                                SortMenuItem(
                                    label    = stringResource(R.string.nav_sort_by_day),
                                    selected = voucherSortMode == VoucherSortMode.BY_DAY,
                                    onClick  = { voucherSortMode = VoucherSortMode.BY_DAY; actions.onVoucherSortMode(VoucherSortMode.BY_DAY); showSortMenu = false }
                                )
                            }
                        }
                    }
                    IconButton(onClick = actions.onShareTrip) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_share), contentDescription = stringResource(R.string.nav_share_trip))
                    }
                    IconButton(onClick = actions.onEditTrip) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.nav_edit_trip))
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
            }
        },
        floatingActionButton = {
            val fabAction: (() -> Unit)? = when (pagerState.currentPage) {
                1    -> actions.onAddVoucher
                2    -> actions.onAddBoardingPass
                3    -> actions.onAddContact
                4    -> actions.onAddNote
                else -> null
            }
            fabAction?.let { action ->
                FloatingActionButton(
                    onClick        = action,
                    containerColor = AmberPrimary,
                    contentColor   = GreenMoss,
                    shape          = RoundedCornerShape(16.dp)
                ) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_add), contentDescription = stringResource(R.string.common_add))
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = AmberPrimary,
                    contentColor   = GreenMoss
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
                        .softDropShadow(
                            color        = GreenMoss.copy(alpha = 0.45f),
                            cornerRadius = 32.dp,
                            blurRadius   = 18.dp,
                            offsetY      = 5.dp
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(Cream)
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TAB_ICON_RES.forEachIndexed { index, iconRes ->
                        PillNavItem(
                            icon     = ImageVector.vectorResource(iconRes),
                            label    = stringResource(TAB_LABEL_RES[index]),
                            selected = pagerState.currentPage == index,
                            onClick  = { coroutineScope.launch { pagerState.animateScrollToPage(index) } }
                        )
                    }
                }
            }
        },
        containerColor = Sand
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
                    coverImage    = trip?.coverImage ?: "",
                    coverEmoji    = trip?.coverEmoji ?: "",
                    tripName      = trip?.name ?: "",
                    onBack        = actions.onBack,
                    onShareTrip   = actions.onShareTrip,
                    onEditTrip    = actions.onEditTrip,
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
                4 -> NotesListContent(
                    notes          = state.generalNotes,
                    contentPadding = innerPadding,
                    onOpenNote     = actions.onOpenNote,
                    onDeleteNote   = actions.onDeleteNote,
                    onReorderNotes = actions.onReorderNotes
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
            Icon(ImageVector.vectorResource(R.drawable.ic_check), contentDescription = null, tint = GreenMoss, modifier = Modifier.size(16.dp))
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
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 52.dp, height = 36.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) GreenMoss else Color.Transparent)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = if (selected) Color.White else GreenMoss.copy(alpha = 0.45f),
                modifier           = Modifier.size(22.dp)
            )
        }
    }
}
