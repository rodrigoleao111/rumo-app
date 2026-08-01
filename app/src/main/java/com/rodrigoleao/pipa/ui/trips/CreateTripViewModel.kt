package com.rodrigoleao.pipa.ui.trips

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.pipa.BuildConfig
import com.rodrigoleao.pipa.R
import dagger.hilt.android.qualifiers.ApplicationContext
import com.rodrigoleao.pipa.data.ai.ItineraryGenerator
import com.rodrigoleao.pipa.data.analytics.AnalyticsService
import com.rodrigoleao.pipa.data.model.AiChatMessage
import com.rodrigoleao.pipa.data.preferences.SettingsRepository
import com.rodrigoleao.pipa.data.repository.AiConversationRepository
import com.rodrigoleao.pipa.data.repository.TripRepository
import com.rodrigoleao.pipa.data.usecase.SaveGeneratedItineraryUseCase
import com.rodrigoleao.pipa.data.weather.GeocodingResult
import com.rodrigoleao.pipa.data.weather.WeatherRepository
import com.rodrigoleao.pipa.ui.components.TripCovers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

// ── Wizard form ───────────────────────────────────────────────────────────────

data class CreateTripForm(
    val name: String         = "",
    val destination: String  = "",
    val coverEmoji: String   = "",
    val coverImage: String   = "",
    val startDate: String?   = null,
    val endDate: String?     = null,
    val latitude: Double?    = null,
    val longitude: Double?   = null,
    val hotelName: String    = "",
    val hotelAddress: String = "",
    val hotelPhone: String   = ""
)

// ── Chat types ────────────────────────────────────────────────────────────────

enum class ChatRole { USER, AI }

data class ChatMessage(
    val role: ChatRole,
    val text: String,
    val isLoading: Boolean = false
)

enum class ChatPhase { CHOOSING, CHATTING, IMPORTING, GENERATING, PREVIEW, SAVING }

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class CreateTripViewModel @Inject constructor(
    private val repo: TripRepository,
    private val saveItineraryUseCase: SaveGeneratedItineraryUseCase,
    private val settingsRepo: SettingsRepository,
    private val aiConversationRepo: AiConversationRepository,
    private val analytics: AnalyticsService,
    @ApplicationContext private val appContext: android.content.Context
) : ViewModel() {

    // ── Wizard form state ─────────────────────────────────────────────────────

    private val _form = MutableStateFlow(CreateTripForm())
    val form: StateFlow<CreateTripForm> = _form.asStateFlow()

    private val _createdTripId = MutableStateFlow<Long?>(null)
    val createdTripId: StateFlow<Long?> = _createdTripId.asStateFlow()

    private val _readyToNavigate = MutableStateFlow(false)
    val readyToNavigate: StateFlow<Boolean> = _readyToNavigate.asStateFlow()

    // ── Destination autocomplete ──────────────────────────────────────────────

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    fun updateDestination(v: String) {
        _form.update { it.copy(destination = v, latitude = null, longitude = null) }
        searchJob?.cancel()
        if (v.length < 2) { _searchResults.value = emptyList(); return }
        searchJob = viewModelScope.launch {
            delay(350)
            _isSearching.value = true
            _searchResults.value = runCatching { WeatherRepository.searchLocations(v) }.getOrDefault(emptyList())
            _isSearching.value = false
        }
    }

    fun selectResult(result: GeocodingResult) {
        searchJob?.cancel()
        _form.update { it.copy(destination = result.displayName, latitude = result.latitude, longitude = result.longitude) }
        _searchResults.value = emptyList()
        _isSearching.value   = false
    }

    fun dismissSearch() {
        searchJob?.cancel()
        _searchResults.value = emptyList()
        _isSearching.value   = false
    }

    // ── Hotel address autocomplete ────────────────────────────────────────────

    private val _hotelSearchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val hotelSearchResults: StateFlow<List<GeocodingResult>> = _hotelSearchResults.asStateFlow()

    private val _isHotelSearching = MutableStateFlow(false)
    val isHotelSearching: StateFlow<Boolean> = _isHotelSearching.asStateFlow()

    private var hotelSearchJob: Job? = null

    fun updateHotelAddress(v: String) {
        _form.update { it.copy(hotelAddress = v) }
        hotelSearchJob?.cancel()
        if (v.length < 2) { _hotelSearchResults.value = emptyList(); return }
        hotelSearchJob = viewModelScope.launch {
            delay(350)
            _isHotelSearching.value = true
            _hotelSearchResults.value = runCatching { WeatherRepository.searchLocations(v) }.getOrDefault(emptyList())
            _isHotelSearching.value = false
        }
    }

    fun selectHotelResult(result: GeocodingResult) {
        hotelSearchJob?.cancel()
        _form.update { it.copy(hotelAddress = result.displayName) }
        _hotelSearchResults.value = emptyList()
        _isHotelSearching.value   = false
    }

    fun dismissHotelSearch() {
        hotelSearchJob?.cancel()
        _hotelSearchResults.value = emptyList()
        _isHotelSearching.value   = false
    }

    // ── Other form fields ─────────────────────────────────────────────────────

    fun updateEmoji(v: String)      { _form.update { it.copy(coverEmoji = v) } }

    /** Seleciona a capa; mantém `coverEmoji` sincronizado com a categoria (fallback na lista). */
    fun updateCover(id: String) {
        _form.update { it.copy(coverImage = id, coverEmoji = TripCovers.emojiFor(id)) }
    }

    fun updateName(v: String)       { _form.update { it.copy(name = v) } }
    fun updateHotelName(v: String)  { _form.update { it.copy(hotelName = v) } }
    fun updateHotelPhone(v: String) { _form.update { it.copy(hotelPhone = v) } }

    fun updateStartDate(v: String) {
        _form.update {
            it.copy(startDate = v, endDate = if (it.endDate != null && it.endDate < v) null else it.endDate)
        }
    }
    fun updateEndDate(v: String) { _form.update { it.copy(endDate = v) } }

    // ── Chat state ────────────────────────────────────────────────────────────

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    private val _chatPhase = MutableStateFlow(ChatPhase.CHATTING)
    val chatPhase: StateFlow<ChatPhase> = _chatPhase.asStateFlow()

    private val _generatedDays = MutableStateFlow<List<ItineraryGenerator.GeneratedDay>>(emptyList())
    val generatedDays: StateFlow<List<ItineraryGenerator.GeneratedDay>> = _generatedDays.asStateFlow()

    val canGenerate: StateFlow<Boolean> = _chatMessages.map { msgs ->
        msgs.count { it.role == ChatRole.USER } >= 1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var generator: ItineraryGenerator? = null

    // Em build DEBUG os limites de IA ficam desligados (facilita testes); a contagem
    // de tokens continua ativa para os logs. Em release, os limites valem normalmente.
    private val limitsEnabled = !BuildConfig.DEBUG

    // ── Orçamento de tokens da conversa (tokens reais via usageMetadata) ─────────
    private val _tokensUsed = MutableStateFlow(0)
    val tokensUsed: StateFlow<Int> = _tokensUsed.asStateFlow()

    private val _chatLimitReached = MutableStateFlow(false)
    val chatLimitReached: StateFlow<Boolean> = _chatLimitReached.asStateFlow()

    private var nudgeGiven          = false   // nudge suave já dado nesta conversa?
    private var conversationCounted = false   // já contou no cap diário nesta conversa?
    private var accumulatedCostBrl  = 0.0     // custo estimado acumulado na conversa (R$)
    private var currentConversationId: Long? = null   // id da conversa persistida (upsert)

    // ── Cap diário de conversas com IA (3/dia/dispositivo) ──────────────────────
    val dailyConversationsUsed: StateFlow<Int> =
        settingsRepo.aiConversationsToday(today())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val canStartConversation: StateFlow<Boolean> =
        dailyConversationsUsed
            .map { !limitsEnabled || it < MAX_DAILY_CONVERSATIONS }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    private val _importJsonText = MutableStateFlow("")
    val importJsonText: StateFlow<String> = _importJsonText.asStateFlow()

    private val _cameFromImport = MutableStateFlow(false)
    val cameFromImport: StateFlow<Boolean> = _cameFromImport.asStateFlow()

    fun updateImportJsonText(text: String) { _importJsonText.value = text }

    fun initChat() {
        val f     = _form.value
        val start = f.startDate ?: return
        val end   = f.endDate   ?: return
        val days  = (LocalDate.parse(end).toEpochDay() - LocalDate.parse(start).toEpochDay() + 1).toInt()

        generator = ItineraryGenerator(
            apiKey = BuildConfig.GEMINI_API_KEY,
            ctx    = ItineraryGenerator.TripContext(
                destination  = f.destination,
                startDate    = start,
                endDate      = end,
                dayCount     = days,
                hotelName    = f.hotelName,
                hotelAddress = f.hotelAddress
            )
        )
        _chatPhase.value = ChatPhase.CHOOSING
    }

    fun startChat() {
        if (!canStartConversation.value) return   // defensivo; a UI já bloqueia o card
        // Reinicia o orçamento a cada nova conversa
        _tokensUsed.value       = 0
        _chatLimitReached.value = false
        nudgeGiven          = false
        conversationCounted = false
        accumulatedCostBrl  = 0.0
        currentConversationId = null
        _chatPhase.value    = ChatPhase.CHATTING
        _chatMessages.value = listOf(ChatMessage(ChatRole.AI, generator?.getInitialGreeting() ?: appContext.getString(R.string.create_default_greeting)))
        Log.d(TAG, "conversa iniciada | dailyConv=${dailyConversationsUsed.value}/$MAX_DAILY_CONVERSATIONS | teto=$MAX_CONVERSATION_TOKENS tk | limits=${if (limitsEnabled) "on" else "off"}")
    }

    fun startImport() {
        _importError.value = null
        _chatPhase.value   = ChatPhase.IMPORTING
    }

    fun backToChoosing() {
        _importError.value = null
        _chatPhase.value   = ChatPhase.CHOOSING
    }

    fun importFromJson(json: String) {
        _importError.value = null
        viewModelScope.launch {
            try {
                val days = ItineraryGenerator.parseJson(json)
                if (days.isEmpty()) throw Exception(appContext.getString(R.string.create_import_no_days))
                _generatedDays.value  = days
                _cameFromImport.value = true
                _chatPhase.value      = ChatPhase.PREVIEW
            } catch (e: Exception) {
                _importError.value = appContext.getString(
                    R.string.create_import_invalid,
                    e.message ?: appContext.getString(R.string.create_import_invalid_fallback)
                )
            }
        }
    }

    fun backToImport() {
        _importError.value = null
        _chatPhase.value   = ChatPhase.IMPORTING
    }

    fun buildImportPrompt(): String {
        val f     = _form.value
        val start = f.startDate ?: return ""
        val end   = f.endDate   ?: return ""
        val days  = (LocalDate.parse(end).toEpochDay() - LocalDate.parse(start).toEpochDay() + 1).toInt()
        val hotel = if (f.hotelName.isNotBlank())
            listOf(f.hotelName, f.hotelAddress).filter { it.isNotBlank() }.joinToString(", ")
        else "não informado"

        return """
Você é um assistente de viagens. Sua única tarefa é produzir um JSON de roteiro para importação em um app mobile.

## Contexto da viagem (já cadastrado no app)
- Destino: ${f.destination}
- Período: $start a $end ($days dias)
- Hospedagem: $hotel

## O que você deve fazer
Use as instruções abaixo conforme o caso:

- Se eu não fornecer nenhum roteiro: crie um roteiro completo para a viagem acima. Peça o perfil dos viajantes se necessário.
- Se eu colar um roteiro em texto livre (de blog, documento ou outra IA): converta-o para o JSON abaixo, preservando as atividades originais. Preencha campos como "address" e "badges" com base no contexto.
- Se eu já trouxer um roteiro estruturado de outra conversa: reformate-o para o JSON abaixo sem alterar o conteúdo.

Em todos os casos, adapte o resultado para exatamente $days dias, com dayNumber de 1 a $days.

## Formato de saída obrigatório

Retorne SOMENTE o JSON a seguir — sem texto antes, sem texto depois, sem bloco markdown, sem explicações.

{
  "days": [
    {
      "dayNumber": 1,
      "title": "Título curto e descritivo do dia",
      "dayAlert": "Alerta importante para o dia, ou null",
      "activities": [
        {
          "time": "09h00",
          "emoji": "🎯",
          "name": "Nome da atividade",
          "detail": "Descrição com dicas práticas",
          "address": "Endereço completo, Cidade, UF",
          "badges": ["FREE"]
        }
      ]
    }
  ]
}

## Regras de preenchimento

- "time": formato HHhMM — ex: "09h00", "14h30"
- "address": endereço completo para Google Maps; use null se não souber
- "dayAlert": texto de alerta visível no topo do dia; use null (sem aspas) se não houver
- "badges": lista com zero ou mais valores entre: FREE, PAID, BOOKED, INCLUDED, WALKING, UBER
  - FREE = gratuito · PAID = pago no local · BOOKED = reservado antecipadamente
  - INCLUDED = incluso na hospedagem/pacote · WALKING = a pé · UBER = recomenda Uber
- Cada dia deve ter entre 3 e 6 atividades com horários realistas e sequenciais
- Qualquer campo desconhecido deve ser null — nunca omita a chave
- Não inclua nenhum texto fora do JSON
        """.trimIndent()
    }

    fun updateChatInput(v: String) { _chatInput.value = v }

    fun sendChatMessage() {
        val text = _chatInput.value.trim().ifEmpty { return }
        if (_chatMessages.value.any { it.isLoading }) return
        if (_chatLimitReached.value) return   // trava dura: não aceita mais mensagens
        _chatInput.value = ""

        // Cap diário: conta a conversa na 1ª mensagem enviada (abrir e só ler não gasta).
        // Em debug (limitsEnabled = false) não conta nem bloqueia.
        if (limitsEnabled && !conversationCounted) {
            conversationCounted = true
            val reachedDailyCap = dailyConversationsUsed.value + 1 >= MAX_DAILY_CONVERSATIONS
            viewModelScope.launch { settingsRepo.incrementAiConversations(today()) }
            if (reachedDailyCap) analytics.logAiLimitReached(AnalyticsService.LIMIT_DAILY_CAP)
        }

        _chatMessages.value = _chatMessages.value +
            ChatMessage(ChatRole.USER, text) +
            ChatMessage(ChatRole.AI, "", isLoading = true)

        viewModelScope.launch {
            val reply     = generator?.sendMessage(text)
            val replyText = reply?.text ?: appContext.getString(R.string.create_assistant_not_initialized)
            _chatMessages.value = _chatMessages.value.dropLast(1) +
                ChatMessage(ChatRole.AI, replyText)

            if (reply != null) {
                analytics.logAiChatMessageSent(
                    totalTokens  = reply.totalTokens,
                    messageIndex = _chatMessages.value.count { it.role == ChatRole.USER }
                )
            }

            // Orçamento de tokens reais: acumula, loga e (se limitsEnabled) decide nudge/trava
            if (reply != null && reply.totalTokens > 0) {
                val used = _tokensUsed.value + reply.totalTokens
                _tokensUsed.value = used
                logAiUsage(reply, used)
                if (limitsEnabled) {
                    when {
                        used >= MAX_CONVERSATION_TOKENS -> {
                            _chatLimitReached.value = true
                            analytics.logAiLimitReached(AnalyticsService.LIMIT_TOKEN_BUDGET)
                        }
                        used >= (MAX_CONVERSATION_TOKENS * NUDGE_RATIO).toInt() && !nudgeGiven -> {
                            nudgeGiven = true
                            _chatMessages.value = _chatMessages.value +
                                ChatMessage(ChatRole.AI, appContext.getString(R.string.create_token_nudge))
                        }
                    }
                }
            }
            persistConversation()
        }
    }

    /** Persiste (upsert) a conversa atual para aparecer em "Minhas conversas com a IA". */
    private fun persistConversation() {
        val msgs = _chatMessages.value.filter { !it.isLoading && it.text.isNotBlank() }
        if (msgs.none { it.role == ChatRole.USER }) return
        val model = msgs.map { AiChatMessage(fromUser = it.role == ChatRole.USER, text = it.text) }
        val f = _form.value
        viewModelScope.launch {
            val id = currentConversationId
            if (id == null) {
                currentConversationId = aiConversationRepo.insert(
                    tripId      = _createdTripId.value,
                    tripName    = f.name,
                    destination = f.destination,
                    startDate   = f.startDate,
                    endDate     = f.endDate,
                    messages    = model,
                    createdAt   = System.currentTimeMillis()
                )
            } else {
                aiConversationRepo.updateMessages(id, model)
            }
        }
    }

    /** Loga consumo e estatísticas (tokens, latência, custo em R$) no Logcat (tag "PipaAiUsage"). */
    private fun logAiUsage(reply: ItineraryGenerator.ChatReply, used: Int) {
        accumulatedCostBrl += reply.costBrl
        val turns = _chatMessages.value.count { it.role == ChatRole.USER }
        val pct   = used * 100 / MAX_CONVERSATION_TOKENS
        val state = when {
            !limitsEnabled                                         -> "OFF(debug)"
            used >= MAX_CONVERSATION_TOKENS                        -> "LIMIT"
            used >= (MAX_CONVERSATION_TOKENS * NUDGE_RATIO).toInt() -> "NUDGE"
            else                                                   -> "OK"
        }
        Log.d(
            TAG,
            "turn=$turns | ${reply.latencyMs}ms | +${reply.totalTokens} tk (prompt ${reply.promptTokens}/out ${reply.candidatesTokens}) | " +
                "total=$used/$MAX_CONVERSATION_TOKENS tk ($pct%) | " +
                "~R$ ${String.format(Locale.US, "%.4f", reply.costBrl)} (acum R$ ${String.format(Locale.US, "%.4f", accumulatedCostBrl)}) | " +
                "budget=$state | dailyConv=${dailyConversationsUsed.value}/$MAX_DAILY_CONVERSATIONS | limits=${if (limitsEnabled) "on" else "off"}"
        )
    }

    /** Exporta o prompt de importação + a conversa, para colar numa IA externa e depois importar o JSON. */
    fun buildConversationExport(): String {
        val transcript = _chatMessages.value
            .filter { !it.isLoading && it.text.isNotBlank() }
            .joinToString("\n") { m ->
                val who = if (m.role == ChatRole.USER) "Usuário" else "Assistente"
                "$who: ${m.text}"
            }
        return buildImportPrompt() +
            "\n\n## Conversa até agora (preferências do viajante)\n" + transcript
    }

    fun generateItinerary() {
        if (_chatPhase.value == ChatPhase.GENERATING) return
        _chatPhase.value = ChatPhase.GENERATING
        viewModelScope.launch {
            try {
                val result = generator?.generateItinerary()
                    ?: throw Exception(appContext.getString(R.string.create_assistant_not_initialized_short))
                _generatedDays.value  = result.days
                _cameFromImport.value = false
                _chatPhase.value      = ChatPhase.PREVIEW
                analytics.logAiItineraryGenerated(
                    success      = true,
                    daysCount    = result.days.size,
                    promptTokens = result.promptTokens,
                    outputTokens = result.candidatesTokens,
                    totalTokens  = result.totalTokens,
                    latencyMs    = result.latencyMs
                )
            } catch (e: Exception) {
                analytics.logAiItineraryGenerated(
                    success = false, daysCount = 0,
                    promptTokens = 0, outputTokens = 0, totalTokens = 0, latencyMs = 0L
                )
                _chatMessages.value = _chatMessages.value +
                    ChatMessage(ChatRole.AI, appContext.getString(R.string.create_generate_failed, e.message ?: appContext.getString(R.string.create_try_again)))
                _chatPhase.value = ChatPhase.CHATTING
            }
        }
    }

    fun saveItinerary() {
        val tripId = _createdTripId.value ?: return
        val days   = _generatedDays.value
        _chatPhase.value = ChatPhase.SAVING
        viewModelScope.launch {
            saveItineraryUseCase(tripId, days)
            repo.touchLastEditedAt(tripId)   // F1: roteiro gerado por IA conta como edição
            analytics.logTripCreated(
                method    = if (_cameFromImport.value) AnalyticsService.METHOD_AI_IMPORT else AnalyticsService.METHOD_AI,
                daysCount = formDaysCount()
            )
            _readyToNavigate.value = true
        }
    }

    fun backToChat() {
        _chatPhase.value = ChatPhase.CHATTING
    }

    fun skipItinerary() {
        analytics.logTripCreated(method = AnalyticsService.METHOD_MANUAL, daysCount = formDaysCount())
        _readyToNavigate.value = true
    }

    // ── Create trip ───────────────────────────────────────────────────────────

    fun createTrip() {
        val f = _form.value
        if (f.startDate == null || f.endDate == null) return
        if (_createdTripId.value != null) return  // guard duplo clique
        viewModelScope.launch {
            val id = repo.createTrip(
                name         = f.name.trim(),
                destination  = f.destination.trim(),
                coverEmoji   = f.coverEmoji,
                coverImage   = f.coverImage,
                startDate    = f.startDate,
                endDate      = f.endDate,
                latitude     = f.latitude,
                longitude    = f.longitude,
                hotelName    = f.hotelName.trim(),
                hotelAddress = f.hotelAddress.trim(),
                hotelPhone   = f.hotelPhone.trim()
            )
            _createdTripId.value = id
            if (f.latitude == null) {
                launch { repo.geocodeAndSaveCoordinates(id, f.destination.trim()) }
            }
        }
    }

    private fun today(): String = LocalDate.now().toString()

    /** Nº de dias (calendário) do formulário; 0 se datas ausentes/inválidas. */
    private fun formDaysCount(): Int {
        val f = _form.value
        val s = f.startDate ?: return 0
        val e = f.endDate ?: return 0
        return runCatching {
            (LocalDate.parse(e).toEpochDay() - LocalDate.parse(s).toEpochDay() + 1).toInt()
        }.getOrDefault(0)
    }

    companion object {
        private const val TAG = "PipaAiUsage"
        /** Teto de tokens (cumulativos, cobrados) por conversa antes da trava dura. */
        private const val MAX_CONVERSATION_TOKENS = 20_000
        /** Fração do teto em que o nudge suave é oferecido. */
        private const val NUDGE_RATIO             = 0.8
        /** Máximo de conversas com IA por dia por dispositivo. */
        private const val MAX_DAILY_CONVERSATIONS = 3
    }
}
