# Arquitetura Geral — Rumo (GramadoApp)

---

## Resumo executivo

O Rumo é um app Android nativo com arquitetura **MVVM em camadas** com **Hilt** para injeção de dependência. O modelo de dados central é o `TripData` (carregado em bloco, não reativamente). A UI é 100% Jetpack Compose com telas stateless que recebem dados via parâmetros e emitem ações via callbacks.

---

## Diagrama de camadas

```
┌─────────────────────────────────────────────────────────────────┐
│  UI Layer                                                        │
│                                                                  │
│  MainActivity                                                    │
│    └─ AppNavigation (NavHost + MainPagerScreen)                  │
│         ├─ Telas stateless (HomeScreen, ContactsScreen, …)       │
│         │    └─ recebem dados via parâmetros                     │
│         │    └─ emitem ações via callbacks                       │
│         └─ Telas com ViewModel (CreateTripScreen, EditActivity…) │
│              └─ collectAsStateWithLifecycle()                    │
│              └─ chamam métodos do ViewModel                      │
├─────────────────────────────────────────────────────────────────┤
│  ViewModel Layer                                                 │
│                                                                  │
│  TripViewModel          ← dados da viagem aberta (TripData)     │
│  TripsListViewModel     ← Flow<List<TripEntity>>                 │
│  CreateTripViewModel    ← wizard + chat Gemini                   │
│  EditActivityViewModel  ← formulário de atividade               │
│  ShareTripViewModel     ← orquestra TravelExporter               │
│  ImportTripViewModel    ← orquestra TravelImporter               │
│  SettingsViewModel      ← lê/grava SettingsRepository            │
│  (+ Edit*ViewModels para viagem, dia, contato, voucher, pass)    │
├─────────────────────────────────────────────────────────────────┤
│  Repository Layer                                                │
│                                                                  │
│  TripRepository         ← viagem: getTripData(), createTrip()   │
│  DayRepository          ← dias: getDayEntity(), updateDay()     │
│  ActivityRepository     ← atividades + badges + walk stops      │
│  ContactRepository      ← contatos + favoritar + reordenar      │
│  VoucherRepository      ← vouchers + grupos + reordenar         │
│  BoardingPassRepository ← passagens aéreas/transporte           │
│  WeatherRepository      ← Open-Meteo API + cache 3h             │
│  SettingsRepository     ← DataStore("rumo_settings")            │
│  ContactCategoryRepository ← SharedPreferences (categorias)     │
├─────────────────────────────────────────────────────────────────┤
│  Data Layer                                                      │
│                                                                  │
│  TravelDatabase (Room v16)                                       │
│    ├─ Entities: Trip, TravelDay, TravelActivity,                 │
│    │   ActivityBadge, WalkStop, Contact,                         │
│    │   Voucher, VoucherGroup, BoardingPass                       │
│    ├─ DAOs: TripDao, TravelDayDao, TravelActivityDao,            │
│    │   ContactDao, VoucherDao, VoucherGroupDao, BoardingPassDao  │
│    └─ Mappers: entity ↔ domain (Mappers.kt)                     │
│                                                                  │
│  Domain Models (Models.kt)                                       │
│    TripEntity, TravelDay, TravelActivity, Badge,                 │
│    WalkStop, Contact, Voucher, BoardingPass                      │
│                                                                  │
│  UseCase Layer                                                   │
│    └─ SaveGeneratedItineraryUseCase ← salva roteiro IA (tx)     │
│                                                                  │
│  Serviços externos (injetáveis via Hilt)                         │
│    ├─ TravelExporter  → ZIP .travel (cacheDir)                   │
│    ├─ TravelImporter  ← ZIP .travel (ContentResolver)            │
│    └─ ItineraryGenerator ← Gemini 2.0 Flash (SDK)               │
└─────────────────────────────────────────────────────────────────┘
```

---

## Padrão arquitetural: MVVM com Hilt

### O que é MVVM aqui

- **Model:** `TripRepository` + `TravelDatabase` + domain models em `Models.kt`
- **ViewModel:** mantém `StateFlow` dos dados; a tela reage a mudanças
- **View:** composables stateless ou com coleta de `StateFlow`

### Hilt para DI

`TravelDatabase`, `TripRepository`, `SettingsRepository` e `ContactCategoryRepository` são singletons gerenciados por Hilt via `AppModule`:

```kotlin
// data/di/AppModule.kt
@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideTripRepository(db: TravelDatabase): TripRepository = TripRepository(db)

    @Provides @Singleton
    fun provideSettingsRepository(@ApplicationContext ctx: Context): SettingsRepository =
        SettingsRepository(ctx.settingsDataStore)
}
```

`MainActivity` é anotada com `@AndroidEntryPoint`. `AppNavigation` obtém `SettingsViewModel` via `hiltViewModel()` ao nível de Activity (para o `showEmergencyContacts` global):

```kotlin
// AppNavigation.kt
val settingsVm: SettingsViewModel = hiltViewModel()  // escopo de Activity
val showEmergencyContacts by settingsVm.showEmergencyContacts.collectAsStateWithLifecycle()

// Dentro de cada composable de rota:
val vm: TripViewModel = hiltViewModel()  // escopo de NavBackStackEntry
```

### `@HiltViewModel` com `SavedStateHandle`

Todos os 12 ViewModels usam `@HiltViewModel @Inject constructor`. ViewModels com parâmetros de navegação (`tripId`, `activityId`, etc.) lêem esses valores do `SavedStateHandle`, que o Navigation Compose popula automaticamente a partir dos argumentos de rota:

```kotlin
@HiltViewModel
class TripViewModel @Inject constructor(
    private val repo: TripRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val tripId: Long = checkNotNull(savedStateHandle["tripId"])
}
```

Os `companion object Factory` foram removidos — Hilt elimina a necessidade de factories manuais.

---

## Dois padrões de tela

### 1. Telas stateless (maioria)

Não têm ViewModel próprio. Recebem todos os dados via parâmetros e emitem ações via callbacks.

**Exemplos:** `HomeScreen`, `DayDetailScreen`, `VouchersScreen`, `ContactsScreen`, `BoardingPassScreen`, `ShareTripScreen`, `ImportTripScreen`, `SettingsScreen`

```kotlin
@Composable
fun VouchersScreen(
    vouchers: List<Voucher>,          // dados de entrada
    onReorderVouchers: (List<Voucher>) -> Unit,  // ação de saída
    onDeleteVoucher: (Long) -> Unit,
    ...
)
```

Os dados chegam de `TripViewModel.tripData` via `AppNavigation` → `MainPagerScreen` → tela. As ações sobem pelo mesmo caminho e são executadas no ViewModel ou no `scope.launch { repo.* }` em `AppNavigation`.

**Por que stateless:** facilita testes, reúso e raciocínio sobre o fluxo de dados. O estado local (listas de drag, flags de dialog) fica com `remember` na própria tela — sem misturar com estado de negócio.

### 2. Telas com ViewModel (formulários e fluxos complexos)

Têm ViewModel próprio com `StateFlow` do estado do formulário.

**Exemplos:** `CreateTripScreen`, `EditActivityScreen`, `EditBoardingPassScreen`, `EditTripScreen`, `EditDayScreen`, `EditContactScreen`, `EditVoucherScreen`

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
```

O ViewModel mantém o estado do formulário entre recomposições e faz chamadas ao repositório. Eventos de navegação e feedback são emitidos via `Channel<UiEvent>` (padrão one-shot — sem re-entrega em recomposição):

```kotlin
// data/model/UiEvent.kt
sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    object NavigateBack : UiEvent()
    object NavigateAfterDelete : UiEvent()  // usado quando o delete sai mais de um nível
}

// No ViewModel
private val _uiEvent = Channel<UiEvent>()
val uiEvent = _uiEvent.receiveAsFlow()

fun save() {
    viewModelScope.launch {
        runCatching { repo.upsert(...) }
            .onSuccess { _uiEvent.send(UiEvent.NavigateBack) }
            .onFailure { _state.value = _state.value.copy(isSaving = false)
                         _uiEvent.send(UiEvent.ShowSnackbar("Erro ao salvar")) }
    }
}
```

A tela coleta os eventos em `LaunchedEffect(Unit)` e delega ao `SnackbarHostState` ou ao callback de navegação conforme o tipo.

---

## `TripData` — modelo de carregamento em bloco

```kotlin
data class TripData(
    val trip: Trip,             // modelo de domínio — não expõe TripEntity à UI
    val days: List<TravelDay>,
    val contacts: List<Contact>,
    val vouchers: List<Voucher>,
    val boardingPasses: List<BoardingPass>
)
```

`TripRepository.getTripData(tripId)` carrega toda a viagem de uma vez em **8 queries fixas** (não N+1):

```
1. trips WHERE id = ?
2. travel_days WHERE tripId = ?
3. travel_activities WHERE dayId IN (...)
4. activity_badges WHERE activityId IN (...)
5. walk_stops WHERE activityId IN (...)
6. contacts WHERE tripId = ?
7. vouchers WHERE tripId = ?
8. boarding_passes WHERE tripId = ?
```

Os dados são montados em memória via `groupBy` antes de construir os domain models. Isso evita queries adicionais por atividade ou badge.

**`TripViewModel`** chama `getTripData` no `init` e a cada `refresh()`. Não usa `Flow` reativo do banco — o modelo é "carregar tudo → exibir → editar em tela separada → voltar → recarregar tudo". O gatilho de recarga é o `SavedStateHandle("refresh")`.

### Por que não `Flow<TripData>`

Um `Flow` reativo exigiria múltiplos `Flow` combinados com `combine()`, um por tabela. Com o volume atual de dados (dezenas de registros), o custo de recarregar tudo via `suspend fun` é desprezível e o código é muito mais simples.

---

## Mecanismo de refresh entre telas

O padrão de refresh é o elo central entre formulários de edição e telas de exibição. Funciona com timestamp no `SavedStateHandle`:

```
EditDayScreen
  → onBack: navController.previousBackStackEntry
                ?.savedStateHandle?.set("refresh", System.currentTimeMillis())
  → navController.popBackStack()

DayDetail / TripMain (pai na backstack)
  val refreshKey by entry.savedStateHandle
      .getStateFlow("refresh", 0L).collectAsStateWithLifecycle()
  LaunchedEffect(refreshKey) {
      if (refreshKey > 0L) vm.refresh()   // relê tudo do banco
  }
```

**Vantagem:** não há broadcast, não há `SharedViewModel`, não há `EventBus`. O `SavedStateHandle` é o canal de comunicação entre entradas adjacentes da backstack — o mecanismo oficial do Navigation Compose para esse caso de uso.

**Por que timestamp e não Boolean:** o `LaunchedEffect` só reage a mudanças de valor. Um Boolean alternando `true → false → true` pode ser coalescido numa única recomposição. O timestamp é monotonicamente crescente e sempre diferente.

---

## Room — entidades e mappers

### Tabelas (9 entidades)

| Entidade | Tabela | Chave externa |
|---|---|---|
| `TripEntity` | `trips` | — |
| `TravelDayEntity` | `travel_days` | `tripId → trips` |
| `TravelActivityEntity` | `travel_activities` | `dayId → travel_days` |
| `ActivityBadgeEntity` | `activity_badges` | `activityId → travel_activities` |
| `WalkStopEntity` | `walk_stops` | `activityId → travel_activities` |
| `ContactEntity` | `contacts` | `tripId → trips` |
| `VoucherEntity` | `vouchers` | `tripId → trips` |
| `VoucherGroupEntity` | `voucher_groups` | `tripId → trips` |
| `BoardingPassEntity` | `boarding_passes` | `tripId → trips` |

### Separação entity / domain

O banco usa `*Entity` (flat, sem listas aninhadas). O domínio usa classes ricas (`TravelDay` contém `List<TravelActivity>`, que contém `List<Badge>` e `List<WalkStop>`). A conversão é feita em `Mappers.kt` via funções de extensão:

```kotlin
// entity → domain (todos os 9 tipos mapeados)
fun TripEntity.toDomain(): Trip
fun TravelDayEntity.toDomain(activities: List<TravelActivity>): TravelDay
fun TravelActivityEntity.toDomain(badges: List<Badge>, walkStops: List<WalkStop>): TravelActivity
fun ActivityBadgeEntity.toDomain(): Badge
fun WalkStopEntity.toDomain(): WalkStop
fun ContactEntity.toDomain(): Contact
fun VoucherEntity.toDomain(): Voucher
fun BoardingPassEntity.toDomain(): BoardingPass

// domain → entity (usado no seeder e em operações de escrita)
fun TravelDay.toEntity(tripId: Long): TravelDayEntity
fun TravelActivity.toEntity(dayId: Long, position: Int): TravelActivityEntity
fun Badge.toEntity(activityId: Long): ActivityBadgeEntity
fun WalkStop.toEntity(activityId: Long, position: Int): WalkStopEntity
fun Contact.toEntity(tripId: Long): ContactEntity
fun Voucher.toEntity(tripId: Long): VoucherEntity
fun BoardingPass.toEntity(tripId: Long): BoardingPassEntity
```

O `TripRepository` é o único lugar que chama `toDomain()` e `toEntity()` — os ViewModels e telas nunca veem as entities diretamente (exceto formulários de edição, que recebem a entity via `getActivity()`, `getDayEntity()` etc. para pré-preencher o formulário).

**`TripEntity` nunca chega à UI.** `TripData.trip` é do tipo `Trip` (modelo de domínio). A `TripEntity` fica confinada à camada de dados — `TripRepository` é o único que a manipula diretamente.

### Migrations explícitas

Todas as 13 migrations (v3→v16) estão em `TravelDatabase.kt` como `MIGRATION_N_(N+1)`. `fallbackToDestructiveMigration()` não é usado — `fallbackToDestructiveMigrationFrom(1, 2)` existe apenas para versões pré-histórico sem schema registrado.

**Regra:** qualquer novo campo no banco exige migration SQL + incremento de `version` + atualização de ambas as direções em `Mappers.kt`.

---

## UseCase layer

Operações de orquestração complexas que envolvem múltiplos repositórios ou transações atômicas ficam em `data/usecase/`. São `@Singleton @Inject constructor` e injetadas diretamente nos ViewModels.

| UseCase | O que encapsula | Caller |
|---|---|---|
| `SaveGeneratedItineraryUseCase` | Transação: salva dias + atividades + badges do roteiro IA de uma vez via `db.withTransaction { }` | `CreateTripViewModel` |

`TravelExporter` e `TravelImporter` também são operações compostas injetáveis por Hilt (`@Singleton @Inject constructor`). Ficam em `data/export/` e `data/import_trip/` respectivamente — eram instanciados manualmente nos ViewModels até a melhoria #10.

---

## `TripRepository` — fachada do agregado raiz

`TripRepository` gerencia operações no nível da viagem completa. Os ViewModels nunca acessam DAOs diretamente.

**Responsabilidades:**
- `getTripData(tripId)` — carrega tudo (8 queries)
- `createTrip(...)` — insere trip + gera dias automaticamente para o período
- `updateTrip`, `deleteTrip`, `saveVoucherSortMode` — CRUD de viagem
- `getDayTitles(tripId)` — query leve para seletor de dia nos vouchers

**`allTrips: Flow<List<TripEntity>>`** — único `Flow` reativo do repositório. Usado por `TripsListViewModel` para atualizar a lista de viagens em tempo real quando uma viagem é criada, editada ou excluída.

---

## Três tipos de persistência

| Tipo | Onde | O que guarda |
|---|---|---|
| **Room** | `TravelDatabase` | Viagens, dias, atividades, contatos, vouchers, passagens — dados estruturados e duráveis |
| **SharedPreferences** | `SettingsRepository` (`"rumo_settings"`) | `autoOpenActiveTrip`, `showEmergencyContacts` — configurações globais do app |
| **SharedPreferences** | `"boarding_passes"` | Portão e URL de passagens — voláteis, mudam no dia do voo, não merecem migration |
| **SharedPreferences** | `"reminders"` | Estado do lembrete de check-in |
| **SharedPreferences** | `ContactCategoryRepository` | Categorias customizadas de contato |
| **`cacheDir/exports/`** | FileProvider | Arquivo `.travel` exportado — temporário, limpo pelo SO |
| **`filesDir/Arquivos/`** | FileProvider | Documentos anexados a dias |
| **`filesDir/Vouchers/`** | FileProvider | Arquivos de vouchers importados |
| **`filesDir/Passagens/`** | FileProvider | Arquivos de passagens importados |

**Regra de divisão:** Room para dados estruturados que precisam de consulta, migração e export. `SharedPreferences` para pares chave-valor simples e voláteis onde uma migration de schema seria desproporcional.

---

## Fluxo de dados: leitura

```
TravelDatabase
  └─ TripRepository.getTripData(tripId)
       └─ TripData (domain models montados em memória)
            └─ TripViewModel._tripData: MutableStateFlow<TripData?>
                 └─ collectAsStateWithLifecycle() em AppNavigation
                      └─ MainPagerScreen(state=TripScreenState(...), actions=TripScreenActions(...))
                           ├─ HomeScreen(days=...)
                           ├─ VouchersScreen(vouchers=...)
                           ├─ BoardingPassScreen(passes=...)
                           └─ ContactsScreen(contacts=...)
```

## Fluxo de dados: escrita

```
Usuário interage com tela stateless
  └─ callback (ex: onDeleteContact(id))
       └─ AppNavigation delega: vm.deleteContact(id)
            └─ TripViewModel.deleteContact → repo.deleteContact() → load()
                 └─ _tripData.value = novo TripData

Usuário salva em tela com ViewModel
  └─ EditContactViewModel.save(onDone)
       └─ repo.upsertContact(...) → banco atualizado
       └─ onDone() = { savedStateHandle.set("refresh", timestamp); popBackStack() }
            └─ TripMain reage ao refreshKey → vm.refresh()
```

**Regra:** `AppNavigation` só conhece rotas, transições e IDs de parâmetros. Toda operação de banco e lógica de atualização de estado fica nos ViewModels.

---

## APIs e integrações externas

| Integração | Como é feita | Cache |
|---|---|---|
| **Open-Meteo Forecast** | `WeatherRepository` — HTTP direto, sem lib | 3h em memória + `SharedPreferences` |
| **Open-Meteo Geocoding** | `WeatherRepository.searchLocations()` | Sem cache (resultado efêmero do autocomplete) |
| **Gemini 2.0 Flash** | SDK `com.google.ai.client.generativeai` | Sem cache — geração única por sessão de chat |
| **Google Maps** | `Intent(ACTION_VIEW, "geo:0,0?q=...")` | — |
| **Uber** | `Intent(ACTION_VIEW, "https://m.uber.com/ul/...")` | — |
| **FileProvider** | `androidx.core.content.FileProvider` | — |

**Chave Gemini:** `local.properties → BuildConfig.GEMINI_API_KEY`. Não versionada.

---

## Ciclo de vida dos ViewModels

```
AppNavigation (sempre vivo enquanto o app existir)
  ├─ TripsListViewModel  → escopo do NavBackStackEntry "trips_list"
  ├─ TripViewModel       → escopo do NavBackStackEntry "trip/{tripId}/main"
  │    (recriado ao navegar para uma viagem diferente)
  └─ EditActivityViewModel → escopo do NavBackStackEntry "trip/.../activity/..."
       (destruído ao popBackStack)
```

`TripViewModel` sobrevive a rotações de tela (gerenciado pelo `viewModel()` do Compose), mas é destruído quando o `NavBackStackEntry` é removido (ex: ao voltar para `TripsList` e navegar para outra viagem).

---

## Padrões recorrentes

### Atualização otimista em `TripViewModel`

Todas as operações de lista (delete, reorder, toggle) atualizam `_tripData` via `_tripData.update { }` **de forma síncrona**, antes de qualquer suspend call. A persistência acontece em `viewModelScope.launch { }` em background.

```kotlin
// Padrão uniforme
fun deleteContact(contactId: Long) {
    _tripData.update { data ->                        // 1. UI atualiza imediatamente
        data?.copy(contacts = data.contacts.filter { it.id != contactId })
    }
    viewModelScope.launch { repo.deleteContact(contactId) }  // 2. Persiste em background
}
```

Exceção: `deleteActivity` e `swapActivityPositions` ainda fazem `load()` pois atividades são listas aninhadas dentro de `TravelDay` — o custo de reconstruir o grafo em memória supera o benefício.

### Drag-to-reorder otimista (tela)

Usado em `VouchersScreen` e `ContactsScreen`. Estado local (`localVouchers`/`localContacts`) para feedback visual imediato durante o gesto; `LaunchedEffect(localList)` chama o ViewModel após soltar.

```kotlin
var localVouchers by remember(vouchers) { mutableStateOf(vouchers) }
// onMove: atualiza localVouchers em memória
LaunchedEffect(localVouchers) {
    if (localVouchers != vouchers) onReorderVouchers(localVouchers)
}
```

### Dialogs gerenciados por flags locais

Flags `Boolean` com `remember` na tela (nunca no ViewModel):
```kotlin
var showDeleteDialog by remember { mutableStateOf(false) }
```

### `upsert` por ID sentinela

`id == 0L` = INSERT, `id != 0L` = UPDATE. Usado em todos os formulários de edição e no `TravelImporter`.

### Sealed class de fase para fluxos complexos

`SharePhase`, `ImportPhase`, `ChatPhase` — máquinas de estado simples que controlam qual UI é exibida sem lógica condicional espalhada.

### Separação de cor e tipo em paletas

`VoucherType` + `VoucherPalette`, `BadgeType` + cores em `BadgeChip` — adicionar um novo tipo nunca requer alterar o composable de exibição.

---

## O que não existe (decisões conscientes)

| Padrão | Por que não está aqui |
|---|---|
| Hilt / Koin | App de escopo pequeno; injeção manual em `AppNavigation` é suficiente e elimina um grafo de anotações |
| `Flow` reativo por entidade | `getTripData()` + `refresh()` é mais simples; o volume de dados não justifica reatividade granular |
| Repository por domínio | Um único `TripRepository` reduz o número de dependências a gerenciar sem perda de coesão no tamanho atual |
| `UseCase` / Interactor | Lógica de negócio está nos ViewModels e no Repository; o app não tem lógica suficientemente complexa para justificar a camada extra |
| Paginação (Paging 3) | Listas pequenas (dezenas de itens); carregamento em bloco é adequado |

### Testes — estado atual

**108 testes no total: 69 unitários JVM (`./gradlew test`) + 39 instrumentados (`./gradlew connectedAndroidTest`, requer emulador/dispositivo).** As 4 fases do plano de testes estão implementadas — ver `docs/guia-testes.md`.

**Unitários JVM** (`app/src/test/`):

| Arquivo | Cobertura |
|---|---|
| `data/db/MappersTest.kt` | 14 testes — todos os mappers `entity → domain` e `domain → entity` |
| `data/ai/ItineraryParserTest.kt` | 12 testes — parser do JSON de roteiro da IA (defaults, `"null"`, cercas de markdown, caminhos de erro); usa `org.json` real via `testImplementation` |
| `data/model/VoucherReindexTest.kt` | 7 testes — `reindexedByGroup()` (função pura) |
| `data/preferences/SettingsRepositoryTest.kt` | 7 testes — DataStore em JVM puro |
| `ui/trips/TripViewModelTest.kt` | 20 testes — métodos de lista do ViewModel + testes de timing de otimismo |
| `ui/edit/EditViewModelUiEventTest.kt` | 7 testes — `UiEvent` (NavigateBack/ShowSnackbar/NavigateAfterDelete) |
| `util/MainDispatcherRule.kt`, `util/Fixtures.kt` | Helpers e fixtures compartilhados |

**Instrumentados** (`app/src/androidTest/`, rodam no emulador/dispositivo):

| Arquivo | Cobertura |
|---|---|
| `data/db/MigrationTest.kt` | 3 testes — cadeia de migrations v3→16 validada contra as entities |
| `data/db/{Voucher,Contact,TravelActivity,Trip}DaoTest.kt` | 29 testes — SQL real dos DAOs (ordenações, bulk queries, CASCADE) |
| `data/export/ExportImportRoundTripTest.kt` | 7 testes — round-trip `.travel` (nenhum campo/arquivo perdido; rejeição de schema futuro) |
| `data/db/DbTestFixtures.kt` | Banco em memória + fixtures de entities |

Schemas do Room exportados em `app/schemas/` (`exportSchema = true`) — versionados no git, base para testes de migration a partir da v17.

---

## Guia de decisão: onde colocar código novo

| Tipo de código | Onde vai |
|---|---|
| Nova query ao banco | Novo método no DAO correspondente → exposto via `TripRepository` |
| Novo campo em entidade existente | Migration SQL + `version++` + ambas as direções em `Mappers.kt` |
| Nova preferência global | `SettingsRepository` + `SettingsViewModel` + toggle em `SettingsScreen` |
| Nova preferência volátil por viagem | `SharedPreferences` direto (sem Room) se não precisar de consulta/export |
| Novo campo no export `.travel` | `TravelExporter.buildJson()` + `TravelImporter.parseTripJson()` + `docs/travel-export-schema.md` |
| Nova tela sem estado próprio | Composable stateless com parâmetros + callbacks → rota em `AppNavigation` |
| Nova tela com formulário | `*State` data class + `*ViewModel` com `StateFlow` + `Factory` → rota em `AppNavigation` |
| Nova aba no pager da viagem | `TAB_ICONS`/`TAB_LABELS` + `when (page)` no `HorizontalPager` + FAB contextual |
| Lógica de negócio complexa | No ViewModel se depende de estado de UI; no Repository se é operação de dados pura |
