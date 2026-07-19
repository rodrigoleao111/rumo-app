# Módulo 03 — Detalhe do Dia

**Tela:** `DayDetailScreen`  
**Arquivo:** `ui/day/DayDetailScreen.kt`  
**ViewModel:** nenhum — tela **stateless**  
**Entry point de navegação:** rota `day_detail/{tripId}/{dayId}`, acessada ao tocar em qualquer card de dia na `HomeScreen`

---

## Visão geral

Tela de detalhes de um dia específico da viagem. Exibe o clima ao vivo, alertas, checklist de vouchers, links e documentos do dia, e a timeline de atividades colapsáveis. É o hub de execução do roteiro no dia da viagem.

---

## Padrão de arquitetura

`DayDetailScreen` é um **composable stateless** — não tem ViewModel próprio. Segue o mesmo padrão de `HomeScreen`: dados chegam via parâmetros, ações saem via callbacks.

| Responsabilidade | Onde vive |
|---|---|
| Dados do dia (atividades, vouchers, links, doc) | `TripViewModel.tripData` → `AppNavigation` → parâmetro `day: TravelDay` |
| Estado do clima | `liveWeather: MutableState<LiveWeatherDay?>` local |
| Refresh manual do clima | `refreshTrigger: MutableState<Int>` local → `WeatherRepository.refresh()` |
| Estado de expansão das atividades | `expandedActivities: SnapshotStateMap<Long, Boolean>` local |
| Confirmação de exclusão | `activityToDelete: MutableState<Long?>` local → `AlertDialog` |
| Snackbar de confirmação | `SnackbarHostState` local + `refreshKey: Long` (parâmetro) |
| Navegação | callbacks (`onBack`, `onEditDay`, `onEditActivity`, `onDeleteActivity`, `onAddActivity`) |

> **Regra de padrão:** `DayDetailScreen` não deve criar nem receber um `ViewModel`. Todo dado persistido vem do `TripViewModel` via `AppNavigation`. Se um estado precisar sobreviver a recriação da tela (navegação), mova-o para `TripViewModel` — não crie um `DayViewModel` separado.

---

## Fluxo de dados

```
TripViewModel.tripData: StateFlow<TripData?>
  └─ AppNavigation (collectAsStateWithLifecycle)
       └─ busca day = tripData?.days?.find { it.dbId == dayId }
            └─ DayDetailScreen(
                 day          = day,
                 refreshKey   = refreshKey,   ← Long que muda após edição
                 tripLat/Lon  = tripData?.trip?.latitude/longitude,
                 tripStart/End = tripData?.trip?.startDate/endDate
               )
```

**`refreshKey`:** Long passado por `AppNavigation` que é incrementado toda vez que o usuário retorna de `EditActivityScreen` ou `EditDayScreen`. O `LaunchedEffect(refreshKey)` na tela exibe o snackbar "Alterações salvas ✓" quando `refreshKey > 0`.

---

## Ordem dos items na LazyColumn

```
1. WeatherCard         ← sempre presente
2. DayAlertCard        ← se day.dayAlert != null
3. VoucherChecklist    ← se day.vouchers.isNotEmpty()
4. DayLinkCard         ← se day.dayLinkUrl != null/blank
5. DayDocumentCard     ← se day.dayDocumentPath != null/blank
6. HorizontalDivider   ← separador da timeline
7. Empty state         ← se day.activities.isEmpty()
8. ActivityItem × N    ← um por atividade + HorizontalDivider entre cada
```

---

## Funcionalidades

### 1. Clima ao vivo com botão de refresh (`WeatherCard`)

**Fetch inicial:** `LaunchedEffect(refreshTrigger)` com `refreshTrigger == 0` → chama `WeatherRepository.getWeather()` (usa cache se válido).

**Refresh manual:** incrementar `refreshTrigger` → `LaunchedEffect` re-executa → chama `WeatherRepository.refresh()` (limpa cache antes de buscar).

```kotlin
var refreshTrigger by remember { mutableStateOf(0) }

LaunchedEffect(refreshTrigger) {
    if (day.date < LocalDate.now()) {
        weatherLoading = false   // dias passados: sem previsão disponível
    } else {
        weatherLoading = true
        val all = if (refreshTrigger == 0)
            WeatherRepository.getWeather(...)
        else
            WeatherRepository.refresh(...)
        liveWeather    = all?.get(day.date.toString())
        weatherLoading = false
    }
}
```

**Dias passados:** se `day.date < LocalDate.now()`, `weatherLoading` é definido como `false` sem chamar a API. A condição `hasData` será `false`, exibindo "Temperatura indisponível no momento".

**Spinner animado:** `rememberInfiniteTransition` + `animateFloat` (0° → 360°, 700ms, `LinearEasing`, `RepeatMode.Restart`). O ângulo de rotação é aplicado via `Modifier.rotate(spinAngle)` no ícone de `Refresh` somente enquanto `isLoading == true`.

```kotlin
val spinAngle by infiniteTransition.animateFloat(
    initialValue  = 0f,
    targetValue   = 360f,
    animationSpec = infiniteRepeatable(
        animation  = tween(700, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    )
)
// Aplicado: Modifier.rotate(if (isLoading) spinAngle else 0f)
```

**Botão de refresh:** `IconButton` desabilitado (`enabled = !isLoading`) enquanto está buscando dados. Tint alterna: `AmberPrimary` durante loading, `GreenSage` quando disponível.

**Estados do `WeatherCard`:**

| Estado | Condição | Exibição |
|---|---|---|
| Carregando | `isLoading == true` | `CircularProgressIndicator` (28dp) + spinner no ícone Refresh |
| Indisponível | `!hasData` | "Temperatura indisponível no momento" (`TextSecondary`) |
| Disponível | `hasData` | Emoji do clima (38sp) + `"X°C ~ Y°C"` (AmberPrimary) + condição + "● ao vivo" |

`hasData = liveWeather != null && !(liveWeather.minTemp == 0 && liveWeather.maxTemp == 0)` — mesma lógica de `HomeScreen`, temperatura 0/0 é tratada como ausente.

---

### 2. Alerta do dia (`DayAlertCard`)

Renderizado apenas se `day.dayAlert != null`.

**Aparência:** `Surface` com fundo `#FFF8F0` (laranja muito claro) e borda `#E8A040` (âmbar). Emoji ⚠️ à esquerda, texto em `#7A3200` (marrom âmbar).

```kotlin
day.dayAlert?.let { alert ->
    item { DayAlertCard(message = alert) }
}
```

O campo `dayAlert` é editado em `EditDayScreen` e persistido em `TravelDayEntity.dayAlert`.

---

### 3. Checklist de vouchers do dia (`VoucherChecklist`)

Renderizado apenas se `day.vouchers.isNotEmpty()`.

**Dados:** `day.vouchers: List<DayVoucher>` — lista de vouchers cujo `dayId` corresponde ao número do dia. Populada em `Mappers.kt` ao converter `TravelDayEntity → TravelDay`.

**Layout:** rótulo "🎫  LEVAR HOJE" (10sp, `GreenMoss`, letterSpacing 2sp) + `FlowRow` de pills.

**Pills:** `Surface(shape = RoundedCornerShape(100.dp), color = BadgeBookedBg)` com borda `BadgeBookedText` 35% alpha. Texto: `"${voucher.emoji}  ${voucher.label}"` (11sp, `BadgeBookedText`).

> **Regra de padrão:** A checklist é somente leitura nesta tela — marcar vouchers como "Usado" é feito na `VouchersScreen`. O `DayVoucher` é uma projeção simplificada (`emoji` + `label`), não o modelo completo `Voucher`.

---

### 4. Card de link externo (`DayLinkCard`)

Renderizado apenas se `day.dayLinkUrl != null` e não em branco.

**Aparência:** `Card` com fundo `#E8F0E8` (verde muito claro) e borda `GreenMoss` 40% alpha. Ícone 🔗 (24sp), rótulo customizável (`day.dayLinkLabel`, fallback `"Ver documento"`), URL truncada em 1 linha.

**Ação ao tocar:** `Intent(Intent.ACTION_VIEW, Uri.parse(url))` — abre no browser ou app registrado para a URL. Usa `runCatching` para suprimir exceção se nenhum app conseguir abrir.

**Campos no modelo:**
- `TravelDay.dayLinkUrl: String?` — URL externa
- `TravelDay.dayLinkLabel: String` — rótulo exibido (editável em `EditDayScreen`)

---

### 5. Card de documento anexado (`DayDocumentCard`)

Renderizado apenas se `day.dayDocumentPath != null` e não em branco.

**Aparência:** idêntico ao `DayLinkCard` (fundo `#E8F0E8`, borda `GreenMoss` 40%), mas com ícone 📎 e texto "Toque para abrir" como subtexto fixo.

**Nome exibido:** prioridade `dayDocumentTitle` → `dayDocumentName` → `"Documento"`:
```kotlin
name = day.dayDocumentTitle.ifBlank { day.dayDocumentName.ifBlank { "Documento" } }
```

**Ação ao tocar (FileProvider):**
```kotlin
val file = File(path)
if (!file.exists()) return@Card
val ext  = file.extension.lowercase()
val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
val uri  = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
val intent = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(uri, mime)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
context.startActivity(Intent.createChooser(intent, "Abrir com…"))
```

O MIME type é inferido da extensão do arquivo via `MimeTypeMap`. O `Intent.createChooser` abre o seletor de apps do sistema.

**Campos no modelo:**
- `TravelDay.dayDocumentPath: String?` — caminho absoluto em `filesDir/Arquivos/<nome>`
- `TravelDay.dayDocumentName: String` — nome original do arquivo
- `TravelDay.dayDocumentTitle: String` — nome de exibição editável pelo usuário

> **Regra de padrão:** O arquivo físico fica em `context.filesDir/Arquivos/`. O `FileProvider` está configurado em `res/xml/file_paths.xml` com `<files-path name="arquivos" path="Arquivos/" />`. Authority: `com.rodrigoleao.gramado2026.fileprovider`.

---

### 6. Timeline de atividades colapsáveis (`ActivityItem`)

Cada `TravelActivity` é renderizado como um `ActivityItem` que contém `SwipeToRevealActivity`.

**Estado de expansão:** `expandedActivities: SnapshotStateMap<Long, Boolean>` — mapa `activityId → Boolean`. Inicializado vazio (todas colapsadas). Persiste enquanto a tela está na backstack.

```kotlin
val expandedActivities = remember { mutableStateMapOf<Long, Boolean>() }
```

**Layout colapsado (sempre visível):**

| Elemento | Fonte | Estilo |
|---|---|---|
| Barra vertical âmbar | — | 3dp largura, `AmberPrimary` 45% alpha quando colapsado, 100% quando expandido |
| Horário | `activity.time` | 11sp, `AmberPrimary`, `Medium` |
| Emoji + Nome | `activity.emoji` + `activity.name` | `titleSmall`, `SemiBold`, `TextPrimary` |
| Badges | `activity.badges` | `FlowRow` de `BadgeChip` (visible apenas se não vazio) |
| Ícone expand/collapse | `ExpandMore` / `ExpandLess` | `GreenSage` 65% alpha, 24dp |

**Animação de expansão:** `Modifier.animateContentSize(animationSpec = tween(260ms, FastOutSlowInEasing))` — aplicado ao `Row` raiz do item. A expansão é suave sem lógica manual de altura.

**Layout expandido (adicional):**

| Elemento | Condição | Detalhe |
|---|---|---|
| Descrição | sempre | `activity.detail`, `bodyMedium`, `TextSecondary`, lineHeight 22sp |
| Rota de caminhada | `activity.walkStops.isNotEmpty()` | `WalkRouteSection` (ver abaixo) |
| Botão Maps | `activity.mapQuery != null` | `OutlinedButton` 30dp altura, borda `GreenMoss` |
| Botão Uber | `activity.uberDestination != null` | `OutlinedButton` 30dp altura, borda `#444444` |

**Deep links das atividades:**

```kotlin
// Maps
val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}"))

// Uber
val url = "https://m.uber.com/ul/?action=setPickup" +
    "&pickup=my_location" +
    "&dropoff[formatted_address]=${Uri.encode(dest)}" +
    "&dropoff[nickname]=${Uri.encode(activity.name)}"
```

Ambos usam `runCatching` para suprimir exceção se o app não estiver instalado.

**`mapQuery` e `uberDestination`** são preenchidos com o campo "Endereço" na `EditActivityScreen` (mesmo valor gravado nos dois campos).

---

### 7. Rota de caminhada (`WalkRouteSection`)

Renderizado dentro do `ActivityItem` expandido quando `activity.walkStops.isNotEmpty()`.

**Layout:** lista vertical de paradas com indicador visual estilo "stepper":

```
● ── 🏛️  Palácio dos Festivais
│
● ── 🌲  Parque Knorr
│
◉ ── 🏁  Ponto final (cor âmbar, SemiBold)
```

- **Círculo:** `Box` 10dp com `CircleShape`. Cor `AmberPrimary` se `stop.isLast`, senão `GreenMoss`.
- **Linha conectora:** `Box` 1.5dp × 30dp, `GreenMoss` 25% alpha. Omitida no último item (`index < stops.size - 1`).
- **Label:** `"${stop.emoji}  ${stop.label}"`, `bodySmall`, `TextSecondary`. `SemiBold` se `stop.isLast`.
- **Sublabel:** `stop.sublabel` (opcional), `labelSmall`, `TextSecondary` 60% alpha.

**Modelo `WalkStop`:** `emoji`, `label`, `sublabel: String?`, `isLast: Boolean`.

---

### 8. Swipe para revelar Editar e Deletar (`SwipeToRevealActivity`)

**Implementação:** customizada com `Animatable<Float>` + `Modifier.draggable` — mesmo padrão de `SwipeToRevealTrip` na lista de viagens. Não usa `SwipeToDismissBox`.

**Parâmetros:**
- `actionWidth = 128.dp` — largura total dos dois botões (2 × 64dp)
- Botão Editar: fundo `AmberPrimary`, ícone `Edit` em `GreenMoss`
- Botão Deletar: fundo `#D32F2F` (vermelho), ícone `Delete` em branco

**Lógica de snap (idêntica à da lista de viagens):**
```kotlin
onDragStopped = { velocity ->
    if (offsetX.value < -actionWidthPx / 2f || velocity < -600f) {
        offsetX.animateTo(-actionWidthPx)  // abre
    } else {
        offsetX.animateTo(0f)              // fecha
    }
}
```

**Ao tocar em botão:** `scope.launch { offsetX.animateTo(0f) }` fecha o swipe antes de disparar a ação.

**Estado por item:** `Animatable(0f)` criado com `remember` simples (sem chave). Como `itemsIndexed` garante que cada `ActivityItem` é um composable distinto, cada um tem seu próprio `Animatable`.

---

### 9. Confirmação de exclusão

`activityToDelete: MutableState<Long?>` armazena o ID da atividade aguardando confirmação.

Ao tocar "Excluir" no swipe:
1. `activityToDelete = activity.id` → abre `AlertDialog`
2. Confirmação → `onDeleteActivity(actId)` → `activityToDelete = null`
3. Cancelamento → `activityToDelete = null`

O `AlertDialog` informa que "Esta atividade será removida permanentemente do dia." Botão de confirmação em `#D32F2F` (vermelho), `Bold`.

---

### 10. FAB para nova atividade

```kotlin
FloatingActionButton(
    onClick        = onAddActivity,
    containerColor = AmberPrimary,
    contentColor   = Color.White,
    shape          = RoundedCornerShape(16.dp)
) {
    Icon(Icons.Default.Add, contentDescription = "Adicionar atividade")
}
```

Callback wired em `AppNavigation` para navegar para `EditActivityScreen` em modo criação (sem `activityId`).

---

### 11. Snackbar de confirmação após edição

```kotlin
LaunchedEffect(refreshKey) {
    if (refreshKey > 0L) snackbarHostState.showSnackbar("Alterações salvas ✓")
}
```

`refreshKey: Long` é um parâmetro da tela incrementado em `AppNavigation` toda vez que o usuário retorna de `EditActivityScreen` ou `EditDayScreen`. O `LaunchedEffect` exibe o snackbar apenas quando `refreshKey > 0` — ignorando o valor inicial (`0L`).

**Estilo do snackbar:** `containerColor = AmberPrimary`, `contentColor = Color.White` — padrão do app.

---

## Assinatura completa do composable

```kotlin
@Composable
fun DayDetailScreen(
    day: TravelDay,
    refreshKey: Long = 0L,
    tripLat: Double? = null,
    tripLon: Double? = null,
    tripStartDate: String? = null,
    tripEndDate: String? = null,
    onBack: () -> Unit = {},
    onEditDay: () -> Unit = {},
    onEditActivity: (Long) -> Unit = {},
    onDeleteActivity: (Long) -> Unit = {},
    onAddActivity: () -> Unit = {},
    onMoveActivity: (from: Int, to: Int) -> Unit = { _, _ -> }
)
```

---

## Composables privados (resumo)

| Composable | Parâmetros relevantes | Responsabilidade |
|---|---|---|
| `WeatherCard` | `liveWeather`, `isLoading`, `onRefresh` | Card de clima com spinner e botão de refresh |
| `DayAlertCard` | `message` | Banner de alerta laranja com ⚠️ |
| `VoucherChecklist` | `vouchers: List<DayVoucher>` | Pills "LEVAR HOJE" em FlowRow |
| `DayLinkCard` | `label`, `url`, `context` | Card clicável que abre URL no browser |
| `DayDocumentCard` | `name`, `path`, `context` | Card clicável que abre arquivo via FileProvider |
| `ActivityItem` | `activity`, `expanded`, `onToggle`, `onEdit`, `onDelete`, `onMapClick`, `onUberClick` | Item colapsável da timeline |
| `SwipeToRevealActivity` | `onEdit`, `onDelete`, `content` | Container de swipe com botões Editar e Excluir |
| `WalkRouteSection` | `stops: List<WalkStop>` | Stepper visual de paradas de caminhada |

---

## Checklist para futuras modificações

- **Novo card de informação do dia:** criar composable análogo a `DayLinkCard` / `DayDocumentCard` → adicionar campo em `TravelDay` + `TravelDayEntity` + migration + mapper → inserir `item {}` na `LazyColumn` antes do divisor da timeline.
- **Novo botão na atividade expandida:** adicionar `OutlinedButton` no bloco `if (expanded)` dentro de `ActivityItem` → criar campo em `TravelActivity` se necessário.
- **Novo botão no swipe:** adicionar `Box` na `Row` de botões em `SwipeToRevealActivity` → ajustar `actionWidth` (atualmente `128.dp` = 2 × 64dp).
- **Persistir estado de expansão entre navegações:** mover `expandedActivities` de `remember` local para `TripViewModel` se for necessário que o estado survive à backstack.
- **Anexar um mapa/documento a um dia:** usar os mecanismos genéricos já existentes — `dayLinkUrl`/`dayLinkLabel` (link externo, ex: "Mapa de Rotas") ou `dayDocumentPath` (arquivo anexado), ambos editáveis no `EditDayScreen` e exportados no `.travel`. (O antigo botão hardcoded do mapa do Bustour, preso a `day.id == 3`, foi removido em favor dessa abordagem.)
- **Alterar comportamento do refresh de clima:** a distinção entre fetch inicial (`getWeather`) e manual (`refresh`) está no valor de `refreshTrigger`. `0` → usa cache; `> 0` → força nova busca. Não chamar `refresh()` no fetch inicial para não invalidar o cache desnecessariamente.
- **Adicionar ação ao snackbar:** `snackbarHostState.showSnackbar(message, actionLabel)` aceita um label de ação. O retorno é `SnackbarResult.ActionPerformed` ou `Dismissed` — verificar o retorno se for necessário agir ao clicar.
