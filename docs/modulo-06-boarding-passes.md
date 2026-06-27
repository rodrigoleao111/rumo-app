# Módulo 06 — Passagens

**Telas:** `BoardingPassScreen` · `EditBoardingPassScreen`  
**Arquivos:** `ui/boarding/BoardingPassScreen.kt` · `ui/edit/EditBoardingPassScreen.kt`  
**ViewModels:** nenhum em `BoardingPassScreen` (stateless) · `EditBoardingPassViewModel` em `EditBoardingPassScreen`  
**Entry point de navegação:** aba "Passagens" dentro de `MainPagerScreen`

---

## Visão geral

Exibe os cartões de embarque agrupados por data. Cada grupo de mesmo voo/trecho é renderizado em um único card que concentra as infos comuns (origem, destino, número do serviço, portão) e lista os passageiros individualmente. Suporta cinco tipos de transporte com layout adaptativo, portão editável inline (somente voos), lembrete de check-in (somente voos) e abertura de passagem por link ou arquivo.

---

## Padrão de arquitetura

| Tela | Padrão | ViewModel |
|---|---|---|
| `BoardingPassScreen` | Stateless — dados chegam via `passes: List<BoardingPass>` | Nenhum |
| `EditBoardingPassScreen` | MVVM — formulário gerenciado em `EditBoardingPassViewModel` | `EditBoardingPassViewModel` |

**Estado local em `BoardingPassScreen`:** portão e URL por passagem são salvos em `SharedPreferences` (`"boarding_passes"`), não no banco Room. São carregados em `SnapshotStateMap` na inicialização e gravados a cada edição. Isso evita uma migration de schema para dados voláteis (portão muda no dia do voo).

---

## Agrupamento de passagens

```
passes: List<BoardingPass>
  └─ groupBy { it.date }                    ← 1.º nível: por data
       └─ passesForDate.groupBy {
            "${it.flightNumber}_${it.origin}_${it.destination}"
          }                                  ← 2.º nível: por trecho (mesmo voo)
```

Passagens com mesmo `flightNumber + origin + destination` na mesma data são exibidas em um único `BoardingPassCard` com múltiplas `PassengerRow`. Passageiros do mesmo voo (ida ou volta) aparecem no mesmo card.

**Chaves de identidade:**
```kotlin
// Identifica o cartão de um passageiro específico (para URL individual)
fun passKey(pass: BoardingPass) =
    "${pass.flightNumber}_${pass.origin}_${pass.destination}_${pass.passenger.split(" ").first().lowercase()}"

// Identifica o portão do voo (compartilhado entre todos os passageiros)
fun gateKey(pass: BoardingPass) =
    "gate_${pass.flightNumber}_${pass.origin}_${pass.destination}"
```

---

## Tipos de transporte

Cinco tipos suportados — todos usam o mesmo modelo `BoardingPass`, mas com labels adaptativas:

| `transportType` | Emoji | Label número | Label horário | Portão |
|---|---|---|---|---|
| `FLIGHT` | ✈️ | `VOO` | `EMBARQUE` | exibido e editável |
| `TRAIN` | 🚂 | `TREM` | `PARTIDA` | não exibido |
| `BUS` | 🚌 | `LINHA` | `PARTIDA` | não exibido |
| `SHIP` | 🚢 | `SERVIÇO` | `PARTIDA` | não exibido |
| `OTHER` | 🎫 | `REF.` | `PARTIDA` | não exibido |

**Cabeçalho adaptativo por tipo:**
- `FLIGHT`: origem e destino em **32sp bold** com cidade abaixo (11sp, branco 75% alpha)
- Outros tipos: origem e destino em **18sp** sem cidade (os campos `originCity`/`destinationCity` são preenchidos com o texto do campo único de origem/destino no formulário de edição)

---

## Layout do `BoardingPassCard`

```
Card (SurfaceWhite, border CardBorder, elevation 2dp)
 ├─ Box (GreenMoss) — cabeçalho
 │    └─ Row: Origem | Emoji + número | Destino
 ├─ TearLine — divisória tracejada estilo boarding pass
 ├─ Row — InfoBlocks: DATA · EMBARQUE/PARTIDA · VOO/LINHA/... · PORTÃO (só voos)
 ├─ [HorizontalDivider + Text] — Observações (se notes.isNotBlank())
 ├─ HorizontalDivider
 └─ PassengerRow × N — um por passageiro
```

### `TearLine`

Simula a linha de picote dos cartões de embarque físicos:

```kotlin
Row {
    Box(12dp × 24dp, topEnd/bottomEnd arredondados 100%) // meia-lua esquerda
    repeat(30) {
        Box(weight(1f) × 1dp)  // traço
        Spacer(2dp)            // espaço entre traços
    }
    Box(12dp × 24dp, topStart/bottomStart arredondados 100%) // meia-lua direita
}
```

As meias-luas têm a cor `GreenLight` (fundo da tela), criando a ilusão de recorte.

### `InfoBlock`

```kotlin
Column(horizontalAlignment = CenterHorizontally) {
    Text(label, 9sp, TextSecondary, letterSpacing 1.5sp)
    Text(value.ifBlank { "—" }, titleMedium, SemiBold, TextPrimary)
}
```

Valor em branco exibe `"—"` para evitar linha vazia.

### `GateBlock` (somente voos)

Portão editável inline — toca qualquer parte do bloco para abrir o `EditGateDialog`:

```kotlin
Column(Modifier.clickable { onClick() }.padding(4.dp)) {
    Text("PORTÃO", 9sp)
    Row {
        Text(if (hasGate) gate else "—", titleMedium, color = if (hasGate) GreenMoss else TextSecondary 50%)
        Icon(Edit, 11dp, tint = if (hasGate) GreenSage else TextSecondary 35%)
    }
}
```

---

## `PassengerRow` — estados do botão de passagem

Cada passageiro tem um botão cujo estado depende do que está configurado:

| Prioridade | Condição | Botão exibido |
|---|---|---|
| 1ª | `pass.documentPath != null` | `Button` "Passagem" (`GreenMoss` + ícone `AttachFile` âmbar) — abre via FileProvider |
| 2ª | `effectiveUrl != null` | `Button` "🎫 Abrir" (`GreenMoss`) + `IconButton` `Edit` para editar URL |
| 3ª (fallback) | sem arquivo e sem URL | `OutlinedButton` "Adicionar link" (borda `AmberPrimary`) |

**`effectiveUrl`:** URL efetiva = URL salva em `SharedPreferences` (editada inline) ou, na ausência, `pass.walletUrl` (salvo no banco pelo formulário). A URL da `SharedPreferences` tem prioridade — permite atualizar o link pós check-in sem editar o registro completo.

**Abertura de arquivo:**
```kotlin
val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
val intent = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
```
MIME type inferido do `ContentResolver` (mais preciso que `MimeTypeMap` para arquivos internos).

Ícone de editar ✏️ ao lado do nome do passageiro navega para `EditBoardingPassScreen`.

---

## Portão de embarque (`EditGateDialog`)

- Campo uppercase limitado a 6 caracteres: `it.uppercase().take(6)`
- Botão "Limpar" (`AmberPrimary`) aparece quando já há portão salvo — permite apagar o valor
- Quando `gate.isBlank()` ao confirmar: remove a chave do `SharedPreferences` e do `savedGates`
- Persistência: `SharedPreferences("boarding_passes")` com chave `gateKey(pass)`

---

## URL da passagem (`AddLinkDialog`)

- `KeyboardType.Uri` no campo de texto
- Botão "Salvar" habilitado apenas se `url.isNotBlank() && url.startsWith("http")`
- Persistência: `SharedPreferences("boarding_passes")` com chave `passKey(pass)`

---

## Lembrete de check-in (`CheckInReminderCard`)

Exibido apenas se `passes.any { it.transportType == "FLIGHT" }`.

**Estado:** `reminderActive: Boolean` carregado de `SharedPreferences("reminders")`, chave `"checkin_active"`.

**Ativar (Android 13+):** solicita permissão `POST_NOTIFICATIONS` via `ActivityResultContracts.RequestPermission()`. Se concedida, chama `NotificationHelper.schedule(context)`.

**Ativar (Android < 13):** chama `NotificationHelper.schedule(context)` diretamente (sem solicitar permissão).

**Cancelar:** `NotificationHelper.cancel(context)` + `reminderActive = false` + grava em `SharedPreferences`.

**UI adaptativa:**
- Inativo: fundo `#FFFDF0` (amarelo suave), borda `AmberPrimary` 40%, botão "🔔 Ativar" (`AmberPrimary`)
- Ativo: fundo `#F0FFF0` (verde suave), borda `GreenMoss` 40%, ícone `Notifications` verde, botão "Cancelar" (`#CC3333`)

---

## Dica de link

Se houver alguma passagem sem `walletUrl` e sem `documentPath`:
```kotlin
if (passes.any { it.walletUrl == null && it.documentPath == null })
```
Exibe card informativo: "Toque em ✏️ ao lado do passageiro para adicionar o link da passagem digital após fazer o check-in." Cor `BadgeBookedBg` com borda `BadgeBookedText` 30%.

---

## `EditBoardingPassScreen` — formulário de edição

**ViewModel:** `EditBoardingPassViewModel` — mantém `EditBoardingPassState` com todos os campos do formulário + flags `isLoading`, `isSaving`, `isDirty`.

**`isDirty`:** `StateFlow<Boolean>` derivado — `true` quando o estado atual difere do estado carregado do banco. Usado para:
- Interceptar back físico/gesto: `BackHandler(enabled = isDirty) { showDiscardDialog = true }`
- Interceptar botão "Voltar" na TopAppBar: `if (isDirty) showDiscardDialog = true else onBack()`

**Campos do formulário:**

| Campo | Tipo de transporte | Observação |
|---|---|---|
| Tipo (`FilterChip` × 5) | todos | `FLIGHT` / `TRAIN` / `BUS` / `SHIP` / `OTHER` |
| Origem / Destino sigla | `FLIGHT` | Campos separados (ex: `REC`, `GRU`) |
| Cidade origem / destino | `FLIGHT` | Campos separados |
| Origem / Destino | outros | Campo único que grava em `originCity` e preenche `origin = originCity.take(3).uppercase()` |
| Número/linha | todos | Label adaptativo por tipo |
| Data | todos | Readonly + `DatePickerDialog` sobreposto por `Box(Modifier.matchParentSize().clickable {...})` |
| Horário | todos | Readonly + `Dialog` com `TimePicker` (24h) |
| Passageiro | todos | Nome completo |
| Link ou arquivo | todos | `SingleChoiceSegmentedButtonRow` (Link / Arquivo) |
| Observações | todos | `OutlinedTextField` multilinha (minLines = 3) |

**Truque de campo readOnly + clicável:**
```kotlin
Box {
    OutlinedTextField(readOnly = true, ...)
    Box(Modifier.matchParentSize().clickable { showDatePicker = true })
}
```
O `OutlinedTextField` exibe o valor mas não abre teclado. A `Box` transparente sobreposta captura o toque e abre o picker.

**Data — formato de armazenamento:**  
O campo `date` é armazenado como `String` no formato `"dd Mmm yyyy"` (ex: `"09 Jun 2026"`), convertido para/de milissegundos para o `DatePickerState`:
```kotlin
// String → millis (para pré-selecionar o picker)
fun dateStringToMillis(s: String): Long?  // parses "09 Jun 2026" → epoch millis

// millis → String (após seleção)
fun millisToDateString(millis: Long): String  // "09 Jun 2026"
```
Usa array `PT_MONTHS` com meses abreviados em português.

**Horário — formato:** `"HHhMM"` (ex: `"06h30"`). `parseTime()` e `formatTime()` convertem entre String e `Pair<Int, Int>`.

**`PassSourceSelector`:** `SingleChoiceSegmentedButtonRow` com dois segmentos ("Link" / "Arquivo"). Em modo `FILE`, exibe chip com nome do arquivo + botão `Close` para remover, seguido de `OutlinedButton` para selecionar/trocar arquivo. Arquivo copiado para `filesDir/Passagens/<timestamp>_<nome>`.

**`canSave`:** `origin.isNotBlank() && destination.isNotBlank() && passenger.isNotBlank()` — campos mínimos para salvar.

**Dialogs presentes:**
- `showDatePicker` — `DatePickerDialog` Material 3 (cores `GreenMoss`)
- `showTimePicker` — `Dialog` customizado com `TimePicker` (24h, cores `GreenMoss`)
- `showDeleteDialog` — confirmação de exclusão permanente
- `showDiscardDialog` — confirmação de descarte de alterações não salvas

---

## Composables e funções privadas (resumo — `BoardingPassScreen`)

| Símbolo | Tipo | Responsabilidade |
|---|---|---|
| `transportEmoji(type)` | função pura | Emoji por tipo de transporte |
| `identifierLabel(type)` | função pura | Label do número (VOO / TREM / LINHA / SERVIÇO / REF.) |
| `departureLabel(type)` | função pura | Label do horário (EMBARQUE / PARTIDA) |
| `passKey(pass)` | função pura | Chave única por passageiro + trecho (para URL em SharedPreferences) |
| `gateKey(pass)` | função pura | Chave única por trecho de voo (para portão em SharedPreferences) |
| `BoardingPassCard` | composable | Card completo de um trecho com todos os passageiros |
| `TearLine` | composable | Divisória estilo picote de boarding pass físico |
| `InfoBlock` | composable | Par label/valor centrado (DATA, EMBARQUE, etc.) |
| `GateBlock` | composable | InfoBlock clicável para portão, só em voos |
| `PassengerRow` | composable | Linha de passageiro com botão adaptativo (arquivo / link / adicionar) |
| `CheckInReminderCard` | composable | Card de lembrete de check-in, só exibido se houver voos |
| `EditGateDialog` | composable | Dialog inline para editar portão |
| `AddLinkDialog` | composable | Dialog inline para adicionar/editar URL da passagem |

---

## Checklist para futuras modificações

- **Novo tipo de transporte:** adicionar `TransportOption` em `TRANSPORT_OPTIONS` (`EditBoardingPassScreen`) → adicionar branch em `transportEmoji()`, `identifierLabel()`, `departureLabel()` → considerar se precisa de campos de rota específicos (como `FLIGHT` tem siglas IATA).
- **Migrar portão/URL para o banco:** atualmente em `SharedPreferences`. Para migrar: adicionar campos `gate` e `savedWalletUrl` em `BoardingPassEntity` + migration → remover `savedUrls`/`savedGates` como `SnapshotStateMap` e ler do banco.
- **Novo campo no card:** adicionar em `BoardingPass` + `BoardingPassEntity` + migration + mapper → exibir em `BoardingPassCard` (ex: como novo `InfoBlock` ou na seção de observações).
- **Novo campo no formulário:** adicionar campo em `EditBoardingPassState` e `EditBoardingPassViewModel` → adicionar `OutlinedTextField` em `EditBoardingPassScreen` → persistir via `save()` no ViewModel.
- **Adicionar portão para outros transportes:** remover `if (isFlight)` no bloco de `GateBlock` em `BoardingPassCard`. Considerar renomear o label para algo genérico como "PLATAFORMA" para trens.
- **Lembrete parametrizável (horário customizado):** atualmente fixo em `NotificationHelper.reminderDisplay`. Para customizar: adicionar campo de hora preferida no `SharedPreferences("reminders")` → passar para `NotificationHelper.schedule()`.
