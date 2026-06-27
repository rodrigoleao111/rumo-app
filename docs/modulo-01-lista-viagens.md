# Módulo 01 — Lista de Viagens

**Tela:** `TripsListScreen`  
**Arquivo:** `ui/trips/TripsListScreen.kt`  
**ViewModel:** `ui/trips/TripsListViewModel.kt`  
**Entry point de navegação:** rota `trips_list` (após `SplashScreen`)

---

## Visão geral

Tela inicial do app após o splash. Exibe todas as viagens cadastradas em uma `LazyColumn` com cards interativos. É o ponto de entrada para criação, edição, importação e exclusão de viagens. Também é responsável pela auto-navegação para a viagem ativa.

---

## Padrão de arquitetura

Este módulo segue **MVVM (Model-View-ViewModel)** com as seguintes responsabilidades:

| Camada | Arquivo | Responsabilidade |
|---|---|---|
| **View** | `TripsListScreen.kt` | Renderiza a UI, captura gestos, dispara eventos via callbacks |
| **ViewModel** | `TripsListViewModel.kt` | Expõe `StateFlow` da lista de viagens, executa `deleteTrip` |
| **Repository** | `TripRepository.kt` | Fornece `allTrips: Flow<List<TripEntity>>` do banco Room |
| **Entity** | `TripEntity` | Modelo de dados persistido (sem conversão para domain model nesta tela) |

> **Regra de padrão:** A `TripsListScreen` não acessa o repositório diretamente. Todo dado vem do `ViewModel` via `StateFlow`. Toda ação destrutiva (delete) é delegada ao `ViewModel`.

A lógica de **auto-navegação** e **wiring de callbacks** vive em `AppNavigation.kt`, mantendo a tela livre de dependências de navegação.

---

## Fluxo de dados

```
Room DB
  └─ TripDao.getAllTrips()          ← Flow<List<TripEntity>> (reativo)
       └─ TripRepository.allTrips
            └─ TripsListViewModel.trips: StateFlow<List<TripEntity>?>
                 └─ TripsListScreen (collectAsStateWithLifecycle)
```

`StateFlow` usa `initialValue = null` para distinguir **carregando** (`null`) de **lista vazia** (`emptyList()`). Isso alimenta três estados distintos na UI (ver seção *Estados da lista* abaixo).

O `Flow` do Room é reativo: qualquer insert/update/delete em `trips` provoca re-emissão automática, atualizando a lista sem intervenção manual.

---

## Funcionalidades

### 1. Card de viagem (`TripCard`)

**Arquivo:** composable privado `TripCard` em `TripsListScreen.kt`

Cada viagem é renderizada como um `Card` Material 3 com:

| Elemento | Fonte de dados | Observação |
|---|---|---|
| Emoji (cover) | `TripEntity.coverEmoji` | Exibido em container `GreenMoss` arredondado (12dp) |
| Nome | `TripEntity.name` | `titleMedium`, `SemiBold`, cor `TextPrimary` |
| Destino | `TripEntity.destination` | `bodySmall`, cor `TextSecondary` |
| Datas | `TripEntity.startDate` + `endDate` | Formatadas por `formatDateRange()` (ver abaixo) |
| Badge de status | calculado por `tripStatus()` | Composable `StatusBadge` |

**Border da viagem ativa:**
```kotlin
border = BorderStroke(
    width = if (status == TripStatus.ACTIVE) 2.dp else 1.dp,
    color = if (status == TripStatus.ACTIVE) GreenMoss else CardBorder
)
```
A viagem `Em curso` recebe border de `2.dp` em `GreenMoss`. As demais recebem `1.dp` em `CardBorder`.

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
| `PLANNING` | `AmberPrimary` | `Color.White` |
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

### 4. Swipe para revelar ações (`SwipeToRevealTrip`)

**Implementação:** customizada com `Animatable<Float>` + `Modifier.draggable` — **não** usa `SwipeToDismissBox`.

**Por que customizado:** o `SwipeToDismissBox` do Material 3 só suporta uma ação. Este componente revela 3 botões simultaneamente.

**Estrutura do layout:**
```
Box (fillMaxWidth, height = IntrinsicSize.Min)
 ├─ Row (align = CenterEnd, width = 162.dp)  ← botões fixos no fundo
 │    ├─ Box Compartilhar (GreenSage, arredondado esquerda)
 │    ├─ Box Editar (AmberPrimary)
 │    └─ Box Excluir (vermelho #D32F2F, arredondado direita)
 └─ Box (offset = offsetX)                   ← conteúdo deslizável (TripCard)
```

**Lógica de snap:**
```kotlin
onDragStopped = { velocity ->
    if (offsetX.value < -actionWidthPx / 2f || velocity < -600f) {
        offsetX.animateTo(-actionWidthPx)   // abre (snap para revelar)
    } else {
        offsetX.animateTo(0f)               // fecha (snap para fechar)
    }
}
```

- Se arrastou mais da metade (`actionWidth = 162.dp`) **ou** velocidade > 600px/s → abre.
- Caso contrário → fecha com animação.
- Ao tocar em qualquer botão, o painel fecha antes da ação via `offsetX.animateTo(0f)`.

**Estado por card:** `Animatable` é criado com `remember(trip.id)`, garantindo estado independente por viagem.

> **Regra de padrão:** Para adicionar ou remover botões do swipe, ajuste apenas a `Row` de botões e o valor `actionWidth`. O mecanismo de drag/snap não precisa ser alterado.

---

### 5. Confirmação de exclusão

`pendingDelete: MutableState<TripEntity?>` armazena a viagem aguardando confirmação.

Ao clicar em "Excluir" no swipe:
1. `pendingDelete = trip` — abre o `AlertDialog`
2. Confirmação → `viewModel.deleteTrip(trip)` → `pendingDelete = null`
3. Cancelamento → `pendingDelete = null`

O `AlertDialog` exibe o nome da viagem e avisa que **dias, atividades, contatos e vouchers** serão apagados.

```kotlin
// ViewModel
fun deleteTrip(trip: TripEntity) {
    viewModelScope.launch { repo.deleteTrip(trip) }
}
```

A deleção no Room provoca re-emissão do `Flow`, removendo o card automaticamente sem intervenção manual na lista.

---

### 6. Cards de ação no rodapé

Dois cards fixos no final da `LazyColumn`, renderizados como `item {}` após os cards de viagem:

| Card | Ícone | Ação |
|---|---|---|
| **Importar viagem** | `FileOpen` (âmbar) | `onImportTrip()` → rota `ImportTrip` |
| **Nova viagem** | `Add` (verde) | `onNewTripClick()` → rota `CreateTrip` |

Ambos usam o composable privado `ActionCard` com parâmetros de ícone, cor e texto. Sempre visíveis, mesmo quando a lista está vazia (o empty state e os cards de ação coexistem).

---

### 7. Estados da lista

```kotlin
when {
    trips == null        -> // Carregando (CircularProgressIndicator)
    trips!!.isEmpty()    -> // Empty state (🗺️ + texto)
    else                 -> // Lista de cards + swipe
}
```

O `null` inicial do `StateFlow` (definido em `TripsListViewModel`) é intencional para separar os estados de *carregando* e *vazio*, evitando flash do empty state durante o boot.

**Empty state:**
- Emoji `🗺️` (56sp)
- Texto "Nenhuma viagem ainda"
- Subtexto "Crie sua primeira viagem no botão abaixo"

---

### 8. Confirmação ao sair do app (`BackHandler`)

```kotlin
BackHandler { showExitDialog = true }
```

Intercepta o botão back do sistema (ou gesto de retorno). Exibe `AlertDialog` com opções "Sair" e "Cancelar". Confirmação chama `activity?.finish()`.

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

**Configuração:** `SettingsRepository.autoOpenActiveTrip` — `SharedPreferences` com chave `"auto_open_active_trip"`, padrão `true`.

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
| `TripCard` | Renderiza um card de viagem (emoji, nome, destino, datas, badge) |
| `StatusBadge` | Pill colorido com label de status |
| `SwipeToRevealTrip` | Container com drag + botões ocultos (Compartilhar, Editar, Excluir) |
| `ImportTripCard` | Card de ação "Importar viagem" |
| `NewTripCard` | Card de ação "Nova viagem" |
| `ActionCard` | Base reutilizável para cards de ação do rodapé |

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
- **Novo botão no swipe:** adicionar `Box` na `Row` de botões em `SwipeToRevealTrip` → ajustar `actionWidth` (atualmente `162.dp` = 3 × 54dp).
- **Novo campo no card:** adicionar campo em `TripEntity` + migration Room → ler o campo em `TripCard`.
- **Novo card de ação no rodapé:** criar composable análogo a `ImportTripCard`/`NewTripCard` usando `ActionCard` → adicionar no `item {}` final da `LazyColumn` → adicionar callback na assinatura de `TripsListScreen` → fazer o wiring em `AppNavigation`.
- **Alterar critério de auto-navegação:** modificar o `LaunchedEffect(trips)` em `AppNavigation.kt` (rota `TripsList`), não em `TripsListScreen`.
