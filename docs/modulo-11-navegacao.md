# Módulo 11 — Navegação Global

**Arquivo principal:** `navigation/AppNavigation.kt`  
**Splash:** `ui/splash/SplashScreen.kt`  
**Entry point:** `MainActivity` → `AppNavigation(importUriState)`

---

## Visão geral

`AppNavigation` é o único grafo de navegação do app. Contém todas as rotas, instancia os ViewModels via `hiltViewModel()` e conecta telas estateless com callbacks. `MainPagerScreen`, definida no mesmo arquivo, encapsula as 5 abas da viagem aberta (HorizontalPager + bottom nav em pill + FAB contextual + Snackbar).

---

## Rotas (`Screen`)

```kotlin
sealed class Screen(val route: String) {
    object Splash            : Screen("splash")
    object TripsList         : Screen("trips_list")
    object TripMain          : Screen("trip/{tripId}/main")
    object DayDetail         : Screen("trip/{tripId}/day/{dayId}")
    object CreateTrip        : Screen("trip/create")
    object EditTrip          : Screen("trip/{tripId}/edit")
    object EditDay           : Screen("trip/{tripId}/day/{dayNumber}/edit")
    object EditActivity      : Screen("trip/{tripId}/day/{dayNumber}/activity/{activityId}")
    object EditContact       : Screen("trip/{tripId}/contact/{contactId}")
    object EditVoucher       : Screen("trip/{tripId}/voucher/{voucherId}")
    object EditBoardingPass  : Screen("trip/{tripId}/pass/{passId}")
    object NoteEditor        : Screen("trip/{tripId}/note/{noteId}")
    object DayNotes          : Screen("trip/{tripId}/day/{dayNumber}/notes")
    object ImportTrip        : Screen("import_trip")
    object ShareTrip         : Screen("trip/{tripId}/share")
    object Settings          : Screen("settings")
}
```

Rotas com parâmetros têm `createRoute(...)` estático para construir a string final com os IDs reais. Ex: `Screen.TripMain.createRoute(42L)` → `"trip/42/main"`.

**Nova vs. edição** — rotas de formulário usam ID `0L` para criação:
- `EditActivity.createRoute(tripId, dayNumber, 0L)` → nova atividade
- `EditContact.createRoute(tripId, 0L)` → novo contato
- `EditVoucher.createRoute(tripId, 0L)` → novo voucher
- `EditBoardingPass.createRoute(tripId, 0L)` → nova passagem

---

## `startDestination`

```kotlin
val startDestination = when {
    importUri != null -> Screen.ImportTrip.route   // app aberto via arquivo .travel externo
    else              -> Screen.Splash.route
}
```

Quando o app é aberto por um intent `ACTION_VIEW` com arquivo `.travel`, pula a splash e vai direto para `ImportTripScreen`.

---

## Transições animadas

Configuradas globalmente no `NavHost`:

```kotlin
NavHost(
    enterTransition    = { slideInHorizontally({ it })       + fadeIn(tween(320)) },
    exitTransition     = { slideOutHorizontally({ -it / 4 }) + fadeOut(tween(320)) },
    popEnterTransition = { slideInHorizontally({ -it / 4 })  + fadeIn(tween(320)) },
    popExitTransition  = { slideOutHorizontally({ it })      + fadeOut(tween(320)) }
)
```

| Tipo | Entrada | Saída |
|---|---|---|
| Push (navegar para frente) | slide da direita (100%) + fade in | slide para esquerda (25%) + fade out |
| Pop (voltar) | slide da esquerda (25%) + fade in | slide para direita (100%) + fade out |

O offset reduzido na saída (`-it / 4` = 25% da largura) cria o efeito de paralaxe — a tela que sai recua mais devagar que a nova tela entra. Duração: `ANIM_DURATION = 320ms` em todos os casos.

---

## Splash Screen

**Arquivo:** `ui/splash/SplashScreen.kt`

```kotlin
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        delay(2000)
        alpha.animateTo(0f, animationSpec = tween(durationMillis = 300))
        onFinished()
    }

    Image(
        painter      = painterResource(R.drawable.splash_background),
        contentScale = ContentScale.Crop,
        modifier     = Modifier.fillMaxSize().alpha(alpha.value)
    )
}
```

**Sequência:**
1. `alpha` começa em `1f` — imagem visível imediatamente (sem fade-in)
2. `delay(2000)` — 2 segundos de exibição
3. `alpha.animateTo(0f, tween(300))` — fade-out de 300ms via `Animatable`
4. `onFinished()` — chamado após a animação; navega para `TripsList` com `popUpTo(Splash) { inclusive = true }`

**Imagem:** `res/drawable-nodpi/splash_background.png`, `ContentScale.Crop` — preenche toda a tela sem distorção.

**`popUpTo(Splash) { inclusive = true }`:** remove a rota da splash do backstack. Pressionar "voltar" em `TripsList` sai do app, não volta para a splash.

**Sistema (antes do Compose):** `Theme.PipaApp.Splash` com `windowBackground = #1B4332` (GreenMoss) e ícone transparente — evita a janela branca padrão do Android enquanto o Compose carrega.

---

## Auto-navegação para viagem ativa

Em `TripsList`:

```kotlin
var autoNavigated by rememberSaveable { mutableStateOf(false) }

LaunchedEffect(trips) {
    if (!autoNavigated && trips != null && settings.autoOpenActiveTrip) {
        val today  = LocalDate.now()
        val active = trips!!.filter { trip ->
            val start = runCatching { LocalDate.parse(trip.startDate) }.getOrNull()
            val end   = runCatching { LocalDate.parse(trip.endDate)   }.getOrNull()
            start != null && end != null && !today.isBefore(start) && !today.isAfter(end)
        }
        if (active.size == 1) {
            autoNavigated = true
            navController.navigate(Screen.TripMain.createRoute(active.first().id)) {
                popUpTo(Screen.TripsList.route)  // mantém TripsList no backstack
            }
        } else {
            autoNavigated = true   // 0 ou 2+ ativas → não navega, mas marca para não repetir
        }
    }
}
```

**`rememberSaveable`:** `autoNavigated` sobrevive a recomposições mas **não** a recriações de processo (o que é desejado — reiniciar o app deve tentar a auto-navegação novamente).

**Condição:** exatamente 1 viagem ativa. Zero ou mais de uma → exibe a lista normalmente.

**`popUpTo(TripsList.route)`** sem `inclusive = true` — mantém `TripsList` na backstack. Ao pressionar "voltar" em `TripMain`, o usuário retorna para a lista.

---

## `MainPagerScreen` — 5 abas da viagem

Composable privado instanciado pelo composable de rota `TripMain`. Recebe todos os dados da viagem como parâmetros (stateless) e emite ações via callbacks.

### `HorizontalPager`

```kotlin
val pagerState = rememberPagerState(pageCount = { TAB_ICON_RES.size })  // 5 páginas

HorizontalPager(
    state                   = pagerState,
    beyondViewportPageCount = 1          // pré-carrega 1 página adjacente
) { page ->
    when (page) {
        0 -> HomeScreen(...)
        1 -> VouchersScreen(...)
        2 -> BoardingPassScreen(...)
        3 -> ContactsScreen(...)
        4 -> NotesListContent(...)
    }
}
```

`beyondViewportPageCount = 1` mantém a aba adjacente composta em memória, evitando rebuild ao deslizar.

### Bottom nav em pill (`PillNavItem`)

Não usa `NavigationBar` do Material 3 — é uma `Row` customizada:

```
Box (fillMaxWidth, navigationBarsPadding, padding start/end 16dp, top 12dp, bottom 16dp)
 └─ Row (softDropShadow blur 18dp / GreenMoss 45% halo, clip RoundedCornerShape(32dp), background Cream)
      └─ PillNavItem × 5
```

> **Sombra da pílula:** usa o helper privado `Modifier.softDropShadow` (em `AppNavigation.kt`), que desenha um halo borrado via `BlurMaskFilter` num `drawBehind`. O `Modifier.shadow` padrão do Compose, com `spotColor`/`ambientColor` tingidos e alpha baixo, fica praticamente invisível sobre o fundo `Sand` (ainda mais com a pílula em `Cream`, quase da cor do fundo) — por isso a sombra é desenhada à mão. Ajuste `color`/`blurRadius`/`offsetY` para calibrar.

Os ícones das abas são vetores da marca (`TAB_ICON_RES`: `ic_home`, `ic_ticket`, `ic_boarding`, `ic_contacts`, `ic_notes_nav`).

**`PillNavItem`:**
```kotlin
Box(modifier = Modifier.clip(RoundedCornerShape(16dp)).clickable { onClick() }.padding(horizontal = 4dp, vertical = 2dp)) {
    Box(
        52dp × 36dp, RoundedCornerShape(14dp),
        background = if (selected) GreenMoss else Color.Transparent
    ) {
        Icon(tint = if (selected) Color.White else GreenMoss 45% alpha, size = 22dp)
    }
}
```

Aba selecionada: box de fundo `GreenMoss` 52×36 (canto 14dp) + ícone branco.  
Abas não selecionadas: ícone `GreenMoss` 45% alpha, sem pill.

**Navegação entre abas:** `coroutineScope.launch { pagerState.animateScrollToPage(index) }` — animação suave de deslize, não salto instantâneo.

### FAB contextual

```kotlin
val fabAction: (() -> Unit)? = when (pagerState.currentPage) {
    1    -> onAddVoucher
    2    -> onAddBoardingPass
    3    -> onAddContact
    4    -> onAddNote
    else -> null   // aba Home (0) não tem FAB
}
fabAction?.let { action ->
    FloatingActionButton(
        onClick        = action,
        containerColor = AmberPrimary,
        shape          = RoundedCornerShape(16dp)
    ) { Icon(Add, ...) }
}
```

O FAB aparece nas abas 1 (Vouchers), 2 (Embarque), 3 (Contatos) e 4 (Notas). Desaparece na aba 0 (Início) — novas atividades são criadas a partir de `DayDetailScreen`.

### TopAppBar adaptativa

A `TopAppBar` verde só é renderizada quando `pagerState.currentPage != 0` — a aba 0 (Início) tem cabeçalho de capa próprio (dentro de `HomeScreen`) e não usa a barra verde. Portanto a app bar verde vale apenas para as abas 1–4, e sempre traz os ícones de ação Compartilhar/Editar.

| Aba | Título | Botão Sort | Ações |
|---|---|---|---|
| 1 — Vouchers | `"$tripName  •  Vouchers"` | ✅ (abre `DropdownMenu`) | Compartilhar / Editar |
| 2 — Embarque | `"$tripName  •  Passagens"` | ❌ | Compartilhar / Editar |
| 3 — Contatos | `"$tripName  •  Contatos"` | ❌ | Compartilhar / Editar |
| 4 — Notas | `"$tripName  •  Notas"` | ❌ | Compartilhar / Editar |

O botão back da aba 0 vive dentro da `HomeScreen` (chama `onBack`, volta para `TripsList`), não na app bar verde. Nas abas 1–4, o botão back físico/gesto fecha o pager normalmente.

**Ícone Sort (aba Vouchers):** `AmberPrimary` quando o modo não é `BY_CATEGORY` (default) — sinaliza que um agrupamento não padrão está ativo.

**`enterAlwaysScrollBehavior`:** a TopAppBar colapsa ao rolar para baixo e reaparece ao rolar para cima — conectada via `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` no `Scaffold`.

### Snackbar de feedback pós-edição

```kotlin
LaunchedEffect(refreshKey) {
    if (refreshKey > 0L) snackbarHostState.showSnackbar("Alterações salvas ✓")
}
```

Disparado sempre que `refreshKey > 0L` — ou seja, toda vez que o usuário volta de uma tela de edição. Cores: `containerColor = AmberPrimary`, `contentColor = GreenMoss`.

---

## Refresh via `SavedStateHandle`

O mecanismo de refresh evita recarregar todo o `TripViewModel` ao voltar de uma edição. Funciona com um timestamp gravado no `SavedStateHandle` da entrada anterior da backstack:

### Nos composables de edição (ao voltar):

```kotlin
onBack = {
    navController.previousBackStackEntry
        ?.savedStateHandle
        ?.set("refresh", System.currentTimeMillis())
    navController.popBackStack()
}
```

Telas que usam este padrão: `EditTrip`, `EditDay`, `EditActivity`, `EditContact`, `EditVoucher`, `EditBoardingPass`, além de `NoteEditor` e `DayNotes`.

### Na tela pai (TripMain ou DayDetail):

```kotlin
val refreshKey by entry.savedStateHandle
    .getStateFlow("refresh", 0L)
    .collectAsStateWithLifecycle()

LaunchedEffect(refreshKey) {
    if (refreshKey > 0L) vm.refresh()
}
```

`SavedStateHandle.getStateFlow("refresh", 0L)` emite sempre que o valor muda. O `LaunchedEffect(refreshKey)` reage à mudança chamando `vm.refresh()` — que relê os dados do banco sem recriar o ViewModel.

**Por que timestamp e não `Boolean`:** um `Boolean` que vai de `false` para `true` e volta para `false` pode não disparar o `LaunchedEffect` se a recomposição coalescer os valores. O timestamp é sempre diferente a cada edição, garantindo que o efeito seja executado.

**Snackbar e refresh juntos:** o mesmo `refreshKey` que aciona `vm.refresh()` em `TripMain` também aciona `snackbarHostState.showSnackbar(...)` — ambos no mesmo `LaunchedEffect(refreshKey)`.

---

## Injeção de dependências (Hilt)

`AppNavigation` não cria mais `db`/`repo`/`settings`/`categoryRepo` manualmente via `remember { ... }`. Toda a árvore de dependências vem do Hilt:

```kotlin
val settingsVm: SettingsViewModel = hiltViewModel()

// em cada composable de rota:
val vm: TripViewModel = hiltViewModel()
```

Cada tela obtém seu ViewModel (`@HiltViewModel`) via `hiltViewModel()`, e o Hilt injeta os repositórios e o `Room` subjacente como singletons no grafo da aplicação. Isso garante que todas as telas compartilhem a mesma instância de repositório sem instanciação manual, evitando caches inconsistentes. As preferências ficam num `SettingsViewModel` (`settingsVm`) obtido no nível de `AppNavigation` — veja a seção seguinte.

---

## Intent externo e backstack de importação

Quando o app é aberto via arquivo `.travel` externo (WhatsApp, e-mail etc.):

1. `startDestination = Screen.ImportTrip.route` — `TripsList` nunca é empilhada
2. `ImportTripScreen.onImported(tripId)`:
   ```kotlin
   navController.navigate(Screen.TripsList.route) {
       popUpTo(0) { inclusive = true }   // limpa a backstack inteira (inclusive ImportTrip)
   }
   navController.navigate(Screen.TripMain.createRoute(tripId))
   ```
3. Resultado: backstack final = `[TripsList, TripMain]` — pressionar "voltar" em `TripMain` leva para `TripsList`, não fecha o app.

**`popUpTo(0) { inclusive = true }`:** `0` é o ID da raiz do grafo — remove tudo incluindo `ImportTrip`. As duas navegações subsequentes reconstroem a backstack correta.

**`onNewIntent` (app já aberto):**
```kotlin
LaunchedEffect(importUri) {
    if (importUri != null && navController.currentDestination?.route != Screen.ImportTrip.route) {
        navController.navigate(Screen.ImportTrip.route) { launchSingleTop = true }
    }
}
```
Navega para `ImportTrip` sem duplicar a rota se já estiver nela.

---

## `showEmergencyContacts` — sincronização após Settings

A preferência é coletada uma única vez no nível de `AppNavigation`, a partir de um `StateFlow` do `SettingsViewModel` (respaldado pelo `DataStore`):

```kotlin
val settingsVm: SettingsViewModel = hiltViewModel()
val showEmergencyContacts by settingsVm.showEmergencyContacts.collectAsStateWithLifecycle()
```

O valor é repassado adiante como estado (`TripScreenState.showEmergencyContacts` → `ContactsScreen`). Como o `DataStore` emite pelo `Flow` sempre que a tela de `Settings` grava a preferência, a mudança se propaga automaticamente — sem `DisposableEffect`, `LifecycleEventObserver` ou releitura manual de `SharedPreferences` no `ON_RESUME`.

---

## Composables e símbolos (resumo)

| Símbolo | Tipo | Responsabilidade |
|---|---|---|
| `Screen` | `sealed class` | Todas as rotas do app com `createRoute()` onde aplicável |
| `ANIM_DURATION` | `const val` (320) | Duração de todas as transições entre telas |
| `TAB_ICON_RES` / `TAB_LABELS` | `val` | Ícones (drawables da marca) e labels das 5 abas (em sincronia com o índice de página) |
| `AppNavigation` | composable | Grafo completo de navegação; obtém ViewModels via Hilt (`hiltViewModel()`) |
| `SplashScreen` | composable | Exibe imagem por 2s, fade-out 300ms, chama `onFinished` |
| `MainPagerScreen` | composable privado | Pager 5 abas + TopAppBar adaptativa + FAB contextual + Snackbar + bottom nav pill |
| `PillNavItem` | composable privado | Item da bottom nav em pill (box `GreenMoss` + ícone branco na aba ativa) |
| `SortMenuItem` | composable privado | Item do `DropdownMenu` de agrupamento de vouchers com check |

---

## Checklist para futuras modificações

- **Nova tela:** adicionar `object Nova : Screen("rota/{param}")` em `Screen` → adicionar `composable(Screen.Nova.route) { ... }` no `NavHost` → adicionar navegação nos composables de origem.
- **Nova aba no pager:** incrementar `TAB_ICON_RES`/`TAB_LABELS` (manter em sincronia) → ajustar `when (page)` no `HorizontalPager` → ajustar FAB (`when (pagerState.currentPage)`) se a nova aba precisar de FAB → ajustar título da TopAppBar.
- **Mudar duração das transições:** alterar `ANIM_DURATION`. Afeta todas as rotas do `NavHost` simultaneamente.
- **Transição customizada por rota:** substituir a transição global do `NavHost` por `enterTransition`/`exitTransition` no `composable(...)` específico — esses parâmetros sobrescrevem o global.
- **Refresh sem timestamp:** se quiser usar `Boolean` em vez de `Long`, garanta que o `onBack` sempre alterne o valor (ex: `set("refresh", !get("refresh", false))`). O timestamp é mais simples e robusto.
- **Persistir aba selecionada:** `rememberSaveable { mutableStateOf(0) }` em vez de `rememberPagerState` — sobrevive a recomposição. Atualmente a aba volta para 0 (Início) ao reentrar em `TripMain`.
