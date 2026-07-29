# Módulo 01 — Lista de Viagens

**Tela:** `TripsListScreen`  
**Arquivo:** `ui/trips/TripsListScreen.kt`  
**ViewModel:** `ui/trips/TripsListViewModel.kt`  
**Entry point de navegação:** rota `trips_list` (após `SplashScreen`)

---

## Visão geral

Tela inicial do app após o splash. Exibe todas as viagens cadastradas em uma `LazyColumn` com cards interativos. É o ponto de entrada para criação, edição, importação e exclusão de viagens. Também é responsável pela auto-navegação para a viagem ativa.

A tela **não tem cabeçalho**: um botão ☰ flutuante (canto superior esquerdo) abre um `ModalNavigationDrawer` (Nova viagem / Importar viagem / Configurações) e um FAB `+` expansível (canto inferior direito) revela as ações "Nova viagem" e "Importar viagem".

---

## Padrão de arquitetura

Este módulo segue **MVVM (Model-View-ViewModel)** com as seguintes responsabilidades:

| Camada | Arquivo | Responsabilidade |
|---|---|---|
| **View** | `TripsListScreen.kt` | Renderiza a UI, captura gestos, dispara eventos via callbacks |
| **ViewModel** | `TripsListViewModel.kt` | Combina `allTrips` com as preferências (ordenar por proximidade / ocultar concluídas) e expõe o `StateFlow` resultante; executa `deleteTrip` (com snackbar de erro) |
| **Repository** | `TripRepository.kt` | Fornece `allTrips: Flow<List<TripEntity>>` do banco Room |
| **Preferências** | `SettingsRepository.kt` | Fornece `sortTripsByProximity` e `hideCompletedTrips` como `Flow<Boolean>` do DataStore |
| **Entity** | `TripEntity` | Modelo de dados persistido (sem conversão para domain model nesta tela) |

> **Regra de padrão:** A `TripsListScreen` não acessa o repositório diretamente. Todo dado vem do `ViewModel` via `StateFlow`. Toda ação destrutiva (delete) é delegada ao `ViewModel`.

A lógica de **auto-navegação** e **wiring de callbacks** vive em `AppNavigation.kt`, mantendo a tela livre de dependências de navegação.

---

## Fluxo de dados

```
Room DB                              DataStore (SettingsRepository)
  └─ TripDao.getAllTrips()             ├─ sortTripsByProximity: Flow<Boolean>
       └─ TripRepository.allTrips      └─ hideCompletedTrips:  Flow<Boolean>
            │                                    │
            └──────────────── combine(...) ──────┘
                 └─ ocultar concluídas + ordenar por proximidade
                      └─ stateIn(WhileSubscribed(5_000), initialValue = null)
                           └─ TripsListViewModel.trips: StateFlow<List<TripEntity>?>
                                └─ TripsListScreen (collectAsStateWithLifecycle)
```

`trips` **não** vem direto de `repo.allTrips`: é o `combine` de três fluxos — a lista do Room e as duas preferências de exibição do `SettingsRepository` (`sortTripsByProximity` e `hideCompletedTrips`). O resultado é materializado com `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)`.

A ordenação (`sortByProximity`/`proximityKey`) e a ocultação (`isCompleted`) são funções privadas do `TripsListViewModel` aplicadas dentro do `combine` — nada é apagado do banco; são apenas filtro/reordenação de exibição.

`initialValue = null` distingue **carregando** (`null`) de **lista vazia** (`emptyList()`), alimentando três estados distintos na UI (ver seção *Estados da lista* abaixo). O `WhileSubscribed(5_000)` mantém o fluxo ativo por 5 s após o último coletor sair, evitando recomputar a lista em rotações/navegações curtas.

Qualquer insert/update/delete no Room — ou a troca de uma das preferências — provoca re-emissão automática do `combine`, atualizando a lista sem intervenção manual.

---

## Funcionalidades

### 1. Card de viagem (`TripCard`)

**Arquivo:** composable privado `TripCard` em `TripsListScreen.kt`

Cada viagem é renderizada como um `Card` Material 3 dividido em duas partes: uma **capa** (≈3/4 da altura) com a imagem de fundo e o título sobreposto, e uma **faixa inferior** (≈1/4) com destino, data e badge.

| Elemento | Fonte de dados | Observação |
|---|---|---|
| Imagem de capa | `TripCovers.resFor(trip.coverImage)` | `Image` com `ContentScale.Crop` preenchendo a capa (ratio `(16f/9f) / coverScale`) |
| Emoji (fallback) | `TripEntity.coverEmoji` | Só quando `coverImage` não resolve (viagens antigas): emoji 44sp centralizado sobre fundo `GreenMoss` |
| Título (sobreposto) | `TripEntity.name` | `titleLarge` com `fontSize = (26f * coverScale).sp`, `Bold`, `Color.White` + `Shadow`; alinhado ao `BottomStart` sobre um scrim vertical escuro |
| Destino | `TripEntity.destination` | `bodyMedium`, `Medium`, cor `TextPrimary` (faixa inferior) |
| Data | `TripEntity.startDate` + `endDate` | `labelMedium`, cor `GreenSage`; formatada por `formatDateRange()`, exibida só quando ambas as datas existem |
| Badge de status | calculado por `tripStatus()` | Composable `StatusBadge` (faixa inferior) |
| Menu ⋮ | — | `MoreVert` no canto superior direito da capa → `DropdownMenu` (ver seção 4) |

**Border da viagem ativa:**
```kotlin
border = BorderStroke(
    width = if (status == TripStatus.ACTIVE) 2.dp else 1.dp,
    color = if (status == TripStatus.ACTIVE) GreenMoss else CardBorder
)
```
A viagem `Em curso` recebe border de `2.dp` em `GreenMoss`. As demais recebem `1.dp` em `CardBorder`.

**Capas responsivas (`coverScale`):**

A altura da capa encolhe conforme a lista cresce, para caber mais viagens na tela:

| Nº de viagens | `coverScale` | Altura da capa |
|---|---|---|
| ≤ 9 | `1f` | cheia (ratio 16:9) |
| 10–18 | `2f / 3f` | 2/3 |
| > 18 | `0.5f` | 1/2 |

`coverScale` é calculado uma vez em `TripsListScreen` (a partir de `trips.size`) e repassado a cada `TripCard`. Ele divide o `aspectRatio` da capa — reduzindo apenas a altura, mantendo a largura cheia — e escala junto a fonte do título (`(26f * coverScale).sp`).

---

### 2. Badge de status (`StatusBadge` + `tripStatus`)

**Lógica em `tripStatus(startDate, endDate): TripStatus`:**

```kotlin
private enum class TripStatus { PLANNING, ACTIVE, COMPLETED }

private fun tripStatus(startDate: String?, endDate: String?): TripStatus {
    if (startDate == null || endDate == null) return PLANNING
    val today = LocalDate.now()
    return when {
        today < LocalDate.parse(startDate) -> PLANNING
        today > LocalDate.parse(endDate)   -> COMPLETED
        else                               -> ACTIVE
    }
}
```

**Lógica em `countdownLabel(startDate): String`** (usado quando `PLANNING`):

| Condição | Rótulo exibido |
|---|---|
| `startDate == null` | `"Planejando"` |
| `days <= 0` | `"Planejando"` |
| `days == 1` | `"amanhã"` |
| `days < 31` | `"em X dias"` |
| `days < 365` | `"em X meses"` |
| `days >= 365` | `"em X anos"` |

**Cores do badge por status:**

| Status | Fundo | Texto |
|---|---|---|
| `PLANNING` | `AmberPrimary` | `GreenMoss` |
| `ACTIVE` | `GreenMoss` | `Color.White` |
| `COMPLETED` | `GreenForest` | `TextSecondary` |

> **Regra de padrão:** Ao adicionar um novo status, crie o valor no enum `TripStatus`, adicione a condição em `tripStatus()` e mapeie cores/label em `StatusBadge`. Não espalhe a lógica de status pela UI.

---

### 3. Formatação de datas (`formatDateRange`)

```kotlin
private fun formatDateRange(startDate: String, endDate: String): String
```

- **Mesmo mês e ano:** `"9–13 jun. 2026"`
- **Meses/anos diferentes:** `"28 jun. – 3 jul. 2026"`

Usa `DateTimeFormatter` com `Locale("pt", "BR")`. As datas são armazenadas no banco como `String` no formato ISO (`"yyyy-MM-dd"`) e convertidas por `LocalDate.parse()` apenas para exibição.

---

### 4. Menu de ações do card (`DropdownMenu`)

Cada `TripCard` tem um ícone `MoreVert` (⋮) no canto superior direito da capa. Ao tocá-lo, `menuOpen` (`remember { mutableStateOf(false) }` por card) abre um `DropdownMenu` com três itens:

| Item | Ícone | Callback |
|---|---|---|
| Compartilhar | `ic_share` (`GreenSage`) | `onShare()` → rota `ShareTrip` |
| Editar | `ic_edit` (`GreenMoss`) | `onEdit()` → rota `EditTrip` |
| Excluir | `ic_delete` (vermelho `#D32F2F`) | `onDelete()` → abre o diálogo de confirmação |

```kotlin
DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = SurfaceWhite) {
    DropdownMenuItem(text = { Text("Compartilhar", ...) }, onClick = { menuOpen = false; onShare() })
    DropdownMenuItem(text = { Text("Editar", ...) },       onClick = { menuOpen = false; onEdit()  })
    DropdownMenuItem(text = { Text("Excluir", ...) },       onClick = { menuOpen = false; onDelete() })
}
```

- Cada item fecha o menu (`menuOpen = false`) antes de disparar seu callback.
- A exclusão **não** é imediata: `onDelete` apenas define `pendingDelete = trip`, que abre o `AlertDialog` de confirmação (ver seção 5).
- `menuOpen` é `remember { }` dentro do `TripCard`, então cada viagem tem estado de menu independente.

> **Regra de padrão:** Para adicionar ou remover uma ação do card, ajuste os `DropdownMenuItem` do `DropdownMenu` em `TripCard` e o callback correspondente na assinatura — o restante do card não muda.

---

### 5. Confirmação de exclusão

`pendingDelete: MutableState<TripEntity?>` armazena a viagem aguardando confirmação.

Ao tocar em "Excluir" no menu ⋮:
1. `pendingDelete = trip` — abre o `AlertDialog`
2. Confirmação → `viewModel.deleteTrip(trip)` → `pendingDelete = null`
3. Cancelamento → `pendingDelete = null`

O `AlertDialog` exibe o nome da viagem e avisa que **dias, atividades, contatos e vouchers** serão apagados.

```kotlin
// ViewModel
fun deleteTrip(trip: TripEntity) {
    viewModelScope.launch {
        runCatching { repo.deleteTrip(trip) }
            .onFailure { _uiEvent.send(UiEvent.ShowSnackbar("Erro ao excluir viagem")) }
    }
}
```

A deleção no Room provoca re-emissão do `Flow`, removendo o card automaticamente sem intervenção manual na lista. Em caso de falha, o `deleteTrip` envia um evento por um `Channel<UiEvent>` (exposto como `uiEvent`); a `TripsListScreen` coleta esse fluxo em um `LaunchedEffect` e exibe um snackbar **"Erro ao excluir viagem"** (`SnackbarHost` com `containerColor = AmberPrimary`, `contentColor = GreenMoss`).

---

### 6. FAB expansível (criar / importar viagem)

No lugar de cards de ação no rodapé, a criação e a importação ficam num **FAB expansível** (`ExpandableFab`) no canto inferior direito, montado no slot `floatingActionButton` do `Scaffold`.

- O FAB principal (`+`, `GreenMoss`) alterna `fabExpanded`; ao expandir, o ícone gira 45° (`+` → `×`) via `animateFloatAsState`.
- Expandido, revela duas ações (`FabAction`) que sobem com `fadeIn + slideInVertically`:

| Ação | Ícone | Cores | Callback |
|---|---|---|---|
| **Importar viagem** | `ic_import` | `GreenSage` / branco | `onImportTrip()` → rota `ImportTrip` |
| **Nova viagem** | `ic_add` | `AmberPrimary` / `GreenMoss` | `onNewTripClick()` → rota `CreateTrip` |

- Ao expandir, um **scrim** semitransparente (`Color.Black` a 32%) cobre a tela; tocá-lo recolhe o FAB (`fabExpanded = false`).
- Cada `FabAction` recolhe o FAB antes de disparar seu callback.

As mesmas ações "Nova viagem" e "Importar viagem" (mais "Configurações") também estão disponíveis na gaveta (`TripsDrawerContent`) aberta pelo botão ☰.

---

### 7. Estados da lista

```kotlin
when {
    trips == null        -> // Carregando (CircularProgressIndicator)
    trips!!.isEmpty()    -> // Empty state (🗺️ + texto)
    else                 -> // Lista de cards (com capa responsiva por coverScale)
}
```

O `null` inicial do `StateFlow` (definido em `TripsListViewModel`) é intencional para separar os estados de *carregando* e *vazio*, evitando flash do empty state durante o boot.

**Empty state:**
- Emoji `🗺️` (56sp)
- Texto "Nenhuma viagem ainda"
- Subtexto "Crie sua primeira viagem\nno botão + do canto inferior" (duas linhas)

---

### 8. Botão back do sistema (três `BackHandler`)

O back do sistema é interceptado por três `BackHandler` com prioridade decrescente, cada um habilitado por uma condição mutuamente exclusiva:

```kotlin
BackHandler(enabled = drawerState.isOpen)                { scope.launch { drawerState.close() } }
BackHandler(enabled = fabExpanded)                       { fabExpanded = false }
BackHandler(enabled = drawerState.isClosed && !fabExpanded) { showExitDialog = true }
```

1. **Gaveta aberta** → fecha a gaveta.
2. **FAB expandido** → recolhe o FAB.
3. **Nenhum dos dois** → abre o `AlertDialog` de saída.

O diálogo de saída exibe `AlertDialog` com opções "Sair" e "Cancelar". Confirmação chama `activity?.finish()`.

```kotlin
val activity = LocalContext.current as? Activity
```

O cast é seguro (`as?`) — retorna `null` em contextos não-Activity (previews, testes), evitando crash.

---

### 9. Auto-navegação para viagem ativa

**Onde vive:** `AppNavigation.kt`, no composable da rota `TripsList` — **não** em `TripsListScreen`.

```kotlin
var autoNavigated by rememberSaveable { mutableStateOf(false) }

LaunchedEffect(trips) {
    if (!autoNavigated && trips != null && settings.autoOpenActiveTrip) {
        val today = LocalDate.now()
        val active = trips!!.filter { trip ->
            val start = runCatching { LocalDate.parse(trip.startDate) }.getOrNull()
            val end   = runCatching { LocalDate.parse(trip.endDate)   }.getOrNull()
            start != null && end != null && !today.isBefore(start) && !today.isAfter(end)
        }
        if (active.size == 1) {
            autoNavigated = true
            navController.navigate(Screen.TripMain.createRoute(active.first().id)) {
                popUpTo(Screen.TripsList.route)
            }
        } else {
            autoNavigated = true  // não navega se há 0 ou 2+ viagens ativas
        }
    }
}
```

**Condições para auto-navegação:**
1. `autoNavigated == false` — executa apenas uma vez por sessão (`rememberSaveable` sobrevive a rotação)
2. `trips != null` — lista já carregou
3. `settings.autoOpenActiveTrip == true` — configuração ativada pelo usuário
4. Exatamente **uma** viagem com `startDate ≤ hoje ≤ endDate`

**Configuração:** `SettingsRepository.autoOpenActiveTrip` — **DataStore (Preferences)** com chave `"auto_open_active_trip"`, padrão `true`.

> **Regra de padrão:** A lógica de auto-navegação deve permanecer em `AppNavigation`, não na tela. `TripsListScreen` não sabe que existe navegação automática.

---

## Wiring de navegação (`AppNavigation.kt`)

```kotlin
TripsListScreen(
    viewModel      = vm,
    onTripClick    = { tripId -> navController.navigate(Screen.TripMain.createRoute(tripId)) },
    onNewTripClick = { navController.navigate(Screen.CreateTrip.route) },
    onTripEdit     = { tripId -> navController.navigate(Screen.EditTrip.createRoute(tripId)) },
    onTripShare    = { tripId -> navController.navigate(Screen.ShareTrip.createRoute(tripId)) },
    onImportTrip   = { navController.navigate(Screen.ImportTrip.route) },
    onSettingsClick = { navController.navigate(Screen.Settings.route) }
)
```

`TripsListScreen` recebe todos os callbacks de navegação como lambdas. Ela nunca importa `NavController` diretamente — a tela é agnóstica de navegação.

---

## Composables privados (resumo)

| Composable | Responsabilidade |
|---|---|
| `TripCard` | Renderiza um card de viagem (capa + título sobreposto, destino, data, badge, menu ⋮) |
| `StatusBadge` | Pill colorido com label de status |
| `ExpandableFab` | FAB `+` que expande em duas ações ("Nova viagem" e "Importar viagem") |
| `FabAction` | Item de ação revelado pelo `ExpandableFab` (label + `SmallFloatingActionButton`) |
| `HeaderIconButton` | Botão flutuante ☰ que abre a gaveta (`ModalNavigationDrawer`) |
| `TripsDrawerContent` | Conteúdo da gaveta: Nova viagem / Importar viagem / Configurações |

---

## Funções puras (helpers)

| Função | Entrada | Saída |
|---|---|---|
| `tripStatus(startDate, endDate)` | `String?`, `String?` | `TripStatus` |
| `countdownLabel(startDate)` | `String?` | `String` (rótulo humanizado) |
| `formatDateRange(startDate, endDate)` | `String`, `String` | `String` formatado em pt-BR |

Todas são `private fun` (não-composables) sem efeitos colaterais — seguras para testar unitariamente.

---

## Checklist para futuras modificações

- **Novo status de viagem:** adicionar valor no enum `TripStatus` → atualizar `tripStatus()` → atualizar `StatusBadge` (cores/label) → atualizar `countdownLabel()` se necessário.
- **Nova ação no card:** adicionar um `DropdownMenuItem` no `DropdownMenu` de `TripCard` (menu ⋮) → adicionar o callback correspondente na assinatura de `TripCard`/`TripsListScreen` → fazer o wiring em `AppNavigation`.
- **Novo campo no card:** adicionar campo em `TripEntity` + migration Room → ler o campo em `TripCard`.
- **Nova ação de criação/importação:** adicionar um `FabAction` no `ExpandableFab` (e/ou um `NavigationDrawerItem` em `TripsDrawerContent`) → adicionar callback na assinatura de `TripsListScreen` → fazer o wiring em `AppNavigation`.
- **Alterar ordenação/ocultação da lista:** ajustar `sortByProximity`/`proximityKey`/`isCompleted` em `TripsListViewModel`; os toggles vêm do `SettingsRepository` e entram pelo `combine` (ver *Fluxo de dados*).
- **Alterar critério de auto-navegação:** modificar o `LaunchedEffect(trips)` em `AppNavigation.kt` (rota `TripsList`), não em `TripsListScreen`.
