package com.rodrigoleao.gramado2026.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.BuildConfig
import com.rodrigoleao.gramado2026.data.ai.ItineraryGenerator
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import com.rodrigoleao.gramado2026.data.weather.GeocodingResult
import com.rodrigoleao.gramado2026.data.weather.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

// ── Wizard form ───────────────────────────────────────────────────────────────

data class CreateTripForm(
    val name: String         = "",
    val destination: String  = "",
    val coverEmoji: String   = "",
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

class CreateTripViewModel(private val repo: TripRepository) : ViewModel() {

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
            _searchResults.value = WeatherRepository.searchLocations(v)
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
            _hotelSearchResults.value = WeatherRepository.searchLocations(v)
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
        _chatPhase.value    = ChatPhase.CHATTING
        _chatMessages.value = listOf(ChatMessage(ChatRole.AI, generator?.getInitialGreeting() ?: "Olá! Vou ajudar a montar seu roteiro."))
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
                if (days.isEmpty()) throw Exception("Nenhum dia encontrado no JSON.")
                _generatedDays.value  = days
                _cameFromImport.value = true
                _chatPhase.value      = ChatPhase.PREVIEW
            } catch (e: Exception) {
                _importError.value = "JSON inválido: ${e.message ?: "verifique o formato e tente novamente."}"
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
        _chatInput.value = ""

        _chatMessages.value = _chatMessages.value +
            ChatMessage(ChatRole.USER, text) +
            ChatMessage(ChatRole.AI, "", isLoading = true)

        viewModelScope.launch {
            val response = generator?.sendMessage(text)
                ?: "Assistente não inicializado. Volte ao passo anterior."
            _chatMessages.value = _chatMessages.value.dropLast(1) +
                ChatMessage(ChatRole.AI, response)
        }
    }

    fun generateItinerary() {
        _chatPhase.value = ChatPhase.GENERATING
        viewModelScope.launch {
            try {
                val days = generator?.generateItinerary()
                    ?: throw Exception("Assistente não inicializado.")
                _generatedDays.value  = days
                _cameFromImport.value = false
                _chatPhase.value      = ChatPhase.PREVIEW
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value +
                    ChatMessage(ChatRole.AI, "Não consegui gerar o roteiro. ${e.message ?: "Tente novamente."}")
                _chatPhase.value = ChatPhase.CHATTING
            }
        }
    }

    fun saveItinerary() {
        val tripId = _createdTripId.value ?: return
        val days   = _generatedDays.value
        _chatPhase.value = ChatPhase.SAVING
        viewModelScope.launch {
            repo.saveGeneratedItinerary(tripId, days)
            _readyToNavigate.value = true
        }
    }

    fun backToChat() {
        _chatPhase.value = ChatPhase.CHOOSING
    }

    fun skipItinerary() {
        _readyToNavigate.value = true
    }

    // ── Create trip ───────────────────────────────────────────────────────────

    fun createTrip() {
        val f = _form.value
        if (f.startDate == null || f.endDate == null) return
        viewModelScope.launch {
            val id = repo.createTrip(
                name         = f.name.trim(),
                destination  = f.destination.trim(),
                coverEmoji   = f.coverEmoji,
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

    companion object {
        fun Factory(repo: TripRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CreateTripViewModel(repo) as T
        }
    }
}
