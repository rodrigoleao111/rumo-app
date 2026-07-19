package com.rodrigoleao.gramado2026.ui.trips

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodrigoleao.gramado2026.data.ai.ItineraryGenerator
import com.rodrigoleao.gramado2026.data.weather.GeocodingResult
import com.rodrigoleao.gramado2026.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val EMOJI_OPTIONS = listOf(
    "⛰️", "🏖️", "🏙️", "🌊", "🌿", "🗺️", "✈️", "🏕️", "🏰", "🎡",
    "🌎", "🚢", "🎭", "🍽️", "🏔️", "🌅", "🎪", "🚂", "🏝️", "🌄"
)

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
        ChatPhase.IMPORTING               -> "Importar roteiro"
        ChatPhase.CHATTING, ChatPhase.GENERATING -> "Chat com IA"
        ChatPhase.PREVIEW, ChatPhase.SAVING      -> "Roteiro gerado"
        else                                     -> "Roteiro com IA"
    }
    val stepTitles = listOf("Nova viagem", "Datas da viagem", "Hospedagem", step4Title)

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
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                            }
                        } else if (showBackInStep4) {
                            IconButton(onClick = { viewModel.backToChoosing() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
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
                                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Ajuda", tint = Color.White)
                            }
                        }
                    }
                    StepIndicator(currentStep = step, totalSteps = 4)
                    Spacer(Modifier.height(12.dp))
                }
            }
        },
        containerColor = GreenLight
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
                onUpdateEmoji   = viewModel::updateEmoji,
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
            text       = "Como montar o roteiro?",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary
        )

        Text(
            text       = "Você tem duas formas de montar o roteiro da viagem. Escolha a que melhor se encaixa no seu momento.",
            fontSize   = 14.sp,
            color      = TextSecondary,
            lineHeight = 20.sp
        )

        // Opção 1: Importar
        HelpOption(
            iconBg      = AmberPrimary.copy(alpha = 0.10f),
            icon        = Icons.Default.FileUpload,
            iconTint    = AmberPrimary,
            title       = "Importar roteiro",
            description = "Ideal se você já tem um roteiro em mente ou encontrou sugestões em algum lugar — um blog, um vídeo, uma conversa com o ChatGPT.\n\nO app gera um texto de instrução que você copia e cola em qualquer IA. Ela devolve o roteiro no formato certo. Depois é só colar aqui e pronto."
        )

        // Opção 2: Chat com IA
        HelpOption(
            iconBg      = GreenMoss.copy(alpha = 0.10f),
            icon        = Icons.Default.AutoAwesome,
            iconTint    = GreenMoss,
            title       = "Chat com IA",
            description = "Ideal se você ainda não tem um roteiro definido e quer montar do zero conversando com a IA do próprio app.\n\nA IA já conhece seu destino, datas e hospedagem. Você conta o perfil da viagem (família, casal, amigos…) e o que prefere fazer, e ela monta um roteiro personalizado diretamente no app."
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
                    text       = "Você pode pular essa etapa e montar o roteiro manualmente depois, direto na tela de cada dia.",
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
    onUpdateEmoji: (String) -> Unit,
    onNext: () -> Unit
) {
    val canProceed   = form.destination.isNotBlank() && form.name.isNotBlank() && form.coverEmoji.isNotEmpty()
    val dropdownOpen = searchResults.isNotEmpty()
    val hasCoords    = form.latitude != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionLabel("Destino")
        ExposedDropdownMenuBox(
            expanded         = dropdownOpen,
            onExpandedChange = { if (!it) onDismissSearch() }
        ) {
            OutlinedTextField(
                value         = form.destination,
                onValueChange = onUpdateDest,
                modifier      = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                placeholder   = { Text("Ex: Gramado, RS") },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                colors        = tripFieldColors(),
                leadingIcon   = {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GreenSage, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Default.LocationOn, contentDescription = null,
                            tint     = if (hasCoords) GreenMoss else TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                supportingText = if (hasCoords) {
                    { Text("📍 Localização confirmada", fontSize = 11.sp, color = GreenMoss) }
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

        SectionLabel("Nome da viagem")
        OutlinedTextField(
            value         = form.name,
            onValueChange = onUpdateName,
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = { Text("Ex: Férias de inverno 2026") },
            singleLine    = true,
            shape         = RoundedCornerShape(12.dp),
            colors        = tripFieldColors()
        )

        SectionLabel("Ícone")
        EmojiPicker(selected = form.coverEmoji, onSelect = onUpdateEmoji)

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
            Text("Próximo →", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
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
                        text     = "Toque no início e depois no fim",
                        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 4.dp),
                        style    = MaterialTheme.typography.labelMedium,
                        color    = TextSecondary
                    )
                },
                colors = DatePickerDefaults.colors(
                    containerColor                    = SurfaceWhite,
                    selectedDayContainerColor         = GreenMoss,
                    selectedDayContentColor           = Color.White,
                    todayContentColor                 = GreenMoss,
                    todayDateBorderColor              = GreenMoss,
                    dayInSelectionRangeContainerColor = GreenMoss.copy(alpha = 0.13f),
                    dayInSelectionRangeContentColor   = GreenMoss,
                    selectedYearContainerColor        = GreenMoss,
                    selectedYearContentColor          = Color.White,
                    currentYearContentColor           = GreenMoss,
                    headlineContentColor              = GreenMoss,
                    subheadContentColor               = TextSecondary
                )
            )
        }

        // ── Rodapé: resumo + botão ────────────────────────────────────────────
        Surface(
            color  = GreenLight,
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
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GreenMoss, modifier = Modifier.size(16.dp))
                            Text(
                                text  = "$startLabel  →  $endLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GreenMoss
                            )
                            Spacer(Modifier.weight(1f))
                            Surface(shape = RoundedCornerShape(12.dp), color = GreenMoss) {
                                Text(
                                    text     = "$dayCount ${if (dayCount == 1) "dia" else "dias"}",
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
                    Text("Próximo →", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
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
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text  = "Preencha os dados da sua hospedagem para facilitar o acesso aos mapas e Uber durante a viagem.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        SectionLabel("Nome da hospedagem")
        OutlinedTextField(
            value         = form.hotelName,
            onValueChange = onUpdateHotelName,
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = { Text("Ex: Hotel Laghetto Stilo") },
            singleLine    = true,
            shape         = RoundedCornerShape(12.dp),
            colors        = tripFieldColors()
        )

        SectionLabel("Endereço")
        ExposedDropdownMenuBox(
            expanded         = dropdownOpen,
            onExpandedChange = { if (!it) onDismissHotelSearch() }
        ) {
            OutlinedTextField(
                value         = form.hotelAddress,
                onValueChange = onUpdateHotelAddress,
                modifier      = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                placeholder   = { Text("Ex: Rua Coberta, Gramado, RS") },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                colors        = tripFieldColors(),
                leadingIcon   = {
                    if (isHotelSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GreenSage, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
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

        SectionLabel("Telefone")
        OutlinedTextField(
            value         = form.hotelPhone,
            onValueChange = onUpdateHotelPhone,
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = { Text("Ex: +55 54 3286-0000") },
            singleLine    = true,
            shape         = RoundedCornerShape(12.dp),
            colors        = tripFieldColors(),
            leadingIcon   = {
                Icon(Icons.Default.Phone, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
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
            Icon(Icons.Default.Check, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Criar viagem e montar roteiro →", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AmberPrimary)
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
                Text("Salvando seu roteiro...", color = TextSecondary)
            }
        }
        else -> ChatScreen(
            messages       = messages,
            input          = input,
            isGenerating   = phase == ChatPhase.GENERATING,
            canGenerate    = canGenerate,
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
    onImport: () -> Unit,
    onChat: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text       = "Como deseja montar o roteiro?",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary
        )
        Text(
            text  = "Converse com a IA do app ou importe um roteiro gerado em qualquer outra ferramenta.",
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
                icon        = Icons.Default.FileUpload,
                iconTint    = AmberPrimary,
                iconBg      = AmberPrimary.copy(alpha = 0.10f),
                title       = "Importar roteiro",
                description = "Traga um roteiro pronto de qualquer lugar",
                onClick     = onImport
            )
            OptionCard(
                modifier    = Modifier.weight(1f).fillMaxHeight(),
                icon        = Icons.Default.AutoAwesome,
                iconTint    = GreenMoss,
                iconBg      = GreenMoss.copy(alpha = 0.10f),
                title       = "Chat com IA",
                description = "Monte o roteiro em conversa com o Gemini",
                onClick     = onChat
            )
        }

        Spacer(Modifier.weight(1f))

        TextButton(
            onClick  = onSkip,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Pular e acessar a viagem", color = TextSecondary, fontSize = 12.sp)
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
    onClick: () -> Unit
) {
    Card(
        modifier  = modifier,
        onClick   = onClick,
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
                        Text("Como importar", fontWeight = FontWeight.SemiBold, color = GreenMoss, fontSize = 13.sp)
                    }
                    Text(
                        text       = "1. Abra qualquer IA (ChatGPT, Gemini…) e monte ou descreva seu roteiro\n2. Com o roteiro finalizado, copie o texto de instrução clicando no botão abaixo e cole na mesma conversa\n3. A IA vai organizar o roteiro no formato que o app entende\n4. Cole a resposta aqui embaixo",
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
                            text       = "Cole a resposta da IA exatamente como ela aparecer, sem editar nada. Qualquer alteração pode causar erro na importação.",
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
                    imageVector        = if (wasCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint               = AmberPrimary,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = if (wasCopied) "Texto copiado!" else "Copiar texto de instrução",
                    fontWeight = FontWeight.SemiBold,
                    color      = AmberPrimary
                )
            }

            SectionLabel("Cole aqui a resposta da IA")

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
                        Icon(Icons.Default.Upload, contentDescription = null, tint = GreenMoss, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Importar arquivo", color = GreenMoss, fontSize = 13.sp)
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
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Importar roteiro", fontWeight = FontWeight.SemiBold, color = AmberPrimary, fontSize = 13.sp)
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
    contentPadding: PaddingValues,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onGenerate: () -> Unit,
    onSkip: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                // Botão de gerar roteiro (aparece após 1ª resposta do usuário)
                if (canGenerate && !isGenerating) {
                    Button(
                        onClick  = onGenerate,
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = GreenMoss)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Gerar roteiro agora", fontWeight = FontWeight.SemiBold, color = AmberPrimary)
                    }
                }

                if (isGenerating) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = GreenMoss, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Montando seu roteiro...", color = TextSecondary, fontSize = 14.sp)
                    }
                } else {
                    // Campo de input
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value         = input,
                            onValueChange = onInputChange,
                            modifier      = Modifier.weight(1f),
                            placeholder   = { Text("Escreva sua mensagem...", fontSize = 14.sp) },
                            singleLine    = false,
                            maxLines      = 4,
                            shape         = RoundedCornerShape(20.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { onSend() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = GreenMoss,
                                unfocusedBorderColor    = CardBorder,
                                focusedContainerColor   = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite,
                                cursorColor             = GreenMoss
                            )
                        )
                        IconButton(
                            onClick  = onSend,
                            enabled  = input.isNotBlank(),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (input.isNotBlank()) GreenMoss else CardBorder)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar",
                                tint     = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    TextButton(
                        onClick  = onSkip,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Pular e acessar a viagem", color = TextSecondary, fontSize = 12.sp)
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
            } else {
                Text(
                    text     = msg.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    fontSize = 14.sp,
                    color    = if (isUser) Color.White else TextPrimary,
                    lineHeight = 20.sp
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
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GreenMoss, modifier = Modifier.size(22.dp))
                Column {
                    Text("Roteiro gerado!", fontWeight = FontWeight.Bold, color = GreenMoss)
                    Text("Revise e salve no app", fontSize = 12.sp, color = TextSecondary)
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
                                    "Dia ${day.dayNumber}",
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
                    Icon(Icons.Default.Check, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Salvar roteiro", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AmberPrimary)
                }
                TextButton(
                    onClick  = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text  = if (cameFromImport) "Voltar à importação" else "Voltar ao chat para ajustar",
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

@Composable
private fun EmojiPicker(selected: String, onSelect: (String) -> Unit) {
    val rows = EMOJI_OPTIONS.chunked(5)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { emoji ->
                    val isSelected = emoji == selected
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onSelect(emoji) },
                        shape  = RoundedCornerShape(12.dp),
                        color  = if (isSelected) AmberPrimary.copy(alpha = 0.15f) else SurfaceWhite,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) AmberPrimary else CardBorder
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }
                // Pad incomplete last row so cells stay equal-width
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
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
