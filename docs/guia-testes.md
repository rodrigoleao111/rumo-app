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
│       ├── parser/              ← ItineraryGeneratorTest, MapperTest
│       └── viewmodel/           ← testes de ViewModel (após Hilt)
└── androidTest/java/…           ← testes instrumentados (rodam no dispositivo/emulador)
    └── com/rodrigoleao/gramado2026/
        ├── db/                  ← DAO tests, Migration tests
        └── export/              ← TravelExporter + TravelImporter round-trip
```

**Regra prática:** se o código que você quer testar importa `android.*` ou `androidx.*`, ele vai para `androidTest/`. Se só importa Kotlin/Java puro, vai para `test/`.

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

### A adicionar nas próximas fases

```kotlin
// Fase 3 — DAO e export/import
androidTestImplementation("androidx.test:runner:1.6.2")
androidTestImplementation("androidx.room:room-testing:2.6.1")
androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
```

> Considerar trocar `assertEquals` por `com.google.truth:truth:1.4.4` nas próximas fases: mensagens de falha mais legíveis (`assertThat(result).hasSize(3)` vs `assertEquals(3, result.size)`).

---

## Fase 1 — Disponível hoje, sem nenhuma refatoração

Esses testes não dependem de DI, de modelo de domínio, nem de repositórios separados. Podem ser escritos agora.

### 1.1 Migrations do Room

O maior risco silencioso do app: uma migration esquecida ou errada corrompe o banco dos usuários após uma atualização. `MigrationTestHelper` replica exatamente o que o Room faz em produção.

**Arquivo:** `androidTest/db/MigrationTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    companion object {
        private const val TEST_DB = "migration-test"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TravelDatabase::class.java
    )

    // Testa cada migration individualmente
    @Test
    fun migration15To16_addsCustomTypeName() {
        // Cria banco na versão 15
        helper.createDatabase(TEST_DB, 15).apply {
            execSQL("""
                INSERT INTO contacts (id, tripId, name, role, phone, type, hasWhatsApp, isEmergency, sortOrder, isFavorite)
                VALUES (1, 1, 'Guia', 'AGENCY', NULL, 'AGENCY', 0, 0, 0, 0)
            """)
            close()
        }

        // Aplica a migration 15→16 e valida que o schema bate com as entidades anotadas
        val db = helper.runMigrationsAndValidate(TEST_DB, 16, true, MIGRATION_15_16)

        // Verifica que a linha existente sobreviveu com o default correto
        val cursor = db.query("SELECT customTypeName FROM contacts WHERE id = 1")
        cursor.moveToFirst()
        assertThat(cursor.getString(0)).isEqualTo("")   // default da migration
        cursor.close()
    }

    // Testa o caminho completo: versão 3 até a atual
    @Test
    fun allMigrationsFromVersion3() {
        helper.createDatabase(TEST_DB, 3).close()
        helper.runMigrationsAndValidate(
            TEST_DB, TravelDatabase.CURRENT_VERSION, true,
            *TravelDatabase.ALL_MIGRATIONS   // array com todas as migrations
        )
    }
}
```

> **Pré-requisito no código de produção:** expor as migrations como constante acessível:
> ```kotlin
> // TravelDatabase.kt
> companion object {
>     const val CURRENT_VERSION = 16
>     val ALL_MIGRATIONS = arrayOf(
>         MIGRATION_3_4, MIGRATION_4_5, /* ... */ MIGRATION_15_16
>     )
> }
> ```

---

### 1.2 `ItineraryGenerator.parseJson()` — parser de JSON da IA

Função estática pura: entra string, sai lista de dias. Nenhuma dependência Android.

**Arquivo:** `test/parser/ItineraryParserTest.kt`

```kotlin
class ItineraryParserTest {

    @Test
    fun parseDiaComAtividadesCompletas() {
        val json = """
        {
          "days": [{
            "date": "2026-06-09",
            "title": "Chegada em Gramado",
            "alert": "Check-in a partir das 14h",
            "activities": [{
              "time": "14h00",
              "name": "Check-in no hotel",
              "emoji": "🏨",
              "detail": "Hotel Serra Azul",
              "address": "Rua Garibaldi, 152",
              "badges": ["BOOKED"]
            }]
          }]
        }
        """
        val result = ItineraryGenerator.parseJson(json)

        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("Chegada em Gramado")
        assertThat(result[0].alert).isEqualTo("Check-in a partir das 14h")
        assertThat(result[0].activities).hasSize(1)
        assertThat(result[0].activities[0].name).isEqualTo("Check-in no hotel")
        assertThat(result[0].activities[0].badges).contains("BOOKED")
    }

    @Test
    fun parseCamposOpcionaisAusentesNaoCrasha() {
        // IA às vezes omite campos — o parser precisa ser tolerante
        val json = """{"days": [{"activities": [{"name": "Passeio livre"}]}]}"""
        val result = ItineraryGenerator.parseJson(json)
        assertThat(result).hasSize(1)
        assertThat(result[0].activities[0].name).isEqualTo("Passeio livre")
        assertThat(result[0].activities[0].emoji).isNotNull()   // deve ter um default
    }

    @Test
    fun parseJsonMalformadoNaoCrasha() {
        // JSON inválido que a IA pode retornar com texto extra antes/depois
        val json = "Claro! Aqui está o roteiro:\n```json\n{\"days\": []}\n```"
        // O parser extrai o JSON do bloco de markdown — não deve lançar exceção
        val result = runCatching { ItineraryGenerator.parseJson(json) }
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun parseBadgesCustomizadosPreservaNome() {
        val json = """
        {"days": [{"activities": [{
          "name": "Jantar",
          "badges": [{"type": "CUSTOM", "label": "Reserva", "color": "#FF5722"}]
        }]}]}
        """
        val result = ItineraryGenerator.parseJson(json)
        val badge = result[0].activities[0].badges.first()
        assertThat(badge).contains("Reserva")
    }
}
```

---

### 1.3 DAOs básicos — banco em memória

Qualquer DAO pode ser testado com banco Room em memória hoje. Escolha começar pelos mais críticos.

**Arquivo:** `androidTest/db/VoucherDaoTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class VoucherDaoTest {

    private lateinit var db: TravelDatabase
    private lateinit var dao: VoucherDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TravelDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.voucherDao()
        // Insere trip de referência (FK obrigatória)
        runBlocking { db.tripDao().insert(TripEntity(id = 1, name = "Gramado", ...)) }
    }

    @After fun tearDown() = db.close()

    @Test
    fun insertAndGetForTrip() = runTest {
        dao.insert(VoucherEntity(id = 0, tripId = 1, name = "Bondinho", sortOrder = 0, isUsed = false, ...))
        val result = dao.getForTrip(1)
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Bondinho")
    }

    @Test
    fun toggleIsUsed_persisteCorretamente() = runTest {
        val id = dao.insert(VoucherEntity(id = 0, tripId = 1, name = "Bondinho", isUsed = false, ...))
        dao.updateIsUsed(id, true)
        assertThat(dao.getForTrip(1)[0].isUsed).isTrue()
    }

    @Test
    fun deletar_removeApenas1Voucher() = runTest {
        val id1 = dao.insert(VoucherEntity(id = 0, tripId = 1, name = "Bondinho", sortOrder = 0, ...))
        dao.insert(VoucherEntity(id = 0, tripId = 1, name = "Dreamland", sortOrder = 1, ...))
        dao.deleteById(id1)
        val result = dao.getForTrip(1)
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Dreamland")
    }
}
```

**DAOs prioritários para testar primeiro** (por risco de bug):

| DAO | Razão |
|---|---|
| `VoucherDao` | Lógica de `sortOrder` — fácil de errar na reindexação |
| `ContactDao` | `sortOrder` por viagem + `isFavorite` + grupo por `type` |
| `TravelActivityDao` | Queries bulk (`IN (:dayIds)`) — risco de N+1 silencioso |
| `TripDao` | `Flow<List<TripEntity>>` — verifica que emite corretamente |

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

**Suite atual: 57 testes JVM, todos passando.**

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

**Suite atual:** 57 testes JVM, todos passando.

---

## Fase 3 — Após melhoria #2 (repositórios por domínio)

Com repositórios menores e injetáveis, os testes de integração ficam cirúrgicos.

### 3.1 `TravelExporter` + `TravelImporter` — round-trip

O teste mais valioso do app: garante que export → import não perde nenhum campo. Qualquer campo novo adicionado ao banco que não foi adicionado ao exporter quebra esse teste.

**Arquivo:** `androidTest/export/ExportImportRoundTripTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class ExportImportRoundTripTest {

    private lateinit var db: TravelDatabase
    private lateinit var tripRepo: TripRepository
    private lateinit var exporter: TravelExporter
    private lateinit var importer: TravelImporter
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, TravelDatabase::class.java)
            .allowMainThreadQueries().build()
        tripRepo  = TripRepository(db)
        exporter  = TravelExporter(context, tripRepo)
        importer  = TravelImporter(context, db)
    }

    @After fun tearDown() = db.close()

    @Test
    fun exportAndImport_preservaDiasEAtividades() = runTest {
        // 1. Cria viagem completa
        val tripId = tripRepo.createTrip("Gramado 2026", "Gramado, RS", "🏔️",
            startDate = "2026-06-09", endDate = "2026-06-13")
        // Adiciona atividade no dia 1
        tripRepo.upsertActivity(tripId, dayNumber = 1, TravelActivity(
            id = 0L, time = "14h00", emoji = "🏨", name = "Check-in",
            detail = "Serra Azul", mapQuery = "Rua Garibaldi", uberDestination = "Rua Garibaldi",
            badges = listOf(Badge(BadgeType.BOOKED, "Reservado", null)),
            walkStops = emptyList()
        ))

        // 2. Exporta
        val uri = exporter.export(tripId)
        assertThat(uri).isNotNull()

        // 3. Importa
        val importedId = importer.import(uri)
        assertThat(importedId).isGreaterThan(0L)

        // 4. Compara
        val original = tripRepo.getTripData(tripId)
        val imported  = tripRepo.getTripData(importedId)

        assertThat(imported.days).hasSize(original.days.size)
        val origDay1 = original.days.first()
        val impDay1  = imported.days.first()
        assertThat(impDay1.activities).hasSize(origDay1.activities.size)
        assertThat(impDay1.activities[0].name).isEqualTo(origDay1.activities[0].name)
        assertThat(impDay1.activities[0].badges).hasSize(1)
    }

    @Test
    fun exportAndImport_preservaVouchersComSortOrderEIsUsed() = runTest {
        val tripId = tripRepo.createTrip(...)
        // Adiciona vouchers com sortOrder e isUsed específicos
        voucherRepo.insert(DayVoucher(name = "Bondinho", sortOrder = 0, isUsed = true, ...))
        voucherRepo.insert(DayVoucher(name = "Dreamland", sortOrder = 1, isUsed = false, ...))

        val uri = exporter.export(tripId)
        val importedId = importer.import(uri)

        val vouchers = tripRepo.getTripData(importedId).vouchers
        assertThat(vouchers.find { it.name == "Bondinho" }?.isUsed).isTrue()
        assertThat(vouchers.find { it.name == "Dreamland" }?.sortOrder).isEqualTo(1)
    }

    @Test
    fun importZipComSchemaVersionInvalida_lancaExcecao() = runTest {
        val zipUri = criarZipComSchemaVersion(999)
        val result = runCatching { importer.import(zipUri) }
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(UnsupportedSchemaVersionException::class.java)
    }
}
```

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

**Suite atual: 57 testes JVM, todos passando.**

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
| **1 — Agora** | Nenhum | Migrations, `parseJson()`, DAOs básicos | Room testing, JUnit | A fazer |
| **2 — Após melhoria #3** | Modelo domínio `Trip` | `Mappers.toDomain()`, `toEntity()` | JUnit | ✅ `MappersTest.kt` |
| **2b — Melhorias #4, #6 e #8** | Lógica nos ViewModels + otimismo + funções puras | `TripViewModel`: todos os métodos de lista, testes de timing; `reindexedByGroup()` | MockK, coroutines-test | ✅ `TripViewModelTest.kt` (20 testes) · `VoucherReindexTest.kt` (7 testes) |
| **3 — Após melhoria #2** | Repositórios por domínio | Export/import round-trip, reindexação | Room in-memory | A fazer |
| **4 — Após melhoria #1** | Hilt DI + `@HiltViewModel` | ViewModels com nav args via `SavedStateHandle`; `UiEvent` emitidos em save/delete | MockK, coroutines-test | ✅ `TripViewModelTest.kt` · `EditViewModelUiEventTest.kt` (27 testes no total) |

Comece pela Fase 1. As migrations e o `parseJson()` são os pontos de maior risco atual e não exigem nenhuma mudança no código de produção.
