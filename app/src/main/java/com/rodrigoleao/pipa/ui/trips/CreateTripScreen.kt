package com.rodrigoleao.pipa.ui.trips

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodrigoleao.pipa.data.ai.ItineraryGenerator
import com.rodrigoleao.pipa.data.weather.GeocodingResult
import com.rodrigoleao.pipa.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.rodrigoleao.pipa.R
import com.rodrigoleao.pipa.ui.components.CoverPicker
import com.rodrigoleao.pipa.ui.components.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripScreen(
    viewModel: CreateTripViewModel,
    onBack: () -> Unit,
    onTripCreated: (Long) -> Unit
) {
    val form               by viewModel.form.collectAsStateWithLifecycle()
    val createdId          by viewModel.createdTripId.collectAsStateWithLifecycle()
    val readyToNavigate    by viewModel.readyToNavigate.collectAsStateWithLifecycle()
    val searchResults      by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching        by viewModel.isSearching.collectAsStateWithLifecycle()
    val hotelSearchResults by viewModel.hotelSearchResults.collectAsStateWithLifecycle()
    val isHotelSearching   by viewModel.isHotelSearching.collectAsStateWithLifecycle()
    val chatMessages       by viewModel.chatMessages.collectAsStateWithLifecycle()
    val chatInput          by viewModel.chatInput.collectAsStateWithLifecycle()
    val chatPhase          by viewModel.chatPhase.collectAsStateWithLifecycle()
    val generatedDays      by viewModel.generatedDays.collectAsStateWithLifecycle()
    val canGenerate        by viewModel.canGenerate.collectAsStateWithLifecycle()
    val importError        by viewModel.importError.collectAsStateWithLifecycle()
    val importJsonText     by viewModel.importJsonText.collectAsStateWithLifecycle()
    val cameFromImport     by viewModel.cameFromImport.collectAsStateWithLifecycle()
    val chatLimitReached   by viewModel.chatLimitReached.collectAsStateWithLifecycle()
    val canStartChat       by viewModel.canStartConversation.collectAsStateWithLifecycle()

    var step          by remember { mutableIntStateOf(0) }
    var showHelpSheet by remember { mutableStateOf(false) }
    val sheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Quando a viagem é criada, avança para o Step 4 e inicia o chat
    LaunchedEffect(createdId) {
        if (createdId != null) {
            viewModel.initChat()
            step = 3
        }
    }

    // Navega para a viagem quando o Step 4 é concluído (salvar ou pular)
    LaunchedEffect(readyToNavigate) {
        if (readyToNavigate) createdId?.let { onTripCreated(it) }
    }

    val step4Title = when (chatPhase) {
        ChatPhase.IMPORTING               -> stringResource(R.string.create_import_itinerary)
        ChatPhase.CHATTING, ChatPhase.GENERATING -> stringResource(R.string.create_chat_ai)
        ChatPhase.PREVIEW, ChatPhase.SAVING      -> stringResource(R.string.create_step_title_generated)
        else                                     -> stringResource(R.string.create_step_title_ai)
    }
    val stepTitles = listOf(
        stringResource(R.string.create_step_title_new),
        stringResource(R.string.create_step_title_dates),
        stringResource(R.string.create_step_title_lodging),
        step4Title
    )

    val showBackInStep4 = chatPhase == ChatPhase.IMPORTING ||
            chatPhase == ChatPhase.CHATTING ||
            chatPhase == ChatPhase.PREVIEW

    Scaffold(
        topBar = {
            Surface(color = GreenMoss) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier          = Modifier.padding(start = 8.dp, top = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step < 3) {
                            IconButton(onClick = { if (step > 0) step-- else onBack() }) {
                                Icon(ImageVector.vectorResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.common_back), tint = Color.White)
                            }
                        } else if (showBackInStep4) {
                            IconButton(onClick = { viewModel.backToChoosing() }) {
                                Icon(ImageVector.vectorResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.common_back), tint = Color.White)
                            }
                        } else {
                            Spacer(Modifier.width(16.dp))
                        }
                        Text(
                            text       = stepTitles[step],
                            style      = MaterialTheme.typography.titleMedium,
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.weight(1f)
                        )
                        if (step == 3 && chatPhase == ChatPhase.CHOOSING) {
                            IconButton(onClick = { showHelpSheet = true }) {
                                Icon(ImageVector.vectorResource(R.drawable.ic_help), contentDescription = stringResource(R.string.create_help_cd), tint = Color.White)
                            }
                        }
                    }
                    StepIndicator(currentStep = step, totalSteps = 4)
                    Spacer(Modifier.height(12.dp))
                }
            }
        },
        containerColor = Sand
    ) { innerPadding ->
        when (step) {
            0 -> Step1Content(
                form            = form,
                searchResults   = searchResults,
                isSearching     = isSearching,
                contentPadding  = innerPadding,
                onUpdateDest    = viewModel::updateDestination,
                onSelectResult  = viewModel::selectResult,
                onDismissSearch = viewModel::dismissSearch,
                onUpdateName    = viewModel::updateName,
                onUpdateCover   = viewModel::updateCover,
                onNext          = { step = 1 }
            )
            1 -> Step2Content(
                form           = form,
                contentPadding = innerPadding,
                onUpdateStart  = viewModel::updateStartDate,
                onUpdateEnd    = viewModel::updateEndDate,
                onNext         = { step = 2 }
            )
            2 -> Step3Content(
                form                 = form,
                hotelSearchResults   = hotelSearchResults,
                isHotelSearching     = isHotelSearching,
                contentPadding       = innerPadding,
                onUpdateHotelName    = viewModel::updateHotelName,
                onUpdateHotelAddress = viewModel::updateHotelAddress,
                onSelectHotelResult  = viewModel::selectHotelResult,
                onDismissHotelSearch = viewModel::dismissHotelSearch,
                onUpdateHotelPhone   = viewModel::updateHotelPhone,
                onCreate             = viewModel::createTrip
            )
            else -> Step4Content(
                form             = form,
                messages         = chatMessages,
                input            = chatInput,
                phase            = chatPhase,
                generatedDays    = generatedDays,
                canGenerate      = canGenerate,
                importError      = importError,
                importPrompt     = remember(form) { viewModel.buildImportPrompt() },
                importJsonText   = importJsonText,
                cameFromImport   = cameFromImport,
                chatLimitReached = chatLimitReached,
                canStartChat     = canStartChat,
                buildExport      = viewModel::buildConversationExport,
                contentPadding   = innerPadding,
                onStartImport    = viewModel::startImport,
                onStartChat      = viewModel::startChat,
                onImport         = viewModel::importFromJson,
                onUpdateJsonText = viewModel::updateImportJsonText,
                onInputChange    = viewModel::updateChatInput,
                onSend           = viewModel::sendChatMessage,
                onGenerate       = viewModel::generateItinerary,
                onSave           = viewModel::saveItinerary,
                onBackToChat     = viewModel::backToChat,
                onBackToImport   = viewModel::backToImport,
                onSkip           = viewModel::skipItinerary
            )
        }
    }

    if (showHelpSheet) {
        ModalBottomSheet(
            onDismissRequest  = { showHelpSheet = false },
            sheetState        = sheetState,
            containerColor    = SurfaceWhite,
            shape             = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            ItineraryHelpSheet()
        }
    }
}

// ── HELP BOTTOM SHEET ─────────────────────────────────────────────────────────

@Composable
private fun ItineraryHelpSheet() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Título
        Text(
            text       = stringResource(R.string.create_help_title),
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary
        )

        Text(
            text       = stringResource(R.string.create_help_intro),
            fontSize   = 14.sp,
            color      = TextSecondary,
            lineHeight = 20.sp
        )

        // Opção 1: Importar
        HelpOption(
            iconBg      = AmberPrimary.copy(alpha = 0.10f),
            icon        = ImageVector.vectorResource(R.drawable.ic_file_upload),
            iconTint    = AmberPrimary,
            title       = stringResource(R.string.create_import_itinerary),
            description = stringResource(R.string.create_help_import_desc)
        )

        // Opção 2: Chat com IA
        HelpOption(
            iconBg      = GreenMoss.copy(alpha = 0.10f),
            icon        = ImageVector.vectorResource(R.drawable.ic_auto_awesome),
            iconTint    = GreenMoss,
            title       = stringResource(R.string.create_chat_ai),
            description = stringResource(R.string.create_help_chat_desc)
        )

        // Dica
        Surface(
            shape  = RoundedCornerShape(12.dp),
            color  = GreenMoss.copy(alpha = 0.07f),
            border = BorderStroke(1.dp, GreenMoss.copy(alpha = 0.15f))
        ) {
            Row(
                modifier              = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.Top
            ) {
                Text("💡", fontSize = 16.sp)
                Text(
                    text       = stringResource(R.string.create_help_tip),
                    fontSize   = 13.sp,
                    color      = TextPrimary,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun HelpOption(
    iconBg: Color,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Box(
            modifier         = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
            Text(description, fontSize = 13.sp, color = TextSecondary, lineHeight = 19.sp)
        }
    }
}

// ── STEP INDICATOR ─────────────────────────────────────────────────────────────

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier              = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(totalSteps) { i ->
            val active = i == currentStep
            val done   = i < currentStep
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            done   -> AmberPrimary
                            active -> Color.White
                            else   -> Color.White.copy(alpha = 0.30f)
                        }
                    )
            )
        }
    }
}

// ── PASSO 1: Identidade ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step1Content(
    form: CreateTripForm,
    searchResults: List<GeocodingResult>,
    isSearching: Boolean,
    contentPadding: PaddingValues,
    onUpdateDest: (String) -> Unit,
    onSelectResult: (GeocodingResult) -> Unit,
    onDismissSearch: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateCover: (String) -> Unit,
    onNext: () -> Unit
) {
    val canProceed   = form.destination.isNotBlank() && form.name.isNotBlank() && form.coverImage.isNotEmpty()
    val dropdownOpen = searchResults.isNotEmpty()
    val hasCoords    = form.latitude != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionLabel(stringResource(R.string.create_label_destination))
        ExposedDropdownMenuBox(
            expanded         = dropdownOpen,
            onExpandedChange = { if (!it) onDismissSearch() }
        ) {
            OutlinedTextField(
                value         = form.destination,
                onValueChange = onUpdateDest,
                modifier      = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                placeholder   = { Text(stringResource(R.string.create_dest_placeholder)) },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                colors        = tripFieldColors(),
                leadingIcon   = {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GreenSage, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_location), contentDescription = null,
                            tint     = if (hasCoords) GreenMoss else TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                supportingText = if (hasCoords) {
                    { Text(stringResource(R.string.create_location_confirmed), fontSize = 11.sp, color = GreenMoss) }
                } else null
            )
            ExposedDropdownMenu(
                expanded         = dropdownOpen,
                onDismissRequest = onDismissSearch,
                modifier         = Modifier.exposedDropdownSize(false)
            ) {
                searchResults.forEach { result ->
                    DropdownMenuItem(
                        leadingIcon = { Text("📍", fontSize = 16.sp) },
                        text = {
                            Column {
                                Text(result.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(
                                    text  = listOfNotNull(result.admin1, result.country).joinToString(", "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        },
                        onClick        = { onSelectResult(result) },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        SectionLabel(stringResource(R.string.create_label_trip_name))
        OutlinedTextField(
            value         = form.name,
            onValueChange = onUpdateName,
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = { Text(stringResource(R.string.create_trip_name_placeholder)) },
            singleLine    = true,
            shape         = RoundedCornerShape(12.dp),
            colors        = tripFieldColors()
        )

        SectionLabel(stringResource(R.string.create_label_cover))
        CoverPicker(
            selectedId = form.coverImage,
            onSelect   = { onUpdateCover(it.id) }
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick  = onNext,
            enabled  = canProceed,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = AmberPrimary,
                disabledContainerColor = AmberPrimary.copy(alpha = 0.35f)
            )
        ) {
            Text(stringResource(R.string.create_next), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = GreenMoss)
        }
    }
}

// ── PASSO 2: Datas ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step2Content(
    form: CreateTripForm,
    contentPadding: PaddingValues,
    onUpdateStart: (String) -> Unit,
    onUpdateEnd: (String) -> Unit,
    onNext: () -> Unit
) {
    val rangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = form.startDate?.let { LocalDate.parse(it).toEpochDay() * 86_400_000L },
        initialSelectedEndDateMillis   = form.endDate?.let { LocalDate.parse(it).toEpochDay() * 86_400_000L }
    )

    // Sync picker → form on every selection change
    val startMillis = rangePickerState.selectedStartDateMillis
    val endMillis   = rangePickerState.selectedEndDateMillis
    LaunchedEffect(startMillis, endMillis) {
        startMillis?.let { onUpdateStart(Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toString()) }
        endMillis?.let   { onUpdateEnd(Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toString()) }
    }

    val dayCount = remember(startMillis, endMillis) {
        if (startMillis == null || endMillis == null) 0
        else {
            val s = Instant.ofEpochMilli(startMillis).atZone(ZoneId.of("UTC")).toLocalDate()
            val e = Instant.ofEpochMilli(endMillis).atZone(ZoneId.of("UTC")).toLocalDate()
            generateSequence(s) { it.plusDays(1) }.takeWhile { !it.isAfter(e) }.count()
        }
    }

    val canProceed = startMillis != null && endMillis != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        // ── Calendário inline ─────────────────────────────────────────────────
        Surface(
            modifier  = Modifier
                .fillMaxWidth()
                .weight(1f),
            color     = SurfaceWhite,
            tonalElevation = 0.dp
        ) {
            DateRangePicker(
                state          = rangePickerState,
                modifier       = Modifier.fillMaxSize(),
                showModeToggle = false,
                title = {
                    Text(
                        text     = stringResource(R.string.create_date_picker_hint),
                        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 4.dp),
                        style    = MaterialTheme.typography.labelMedium,
                        color    = TextSecondary
                    )
                },
                colors = DatePickerDefaults.colors(
                    containerColor                    = SurfaceWhite,
                    selectedDayContainerColor         = AmberPrimary,
                    selectedDayContentColor           = GreenMoss,
                    todayContentColor                 = GreenMoss,
                    todayDateBorderColor              = GreenMoss,
                    dayInSelectionRangeContainerColor = AmberPrimary.copy(alpha = 0.28f),
                    dayInSelectionRangeContentColor   = GreenMoss,
                    selectedYearContainerColor        = AmberPrimary,
                    selectedYearContentColor          = GreenMoss,
                    currentYearContentColor           = GreenMoss,
                    headlineContentColor              = GreenMoss,
                    subheadContentColor               = TextSecondary
                )
            )
        }

        // ── Rodapé: resumo + botão ────────────────────────────────────────────
        Surface(
            color  = Sand,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Chip de resumo
                if (canProceed) {
                    val fmt = DateTimeFormatter.ofPattern("d MMM", Locale("pt", "BR"))
                    val startLabel = Instant.ofEpochMilli(startMillis!!).atZone(ZoneId.of("UTC")).toLocalDate().format(fmt)
                    val endLabel   = Instant.ofEpochMilli(endMillis!!).atZone(ZoneId.of("UTC")).toLocalDate().format(fmt)
                    Surface(
                        shape  = RoundedCornerShape(24.dp),
                        color  = GreenMoss.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, GreenMoss.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_calendar), contentDescription = null, tint = GreenMoss, modifier = Modifier.size(16.dp))
                            Text(
                                text  = "$startLabel  →  $endLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GreenMoss
                            )
                            Spacer(Modifier.weight(1f))
                            Surface(shape = RoundedCornerShape(12.dp), color = GreenMoss) {
                                Text(
                                    text     = pluralStringResource(R.plurals.create_day_count, dayCount, dayCount),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    style    = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color    = Color.White
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick  = onNext,
                    enabled  = canProceed,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = AmberPrimary,
                        disabledContainerColor = AmberPrimary.copy(alpha = 0.35f)
                    )
                ) {
                    Text(stringResource(R.string.create_next), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = GreenMoss)
                }
            }
        }
    }
}

// ── PASSO 3: Hospedagem (opcional) ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step3Content(
    form: CreateTripForm,
    hotelSearchResults: List<GeocodingResult>,
    isHotelSearching: Boolean,
    contentPadding: PaddingValues,
    onUpdateHotelName: (String) -> Unit,
    onUpdateHotelAddress: (String) -> Unit,
    onSelectHotelResult: (GeocodingResult) -> Unit,
    onDismissHotelSearch: () -> Unit,
    onUpdateHotelPhone: (String) -> Unit,
    onCreate: () -> Unit
) {
    val dropdownOpen = hotelSearchResults.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text  = stringResource(R.string.create_hotel_intro),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        SectionLabel(stringResource(R.string.create_label_hotel_name))
        OutlinedTextField(
            value         = form.hotelName,
            onValueChange = onUpdateHotelName,
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = { Text(stringResource(R.string.create_hotel_name_placeholder)) },
            singleLine    = true,
            shape         = RoundedCornerShape(12.dp),
            colors        = tripFieldColors()
        )

        SectionLabel(stringResource(R.string.create_label_address))
        ExposedDropdownMenuBox(
            expanded         = dropdownOpen,
            onExpandedChange = { if (!it) onDismissHotelSearch() }
        ) {
            OutlinedTextField(
                value         = form.hotelAddress,
                onValueChange = onUpdateHotelAddress,
                modifier      = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                placeholder   = { Text(stringResource(R.string.create_hotel_address_placeholder)) },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                colors        = tripFieldColors(),
                leadingIcon   = {
                    if (isHotelSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GreenSage, strokeWidth = 2.dp)
                    } else {
                        Icon(ImageVector.vectorResource(R.drawable.ic_location), contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                    }
                }
            )
            ExposedDropdownMenu(
                expanded         = dropdownOpen,
                onDismissRequest = onDismissHotelSearch,
                modifier         = Modifier.exposedDropdownSize(false)
            ) {
                hotelSearchResults.forEach { result ->
                    DropdownMenuItem(
                        leadingIcon = { Text("📍", fontSize = 16.sp) },
                        text = {
                            Column {
                                Text(result.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(
                                    text  = listOfNotNull(result.admin1, result.country).joinToString(", "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        },
                        onClick        = { onSelectHotelResult(result) },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        SectionLabel(stringResource(R.string.create_label_phone))
        OutlinedTextField(
            value         = form.hotelPhone,
            onValueChange = onUpdateHotelPhone,
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = { Text(stringResource(R.string.create_hotel_phone_placeholder)) },
            singleLine    = true,
            shape         = RoundedCornerShape(12.dp),
            colors        = tripFieldColors(),
            leadingIcon   = {
                Icon(ImageVector.vectorResource(R.drawable.ic_phone), contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick  = onCreate,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = GreenMoss)
        ) {
            Icon(ImageVector.vectorResource(R.drawable.ic_check), contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.create_create_and_build), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AmberPrimary)
        }
    }
}

// ── PASSO 4: Roteiro com IA ───────────────────────────────────────────────────

@Composable
private fun Step4Content(
    form: CreateTripForm,
    messages: List<ChatMessage>,
    input: String,
    phase: ChatPhase,
    generatedDays: List<ItineraryGenerator.GeneratedDay>,
    canGenerate: Boolean,
    importError: String?,
    importPrompt: String,
    importJsonText: String,
    cameFromImport: Boolean,
    chatLimitReached: Boolean,
    canStartChat: Boolean,
    buildExport: () -> String,
    contentPadding: PaddingValues,
    onStartImport: () -> Unit,
    onStartChat: () -> Unit,
    onImport: (String) -> Unit,
    onUpdateJsonText: (String) -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onGenerate: () -> Unit,
    onSave: () -> Unit,
    onBackToChat: () -> Unit,
    onBackToImport: () -> Unit,
    onSkip: () -> Unit
) {
    when (phase) {
        ChatPhase.CHOOSING -> ChoosingScreen(
            contentPadding = contentPadding,
            canChat        = canStartChat,
            onImport       = onStartImport,
            onChat         = onStartChat,
            onSkip         = onSkip
        )
        ChatPhase.IMPORTING -> ImportScreen(
            importError      = importError,
            importPrompt     = importPrompt,
            importJsonText   = importJsonText,
            contentPadding   = contentPadding,
            onUpdateJsonText = onUpdateJsonText,
            onImport         = onImport
        )
        ChatPhase.PREVIEW -> ItineraryPreview(
            days           = generatedDays,
            contentPadding = contentPadding,
            cameFromImport = cameFromImport,
            onSave         = onSave,
            onBack         = if (cameFromImport) onBackToImport else onBackToChat
        )
        ChatPhase.SAVING -> Box(
            Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator(color = GreenMoss)
                Text(stringResource(R.string.create_saving), color = TextSecondary)
            }
        }
        else -> ChatScreen(
            messages       = messages,
            input          = input,
            isGenerating   = phase == ChatPhase.GENERATING,
            canGenerate    = canGenerate,
            limitReached   = chatLimitReached,
            buildExport    = buildExport,
            contentPadding = contentPadding,
            onInputChange  = onInputChange,
            onSend         = onSend,
            onGenerate     = onGenerate,
            onSkip         = onSkip
        )
    }
}

// ── TELA: Escolha de modo ─────────────────────────────────────────────────────

@Composable
private fun ChoosingScreen(
    contentPadding: PaddingValues,
    canChat: Boolean,
    onImport: () -> Unit,
    onChat: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text       = stringResource(R.string.create_choose_title),
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary
        )
        Text(
            text  = stringResource(R.string.create_choose_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier              = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OptionCard(
                modifier    = Modifier.weight(1f).fillMaxHeight(),
                icon        = ImageVector.vectorResource(R.drawable.ic_file_upload),
                iconTint    = AmberPrimary,
                iconBg      = AmberPrimary.copy(alpha = 0.10f),
                title       = stringResource(R.string.create_import_itinerary),
                description = stringResource(R.string.create_option_import_desc),
                onClick     = onImport
            )
            OptionCard(
                modifier    = Modifier.weight(1f).fillMaxHeight(),
                icon        = ImageVector.vectorResource(R.drawable.ic_auto_awesome),
                iconTint    = GreenMoss,
                iconBg      = GreenMoss.copy(alpha = 0.10f),
                title       = stringResource(R.string.create_chat_ai),
                description = stringResource(R.string.create_option_chat_desc),
                enabled     = canChat,
                onClick     = onChat
            )
        }

        if (!canChat) {
            Text(
                text       = stringResource(R.string.create_daily_limit_note),
                fontSize   = 12.sp,
                lineHeight = 16.sp,
                color      = TextSecondary,
                modifier   = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        TextButton(
            onClick  = onSkip,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(R.string.create_skip), color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun OptionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier  = modifier.alpha(if (enabled) 1f else 0.45f),
        onClick   = onClick,
        enabled   = enabled,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border    = BorderStroke(0.5.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp, lineHeight = 17.sp)
                Text(description, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
            }
        }
    }
}

// ── TELA: Importação de JSON ──────────────────────────────────────────────────

@Composable
private fun ImportScreen(
    importError: String?,
    importPrompt: String,
    importJsonText: String,
    contentPadding: PaddingValues,
    onUpdateJsonText: (String) -> Unit,
    onImport: (String) -> Unit
) {
    val context   = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var wasCopied by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
        onUpdateJsonText(content)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        // ── Seção superior com scroll próprio ─────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instruções
            Surface(
                shape  = RoundedCornerShape(14.dp),
                color  = GreenMoss.copy(alpha = 0.07f),
                border = BorderStroke(1.dp, GreenMoss.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💡", fontSize = 16.sp)
                        Text(stringResource(R.string.create_how_to_import), fontWeight = FontWeight.SemiBold, color = GreenMoss, fontSize = 13.sp)
                    }
                    Text(
                        text       = stringResource(R.string.create_import_instructions),
                        fontSize   = 12.sp,
                        color      = TextPrimary,
                        lineHeight = 18.sp
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(top = 6.dp),
                        thickness = 0.5.dp,
                        color     = GreenMoss.copy(alpha = 0.15f)
                    )
                    Row(
                        verticalAlignment     = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("⚠️", fontSize = 11.sp)
                        Text(
                            text       = stringResource(R.string.create_import_warning),
                            fontSize   = 11.sp,
                            color      = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Botão copiar prompt
            Button(
                onClick  = {
                    clipboard.setText(AnnotatedString(importPrompt))
                    wasCopied = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenMoss)
            ) {
                Icon(
                    imageVector        = if (wasCopied) ImageVector.vectorResource(R.drawable.ic_check) else ImageVector.vectorResource(R.drawable.ic_copy),
                    contentDescription = null,
                    tint               = AmberPrimary,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = if (wasCopied) stringResource(R.string.create_text_copied) else stringResource(R.string.create_copy_instruction),
                    fontWeight = FontWeight.SemiBold,
                    color      = AmberPrimary
                )
            }

            SectionLabel(stringResource(R.string.create_label_paste))

            // Campo de texto ocupa o espaço restante
            OutlinedTextField(
                value         = importJsonText,
                onValueChange = onUpdateJsonText,
                modifier      = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder   = { Text("{ \"days\": [ ... ] }", fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.5f)) },
                shape         = RoundedCornerShape(12.dp),
                colors        = tripFieldColors()
            )

            Spacer(Modifier.height(4.dp))
        }

        // ── Rodapé fixo com botões ────────────────────────────────────────────
        Surface(color = SurfaceWhite, tonalElevation = 2.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (importError != null) {
                    Text(
                        text       = importError,
                        color      = MaterialTheme.colorScheme.error,
                        fontSize   = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick  = { filePicker.launch(arrayOf("application/json", "text/plain", "text/*")) },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, GreenMoss.copy(alpha = 0.4f))
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_upload), contentDescription = null, tint = GreenMoss, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.create_import_file), color = GreenMoss, fontSize = 13.sp)
                    }
                    Button(
                        onClick  = { onImport(importJsonText) },
                        enabled  = importJsonText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = GreenMoss,
                            disabledContainerColor = GreenMoss.copy(alpha = 0.35f)
                        )
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_file_upload), contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.create_import_itinerary), fontWeight = FontWeight.SemiBold, color = AmberPrimary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(
    messages: List<ChatMessage>,
    input: String,
    isGenerating: Boolean,
    canGenerate: Boolean,
    limitReached: Boolean,
    buildExport: () -> String,
    contentPadding: PaddingValues,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onGenerate: () -> Unit,
    onSkip: () -> Unit
) {
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current
    var conversationCopied by remember { mutableStateOf(false) }

    // Envia a mensagem e colapsa o teclado (esconde + tira o foco do campo).
    val submit: () -> Unit = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onSend()
    }

    // Rola até a mensagem mais recente sempre que a lista cresce OU quando o
    // conteúdo da última mensagem muda. Este segundo caso é essencial: quando a
    // resposta da IA substitui o placeholder "digitando…", messages.size não muda
    // (remove o placeholder e adiciona a resposta), só o conteúdo — então observar
    // apenas o tamanho não dispararia o scroll e a resposta ficaria escondida.
    val lastMessage = messages.lastOrNull()
    LaunchedEffect(messages.size, lastMessage?.text, lastMessage?.isLoading) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()               // sobe o input acima do teclado (edge-to-edge não redimensiona a janela)
            .padding(contentPadding)
    ) {
        // ── Mensagens ─────────────────────────────────────────────────────────
        LazyColumn(
            state           = listState,
            modifier        = Modifier.weight(1f),
            contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
        }

        // ── Área inferior ─────────────────────────────────────────────────────
        Surface(color = SurfaceWhite, tonalElevation = 2.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    // Gerando roteiro
                    isGenerating -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = GreenMoss, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.create_building), color = TextSecondary, fontSize = 14.sp)
                    }

                    // Trava dura: limite de tokens da conversa atingido
                    limitReached -> {
                        Text(stringResource(R.string.create_limit_title), fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                        Text(stringResource(R.string.create_limit_body), color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                        Button(
                            onClick  = onGenerate,
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = GreenMoss)
                        ) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_auto_awesome), contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.create_generate_now), fontWeight = FontWeight.SemiBold, color = AmberPrimary)
                        }
                        OutlinedButton(
                            onClick  = {
                                clipboard.setText(AnnotatedString(buildExport()))
                                conversationCopied = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(12.dp),
                            border   = BorderStroke(1.dp, GreenMoss)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = GreenMoss, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(if (conversationCopied) R.string.create_copied else R.string.create_copy_conversation),
                                fontWeight = FontWeight.SemiBold, color = GreenMoss, fontSize = 13.sp
                            )
                        }
                        TextButton(
                            onClick  = onSkip,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(stringResource(R.string.create_skip), color = TextSecondary, fontSize = 12.sp)
                        }
                    }

                    // Fluxo normal: (botão gerar após 1ª troca) + campo de input
                    else -> {
                        if (canGenerate) {
                            Button(
                                onClick  = onGenerate,
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(12.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = GreenMoss)
                            ) {
                                Icon(ImageVector.vectorResource(R.drawable.ic_auto_awesome), contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.create_generate_now), fontWeight = FontWeight.SemiBold, color = AmberPrimary)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value         = input,
                                onValueChange = onInputChange,
                                modifier      = Modifier.weight(1f),
                                placeholder   = { Text(stringResource(R.string.create_message_placeholder), fontSize = 14.sp) },
                                singleLine    = false,
                                maxLines      = 4,
                                shape         = RoundedCornerShape(20.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { submit() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor      = GreenMoss,
                                    unfocusedBorderColor    = CardBorder,
                                    focusedContainerColor   = SurfaceWhite,
                                    unfocusedContainerColor = SurfaceWhite,
                                    cursorColor             = GreenMoss
                                )
                            )
                            IconButton(
                                onClick  = submit,
                                enabled  = input.isNotBlank(),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (input.isNotBlank()) GreenMoss else CardBorder)
                            ) {
                                Icon(
                                    ImageVector.vectorResource(R.drawable.ic_send),
                                    contentDescription = stringResource(R.string.create_send),
                                    tint     = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        TextButton(
                            onClick  = onSkip,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(stringResource(R.string.create_skip), color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == ChatRole.USER
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(GreenMoss),
                contentAlignment = Alignment.Center
            ) {
                Text("✨", fontSize = 16.sp)
            }
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape    = RoundedCornerShape(
                topStart     = if (isUser) 18.dp else 4.dp,
                topEnd       = if (isUser) 4.dp  else 18.dp,
                bottomStart  = 18.dp,
                bottomEnd    = 18.dp
            ),
            color = if (isUser) GreenMoss else SurfaceWhite,
            tonalElevation = if (isUser) 0.dp else 1.dp
        ) {
            if (msg.isLoading) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(TextSecondary.copy(alpha = 0.4f))
                        )
                    }
                }
            } else if (isUser) {
                Text(
                    text     = msg.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    fontSize = 14.sp,
                    color    = Color.White,
                    lineHeight = 20.sp
                )
            } else {
                MarkdownText(
                    text     = msg.text,
                    color    = TextPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun ItineraryPreview(
    days: List<ItineraryGenerator.GeneratedDay>,
    contentPadding: PaddingValues,
    cameFromImport: Boolean,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Cabeçalho
        Surface(color = GreenMoss.copy(alpha = 0.08f)) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(ImageVector.vectorResource(R.drawable.ic_auto_awesome), contentDescription = null, tint = GreenMoss, modifier = Modifier.size(22.dp))
                Column {
                    Text(stringResource(R.string.create_itinerary_generated), fontWeight = FontWeight.Bold, color = GreenMoss)
                    Text(stringResource(R.string.create_review_and_save), fontSize = 12.sp, color = TextSecondary)
                }
            }
        }

        // Lista de dias gerados
        LazyColumn(
            modifier       = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, top = 52.dp, bottom = 12.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(days) { day ->
                Card(
                    shape  = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Cabeçalho do dia
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(shape = RoundedCornerShape(8.dp), color = GreenMoss) {
                                Text(
                                    stringResource(R.string.create_day_number, day.dayNumber),
                                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White
                                )
                            }
                            Text(day.title, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                        }

                        if (!day.dayAlert.isNullOrBlank()) {
                            Text(
                                "⚠ ${day.dayAlert}",
                                fontSize = 12.sp,
                                color    = AmberPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                        // Atividades
                        day.activities.forEach { act ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment     = Alignment.Top
                            ) {
                                Text(act.time, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.width(44.dp))
                                Text(act.emoji, fontSize = 14.sp)
                                Column {
                                    Text(act.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                    if (act.detail.isNotBlank()) {
                                        Text(act.detail, fontSize = 11.sp, color = TextSecondary, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Ações
        Surface(color = SurfaceWhite, tonalElevation = 2.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick  = onSave,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = GreenMoss)
                ) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_check), contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.create_save_itinerary), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AmberPrimary)
                }
                TextButton(
                    onClick  = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text  = if (cameFromImport) stringResource(R.string.create_back_to_import) else stringResource(R.string.create_back_to_chat),
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

// ── COMPONENTES AUXILIARES ────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text.uppercase(),
        fontSize      = 10.sp,
        color         = GreenMoss,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 1.5.sp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun tripFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = GreenMoss,
    unfocusedBorderColor    = CardBorder,
    focusedLabelColor       = GreenMoss,
    cursorColor             = GreenMoss,
    focusedContainerColor   = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite
)

private fun String.before(other: String): Boolean = this < other
