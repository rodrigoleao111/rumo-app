# Módulo 08 — Compartilhamento e Importação de Viagens

**Telas:** `ShareTripScreen` · `ImportTripScreen`  
**Arquivos:** `ui/share_trip/ShareTripScreen.kt` · `ui/import_trip/ImportTripScreen.kt`  
**ViewModels:** `ShareTripViewModel` · `ImportTripViewModel`  
**Camada de dados:** `data/export/TravelExporter.kt` · `data/import/TravelImporter.kt`  
**Entry points de navegação:**
- Compartilhar: ícone de compartilhar na TopAppBar da viagem aberta; swipe esquerdo na lista de viagens
- Importar: card "Importar viagem" em `TripsListScreen`; intent externo `ACTION_VIEW` com arquivo `.travel`

---

## Visão geral

Implementa o ciclo completo de exportação e importação de viagens no formato `.travel` — um arquivo ZIP renomeado contendo `trip.json` + pastas de arquivos (`documents/`, `vouchers/`, `boarding/`). A exportação gera o arquivo em `cacheDir` e o compartilha via `ACTION_SEND`. A importação aceita o arquivo tanto pelo seletor interno quanto por intent externo (arquivo `.travel` aberto de fora do app).

---

## Padrão de arquitetura

Ambas as telas seguem **MVVM** com um padrão de estado de fase (`sealed class`).

| Camada | Compartilhamento | Importação |
|---|---|---|
| **View** | `ShareTripScreen` — stateless, reage ao `SharePhase` | `ImportTripScreen` — stateless, reage ao `ImportPhase` |
| **ViewModel** | `ShareTripViewModel` — orquestra `TravelExporter` | `ImportTripViewModel` — orquestra `TravelImporter` |
| **Dados** | `TravelExporter.export(tripId): Uri` | `TravelImporter.import(uri): Long` |

---

## Fluxo de compartilhamento

```
Usuário toca "Compartilhar viagem"
  └─ ShareTripViewModel.export()
       ├─ phase → Exporting  (dialog de loading)
       ├─ TravelExporter.export(tripId)
       │    ├─ repo.getTripData(tripId) → TripData completo
       │    ├─ buildJson(data) → String
       │    ├─ ZipOutputStream → cacheDir/exports/<nome>.travel
       │    │    ├─ trip.json
       │    │    ├─ documents/<nome>  (dayDocumentPath, se existir)
       │    │    ├─ boarding/<nome>   (documentPath, se existir)
       │    │    └─ vouchers/<assetPath> (asset ou filesDir)
       │    └─ FileProvider.getUriForFile(...)
       └─ phase → Ready(uri)
            └─ ShareTripScreen LaunchedEffect(phase)
                 ├─ Intent(ACTION_SEND, application/octet-stream, FLAG_GRANT_READ_URI_PERMISSION)
                 └─ viewModel.clearReady()  → phase → Idle
```

## Fluxo de importação

```
Usuário seleciona arquivo (ou abre .travel externamente)
  └─ ImportTripViewModel.startImport(uri)
       ├─ phase → Importing  (dialog de loading)
       ├─ TravelImporter.import(uri)
       │    ├─ parseZip(uri)
       │    │    ├─ ZipInputStream via ContentResolver.openInputStream(uri)
       │    │    ├─ "trip.json" → parseTripJson()
       │    │    ├─ "documents/*" → Map<name, ByteArray>
       │    │    ├─ "vouchers/*"  → Map<path, ByteArray>
       │    │    └─ "boarding/*"  → Map<name, ByteArray>
       │    ├─ tripRepo.createTrip(...)
       │    ├─ copia documents → filesDir/Arquivos/
       │    ├─ dayRepo.updateDay() × N  (título, alerta, link, documento)
       │    ├─ activityRepo.upsertActivity() + badges + walkStops × N
       │    ├─ contactRepo.upsertContact() × N
       │    ├─ copia voucherFiles → filesDir/Vouchers/
       │    ├─ voucherRepo.upsertVoucher() × N
       │    ├─ copia boardingFiles → filesDir/Passagens/
       │    └─ boardingPassRepo.upsertBoardingPass() × N
       └─ phase → Done(tripId)
            └─ ImportTripScreen LaunchedEffect(phase)
                 └─ onImported(tripId)  → navega para a viagem
```

---

## `SharePhase` e `ImportPhase`

### `SharePhase` (em `ShareTripViewModel.kt`)

```kotlin
sealed class SharePhase {
    object Idle      : SharePhase()
    object Exporting : SharePhase()
    data class Ready(val uri: Uri)        : SharePhase()
    data class Error(val message: String) : SharePhase()
}
```

| Fase | UI exibida |
|---|---|
| `Idle` | Tela de informações + botão "Compartilhar" |
| `Exporting` | `Dialog` não cancelável com spinner ("Preparando arquivo…") |
| `Ready(uri)` | `LaunchedEffect` dispara `ACTION_SEND`; transição imediata para `Idle` |
| `Error(msg)` | `AlertDialog` com mensagem de erro + botão "OK" → `Idle` |

### `ImportPhase` (em `ImportTripViewModel.kt`)

```kotlin
sealed class ImportPhase {
    object Idle      : ImportPhase()
    object Importing : ImportPhase()
    data class Done(val tripId: Long)      : ImportPhase()
    data class Error(val message: String)  : ImportPhase()
    data class Duplicate(                    // F1 — UUID já existe no banco
        val existingTripId: Long,
        val existingTripName: String,
        val existingLastEditedAt: Long,
        val incomingLastEditedAt: Long,
        val pendingUri: Uri
    ) : ImportPhase()
}
```

| Fase | UI exibida |
|---|---|
| `Idle` | Tela de informações + botão "Selecionar arquivo" |
| `Importing` | `Dialog` não cancelável com spinner ("Importando viagem…") |
| `Done(tripId)` | `LaunchedEffect` chama `onImported(tripId)` → navega |
| `Error(msg)` | `AlertDialog` com "Tentar novamente" → `Idle` |
| `Duplicate(...)` | `AlertDialog` de conflito (F1) — "Manter local" (`dismissDuplicate`) × "Importar" (`overwriteImport`) |

**Diferença entre as telas:** `ShareTripScreen` usa "OK" no dialog de erro; `ImportTripScreen` usa "Tentar novamente". A ação é `dismissError()` em ambos → `Idle`.

---

## `ShareTripViewModel`

```kotlin
@HiltViewModel
class ShareTripViewModel @Inject constructor(
    private val exporter: TravelExporter,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val tripId: Long = checkNotNull(savedStateHandle["tripId"])
    private val _phase = MutableStateFlow<SharePhase>(SharePhase.Idle)
    val phase: StateFlow<SharePhase> = _phase.asStateFlow()

    fun export() {
        if (_phase.value is SharePhase.Exporting) return   // guard contra double-tap
        _phase.value = SharePhase.Exporting
        viewModelScope.launch {
            _phase.value = try {
                SharePhase.Ready(exporter.export(tripId))
            } catch (e: Exception) {
                SharePhase.Error(e.message ?: "Erro ao preparar o arquivo.")
            }
        }
    }

    fun clearReady()    { _phase.value = SharePhase.Idle }
    fun dismissError()  { _phase.value = SharePhase.Idle }
}
```

**Guard de duplo toque:** `if (_phase.value is SharePhase.Exporting) return` — evita iniciar uma segunda exportação enquanto a primeira ainda está em andamento.

**`TravelExporter` injetado via Hilt:** `@Singleton @Inject constructor` — não é mais instanciado manualmente. `tripId` vem do `SavedStateHandle` (populado pelo Navigation Compose a partir dos argumentos de rota).

---

## `ImportTripViewModel`

```kotlin
@HiltViewModel
class ImportTripViewModel @Inject constructor(
    private val importer: TravelImporter
) : ViewModel() {
    private val _phase = MutableStateFlow<ImportPhase>(ImportPhase.Idle)
    val phase: StateFlow<ImportPhase> = _phase.asStateFlow()

    fun startImport(uri: Uri) {
        if (_phase.value is ImportPhase.Importing) return
        _phase.value = ImportPhase.Importing
        viewModelScope.launch {
            _phase.value = try {
                ImportPhase.Done(importer.import(uri))
            } catch (e: Exception) {
                ImportPhase.Error(e.message ?: "Erro ao importar viagem.")
            }
        }
    }

    fun dismissError() { _phase.value = ImportPhase.Idle }
}
```

**`TravelImporter` injetado via Hilt:** `@Singleton @Inject constructor(@ApplicationContext context, TripRepository, DayRepository, ActivityRepository, ContactRepository, VoucherRepository, BoardingPassRepository)` — todos os 6 repositórios são injetados pelo Hilt automaticamente. O ViewModel não precisa conhecê-los.

---

## `ShareTripScreen` — comportamento da UI

**Disparo do `ACTION_SEND`:**
```kotlin
LaunchedEffect(phase) {
    if (phase is SharePhase.Ready) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, phase.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar viagem"))
        viewModel.clearReady()
    }
}
```

O intent é disparado dentro de `LaunchedEffect(phase)` — não dentro de um callback de botão — para garantir execução após a composição e evitar que seja chamado múltiplas vezes em recomposições. `clearReady()` é chamado imediatamente após o intent para voltar ao estado `Idle`.

**Dialogs:**
- `Exporting` → `Dialog(properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false))` — não cancelável
- `Error` → `AlertDialog` com botão "OK"

**Conteúdo informativo da tela:** lista com ícones do que o arquivo contém (🗓️ dias/atividades, 🏨 hospedagem, 👥 contatos, 📄 documentos, 🎟️ vouchers, ✈️ cartões de embarque) + aviso sobre dados pessoais antes do botão.

---

## `ImportTripScreen` — comportamento da UI

**Intent externo (auto-importação):**
```kotlin
ImportTripScreen(initialUri: Uri? = null, ...)

LaunchedEffect(initialUri) {
    if (initialUri != null) viewModel.startImport(initialUri)
}
```

Quando `initialUri != null` (arquivo `.travel` aberto de fora do app via `ACTION_VIEW`), a importação começa automaticamente sem interação do usuário. Quando `null`, o usuário deve selecionar o arquivo manualmente.

**Seletor de arquivo:**
```kotlin
val fileLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri -> uri?.let { viewModel.startImport(it) } }

// acionado pelo botão:
fileLauncher.launch(arrayOf("*/*"))
```

`arrayOf("*/*")` é necessário porque alguns gerenciadores de arquivos não reconhecem MIME types customizados para `.travel`.

**Navegação após importação:**
```kotlin
LaunchedEffect(phase) {
    if (phase is ImportPhase.Done) onImported(phase.tripId)
}
```

**Nota exibida ao usuário:** "Uma nova viagem será criada. Suas viagens atuais não serão alteradas." — evita confusão sobre possível sobrescrita. **Exceção (F1):** se o arquivo for de uma viagem que já existe (mesmo `tripUuid`), abre-se o diálogo de conflito, onde o usuário pode optar por substituir a versão local.

### Diálogo de conflito (`ImportPhase.Duplicate`)

Renderizado quando `import()` detecta um UUID já existente. Mostra o nome da viagem, "Versão local" × "Versão importada" (`dd/MM/yyyy HH:mm`; timestamp 0 vira `—`) e uma mensagem que varia com a comparação dos timestamps:

| Comparação | Mensagem | Botão "Importar" |
|---|---|---|
| importada mais recente | "A versão importada é mais recente. Deseja substituir a versão local?" | `GreenMoss` |
| local mais recente | "⚠ Atenção: a versão local é mais recente. Importar substituirá dados mais novos." | `colorScheme.error` |
| idênticas | "As versões são idênticas." | `GreenMoss` |

Dois botões: **"Manter local"** (`dismissDuplicate()` → `Idle`) e **"Importar"** (`overwriteImport(pendingUri, existingTripId)`). O roadmap previa um terceiro botão "Cancelar", unificado com "Manter local" por serem a mesma ação.

---

## `TravelExporter` — camada de dados

**Localização:** `data/export/TravelExporter.kt`

**Método principal:** `suspend fun export(tripId: Long): Uri`

### Estrutura do ZIP gerado

```
<nome_viagem>.travel  (ZIP renomeado)
├── trip.json              ← roteiro completo (schema v1)
├── documents/
│   └── <nome_arquivo>     ← dayDocumentPath de cada dia (se existir)
├── boarding/
│   └── <documentName>     ← documentPath de cada boarding pass (se existir)
└── vouchers/
    └── <assetPath>        ← arquivo do voucher (asset ou filesDir)
```

**Destino do arquivo:** `context.cacheDir/exports/<nome_sanitizado>.travel`. O nome é sanitizado com `replace(Regex("[^a-zA-Z0-9_\\-]"), "_")`.

**URI retornada:** via `FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)` — necessário para compartilhar arquivos de `cacheDir` com outros apps.

### Lógica de leitura de vouchers (`tryReadVoucherFile`)

```kotlin
private fun tryReadVoucherFile(absoluteOrRelative: String, assetPath: String): ByteArray? {
    val localFile = File(absoluteOrRelative)
    if (localFile.isAbsolute && localFile.exists()) return localFile.readBytes()
    val inVouchersDir = File(context.filesDir, "Vouchers/$assetPath")
    if (inVouchersDir.exists()) return inVouchersDir.readBytes()
    return runCatching { context.assets.open(assetPath).use { it.readBytes() } }.getOrNull()
}
```

Três tentativas em ordem de prioridade:
1. Caminho absoluto (`assetPath` começa com `/` ou é `filesDir/...`)
2. `filesDir/Vouchers/<assetPath>` (voucher importado de outro dispositivo)
3. `assets/<assetPath>` (voucher bundled com o app)

Se nenhuma funcionar, o voucher é omitido do ZIP silenciosamente (sem lançar exceção).

### `buildJson` — campos do `trip.json`

| Campo raiz | Tipo | Origem |
|---|---|---|
| `schemaVersion` | `Int` (1) | fixo |
| `exportedAt` | `String` ISO datetime | `LocalDateTime.now()` |
| `trip` | `Object` | ver abaixo |

**Objeto `trip`:** `name`, `destination`, `coverEmoji`, `startDate`, `endDate`, `latitude` (null → `JSONObject.NULL`), `longitude`, `voucherSortMode`, `hotel { name, address, phone }`, `days[]`, `contacts[]`, `vouchers[]`, `boardingPasses[]`.

**Objeto `day`:** `dayNumber`, `date`, `dayOfWeek`, `title`, `dayAlert` (null → `JSONObject.NULL`), `linkUrl`, `linkLabel`, `documentName` (nome do arquivo sem caminho, null se ausente), `documentTitle`, `activities[]`.

**Objeto `activity`:** `position`, `time`, `emoji`, `name`, `detail`, `address` (= `mapQuery`), `badges[]`, `walkStops[]`.

**Objeto `badge`:** `type` (enum name), `label`, `color` (null → `JSONObject.NULL`).

**Objeto `walkStop`:** `order`, `name`, `detail`, `emoji`, `isLast`.

**Objeto `contact`:** `name`, `role`, `phone`, `type`, `hasWhatsApp`, `isEmergency`, `customTypeName` (vazio → `JSONObject.NULL`), `sortOrder`, `isFavorite`.

**Objeto `voucher`:** `emoji`, `groupName`, `name`, `person`, `assetPath`, `dayId`, `sortOrder`, `isUsed`.

**Objeto `boardingPass`:** `transportType`, `origin`, `originCity`, `destination`, `destinationCity`, `flightNumber`, `date`, `boardingTime`, `passenger`, `walletUrl`, `documentName` (vazio → `JSONObject.NULL`), `notes` (vazio → `JSONObject.NULL`).

---

## `TravelImporter` — camada de dados

**Localização:** `data/import/TravelImporter.kt` (package `data.import_trip`)

**Método principal:** `suspend fun import(uri: Uri): Long` — retorna o `tripId` criado.

### Validação de versão

```kotlin
val schemaVer = root.optInt("schemaVersion", 1)
if (schemaVer > SUPPORTED_SCHEMA_VERSION)
    throw Exception("Este arquivo foi criado por uma versão mais recente do app. Atualize o app para importá-lo.")

companion object {
    private const val SUPPORTED_SCHEMA_VERSION = 2   // F1: tripUuid + lastEditedAt
}
```

Arquivos com `schemaVersion` maior que o suportado lançam exceção — a mensagem chega até o `AlertDialog` de erro via `ImportPhase.Error`.

### Detecção de duplicata e sobrescrita (F1)

`import(uri)` = **parse → detecção → `writeToDb`**:

```kotlin
suspend fun import(uri: Uri): Long {
    val parsed = parseZip(uri)
    tripRepo.findByUuid(parsed.trip.tripUuid)?.let { existing ->
        throw DuplicateTripException(existing.id, existing.name, existing.lastEditedAt, parsed.trip.lastEditedAt)
    }
    return writeToDb(parsed)   // insere como nova viagem
}
```

- `findByUuid` retorna `null` para UUID vazio (arquivos v1) → importação normal, sem detecção.
- Se encontra, lança `DuplicateTripException`; o `ImportTripViewModel` transforma em `ImportPhase.Duplicate` e a tela abre o diálogo de conflito.

`overwriteImport(uri, existingTripId)` (quando o usuário confirma) segue uma ordem deliberada, **mais segura que delete-antes-de-inserir**:

```kotlin
suspend fun overwriteImport(uri: Uri, existingTripId: Long): Long {
    val oldPaths = collectManagedFilePaths(tripRepo.getTripData(existingTripId))
    val newTripId = writeToDb(parseZip(uri))          // 1. importa a nova PRIMEIRO (pode lançar)
    tripRepo.getTripEntity(existingTripId)?.let { tripRepo.deleteTrip(it) }  // 2. remove a antiga (CASCADE)
    val newPaths = collectManagedFilePaths(tripRepo.getTripData(newTripId))
    (oldPaths - newPaths).forEach { runCatching { File(it).delete() } }      // 3. limpa órfãos
    return newTripId
}
```

- Se a importação (passo 1) falhar, a viagem local **não é deletada** → o usuário não perde dados (UC-F1-10). Escrita de arquivo não é transacional, por isso essa ordem é preferível a envolver tudo num `withTransaction`.
- A limpeza remove só os arquivos da antiga **que a nova não reutiliza** e **apenas dentro de `filesDir`** (`collectManagedFilePaths`) — nunca apaga anexo de outra viagem.

### Sequência de `writeToDb` (importação propriamente dita)

1. `parseZip(uri)` → `ParsedZip(trip, documents, voucherFiles, boardingFiles)`
   - Lê o ZIP via `ContentResolver.openInputStream(uri)` + `ZipInputStream`
   - `trip.json` → `parseTripJson()` → `ExportedTrip`
   - `documents/*` → `Map<String, ByteArray>` (chave = nome sem prefixo)
   - `vouchers/*` → `Map<String, ByteArray>` (chave = caminho relativo sem `vouchers/`)
   - `boarding/*` → `Map<String, ByteArray>` (chave = nome sem prefixo)

2. `tripRepo.createTrip(..., tripUuid, lastEditedAt)` → cria viagem e dias; **preserva o UUID/timestamp do arquivo** (v2) ou gera novos (v1). Retorna `tripId`

3. `tripRepo.saveVoucherSortMode(tripId, mode)` — só se `mode != "BY_CATEGORY"` (default não precisa ser gravado)

4. Copia `documents` → `filesDir/Arquivos/<nome>`. Mapa `docPaths: Map<String, String>` guarda `documentName → localPath`.

5. Para cada dia: `dayRepo.getDayEntity(tripId, dayNumber)` → `dayRepo.updateDay(entity.copy(...))` com título, alerta, link e caminho do documento. Atividades, badges e walkStops via `activityRepo.upsertActivity()` + `activityRepo.insertWalkStop()`.

6. Contatos: `contactRepo.upsertContact()`. Tipos desconhecidos são normalizados para `CUSTOM`.
   ```kotlin
   val isBuiltinType = ContactType.entries.any { it.name == expContact.type && it != CUSTOM }
   contactType = if (isBuiltinType) expContact.type else "CUSTOM"
   ```

7. Vouchers: copia arquivo de `voucherFiles[assetPath]` → `filesDir/Vouchers/<assetPath>`. Se o arquivo não está no ZIP (voucher de outro dispositivo sem o original), mantém `assetPath` original. `voucherRepo.upsertVoucher()`.

8. Boarding passes: copia arquivo de `boardingFiles[documentName]` → `filesDir/Passagens/<documentName>`. `boardingPassRepo.upsertBoardingPass()`.

### `parseTripJson` — parsing defensivo

O parser usa `optString`/`optInt`/`optBoolean` com defaults em vez de `getString`/`getInt` para campos opcionais, garantindo compatibilidade com arquivos mais antigos que não tinham todos os campos. Campos que podem ser `null` no JSON são verificados com `takeIf { it.isNotBlank() && it != "null" }` — necessário porque `optString()` retorna `"null"` (string literal) para valores JSON nulos.

**Exemplo:**
```kotlin
dayAlert = day.optString("dayAlert").takeIf { it.isNotBlank() && it != "null" }
```

### Estruturas internas de parse (`private data class`)

O `TravelImporter` define estruturas intermediárias privadas para o parse do JSON: `ExportedTrip`, `ExportedDay`, `ExportedActivity`, `ExportedBadge`, `ExportedWalkStop`, `ExportedContact`, `ExportedVoucher`, `ExportedBoardingPass`. Essas classes existem apenas para desacoplar o parse do JSON da escrita no banco — não são expostas fora do arquivo.

---

## Intent externo (`.travel` aberto de fora do app)

O `AndroidManifest` registra filtros de intent para `ACTION_VIEW` com três MIME types:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:mimeType="application/octet-stream" />
    <data android:mimeType="application/zip" />
    <data android:mimeType="application/x-zip-compressed" />
</intent-filter>
```

`MainActivity` detecta o intent e passa a URI para `AppNavigation` → `ImportTripScreen(initialUri = uri)`, que inicia a importação automaticamente via `LaunchedEffect(initialUri)`.

---

## FileProvider

Arquivo exportado (`cacheDir/exports/`) está coberto pelo FileProvider (`file_paths.xml`) via `<cache-path name="exports" path="exports/" />`. Documentos internos importados (`filesDir/Arquivos/`, `filesDir/Vouchers/`, `filesDir/Passagens/`) têm caminhos registrados separadamente.

Authority: `com.rodrigoleao.gramado2026.fileprovider`

---

## Composables e símbolos principais (resumo)

| Símbolo | Arquivo | Responsabilidade |
|---|---|---|
| `SharePhase` | `ShareTripViewModel.kt` | Estados do fluxo de exportação |
| `ShareTripViewModel` | `ShareTripViewModel.kt` | Orquestra `TravelExporter`, expõe `phase` |
| `ShareTripScreen` | `ShareTripScreen.kt` | UI de compartilhamento; dispara `ACTION_SEND` em `LaunchedEffect` |
| `ImportPhase` | `ImportTripViewModel.kt` | Estados do fluxo de importação |
| `ImportTripViewModel` | `ImportTripViewModel.kt` | Orquestra `TravelImporter`, expõe `phase` |
| `ImportTripScreen` | `ImportTripScreen.kt` | UI de importação; suporta `initialUri` para intent externo |
| `TravelExporter` | `data/export/TravelExporter.kt` | Gera ZIP `.travel` em `cacheDir`; retorna URI via FileProvider |
| `TravelImporter` | `data/import/TravelImporter.kt` | Lê ZIP `.travel`, parseia JSON, grava tudo no banco |
| `ExportedTrip` (e derivados) | `TravelImporter.kt` | Estruturas privadas de parse intermediário |
| `ParsedZip` | `TravelImporter.kt` | Resultado do `parseZip()`: trip + arquivos |

---

## Checklist para futuras modificações

- **Novo campo no `trip.json`:** adicionar em `buildJson()` (TravelExporter) → adicionar em `parseTripJson()` com `opt*` defensivo → atualizar `ExportedTrip`/`ExportedDay`/etc. → passar para `repo.createTrip()` ou `repo.updateDay()` → atualizar `docs/travel-export-schema.md`.
- **Novo tipo de arquivo no ZIP:** adicionar nova pasta (ex: `images/`) — em `TravelExporter.export()` dentro do `ZipOutputStream.use` e em `TravelImporter.parseZip()` dentro do `while (entry != null)`.
- **Incrementar `schemaVersion`:** ao fazer mudanças incompatíveis com versões anteriores, incrementar a constante `SUPPORTED_SCHEMA_VERSION` em `TravelImporter` e o valor literal em `TravelExporter.buildJson()`. Versões mais antigas do app lançarão erro ao tentar importar o arquivo novo.
- **Novo MIME type para `.travel`:** adicionar `<data android:mimeType="..."/>` no intent filter do `AndroidManifest`. Garantir que `ShareTripScreen` usa `type = "application/octet-stream"` no `ACTION_SEND` (compatível com a maioria dos apps de compartilhamento).
- **Erro silencioso de voucher:** `tryReadVoucherFile` retorna `null` sem lançar exceção quando o arquivo não é encontrado. Para reportar ao usuário quais vouchers falharam, acumule os nomes em uma lista e exiba um aviso pós-exportação.
- **Migrar `schemaVersion` para suporte a múltiplas versões:** atualmente qualquer arquivo com `schemaVersion > 1` é rejeitado. Para suportar migração progressiva, implemente branches no `parseTripJson()` por versão (ex: `when (schemaVer) { 1 -> parseV1(); 2 -> parseV2() }`).
- **Portão e URL da passagem não são exportados:** esses dados ficam em `SharedPreferences` no dispositivo e não fazem parte do `trip.json`. Para incluí-los no export, seria necessário ler o `SharedPreferences` em `TravelExporter` e adicioná-los ao JSON de cada boarding pass — e ajustar `TravelImporter` para gravá-los (em SharedPreferences ou migrar para o banco).
