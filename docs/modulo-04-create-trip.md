# Módulo 04 — Criação de Viagem

**Tela:** `CreateTripScreen`  
**Arquivo:** `ui/trips/CreateTripScreen.kt`  
**ViewModel:** `ui/trips/CreateTripViewModel.kt`  
**Entry point de navegação:** rota `create_trip`, acessada pelo card "Nova viagem" em `TripsListScreen`

---

## Visão geral

Wizard de 4 passos para criar uma nova viagem. Os três primeiros passos coletam dados da viagem; o quarto monta o roteiro com IA. A viagem é gravada no banco ao final do Passo 3 — o Passo 4 opera sobre um `tripId` já existente.

---

## Padrão de arquitetura

Este módulo segue **MVVM** com separação clara entre form state, autocomplete state e chat state — todos no mesmo ViewModel.

| Camada | Arquivo | Responsabilidade |
|---|---|---|
| **View** | `CreateTripScreen.kt` | Renderiza os 4 passos, captura eventos via callbacks |
| **ViewModel** | `CreateTripViewModel.kt` | Mantém `CreateTripForm`, estados de chat, faz chamadas ao repo/usecase e à API |
| **Repository** | `TripRepository.kt` | `createTrip()`, `geocodeAndSaveCoordinates()` |
| **UseCase** | `SaveGeneratedItineraryUseCase.kt` | Transação atômica: salva dias + atividades + badges do roteiro gerado |
| **AI** | `ItineraryGenerator.kt` | Gemini chat, geração de itinerário, `parseJson()` estático |
| **Geocoding** | `WeatherRepository.searchLocations()` | Autocomplete de destino e endereço de hotel |

> **Regra de padrão:** Todo estado do wizard (form, chat, fases, preview) fica no `CreateTripViewModel`. A tela é stateless exceto pela variável `step: Int` e `showHelpSheet: Boolean`, que são estados puramente visuais sem valor de negócio.

---

## Fluxo de dados

```
CreateTripViewModel
  ├─ form: StateFlow<CreateTripForm>
  ├─ createdTripId: StateFlow<Long?>
  ├─ readyToNavigate: StateFlow<Boolean>
  ├─ searchResults / isSearching              ← autocomplete destino
  ├─ hotelSearchResults / isHotelSearching    ← autocomplete hotel
  ├─ chatMessages / chatInput / chatPhase     ← estado do chat
  ├─ generatedDays / cameFromImport           ← resultado da IA
  ├─ importJsonText / importError             ← modo importação
  └─ canGenerate: StateFlow<Boolean>          ← derivado de chatMessages

CreateTripScreen
  ├─ step: Int (0–3) — local, não no ViewModel
  └─ showHelpSheet: Boolean — local
```

**Transição de passos:**
- Passos 0→1→2: avanço manual pelo botão "Próximo →"
- Passo 2→3: acionado por `viewModel.createTrip()`, que persiste a viagem. A tela avança via `LaunchedEffect(createdId)` quando `createdId != null`
- Saída do Passo 3: `readyToNavigate = true` → `LaunchedEffect(readyToNavigate)` → `onTripCreated(tripId)`

---

## Indicador de progresso (`StepIndicator`)

Quatro barras horizontais (4dp de altura) no cabeçalho `GreenMoss`:

| Estado da barra | Cor |
|---|---|
| Passo concluído (`i < currentStep`) | `AmberPrimary` |
| Passo atual (`i == currentStep`) | `Color.White` |
| Passo futuro | `Color.White` 30% alpha |

---

## Passo 1 — Identidade da viagem (`Step1Content`)

**Campos:**

| Campo | Componente | Validação |
|---|---|---|
| Destino | `OutlinedTextField` + `ExposedDropdownMenu` | `isNotBlank()` |
| Nome da viagem | `OutlinedTextField` | `isNotBlank()` |
| Ícone (emoji) | `EmojiPicker` | seleção obrigatória |

**Autocomplete de destino:**

```
updateDestination(v) → cancela job anterior → delay(350ms) → WeatherRepository.searchLocations(v)
```

- Debounce de 350ms via `Job` cancelável (`searchJob`)
- Mínimo 2 caracteres para disparar a busca (`v.length < 2` → lista vazia)
- Spinner no `leadingIcon` enquanto `isSearching == true`
- Seleção de resultado: `selectResult(result)` → preenche `destination`, `latitude`, `longitude` → fecha dropdown
- Ícone de pin fica `GreenMoss` quando `latitude != null` (localização confirmada); `TextSecondary` 40% alpha caso contrário
- `supportingText` "📍 Localização confirmada" visível após seleção

**EmojiPicker:** grade 4×5 de emojis. Célula selecionada tem fundo `AmberPrimary` 15% alpha e borda `AmberPrimary` 2dp. Células não selecionadas têm fundo `SurfaceWhite` e borda `CardBorder` 1dp. Linhas incompletas são preenchidas com `Spacer(weight(1f))` para manter o alinhamento.

**`canProceed`:** `destination.isNotBlank() && name.isNotBlank() && coverEmoji.isNotEmpty()` — botão "Próximo →" desabilitado (`AmberPrimary` 35% alpha) se falso.

---

## Passo 2 — Período da viagem (`Step2Content`)

**Componente:** `DateRangePicker` do Material 3 (`ExperimentalMaterial3Api`) inline (sem dialog), `showModeToggle = false`.

**Sincronização picker → form:**
```kotlin
LaunchedEffect(startMillis, endMillis) {
    startMillis?.let { onUpdateStart(Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toString()) }
    endMillis?.let   { onUpdateEnd(Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toString()) }
}
```
As datas são armazenadas como `String` ISO (`"yyyy-MM-dd"`). A conversão é feita via `Instant + ZoneId.of("UTC")` para evitar discrepância de fuso horário.

**Cuidado com `updateStartDate`:** se a nova `startDate > endDate` atual, `endDate` é resetado para `null`:
```kotlin
fun updateStartDate(v: String) {
    _form.update {
        it.copy(startDate = v, endDate = if (it.endDate != null && it.endDate < v) null else it.endDate)
    }
}
```

**Chip de resumo:** exibido no rodapé quando `canProceed == true`. Mostra `"d MMM → d MMM"` (pt-BR) e um badge `GreenMoss` com a contagem de dias.

**Contagem de dias:**
```kotlin
val dayCount = generateSequence(startDate) { it.plusDays(1) }.takeWhile { !it.isAfter(endDate) }.count()
```
Inclui os dias de início e fim (contagem inclusiva).

**Cores do `DateRangePicker`:** totalmente customizado — `selectedDayContainerColor = GreenMoss`, range em `GreenMoss` 13% alpha, dia de hoje com borda `GreenMoss`.

---

## Passo 3 — Hospedagem (`Step3Content`)

Dados opcionais. O botão "Criar viagem e montar roteiro →" não tem validação de campos — hospedagem pode ficar em branco.

**Campos:**

| Campo | Observação |
|---|---|
| Nome da hospedagem | Texto livre |
| Endereço | Autocomplete via `WeatherRepository.searchLocations()` (mesmo padrão do destino, job próprio `hotelSearchJob`) |
| Telefone | `KeyboardType.Phone` |

**Ação do botão:** `viewModel.createTrip()` → grava no banco via `TripRepository.createTrip()` → retorna `tripId` → `_createdTripId.value = id`.

**Geocoding automático:** se o destino foi digitado mas não confirmado via dropdown (sem `latitude`), é lançado um job paralelo: `launch { repo.geocodeAndSaveCoordinates(id, destination) }`. Isso garante que a viagem terá coordenadas mesmo que o usuário não tenha selecionado um resultado do autocomplete.

**Estilo do botão de criar:** `containerColor = GreenMoss`, ícone `Check` e texto em `AmberPrimary` — padrão de ação principal do app.

---

## Passo 4 — Wizard de IA (`Step4Content` + `ChatPhase`)

O passo 4 é uma máquina de estados controlada pelo enum `ChatPhase`:

```kotlin
enum class ChatPhase { CHOOSING, CHATTING, IMPORTING, GENERATING, PREVIEW, SAVING }
```

**Inicialização:** `LaunchedEffect(createdId)` chama `viewModel.initChat()` quando a viagem é criada. `initChat()` instancia `ItineraryGenerator` com o contexto da viagem (destino, datas, hotel) e define `chatPhase = CHOOSING`.

### Fase CHOOSING — Tela de escolha

Dois `OptionCard` lado a lado (`IntrinsicSize.Max` para altura igual):

| Card | Ícone | Ação |
|---|---|---|
| Importar roteiro | `FileUpload` (`AmberPrimary`) | `startImport()` → `IMPORTING` |
| Chat com IA | `AutoAwesome` (`GreenMoss`) | `startChat()` → `CHATTING` + mensagem de boas-vindas da IA |

Botão "Pular e acessar a viagem" (`TextButton`, `TextSecondary`) — chama `skipItinerary()` → `readyToNavigate = true`.

Ícone `?` (`HelpOutline`) na TopAppBar abre `ModalBottomSheet` com `ItineraryHelpSheet` — explica os dois modos em linguagem simples.

**Botão back no Passo 4:** visível nas fases `IMPORTING`, `CHATTING` e `PREVIEW`. Chama `viewModel.backToChoosing()` → volta para `CHOOSING`.

### Fase IMPORTING — Importação de JSON

**Fluxo:**
1. Usuário vê instruções passo a passo em um card verde claro
2. Botão "Copiar texto de instrução" → `LocalClipboardManager.setText(AnnotatedString(importPrompt))` → ícone troca para `Check`, texto vira "Texto copiado!"
3. Usuário cola o JSON no `OutlinedTextField` grande ou usa "Importar arquivo" (file picker para `.json` / `.txt` / `text/*`)
4. Botão "Importar roteiro" → `onImport(importJsonText)` → `ItineraryGenerator.parseJson(json)` → `PREVIEW`

**`buildImportPrompt()`:** constrói um prompt completo com contexto da viagem (destino, período, dias, hotel) e as regras de formato JSON. O prompt cobre três cenários: gerar do zero, converter roteiro em texto livre, reformatar de outra IA. Ver `docs/ai-itinerary-schema.md` para o conteúdo completo.

**File picker:** `ActivityResultContracts.OpenDocument()` com tipos `["application/json", "text/plain", "text/*"]`. Conteúdo lido via `ContentResolver.openInputStream(uri).bufferedReader().readText()` e inserido em `importJsonText`.

**Erro:** `importError: String?` exibido acima dos botões em `MaterialTheme.colorScheme.error`. Limpo ao iniciar nova tentativa.

### Fase CHATTING / GENERATING — Chat com Gemini

**Chat:**
- `LazyColumn` de `ChatBubble` com scroll automático para a última mensagem: `LaunchedEffect(messages.size) { listState.animateScrollToItem(messages.size - 1) }`
- Mensagem de loading da IA: 3 círculos cinzas (`TextSecondary` 40% alpha, 6dp, `CircleShape`)
- Guard contra envio duplo: `if (_chatMessages.value.any { it.isLoading }) return`

**Bolhas de chat (`ChatBubble`):**

| Papel | Alinhamento | Fundo | Texto |
|---|---|---|---|
| Usuário (`USER`) | `Arrangement.End` | `GreenMoss` | `Color.White` |
| IA (`AI`) | `Arrangement.Start` | `SurfaceWhite` + tonalElevation 1dp | `TextPrimary` |

A IA tem avatar ✨ em círculo `GreenMoss` 32dp à esquerda. O `shape` da bolha adapta o canto superior: usuário → canto superior direito 4dp; IA → canto superior esquerdo 4dp.

**`canGenerate`:** `StateFlow<Boolean>` derivado de `chatMessages.map { msgs -> msgs.count { it.role == ChatRole.USER } >= 1 }` — o botão "Gerar roteiro agora" aparece após ao menos 1 mensagem do usuário.

**Geração:** `generateItinerary()` → `chatPhase = GENERATING` → `ItineraryGenerator.generateItinerary()` → `_generatedDays.value = days` → `chatPhase = PREVIEW`. Em caso de erro: mensagem de IA com o erro + volta para `CHATTING`.

**Fase `GENERATING`:** substitui o campo de input por um `CircularProgressIndicator` + "Montando seu roteiro...".

### Fase PREVIEW — Preview do roteiro gerado

Lista de cards por dia com:
- Badge "Dia N" (`GreenMoss`)
- Título do dia
- Alerta (`⚠ texto`, `AmberPrimary`) se `dayAlert != null`
- Lista de atividades: `time` (44dp fixo) + `emoji` + `name` + `detail` (2 linhas máximo)

**Botão "Salvar roteiro":** `GreenMoss` + ícone/texto `AmberPrimary` — padrão de ação principal.

**Botão "Voltar":** rótulo dinâmico baseado em `cameFromImport`:
- `true` → "Voltar à importação" → `onBackToImport()` → `IMPORTING`
- `false` → "Voltar ao chat para ajustar" → `onBackToChat()` → `CHATTING`

### Fase SAVING

Tela simples com `CircularProgressIndicator` + "Salvando seu roteiro...". Não tem botão de retorno — operação não cancelável.

`saveItinerary()` chama `saveItineraryUseCase(tripId, days)` (`SaveGeneratedItineraryUseCase`, wrapped em `withTransaction`) → `readyToNavigate = true`.

---

## Modelo de form (`CreateTripForm`)

```kotlin
data class CreateTripForm(
    val name: String         = "",
    val destination: String  = "",
    val coverEmoji: String   = "",
    val startDate: String?   = null,   // ISO "yyyy-MM-dd"
    val endDate: String?     = null,   // ISO "yyyy-MM-dd"
    val latitude: Double?    = null,   // null = não confirmado
    val longitude: Double?   = null,
    val hotelName: String    = "",
    val hotelAddress: String = "",
    val hotelPhone: String   = ""
)
```

`latitude/longitude` são `null` quando o usuário digitou o destino sem selecionar um resultado do autocomplete. Nesse caso, `geocodeAndSaveCoordinates` é chamado em background após a criação.

---

## Composables privados (resumo)

| Composable | Fase | Responsabilidade |
|---|---|---|
| `StepIndicator` | todas | Barras de progresso no cabeçalho |
| `Step1Content` | 0 | Destino + nome + emoji |
| `Step2Content` | 1 | Seletor de período inline |
| `Step3Content` | 2 | Hospedagem + criar viagem |
| `Step4Content` | 3 | Dispatcher entre fases do Passo 4 |
| `ChoosingScreen` | `CHOOSING` | Dois cards de escolha + pular |
| `ImportScreen` | `IMPORTING` | Instruções + copiar prompt + campo JSON + file picker |
| `ChatScreen` | `CHATTING` / `GENERATING` | Chat de mensagens + campo de input |
| `ItineraryPreview` | `PREVIEW` | Lista de dias gerados + salvar/voltar |
| `ChatBubble` | `CHATTING` | Bolha individual de mensagem |
| `OptionCard` | `CHOOSING` | Card clicável de seleção de modo |
| `EmojiPicker` | Passo 1 | Grade de seleção de ícone |
| `SectionLabel` | várias | Rótulo de seção em maiúsculas (10sp, `GreenMoss`) |
| `ItineraryHelpSheet` | `CHOOSING` | Conteúdo do `ModalBottomSheet` de ajuda |
| `tripFieldColors()` | várias | Cores padronizadas para `OutlinedTextField` |

---

## Checklist para futuras modificações

- **Novo campo no wizard (Passos 1–3):** adicionar campo em `CreateTripForm` → método `updateX()` no ViewModel → campo de formulário no `StepNContent` → passar para `repo.createTrip()`.
- **Novo passo no wizard:** incrementar `totalSteps` em `StepIndicator` → adicionar `stepTitles[4]` → adicionar `step == 4 -> Step5Content(...)` no `when(step)` → ajustar lógica de avanço.
- **Nova fase no Passo 4:** adicionar valor ao enum `ChatPhase` → adicionar `ChatPhase.NOVA -> NovaScreen(...)` no `when(phase)` em `Step4Content` → adicionar transição no ViewModel.
- **Alterar modelo Gemini:** mudar em `ItineraryGenerator.kt` (parâmetro `model` na inicialização do SDK). Ver restrições em `docs/ai-itinerary-schema.md`.
- **Alterar debounce do autocomplete:** mudar o `delay(350)` em `updateDestination()` e/ou `updateHotelAddress()`.
- **Adicionar validação no Step 3:** atualmente qualquer dado de hospedagem é aceito (inclusive em branco). Para tornar obrigatório, adicionar `canProceed` e desabilitar o botão de criar.
