# Guia de Implementação de Testes

**Contexto:** este guia é um complemento direto de `arquitetura-melhorias.md`. As fases de teste estão alinhadas com a sequência de refatoração proposta lá — alguns testes ficam viáveis apenas após certas melhorias estarem no lugar.

Objetivo: cobertura incremental e sustentável, começando pelo que é possível hoje e evoluindo conforme a arquitetura amadurece.

---

## Estrutura de pastas de teste

O Android tem duas source sets de teste com propósitos distintos:

```
app/src/
├── main/java/…                  ← código de produção
├── test/java/…                  ← testes JVM (rodam no computador, sem emulador)
│   └── com/rodrigoleao/gramado2026/
│       ├── data/ai/             ← ItineraryParserTest (org.json real via testImplementation)
│       ├── data/db/             ← MappersTest
│       ├── data/model/          ← VoucherReindexTest
│       ├── data/preferences/    ← SettingsRepositoryTest (DataStore em JVM)
│       ├── ui/…                 ← testes de ViewModel (MockK)
│       └── util/                ← MainDispatcherRule, Fixtures
└── androidTest/java/…           ← testes instrumentados (rodam no dispositivo/emulador)
    └── com/rodrigoleao/gramado2026/
        ├── data/db/             ← MigrationTest, 4 DAO tests, DbTestFixtures
        └── data/export/         ← ExportImportRoundTripTest (round-trip .travel)
```

**Regra prática:** se o código que você quer testar **executa** `android.*`/`androidx.*` de verdade (SQLite, ContentResolver…), ele vai para `androidTest/`. Se é Kotlin/Java puro — ou a dependência Android tem substituto na JVM (ex.: `org.json` real, DataStore) — vai para `test/`.

**Comandos:** `./gradlew test` (JVM) · `./gradlew connectedAndroidTest` (instrumentados — exige emulador/dispositivo conectado).

---

## Dependências em `build.gradle.kts`

### Adicionadas (Fase 2)

```kotlin
// Testes JVM
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.24")

// Testes instrumentados
androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
```

### Adicionadas antecipadamente (Melhoria #4 — testes de ViewModel)

```kotlin
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
testImplementation("io.mockk:mockk:1.13.12")
```

### Adicionadas (Fase 1)

```kotlin
// JVM
testImplementation("org.json:json:20240303")   // org.json real (o do SDK é stub na JVM) — ItineraryParserTest
testImplementation("com.google.truth:truth:1.4.4")

// Instrumentação
androidTestImplementation("androidx.test:runner:1.6.2")
androidTestImplementation("androidx.room:room-testing:$roomVersion")
androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
androidTestImplementation("com.google.truth:truth:1.4.4")
```

Também adicionados na Fase 1 (pré-requisitos de infra que **não existiam**):
- `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` no `defaultConfig` — sem ele, nenhum teste instrumentado roda;
- `exportSchema = true` + `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` — o schema de cada versão do Room passa a ser exportado em `app/schemas/` (versionado no git);
- `app/schemas/` exposto como asset do `androidTest` (para `MigrationTestHelper` em migrations futuras);
- `TravelDatabase.CURRENT_VERSION` e `TravelDatabase.ALL_MIGRATIONS` públicos (a anotação `@Database` referencia `CURRENT_VERSION` — fonte única).

---

## Fase 1 — Disponível hoje, sem nenhuma refatoração ✅ Implementada

Esses testes não dependem de DI, de modelo de domínio, nem de repositórios separados.

### 1.1 Migrations do Room ✅

O maior risco silencioso do app: uma migration esquecida ou errada corrompe o banco dos usuários após uma atualização.

**Arquivo:** `androidTest/data/db/MigrationTest.kt` (4 testes — 3 da cadeia v3→16 + a 16→17 da F1)

**Adaptação necessária:** o plano original usava `MigrationTestHelper`, mas ele exige os schemas JSON exportados de **cada versão antiga** (`3.json`…`15.json`) — e `exportSchema` era `false` até a v16, então esses arquivos nunca existiram e não há como regenerá-los. A estratégia implementada:

1. Cria um arquivo SQLite real com o **schema da v3 escrito à mão** — derivado do `16.json` revertendo cada migration (todas são aditivas: `ADD COLUMN` / `CREATE TABLE`);
2. Semeia uma linha em cada tabela e grava `user_version = 3`;
3. Abre o banco com `Room.databaseBuilder(...).addMigrations(*TravelDatabase.ALL_MIGRATIONS)` — o Room executa as 13 migrations e **valida o schema resultante contra as entities anotadas**, lançando `IllegalStateException("Migration didn't properly handle…")` em qualquer divergência (o mesmo erro de produção);
4. Verifica que os dados semeados sobreviveram e que os DEFAULTs de cada migration foram aplicados (`hotelPhone = ''`, `voucherSortMode = 'BY_CATEGORY'`, `is_used = 0`, `transportType = 'FLIGHT'`, etc.), e que a tabela `voucher_groups` (criada pela 8→9) aceita insert com FK.

> **Migrations a partir da 16→N: `MigrationTestHelper`** (já em uso na F1). Com `exportSchema = true`, o `16.json` (e seguintes) ficam em `app/schemas/` — versionados no git e expostos como asset do androidTest. A migração 16→17 (`migracao16Para17_...`) é o primeiro exemplo real:
> ```kotlin
> @get:Rule
> val migrationHelper = MigrationTestHelper(
>     InstrumentationRegistry.getInstrumentation(),
>     TravelDatabase::class.java, emptyList(), FrameworkSQLiteOpenHelperFactory()
> )
>
> @Test fun migracao16Para17_...() {
>     migrationHelper.createDatabase("m.db", 16).apply { execSQL("INSERT INTO trips ..."); close() }
>     // ALL_MIGRATIONS é público; o helper aplica só a 16→17 e valida o schema contra o 17.json
>     migrationHelper.runMigrationsAndValidate("m.db", 17, true, *TravelDatabase.ALL_MIGRATIONS)
> }
> ```
> **Nunca apagar os JSONs de `app/schemas/`** — são o histórico que torna esse teste possível.

---

### 1.2 `ItineraryGenerator.parseJson()` — parser de JSON da IA ✅

Função estática pura: entra string, sai lista de `GeneratedDay`. Roda **na JVM** — o `org.json` real entra no classpath via `testImplementation("org.json:json")` (o do SDK Android é stub que lança em testes JVM).

**Arquivo:** `test/data/ai/ItineraryParserTest.kt` (12 testes)

**Contrato real do parser** (os exemplos originais deste guia usavam um schema hipotético — o real está em `docs/ai-itinerary-schema.md`):

| Campo | Regra |
|---|---|
| `days[]`, `dayNumber`, `title`, `activities[]`, `name` | **Obrigatórios** — ausência lança `JSONException` (quem chama trata) |
| `time`, `emoji`, `detail` | Opcionais — defaults `""`, `"📍"`, `""` |
| `dayAlert`, `address` | Opcionais — ausente, em branco ou a string `"null"` → `null` |
| `badges` | Opcional — lista de **strings** (objetos não são suportados); ausente → vazia |
| Cerca de markdown | ` ```json … ``` ` ou ` ``` … ``` ` **no início** do texto é removida; prosa antes do JSON **não** é suportada (lança) |

Cobertura: dia completo, defaults, `"null"` string → null, cercas de markdown, ordem preservada, lista vazia, e os 4 caminhos de erro (`@Test(expected = JSONException::class)`).

---

### 1.3 DAOs básicos — banco em memória ✅

Os 4 DAOs prioritários estão cobertos com banco Room em memória (`inMemoryDatabaseBuilder`, FKs ativas). As fixtures de entities ficam em `androidTest/data/db/DbTestFixtures.kt` (funções `tripEntity()`, `voucherEntity()`, etc. — defaults mínimos, sobrescreve-se só o que importa no teste).

| Arquivo | Testes | O que cobre |
|---|---|---|
| `VoucherDaoTest.kt` | 7 | Ordenação `groupName → sort_order → id`, `updateSortOrder`, `updateIsUsed` (ida e volta), `getMaxSortOrderInGroup` (inclusive `-1` para grupo vazio), `deleteById`, CASCADE ao deletar a viagem |
| `ContactDaoTest.kt` | 7 | Ordenação por `sortOrder`, telefone nulo, `updateIsFavorite`, `updateSortOrder`, `deleteById`, CASCADE |
| `TravelActivityDaoTest.kt` | 9 | Queries bulk (`getActivitiesForDays`, `getBadgesForActivities`, `getWalkStopsForActivities`) com ordenação `dayId → position`, `insertBadges` em lote, `deleteBadgesForActivity`, `updatePosition`, `countForDay`, CASCADE badge/walk stop ao deletar atividade |
| `TripDaoTest.kt` | 6 | `Flow` de `getAllTrips` (emissão + ordenação por `createdAt`), `count`, `updateCoordinates`, `updateVoucherSortMode`, CASCADE transitivo trip → dias → atividades |

> **Armadilha Kotlin + JUnit4:** métodos `@Test` escritos como `fun x() = runBlocking { … }` precisam terminar em expressão `Unit`. Um `assertThat(...).containsExactly(...)` **sem** `.inOrder()` retorna `Ordered` — o método deixa de ser void e o JUnit rejeita a **classe inteira** com `initializationError / Failed to instantiate test runner`. Solução: `runBlocking<Unit> { … }` (aconteceu no `TravelActivityDaoTest`).

---

## Fase 2 — Após melhoria #3 (modelo de domínio `Trip`) ✅ Implementada

`TripEntity` não vaza mais para a UI. Os mappers são testados de forma isolada.

### 2.1 Mappers `toDomain()` e `toEntity()`

**Arquivo:** `test/java/…/data/db/MappersTest.kt` ← **já implementado**

```kotlin
class MapperTest {

    @Test
    fun tripEntityToDomain_preservaTodosOsCampos() {
        val entity = TripEntity(
            id = 1L,
            name = "Gramado 2026",
            destination = "Gramado, RS",
            coverEmoji = "🏔️",
            hotelName = "Serra Azul",
            hotelAddress = "Rua Garibaldi, 152",
            hotelPhone = "(54) 3286-0000",
            startDate = "2026-06-09",
            endDate   = "2026-06-13",
            latitude  = -29.374,
            longitude = -50.877,
            voucherSortMode = "BY_CATEGORY",
            createdAt = 0L
        )

        val domain = entity.toDomain()

        assertThat(domain.id).isEqualTo(1L)
        assertThat(domain.name).isEqualTo("Gramado 2026")
        assertThat(domain.hotelPhone).isEqualTo("(54) 3286-0000")
        assertThat(domain.latitude).isEqualTo(-29.374)
    }

    @Test
    fun contactEntityToDomain_normalizaTypePadrao() {
        // ContactType desconhecido (de versões antigas) deve virar CUSTOM
        val entity = ContactEntity(id = 1, type = "TIPO_ANTIGO", ...)
        val domain = entity.toDomain()
        assertThat(domain.type).isEqualTo(ContactType.CUSTOM)
    }

    @Test
    fun travelActivityRoundTrip_naoPerdeInformacao() {
        val original = TravelActivity(
            id = 1L, time = "14h00", emoji = "🏨",
            name = "Check-in", detail = "Apresentar documento",
            mapQuery = "Rua Garibaldi, 152", uberDestination = "Rua Garibaldi, 152",
            badges = listOf(Badge(BadgeType.BOOKED, "Reservado", null)),
            walkStops = emptyList()
        )
        val entity = original.toEntity(dayId = 10L, position = 0)
        val restored = entity.toDomain(badges = original.badges.map { it.toEntity(activityId = 1L) }
                                                .map { it.toDomain() }, walkStops = emptyList())

        assertThat(restored.name).isEqualTo(original.name)
        assertThat(restored.mapQuery).isEqualTo(original.mapQuery)
        assertThat(restored.badges).hasSize(1)
    }
}
```

### 2.2 DataStore — Melhoria #9 ✅ Implementada

DataStore é testável em JVM puro com `PreferenceDataStoreFactory.create`.

**Arquivo:** `test/java/…/data/preferences/SettingsRepositoryTest.kt` ← **já implementado** (7 testes)

```kotlin
class SettingsRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun buildRepo(testScope: TestScope, fileName: String): SettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            // backgroundScope: não é aguardado pelo runTest — necessário porque
            // DataStore mantém uma coroutine interna de longa duração (leitor do arquivo).
            // Usar testScope diretamente causa UncompletedCoroutinesError.
            scope = testScope.backgroundScope,
            produceFile = { tmpFolder.newFile("$fileName.preferences_pb") }
        )
        return SettingsRepository(dataStore)
    }

    @Test
    fun autoOpenActiveTrip_default_isTrue() = runTest {
        val repo = buildRepo(this, "defaults_auto")
        assertTrue(repo.autoOpenActiveTrip.first())
    }

    @Test
    fun setAutoOpenActiveTrip_false_persistsAndEmits() = runTest {
        val repo = buildRepo(this, "auto_false")
        repo.setAutoOpenActiveTrip(false)
        assertFalse(repo.autoOpenActiveTrip.first())
    }
}
```

Coberturas: defaults verdadeiros, persist/emit em false, persist/emit em true-after-false, independência entre as duas chaves.

---

### 2.3 ViewModel UiEvents — Melhoria #7 ✅ Implementada

Após a adição do `Channel<UiEvent>` em todos os ViewModels de edição, testamos que as operações emitem os eventos corretos. O padrão é abrir um coletor coroutine antes de invocar o método e verificar o evento emitido.

**Arquivo:** `test/java/…/ui/edit/EditViewModelUiEventTest.kt` ← **já implementado** (9 testes)

```kotlin
@Test
fun save_success_emitsNavigateBack() = runTest {
    val vm = buildContactVm()
    vm.updateName("João")
    coEvery { repo.upsertContact(any(), any()) } returns 1L

    val events = mutableListOf<UiEvent>()
    val job = launch(UnconfinedTestDispatcher(testScheduler)) {
        vm.uiEvent.toList(events)
    }

    vm.save()
    advanceUntilIdle()

    assertIs<UiEvent.NavigateBack>(events.firstOrNull())
    job.cancel()
}

@Test
fun save_failure_resetsisSaving() = runTest {
    coEvery { repo.upsertContact(any(), any()) } throws RuntimeException("DB error")
    vm.save()
    advanceUntilIdle()
    assertFalse(vm.state.value.isSaving)
}
```

Coberturas implementadas:
- `NavigateBack` emitido em save com sucesso (`EditContactViewModel`)
- `ShowSnackbar` emitido em save com falha (`EditContactViewModel`)
- `isSaving` resetado para `false` em save com falha
- `NavigateBack` emitido em delete com sucesso (`EditContactViewModel`)
- `ShowSnackbar` emitido em delete com falha (`EditContactViewModel`)
- `NavigateAfterDelete` emitido em deleteTrip com sucesso (`EditTripViewModel`)
- `isDeleting` resetado para `false` em deleteTrip com falha

---

## Fase 3 — Após melhoria #2 (repositórios por domínio) ✅ Implementada

### 3.1 `TravelExporter` + `TravelImporter` — round-trip ✅

O teste mais valioso do app: garante que export → import não perde nenhum campo. Qualquer campo novo adicionado ao banco que não for adicionado ao exporter/importer quebra esse teste.

**Arquivo:** `androidTest/data/export/ExportImportRoundTripTest.kt` (10 testes — 6 de round-trip/rejeição + 4 da F1: detecção de duplicata, backward-compat v1, `overwriteImport` substitui, `overwriteImport` preserva local em falha)

**Montagem:** banco Room em memória + os 6 repositórios reais construídos à mão (não precisa de Hilt — os `@Inject constructor` são construtores normais) + `TravelExporter`/`TravelImporter` reais. O resto do caminho é o de produção: ZIP em `cacheDir/exports/` via FileProvider, leitura via `ContentResolver`, arquivos restaurados em `filesDir/{Arquivos,Vouchers,Passagens}`.

| Teste | O que garante |
|---|---|
| `roundTrip_preservaViagemDiasAtividadesBadgesEWalkStops` | Todos os campos de trip (datas, coordenadas, hotel), dias (título, alerta, link), atividades (horário, endereço em `mapQuery`+`uberDestination`), badges (inclusive `CUSTOM` com cor) e walk stops (ordem, `sublabel` nulo, `isLast`) |
| `roundTrip_preservaContatosVouchersEPassagens` | Contatos (tipo builtin e `CUSTOM` + `customTypeName`, favorito, telefone nulo), vouchers (`groupName`, `sortOrder`, `isUsed`, `dayId` nulo/preenchido, voucher-link mantém `assetPath` original) e passagem (todos os campos + `notes`) |
| `roundTrip_preservaVoucherSortMode` | Preferência de agrupamento (`BY_DAY`) sobrevive |
| `roundTrip_arquivosDeVoucherDocumentoDoDiaEPassagemViajamNoZip` | Bytes dos 3 tipos de anexo entram no ZIP e são restaurados — os originais são **apagados antes do import** para provar que vieram do ZIP, não de sobras no disco |
| `import_sempreCriaNovaViagem` | Importar 2× o mesmo arquivo cria 2 viagens novas (regra do schema: import nunca sobrescreve) |
| `import_schemaVersionSuperior_recusaComMensagemDeAtualizacao` | `.travel` forjado com `schemaVersion` acima do `SUPPORTED_SCHEMA_VERSION` é rejeitado com a mensagem de "atualize o app" |
| `import_zipSemTripJson_recusa` | ZIP sem `trip.json` é rejeitado |

> **Notas de implementação** (o pseudo-código original deste guia divergia das APIs reais):
> - `TravelImporter` recebe os **6 repositórios**, não o `db`; não existe `UnsupportedSchemaVersionException` — o importer lança `Exception` com mensagem (testa-se via `runCatching` + `message`).
> - O seed usa os repositórios (`createTrip` gera os dias do período automaticamente; `upsertActivity(dayEntityId, entity, badges)`).
> - Para os `.travel` forjados dos testes de rejeição, basta gravar um ZIP em `cacheDir` e passar `Uri.fromFile(...)` — `ContentResolver.openInputStream` aceita `file://` no próprio processo.

---

## Fase 4 — Após melhoria #1 (Hilt) ✅ Implementada

Com Hilt, os ViewModels deixam de ter `companion object Factory`. Os parâmetros de nav (tripId, dayNumber etc.) chegam via `SavedStateHandle`, que o Hilt popula automaticamente a partir dos argumentos da rota do Navigation Compose.

### Convenção: SavedStateHandle em testes JVM

Nos testes JVM, **Hilt não é usado** — instancia-se o ViewModel diretamente. `SavedStateHandle(mapOf(...))` substitui os parâmetros que antes eram passados manualmente para o factory:

```kotlin
// Antes (factory manual):
TripViewModel(repo, tripId = 1L)
EditContactViewModel(repo, tripId = 1L, contactId = 0L, categoryRepo = categoryRepo)

// Depois (padrão Hilt — SavedStateHandle):
TripViewModel(repo, SavedStateHandle(mapOf("tripId" to 1L)))
EditContactViewModel(repo, categoryRepo, SavedStateHandle(mapOf("tripId" to 1L, "contactId" to 0L)))
```

Regras:
- No construtor `@HiltViewModel`, as deps injetáveis (repo, categoryRepo, @ApplicationContext) vêm **antes** do `SavedStateHandle`
- Nos testes, a ordem dos argumentos segue a mesma sequência do construtor
- Não é necessária nenhuma anotação de DI nos arquivos de teste

### 4.1 ViewModels com MockK ✅ Implementado

**Arquivos implementados:**
- `test/ui/trips/TripViewModelTest.kt` — 20 testes (carregamento, contatos, vouchers, atividades, otimismo de estado)
- `test/ui/edit/EditViewModelUiEventTest.kt` — 7 testes de `UiEvent` (NavigateBack, ShowSnackbar, NavigateAfterDelete, reset de isSaving/isDeleting)

**Padrão de buildVm — ViewModel com 1 nav arg:**
```kotlin
private fun buildVm(tripId: Long = 1L): TripViewModel {
    coEvery { repo.getTripData(tripId) } returns fakeTripData()
    return TripViewModel(repo, SavedStateHandle(mapOf("tripId" to tripId)))
}
```

**Padrão de buildVm — ViewModel com múltiplos nav args e deps extras:**
```kotlin
private fun buildContactVm(contactId: Long = 0L) =
    EditContactViewModel(
        repo, categoryRepo,
        SavedStateHandle(mapOf("tripId" to 1L, "contactId" to contactId))
    )
```

**`MainDispatcherRule`** — necessário para testes de coroutine com `viewModelScope`:
```kotlin
// test/util/MainDispatcherRule.kt
class MainDispatcherRule : TestWatcher() {
    val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
    override fun starting(desc: Description?) = Dispatchers.setMain(testDispatcher)
    override fun finished(desc: Description?) = Dispatchers.resetMain()
}
```

**Fixtures — `test/util/Fixtures.kt`:**
```kotlin
fun fakeTripData(
    tripId: Long = 1L,
    vouchers: List<DayVoucher> = emptyList(),
    contacts: List<Contact> = emptyList()
) = TripData(trip = Trip(id = tripId, ...), days = emptyList(),
             contacts = contacts, vouchers = vouchers, boardingPasses = emptyList())

fun fakeVoucher(id: Long = 1L, sortOrder: Int = 0, isUsed: Boolean = false) =
    DayVoucher(id = id, name = "Voucher $id", sortOrder = sortOrder, isUsed = isUsed, ...)

fun fakeContact(id: Long = 1L, sortOrder: Int = 0, isFavorite: Boolean = false) =
    Contact(id = id, name = "Contato $id", sortOrder = sortOrder, isFavorite = isFavorite, ...)
```

---

## Padrões a seguir em todos os testes

### Nomenclatura

Use o padrão `objetoTestado_condicaoOuEntrada_resultadoEsperado`:

```
toggleVoucherUsed_atualizaEstadoImediatamente     ✓
testToggle                                          ✗
test1                                               ✗
```

### Estrutura AAA (Arrange, Act, Assert)

```kotlin
@Test
fun nomeDoTeste() = runTest {
    // Arrange — preparar dados e mocks
    val voucher = fakeVoucher(id = 42L, isUsed = false)
    coEvery { repo.getVoucher(42L) } returns voucher

    // Act — executar a operação
    vm.toggleVoucherUsed(42L, false)

    // Assert — verificar resultado
    assertThat(vm.tripData.value?.vouchers?.find { it.id == 42L }?.isUsed).isTrue()
}
```

Separe as três seções com comentários quando o teste for longo. Em testes curtos e óbvios, os comentários são desnecessários.

### O que testar e o que não testar

| Testar | Não testar |
|---|---|
| Lógica de negócio (reindexação, toggle, parse) | Chamadas triviais de passagem (`vm.name = value`) |
| Comportamento de bordas (JSON malformado, FK nula) | UI renderiza o que recebeu (isso é teste de UI) |
| Invariantes de dados (round-trip export/import) | Internals de framework (Room, Compose) |
| Migrations explícitas | Código gerado pelo KSP |

### Cobertura realista por fase

Não existe cobertura-alvo numérica útil. O critério é: **cada caminho de negócio não-trivial tem pelo menos um teste**. Um teste que verifica o caminho feliz + um que verifica o caso de borda já protege a maioria dos regressions.

---

## Resumo das fases

| Fase | Pré-requisito | O que testar | Ferramenta | Status |
|---|---|---|---|---|
| **1 — Agora** | Nenhum | Migrations, `parseJson()`, DAOs básicos | Room testing, JUnit, Truth | ✅ `MigrationTest.kt` (4, inclui 16→17 da F1) · `ItineraryParserTest.kt` (12, JVM) · `VoucherDaoTest.kt` (7) · `ContactDaoTest.kt` (7) · `TravelActivityDaoTest.kt` (9) · `TripDaoTest.kt` (6) |
| **2 — Após melhoria #3** | Modelo domínio `Trip` | `Mappers.toDomain()`, `toEntity()` | JUnit | ✅ `MappersTest.kt` |
| **2b — Melhorias #4, #6 e #8** | Lógica nos ViewModels + otimismo + funções puras | `TripViewModel`: todos os métodos de lista, testes de timing; `reindexedByGroup()` | MockK, coroutines-test | ✅ `TripViewModelTest.kt` (20 testes) · `VoucherReindexTest.kt` (7 testes) |
| **3 — Após melhoria #2** | Repositórios por domínio | Export/import round-trip, reindexação, detecção de duplicata (F1) | Room in-memory + exporter/importer reais | ✅ `ExportImportRoundTripTest.kt` (10 testes) |
| **4 — Após melhoria #1** | Hilt DI + `@HiltViewModel` | ViewModels com nav args via `SavedStateHandle`; `UiEvent` emitidos em save/delete | MockK, coroutines-test | ✅ `TripViewModelTest.kt` · `EditViewModelUiEventTest.kt` (27 testes no total) |

**Suite atual: 69 testes JVM (`./gradlew test`) + 57 instrumentados (`./gradlew connectedAndroidTest`, requer emulador/dispositivo) = 126 testes. Todas as 4 fases do plano estão implementadas.** (Os instrumentados incluem `NoteRepositoryTest` e o round-trip de notas da F4.)

> **Testes de UI Compose:** tentados na F1 (diálogo de conflito), mas o emulador disponível (API 37) é incompatível com o toolchain de instrumentação Compose desta versão (`espresso`/`mockk-android`). Ficam pendentes de um device/emulador API ≤ 34. A lógica do diálogo é coberta indiretamente pelos testes de dados da F1 (detecção + `overwriteImport`).

Próximos alvos naturais (fora do plano original): testes de UI Compose das telas críticas e um workflow de CI que rode `test` em cada push (o `connectedAndroidTest` exige emulador — viável com `gradle-managed devices` ou emulador no runner).
