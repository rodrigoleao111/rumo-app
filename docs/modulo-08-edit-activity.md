# Módulo 08 — Edição de Atividade

**Tela:** `EditActivityScreen`  
**Arquivo:** `ui/edit/EditActivityScreen.kt`  
**ViewModel:** `ui/edit/EditActivityViewModel.kt`  
**Entry points de navegação:**
- FAB "+" em `DayDetailScreen` → nova atividade (`activityId = 0`)
- Swipe Editar em `DayDetailScreen` → edição (`activityId != 0`)

---

## Visão geral

Formulário único para criar e editar atividades de um dia de viagem. Suporta horário (relógio Material 3 24h), picker de emoji com acesso rápido + dialog completo, nome, descrição, endereço e badges — tanto pré-definidos quanto categorias personalizadas com cor. A TopAppBar adapta título e botão Excluir conforme o modo (novo vs. edição).

---

## Padrão de arquitetura

Segue **MVVM** com `EditActivityState` como único ponto de verdade do formulário.

| Camada | Arquivo | Responsabilidade |
|---|---|---|
| **View** | `EditActivityScreen.kt` | Renderiza o formulário; dialogs gerenciados por flags `Boolean` locais |
| **ViewModel** | `EditActivityViewModel.kt` | Carrega atividade do banco, mantém `EditActivityState`, persiste via `repo` |
| **Repository** | `TripRepository` | `getDayEntity()`, `getActivity()`, `getBadgesForActivity()`, `upsertActivity()`, `deleteActivity()` |

> **Flags de dialog na View:** `showTimePicker`, `showEmojiDialog`, `showNewBadgeDialog`, `showDeleteDialog` são `Boolean` locais com `remember` — não estão no ViewModel porque não têm valor de negócio, são puramente estado de visibilidade de UI.

---

## `EditActivityState`

```kotlin
data class EditActivityState(
    val activityId: Long = 0L,          // 0L = nova atividade
    val dayEntityId: Long = 0L,         // ID interno do TravelDayEntity no banco
    val time: String = "",              // "HHhMM" ex: "09h30" — vazio = sem horário
    val emoji: String = "📍",
    val name: String = "",              // obrigatório para salvar
    val detail: String = "",
    val address: String = "",           // gravado em mapQuery e uberDestination
    val selectedBadges: Set<BadgeType> = emptySet(),
    val customBadges: List<CustomBadge> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
)

data class CustomBadge(val name: String, val colorHex: String)
```

**`activityId = 0L`** é a sentinela de "nova atividade". A tela usa `isEditing = state.activityId != 0L` para adaptar título, exibir botão Excluir e rotular o botão de salvar.

**`address`** popula tanto `mapQuery` quanto `uberDestination` em `TravelActivityEntity` — são o mesmo valor, mantidos separados no modelo por razões históricas.

---

## Fluxo de dados

```
AppNavigation → EditActivityScreen(viewModel, onBack)
                    └─ EditActivityViewModel(repo, tripId, dayNumber, activityId)
                         ├─ init { repo.getDayEntity(tripId, dayNumber) → dayEntityId }
                         ├─ init { repo.getActivity(activityId) → preenche state }
                         ├─ init { repo.getBadgesForActivity(activityId) → separa padrão/custom }
                         └─ state: StateFlow<EditActivityState>

save()  → repo.upsertActivity(dayEntityId, TravelActivityEntity, badges) → onBack()
delete() → repo.deleteActivity(activityId) → onBack()
```

---

## Inicialização no `init`

O `init` do ViewModel resolve `dayEntityId` a partir de `(tripId, dayNumber)` — a navegação passa o número do dia (1–N), mas o banco usa o ID interno da entidade:

```kotlin
val dayEntity = repo.getDayEntity(tripId, dayNumber)
val dayDbId   = dayEntity?.id ?: 0L
```

Para **nova atividade** (`activityId == 0L`): inicializa o state com defaults e `isLoading = false`.

Para **edição** (`activityId != 0L`):
1. `repo.getActivity(activityId)` → `TravelActivityEntity`
2. `repo.getBadgesForActivity(activityId)` → `List<ActivityBadgeEntity>`
3. Separa os badges pelo campo `badgeType`:
   - `badgeType != "CUSTOM"` → `BadgeType.valueOf(it.badgeType)` → `selectedBadges: Set<BadgeType>`
   - `badgeType == "CUSTOM"` → `CustomBadge(label, color ?: "#607D8B")` → `customBadges: List<CustomBadge>`
4. `address` é lido de `act.mapQuery ?: act.uberDestination ?: ""`

---

## `canSave` e `isDirty`

```kotlin
val canSave = state.name.isNotBlank() && !state.isLoading
```

Controla o estado habilitado do ícone `Check` na TopAppBar e do botão principal.

```kotlin
val isDirty: StateFlow<Boolean> = _state.map { s ->
    !s.isLoading && (s.activityId == 0L && s.name.isNotBlank() ||
                     s.activityId != 0L && s.name.isNotBlank())
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
```

`isDirty` é exposto mas **não usado para interceptar back** nesta tela — diferente de `EditBoardingPassScreen`. A navegação de volta ocorre diretamente via `onBack()` sem confirmação de descarte.

---

## TopAppBar — modo criação vs. edição

| Elemento | Nova atividade | Edição |
|---|---|---|
| Título | "Nova atividade" | "Editar atividade" |
| Ícone Excluir (lixeira) | ausente | presente — abre `AlertDialog` de confirmação |
| Ícone Check | habilitado se `canSave` | habilitado se `canSave` |

---

## Seções do formulário

### 1. Horário (`TimePickerField` + `TimePickerDialog`)

`TimePickerField` é uma `Surface` clicável que exibe o horário atual ou "Selecionar horário" (placeholder cinza). Toque abre `showTimePicker = true`.

**`TimePickerDialog`:** `Dialog` customizado com `TimePicker` Material 3 no modo relógio (não input):
```kotlin
rememberTimePickerState(initialHour, initialMinute, is24Hour = true)
```

**Parsing do valor atual:**
```kotlin
private fun parseTimeString(time: String): Pair<Int, Int> {
    val cleaned = time.replace("h", ":").replace("H", ":")
    val parts   = cleaned.split(":")
    Pair(parts[0].toInt().coerceIn(0, 23), parts[1].toInt().coerceIn(0, 59))
}
```

Suporta o formato `"HHhMM"` (ex: `"09h30"`). Em caso de falha de parsing, retorna `Pair(9, 0)`.

**Gravação após confirmação:** `viewModel.updateTime("%02dh%02d".format(h, m))`

**Cores do `TimePicker`:** `clockDialColor = GreenLight`, `selectorColor = GreenMoss`, selecionado `GreenMoss` + texto branco.

---

### 2. Ícone (`ActivityEmojiRow` + `EmojiPickerDialog`)

**Linha rápida (`ActivityEmojiRow`):** 5 slots + 1 botão "mais":

```
Slot 0 = emoji selecionado atual
Slots 1–4 = QUICK_EMOJIS filtrando o selecionado (para não duplicar)
Slot 5 = botão Add (abre EmojiPickerDialog)
```

`QUICK_EMOJIS = ["🏨", "🍽️", "🎡", "🏔️", "☕"]` — hardcoded, não configurável.

Cada slot é `Surface(weight(1f), aspectRatio(1f))`. O selecionado tem fundo `AmberPrimary` 15% + borda `AmberPrimary` 2dp; os demais têm fundo `SurfaceWhite` + borda `CardBorder` 1dp.

**`EmojiPickerDialog`:** `Dialog` com `LazyVerticalGrid(GridCells.Fixed(6), heightIn(max = 340dp))` mostrando `ALL_ACTIVITY_EMOJIS` — lista de 40 emojis organizados por categoria (acomodação, comida, natureza, atrações, transporte, misc). Emoji selecionado tem o mesmo estilo da linha rápida.

---

### 3. Nome, descrição e endereço

Todos usam `EditTextField` — wrapper de `OutlinedTextField` padronizado com cores `GreenMoss`.

| Campo | `singleLine` | `minLines` | Validação |
|---|---|---|---|
| Nome | `true` | — | `isNotBlank()` para habilitar salvar |
| Descrição | `false` | 3 | opcional |
| Endereço | `true` | — | opcional |

**Endereço:** acompanhado de `Text("Usado para abrir no Maps e chamar Uber.")` como sublabel explicativo. Na gravação, o valor é escrito em `mapQuery` e `uberDestination` simultaneamente:
```kotlin
mapQuery        = s.address.trim().ifEmpty { null },
uberDestination = s.address.trim().ifEmpty { null }
```

---

### 4. Badges (`BadgeSelector`)

Usa `FlowRow` (`ExperimentalLayoutApi`) — chips que quebram linha automaticamente conforme o espaço.

**6 tipos pré-definidos** (em `ALL_BADGES`):

| `BadgeType` | Label exibida |
|---|---|
| `FREE` | Grátis |
| `PAID` | Pago |
| `BOOKED` | Reservado |
| `INCLUDED` | Incluído |
| `UBER` | Uber |
| `WALKING` | A pé |

**Chip padrão:** `Surface` pill (`RoundedCornerShape(100.dp)`).
- Não selecionado: `SurfaceWhite` + borda `CardBorder`, texto `TextSecondary`
- Selecionado: `AmberPrimary` sólido, texto branco SemiBold

**Toggle:** `viewModel.toggleBadge(type)` — adiciona ao `Set` se ausente, remove se presente.

**Chips de categoria customizada:** renderizados após os pré-definidos. Cor derivada de `colorHex`:
```kotlin
val base = Color(android.graphics.Color.parseColor(cb.colorHex))
// fundo: base 15% alpha | borda: base 50% alpha | texto: base 100%
```

Cada chip customizado tem botão `Close` (14dp) à direita para remover via `viewModel.removeCustomBadge(badge)`.

**Botão "Nova" (+ Nova):** `TextButton` ao lado do label "Tags". Abre `showNewBadgeDialog = true`.

---

### 5. `NewBadgeDialog` — nova categoria personalizada

`AlertDialog` com dois elementos:

**Campo de nome:** `OutlinedTextField(singleLine = true)` com label "Nome da categoria". Cores `GreenMoss`.

**Color picker — 8 cores (`BADGE_PALETTE`):**
```kotlin
val BADGE_PALETTE = listOf(
    "#E53935", "#8E24AA", "#1E88E5", "#00897B",
    "#43A047", "#F4511E", "#6D4C41", "#546E7A"
)
```

Renderizado como `FlowRow` de círculos 32dp (`CircleShape`). A cor selecionada exibe ícone `Check` branco (16dp) centralizado. Cor inicial: `BADGE_PALETTE.first()` (`#E53935`, vermelho).

**Botão "Adicionar":** habilitado apenas se `name.isNotBlank()`. Chama `viewModel.addCustomBadge(name, selectedColor)`.

---

## Persistência — `save()`

```kotlin
fun save(onDone: () -> Unit) {
    val entity = TravelActivityEntity(
        id              = s.activityId,   // 0L = INSERT; != 0L = UPDATE
        dayId           = s.dayEntityId,
        position        = 0,
        time            = s.time.trim(),
        emoji           = s.emoji,
        name            = s.name.trim(),
        detail          = s.detail.trim(),
        mapQuery        = s.address.trim().ifEmpty { null },
        uberDestination = s.address.trim().ifEmpty { null }
    )
    val badges = s.selectedBadges.map { type ->
        ActivityBadgeEntity(badgeType = type.name, label = badgeLabel(type))
    } + s.customBadges.map { cb ->
        ActivityBadgeEntity(badgeType = BadgeType.CUSTOM.name, label = cb.name, color = cb.colorHex)
    }
    repo.upsertActivity(s.dayEntityId, entity, badges)
    onDone()
}
```

`repo.upsertActivity()` apaga os badges existentes da atividade e insere os novos a cada save — não faz diff, substitui a lista completa. `position = 0` é ignorado pelo Room no upsert (o campo é mantido da versão anterior para edições).

**Labels dos badges padrão** são derivados pelo método estático `badgeLabel(type: BadgeType): String` — garante consistência entre criação e edição.

---

## Exclusão — `deleteActivity()`

```kotlin
fun deleteActivity(onDone: () -> Unit) {
    if (id == 0L) return    // guard: não deleta se for nova atividade sem ID
    _state.value = _state.value.copy(isSaving = true)
    viewModelScope.launch {
        repo.deleteActivity(id)
        onDone()
    }
}
```

Acionado pelo `AlertDialog` de confirmação na tela:
```
"Excluir atividade?" / "Esta atividade será removida permanentemente do dia."
[Excluir (vermelho bold)] [Cancelar]
```

---

## Composables e símbolos privados (resumo)

| Símbolo | Tipo | Responsabilidade |
|---|---|---|
| `EditActivityState` | `data class` | Estado completo do formulário |
| `CustomBadge` | `data class` | Badge personalizado com nome e cor hex |
| `EditActivityViewModel` | `ViewModel` | Carrega, mantém e persiste o estado |
| `EditActivityScreen` | composable | Tela principal — orquestra seções e dialogs |
| `TimePickerField` | composable privado | Campo clicável que exibe o horário formatado |
| `TimePickerDialog` | composable privado | Dialog com `TimePicker` Material 3 (24h, cores GreenMoss) |
| `parseTimeString` | função privada | Converte `"HHhMM"` para `Pair<Int, Int>` com fallback `(9, 0)` |
| `ActivityEmojiRow` | composable privado | 5 slots rápidos + botão para o dialog completo |
| `EmojiPickerDialog` | composable privado | Grid 6 colunas com 40 emojis |
| `BadgeSelector` | composable privado | `FlowRow` de chips pré-definidos + customizados |
| `NewBadgeDialog` | composable | `AlertDialog` com campo de nome + color picker de 8 cores |
| `QUICK_EMOJIS` | `val` | 5 emojis para a linha rápida |
| `ALL_ACTIVITY_EMOJIS` | `val` | 40 emojis para o dialog completo |
| `ALL_BADGES` | `val` | 6 pares `(BadgeType, label)` pré-definidos |
| `BADGE_PALETTE` | `val` | 8 cores hex para categorias customizadas |
| `badgeLabel(type)` | fun estática | Label canônica por `BadgeType` — usada no save |

---

## Checklist para futuras modificações

- **Novo emoji na linha rápida:** adicionar em `QUICK_EMOJIS`. Manter em 5 itens — o 6.º slot é fixo para o botão "mais".
- **Novo emoji no dialog completo:** adicionar em `ALL_ACTIVITY_EMOJIS`. O `LazyVerticalGrid` se expande automaticamente.
- **Novo badge pré-definido:** adicionar valor em `BadgeType` (enum em `Models.kt`) → adicionar entrada em `ALL_BADGES` → adicionar case em `badgeLabel()` → adicionar cor em `BadgeChip.kt` (componente de exibição). A ordem em `ALL_BADGES` determina a ordem dos chips na tela.
- **Nova cor no color picker:** adicionar hex em `BADGE_PALETTE`. O `FlowRow` acomoda automaticamente.
- **Interceptar back com confirmação de descarte:** adicionar `BackHandler(enabled = isDirty.collectAsStateWithLifecycle().value) { showDiscardDialog = true }` na tela (mesmo padrão de `EditBoardingPassScreen`). O `isDirty` já existe no ViewModel.
- **Campo `position` na nova atividade:** atualmente `position = 0` para novas atividades. Para inserir no final do dia, leia `repo.getActivitiesForDay(dayEntityId).size` antes de salvar e use esse valor como `position`.
- **Endereço diferente para Maps e Uber:** atualmente `mapQuery == uberDestination`. Para suportar endereços distintos, adicionar campo separado no state e `OutlinedTextField` adicional na tela — e remover a duplicação no `save()`.
