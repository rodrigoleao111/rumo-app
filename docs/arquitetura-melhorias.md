# Proposta de Modernização da Arquitetura

**Contexto:** o app está funcional e estável. Este documento não trata de reescritas — trata de um caminho incremental para tornar a base de código mais consistente, mais fácil de manter e mais alinhada com o que a comunidade Android considera padrão em 2025.

As melhorias estão ordenadas por **impacto vs. esforço**: as primeiras entregam muito com pouco risco; as últimas são transformações maiores que vale adiar até que o app esteja crescendo.

---

## 1. Injeção de dependência — substituir factories manuais por Hilt

### Problema atual

Cada ViewModel que recebe parâmetros no construtor precisa de um `companion object` com uma `Factory` explícita:

```kotlin
// Repetido ~10 vezes no projeto
companion object {
    fun factory(repo: TripRepository, tripId: Long) =
        viewModelFactory { initializer { TripViewModel(repo, tripId) } }
}
```

E em `AppNavigation` cada tela repete o bloco:

```kotlin
val vm: TripViewModel = viewModel(
    factory = TripViewModel.factory(repo, tripId)
)
```

### Solução

Adotar **Hilt** (`com.google.dagger:hilt-android`) com `@HiltViewModel` e `SavedStateHandle` para parâmetros de navegação.

```kotlin
// Antes
class TripViewModel(private val repo: TripRepository, private val tripId: Long) : ViewModel()

// Depois
@HiltViewModel
class TripViewModel @Inject constructor(
    private val repo: TripRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val tripId: Long = checkNotNull(savedStateHandle["tripId"])
}
```

`AppNavigation` passa a usar simplesmente:

```kotlin
val vm: TripViewModel = hiltViewModel()
```

### Impacto

- Elimina ~10 `companion object Factory` e todos os blocos `viewModel(factory = ...)` em `AppNavigation`
- Torna a criação de novos ViewModels trivial
- `TravelDatabase`, `TripRepository`, `WeatherRepository` e `SettingsRepository` viram singletons anotados com `@Singleton` em um módulo Hilt — sem `remember { }` em `AppNavigation`
- A lógica que hoje vive em `AppNavigation` para construir dependências some completamente

### O que foi feito

- Plugin `com.google.dagger.hilt.android:2.51.1` adicionado ao Gradle (projeto e app)
- Dependências: `hilt-android`, `hilt-compiler` (KSP), `hilt-navigation-compose`
- `RumoApplication` criada com `@HiltAndroidApp`
- `AndroidManifest.xml` atualizado com `android:name=".RumoApplication"`
- `MainActivity` anotada com `@AndroidEntryPoint`
- `data/di/AppModule.kt` criado com `@Singleton @Provides` para `TravelDatabase`, `TripRepository`, `SettingsRepository`, `ContactCategoryRepository`
- 12 ViewModels migrados para `@HiltViewModel @Inject constructor` — `companion object Factory` removidos de todos
- ViewModels com parâmetros de navegação (`tripId`, `activityId`, etc.) agora usam `SavedStateHandle`; Navigation Compose popula automaticamente a partir dos argumentos de rota
- `AppNavigation` simplificado: removidos `remember { TravelDatabase }`, `remember { TripRepository }`, `remember { SettingsRepository }`, `remember { ContactCategoryRepository }` e todos os blocos `viewModel(factory = ...)`. Substituídos por `hiltViewModel()`. `SettingsViewModel` obtido via `hiltViewModel()` no topo de `AppNavigation` (escopo de Activity) para expor `showEmergencyContacts` globalmente
- Testes atualizados: `TripViewModel(repo, SavedStateHandle(...))` em `TripViewModelTest`; `EditContactViewModel(repo, categoryRepo, SavedStateHandle(...))` e `EditTripViewModel(repo, SavedStateHandle(...))` em `EditViewModelUiEventTest`

**Suite total: 57 testes, todos passando.**

### Esforço: médio

---

## 2. Dividir `TripRepository` em repositórios por domínio

### Problema atual

`TripRepository` (~320 linhas) concentra todas as operações do banco: viagens, dias, atividades, badges, walk stops, vouchers, boarding passes, contatos e documento de dia. É um **God class** de repositório.

Consequências práticas:
- Qualquer mudança de schema afeta a mesma classe gigante
- Impossível testar operações de vouchers sem instanciar toda a lógica de viagens
- Futuros colaboradores não sabem onde adicionar operações novas

### Solução

Criar repositórios por domínio, todos com acesso ao mesmo banco Room:

```
data/repository/
├── TripRepository.kt          ← apenas viagem: create, update, delete, getTripData()
├── DayRepository.kt           ← dias: update, document
├── ActivityRepository.kt      ← atividades + badges + walk stops
├── VoucherRepository.kt       ← vouchers: CRUD + reordenar + toggleUsed
├── BoardingPassRepository.kt  ← passagens
└── ContactRepository.kt       ← contatos + favoritar + reordenar
```

`getTripData()` permanece em `TripRepository` — é a query central da app e faz sentido ficar com o agregado raiz (`Trip`).

Cada repositório recebe apenas os DAOs de que precisa:

```kotlin
class VoucherRepository(
    private val voucherDao: VoucherDao,
    private val voucherGroupDao: VoucherGroupDao
)
```

### Impacto

- Responsabilidade única por repositório
- Com Hilt (melhoria 1), cada ViewModel injeta apenas o repositório que precisa — sem acesso acidental a operações de outros domínios
- Testes unitários ficam viáveis: basta mockar 1 DAO em vez de 7

### O que foi feito

**5 novos repositórios criados** em `data/repository/`, todos com `@Singleton @Inject constructor(db: TravelDatabase)`:

| Repositório | Métodos |
|---|---|
| `DayRepository` | `getDayEntity`, `updateDay` |
| `ActivityRepository` | `getActivity`, `upsertActivity`, `insertWalkStop`, `getBadgesForActivity`, `deleteActivity`, `swapActivityPositions` |
| `ContactRepository` | `getContactEntity`, `upsertContact`, `deleteContact`, `reorderContacts`, `toggleFavoriteContact` |
| `VoucherRepository` | `getVoucherEntity`, `upsertVoucher`, `deleteVoucher`, `deleteVoucherAndReindex`, `reorderVouchers`, `toggleVoucherUsed`, `getVoucherGroups`, `addVoucherGroup` |
| `BoardingPassRepository` | `getBoardingPassEntity`, `upsertBoardingPass`, `deleteBoardingPass` |

**`TripRepository` reduzido** a: `allTrips`, `getTripData()`, `getDays()`, `getDayTitles()`, `getContacts()`, `getVouchers()`, `getBoardingPasses()`, `getTripEntity()`, `updateTrip()`, `deleteTrip()`, `saveVoucherSortMode()`, `createTrip()`, `geocodeAndSaveCoordinates()`, `saveGeneratedItinerary()`

**ViewModels atualizados** para injetar apenas os repos necessários:
- `TripViewModel` — `TripRepository` + `VoucherRepository` + `ContactRepository` + `ActivityRepository`
- `EditDayViewModel` — `DayRepository`
- `EditActivityViewModel` — `ActivityRepository` + `DayRepository`
- `EditContactViewModel` — `ContactRepository` + `ContactCategoryRepository`
- `EditVoucherViewModel` — `VoucherRepository` + `TripRepository` (para `getDayTitles`)
- `EditBoardingPassViewModel` — `BoardingPassRepository`
- `ImportTripViewModel` — todos os 6 repos (repassa ao `TravelImporter`)

**`TravelImporter` atualizado**: construtor expandido para receber os 6 repos; todas as chamadas de banco redirecionadas ao repo correto. `TravelExporter` não precisou de mudança (usa apenas `getTripData()` que permanece em `TripRepository`).

**Testes atualizados:**
- `TripViewModelTest`: `buildVm` agora passa 4 mocks (`tripRepo`, `voucherRepo`, `contactRepo`, `activityRepo`); todos os `coEvery { repo.* }` redirecionados ao mock correto
- `EditViewModelUiEventTest`: `buildContactVm` usa `ContactRepository` em vez de `TripRepository`

**Suite total: 57 testes, todos passando.**

### Esforço: médio-alto

Extração cirúrgica — mover métodos, ajustar imports, atualizar os ViewModels. Não muda nenhuma interface de UI.

---

## 3. Introduzir um modelo de domínio `Trip` (remover `TripEntity` da UI) ✅ Concluído

### Problema atual

`TripData` usa `TripEntity` diretamente:

```kotlin
data class TripData(
    val trip: TripEntity,      // ← Room entity vazando para a UI
    val days: List<TravelDay>,
    ...
)
```

`TripEntity` tem colunas Room (`@PrimaryKey`, `@ColumnInfo`) que nada têm a ver com a UI. A tela de edição precisa conhecer o schema do banco.

### Solução

Criar um modelo de domínio `Trip` em `data/model/Models.kt`:

```kotlin
data class Trip(
    val id: Long,
    val name: String,
    val destination: String,
    val coverEmoji: String,
    val hotelName: String,
    val hotelAddress: String,
    val hotelPhone: String,
    val startDate: String,
    val endDate: String,
    val latitude: Double,
    val longitude: Double,
    val voucherSortMode: String
)
```

Adicionar `TripEntity.toDomain(): Trip` em `Mappers.kt` (mesmo padrão já existente para `TravelDay`, `TravelActivity`, etc.).

`TripData.trip` passa a ser `Trip`.

### Impacto

- A UI nunca importa `TripEntity` — a camada de dados fica selada ✅
- `Mappers.kt` agora tem mapeamento completo para todos os 9 tipos ✅
- `TravelExporter` não precisou de mudança — campos de `Trip` são idênticos ✅

### O que foi feito

- `data class Trip` adicionada em `Models.kt`
- `TripEntity.toDomain(): Trip` adicionada em `Mappers.kt`
- `TripData.trip` alterado de `TripEntity` para `Trip` em `TripRepository.kt`
- `AppNavigation.kt` atualizado (renomeação de variável, mesmos campos)
- 14 testes unitários em `test/data/db/MappersTest.kt` — todos os mappers cobertos

---

## 4. Extrair lógica de negócio de `AppNavigation` para `TripViewModel` ✅ Concluído

### Problema atual

`AppNavigation` contém chamadas diretas a repositório e ViewModel fora de composables:

```kotlin
// Dentro de AppNavigation — lógica de negócio no grafo de navegação
scope.launch {
    repo.deleteContact(contactId)
    vm.refresh()
}

scope.launch {
    repo.reorderContacts(tripId, newOrder)
    vm.refresh()
}
```

`AppNavigation` deve conhecer rotas e transições — não operações de banco.

### Solução

Mover cada operação para o ViewModel responsável:

```kotlin
// ContactsViewModel (ou TripViewModel)
fun deleteContact(contactId: Long) {
    viewModelScope.launch {
        repo.deleteContact(contactId)
        refresh()
    }
}

fun reorderContacts(newOrder: List<Contact>) {
    viewModelScope.launch {
        repo.reorderContacts(tripId, newOrder)
        _tripData.update { it?.copy(contacts = newOrder) }  // otimista
    }
}
```

`AppNavigation` passa a chamar apenas `vm.deleteContact(id)` — sem `scope.launch`, sem acesso direto a `repo`.

### Impacto

- `AppNavigation` não chama mais `repo.*()` diretamente — apenas delega ao ViewModel ✅
- `scope` (rememberCoroutineScope) removido de `AppNavigation` — não há mais `launch` no nível do grafo ✅
- Lógica de negócio testável no ViewModel — coberta por testes unitários ✅

### O que foi feito

- 5 métodos adicionados em `TripViewModel`: `deleteContact`, `reorderContacts`, `toggleFavoriteContact`, `deleteActivity`, `swapActivityPositions`
- `AppNavigation`: 5 `scope.launch { repo.* }` substituídos por chamadas diretas ao ViewModel; `val scope = rememberCoroutineScope()` removido do nível do `NavHost`
- Dependências de teste adicionadas: `kotlinx-coroutines-test`, `mockk`
- Infraestrutura de teste: `util/MainDispatcherRule.kt`, `util/Fixtures.kt`
- 13 testes em `ui/trips/TripViewModelTest.kt` — cobre os 5 novos métodos + 2 regressões nos métodos existentes de vouchers

---

## 5. Reduzir os parâmetros de `MainPagerScreen` ✅ Concluído

### Problema atual

`MainPagerScreen` recebia **34 parâmetros**: 13 campos de `TripData`/`Trip` extraídos individualmente + refreshKey + showEmergencyContacts + savedVoucherSortMode + 18 callbacks. Cada nova feature na tela de viagem aumentava esse número.

### Solução

Dois data classes `private` definidos em `AppNavigation.kt`, imediatamente antes de `MainPagerScreen`:

```kotlin
private data class TripScreenState(
    val tripData: TripData?,          // substitui 13 campos individuais de Trip
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
```

`MainPagerScreen` reduzido a 2 parâmetros:
```kotlin
@Composable
private fun MainPagerScreen(state: TripScreenState, actions: TripScreenActions)
```

Dentro do corpo: `val trip = state.tripData?.trip` e todos os acessos usam `state.` / `actions.` / `trip?.`.

### O que foi feito

- `TripScreenState` (3 campos) e `TripScreenActions` (18 campos) adicionados em `AppNavigation.kt`
- `MainPagerScreen`: assinatura de 34 → 2 parâmetros; corpo atualizado com `state.` / `actions.`
- Call site: `val trip = tripData?.trip` removido; substituído pela construção dos dois objetos
- `TripData` adicionado aos imports de `AppNavigation.kt`
- 41 testes existentes: BUILD SUCCESSFUL — nenhuma regressão

### Esforço: baixo

Refatoração estrutural sem mudança de comportamento.

---

## 6. Padronizar atualização otimista em todos os ViewModels ✅ Concluído

### Problema atual

O tratamento de estado após operações de banco é inconsistente:

| Operação | Comportamento |
|---|---|
| `toggleVoucherUsed` | Otimista: atualiza `_tripData` imediatamente |
| `reorderVouchers` | Otimista: atualiza `_tripData` imediatamente |
| `deleteContact` | **Não otimista:** chama `refresh()` após deletar (percurso completo ao banco) |
| `deleteVoucher` | **Não otimista:** chama `refresh()` após deletar |

O usuário percebe: após deletar um contato, há uma pisca visível enquanto a lista recarrega.

### Solução

Adotar atualização otimista consistente para todas as operações de lista:

```kotlin
fun deleteVoucher(voucherId: Long) {
    viewModelScope.launch {
        // 1. Atualiza UI imediatamente
        _tripData.update { data ->
            data?.copy(vouchers = data.vouchers.filter { it.id != voucherId })
        }
        // 2. Persiste em background
        repo.deleteVoucher(voucherId)
        // 3. Recarrega só se necessário (ex: reindexação)
    }
}
```

### Impacto

- UI sem pisca em todas as operações de lista ✅
- Comportamento consistente — `_tripData.update { }` síncrono antes de qualquer `launch { }` ✅
- `deleteContact` e `toggleFavoriteContact` pararam de fazer 8 queries ao banco desnecessariamente ✅

### O que foi feito

Regra uniforme aplicada em `TripViewModel`: `_tripData.update { }` (síncrono) **antes** do `viewModelScope.launch { repo.* }` para todas as operações de lista.

| Método | Antes | Depois |
|---|---|---|
| `deleteContact` | `launch { repo.delete(); load() }` | `_tripData.update { filter }` → `launch { repo.delete() }` |
| `toggleFavoriteContact` | `launch { repo.toggle(); load() }` | `_tripData.update { map }` → `launch { repo.toggle() }` |
| `deleteVoucher` | `launch { repo.delete(); filtra; repo.reorder(); state = }` | `filtra; reindex; _tripData.update { }` → `launch { repo.delete(); repo.reorder() }` |
| `reorderVouchers` | `launch { repo.reorder(); state = }` | `_tripData.update { }` → `launch { repo.reorder() }` |
| `reorderContacts` | `launch { repo.reorder(); state = }` | `_tripData.update { }` → `launch { repo.reorder() }` |

Mantidos com `load()` (otimismo impraticável por estrutura aninhada):
- `deleteActivity` — atividades são listas dentro de `TravelDay`
- `swapActivityPositions` — idem

Testes de TripViewModel expandidos de 13 para 22: cada método otimista tem um teste de timing (sem `advanceUntilIdle`) que comprova que o estado atualiza antes de qualquer operação de banco.

---

## 7. Padronizar tratamento de erros nos ViewModels ✅ Concluído

### Problema anterior

Apenas `ShareTripViewModel` e `ImportTripViewModel` tinham estados de erro expostos para a UI. Os outros ~8 ViewModels faziam `viewModelScope.launch { repo.*(…) }` sem `try/catch` — erros de banco sumiam silenciosamente.

### Solução implementada

**Sealed class central em `data/model/UiEvent.kt`:**

```kotlin
sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    object NavigateBack : UiEvent()
    object NavigateAfterDelete : UiEvent()  // para deletes que navegam mais de um nível
}
```

`NavigateAfterDelete` foi necessário para `EditTripScreen`, cujo delete deve navegar até `TripsListScreen` (não apenas um passo atrás como o save).

**Padrão aplicado a todos os ViewModels de edição:**

```kotlin
private val _uiEvent = Channel<UiEvent>()
val uiEvent = _uiEvent.receiveAsFlow()

fun save() {
    _state.value = _state.value.copy(isSaving = true)
    viewModelScope.launch {
        runCatching { repo.upsertFoo(...) }
            .onSuccess { _uiEvent.send(UiEvent.NavigateBack) }
            .onFailure { _state.value = _state.value.copy(isSaving = false)
                         _uiEvent.send(UiEvent.ShowSnackbar("Erro ao salvar")) }
    }
}
```

Em caso de falha, `isSaving` (ou `isDeleting`) é resetado para `false` para que o botão re-habilite e o usuário possa tentar novamente.

**ViewModels atualizados:** `EditContactViewModel`, `EditDayViewModel`, `EditTripViewModel`, `EditActivityViewModel`, `EditVoucherViewModel`, `EditBoardingPassViewModel`, `TripsListViewModel`.

**Telas atualizadas:** todas as 6 telas de edição + `TripsListScreen` — cada uma coleta os eventos em `LaunchedEffect` e exibe `SnackbarHost(AmberPrimary)` no Scaffold.

### Impacto

- O usuário vê um snackbar AmberPrimary com mensagem de erro quando qualquer operação de salvar/excluir falha no banco
- O padrão `Channel<UiEvent>` é o recomendado pelo Google para eventos one-shot (evita re-entrega em recomposição)
- Os callbacks `onDone: () -> Unit` foram removidos dos ViewModels — toda a navegação pós-operação passa pelos UiEvents

### Testes

9 novos testes em `EditViewModelUiEventTest.kt` — total da suite: **50 testes**, todos passando.

---

## 8. Eliminar lógica duplicada de reindexação de vouchers ✅ Concluído

### Problema atual

A lógica de reindexar `sortOrder` por grupo estava inline no ViewModel — `groupBy + mapIndexed` escrito diretamente em `TripViewModel.deleteVoucher()` — e o delete + reindex no banco ocorria em duas chamadas separadas (sem transação atômica).

### Solução

**Função pura extraída para o modelo de domínio:**

```kotlin
// Models.kt
fun List<Voucher>.reindexedByGroup(): List<Voucher> =
    groupBy { it.groupName }
        .flatMap { (_, items) -> items.mapIndexed { i, v -> v.copy(sortOrder = i) } }
```

**Operação atômica no repositório:**

```kotlin
// TripRepository.kt
suspend fun deleteVoucherAndReindex(voucherId: Long, tripId: Long) = db.withTransaction {
    db.voucherDao().deleteById(voucherId)
    val remaining = db.voucherDao().getVouchersForTrip(tripId).map { it.toDomain() }
    remaining.reindexedByGroup().forEach { v -> db.voucherDao().updateSortOrder(v.id, v.sortOrder) }
}
```

**ViewModel simplificado:**

```kotlin
fun deleteVoucher(voucherId: Long) {
    val remaining = _tripData.value?.vouchers?.filter { it.id != voucherId } ?: return
    _tripData.update { it?.copy(vouchers = remaining.reindexedByGroup()) }
    viewModelScope.launch { repo.deleteVoucherAndReindex(voucherId, tripId) }
}
```

### O que foi feito

- `fun List<Voucher>.reindexedByGroup()` adicionada em `Models.kt` — função pura, testável isoladamente
- `deleteVoucherAndReindex(voucherId, tripId)` adicionado em `TripRepository.kt` — delete + reindex atômico em uma transação
- `TripViewModel.deleteVoucher()` simplificado: usa `reindexedByGroup()` para o update otimista e `deleteVoucherAndReindex` para o banco
- 7 testes em `test/data/model/VoucherReindexTest.kt` — cobre lista vazia, grupo único, múltiplos grupos, reindexação após remoção, preservação de campos não-sortOrder
- `TripViewModelTest.kt` atualizado: 3 testes de `deleteVoucher` agora verificam a nova API `deleteVoucherAndReindex`

### Esforço: baixo

---

## 9. Migrar `SettingsRepository` para DataStore ✅ Concluído

### Problema anterior

`SharedPreferences` era síncrono e não suportava `Flow`. Em `AppNavigation` era necessário um `DisposableEffect + LifecycleEventObserver` para re-ler `showEmergencyContacts` no `ON_RESUME` — um workaround de reatividade.

### Solução implementada

**`SettingsRepository`** expõe `Flow<Boolean>` em vez de propriedades `var` síncronas:

```kotlin
// Singleton via extension property no Context
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "rumo_settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val autoOpenActiveTrip: Flow<Boolean> = dataStore.data.map { it[KEY_AUTO_OPEN] ?: true }
    val showEmergencyContacts: Flow<Boolean> = dataStore.data.map { it[KEY_EMERGENCY_CONTACTS] ?: true }

    suspend fun setAutoOpenActiveTrip(enabled: Boolean) { dataStore.edit { it[KEY_AUTO_OPEN] = enabled } }
    suspend fun setShowEmergencyContacts(enabled: Boolean) { dataStore.edit { it[KEY_EMERGENCY_CONTACTS] = enabled } }
}
```

**`SettingsViewModel`** — `MutableStateFlow` manuais substituídos por `stateIn`:

```kotlin
val autoOpenActiveTrip: StateFlow<Boolean> = settings.autoOpenActiveTrip
    .stateIn(viewModelScope, SharingStarted.Eagerly, true)

fun setAutoOpenActiveTrip(enabled: Boolean) {
    viewModelScope.launch { settings.setAutoOpenActiveTrip(enabled) }
}
```

**`AppNavigation`** — `DisposableEffect + LifecycleEventObserver` eliminados. As duas configurações são coletadas reativamente com `collectAsStateWithLifecycle`:

```kotlin
val showEmergencyContacts by settings.showEmergencyContacts.collectAsStateWithLifecycle(initialValue = true)
val autoOpenEnabled by settings.autoOpenActiveTrip.collectAsStateWithLifecycle(initialValue = true)
```

Mudanças feitas nas configurações agora propagam automaticamente para todas as telas que observam o Flow — sem necessidade de re-leitura manual ao voltar da tela de Configurações.

### Dependência adicionada

`implementation("androidx.datastore:datastore-preferences:1.1.1")`

### Testes

7 novos testes em `SettingsRepositoryTest.kt` — testáveis em JVM puro via `PreferenceDataStoreFactory.create + TemporaryFolder`. O DataStore cria uma coroutine interna de longa duração; o scope passado deve ser `testScope.backgroundScope` para evitar `UncompletedCoroutinesError`.

**Suite total: 57 testes, todos passando.**

---

## 10. UseCase layer para operações compostas ✅ Concluído

### Quando considerar

Quando uma mesma sequência de operações for executada por múltiplos ViewModels, ou quando uma operação envolver regras de negócio suficientemente complexas para justificar teste isolado.

### O que foi feito

**`SaveGeneratedItineraryUseCase`** criado em `data/usecase/`:
- Extrai `saveGeneratedItinerary()` de `TripRepository` para um UseCase dedicado
- `@Singleton @Inject constructor(db: TravelDatabase, dayRepo: DayRepository)`
- `suspend operator fun invoke(tripId, days)` — interface idiomática para UseCases Kotlin
- Mantém a mesma transação atômica (`db.withTransaction { }`) que estava no repositório
- `CreateTripViewModel` passa a injetar `SaveGeneratedItineraryUseCase` e chama `saveItineraryUseCase(tripId, days)`
- `saveGeneratedItinerary()` removido de `TripRepository` — repositório não tem mais responsabilidade de orquestração multi-domínio

**`TravelExporter` e `TravelImporter`** tornados Hilt-injetáveis:
- Ambos anotados com `@Singleton` e `@Inject constructor`
- `Context` anotado com `@ApplicationContext` — Hilt injeta automaticamente
- `ShareTripViewModel`: `private val exporter: TravelExporter` agora é injetado — removida a linha `private val exporter = TravelExporter(appContext, repo)` e os parâmetros `TripRepository` + `@ApplicationContext appContext`
- `ImportTripViewModel`: `private val importer: TravelImporter` agora é injetado — removidos os 6 repos + `@ApplicationContext appContext` + a linha `private val importer = TravelImporter(appContext, ...)`

**Suite total: 57 testes, todos passando.**

---

## Mapa de prioridade

| # | Melhoria | Impacto | Esforço | Status |
|---|---|---|---|---|
| 3 | Modelo domínio `Trip` | Alto | Baixo | ✅ Concluído |
| 4 | Lógica fora de `AppNavigation` | Alto | Baixo | ✅ Concluído |
| 6 | Atualização otimista consistente | Médio | Baixo | ✅ Concluído |
| 8 | Deduplicar reindexação de vouchers | Médio | Baixo | ✅ Concluído |
| 5 | Reduzir params de `MainPagerScreen` | Médio | Baixo | ✅ Concluído |
| 7 | Padronizar erros nos ViewModels | Alto | Médio | ✅ Concluído |
| 9 | DataStore em vez de SharedPreferences | Médio | Médio | ✅ Concluído |
| 1 | Hilt para DI | Alto | Médio | ✅ Concluído |
| 2 | Dividir `TripRepository` | Alto | Alto | ✅ Concluído |
| 10 | UseCase layer | Médio | Alto | ✅ Concluído |

---

## Princípio orientador

A arquitetura atual tem um problema de **crescimento**: cada nova feature tende a engordar `TripRepository`, `AppNavigation` e `MainPagerScreen`. As melhorias de alta prioridade (3, 4, 5) atacam exatamente esses três pontos de acumulação — sem reescrita, sem framework novo, sem risco de regressão.

Hilt e a divisão do repositório são a evolução natural quando o custo das factories manuais superar o custo da migração — provavelmente quando o app atingir ~15 ViewModels.
