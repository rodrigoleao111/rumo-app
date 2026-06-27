# Módulo 02 — Home da Viagem

**Tela:** `HomeScreen`  
**Arquivo:** `ui/home/HomeScreen.kt`  
**ViewModel:** nenhum — tela **stateless**  
**Entry point de navegação:** aba "Início" dentro de `MainPagerScreen` (rota `trip_main/{tripId}`)

---

## Visão geral

Tela principal de uma viagem. Exibe a lista de dias em cards verticais com clima ao vivo, e um card de hotel fixo no rodapé. É o ponto de entrada para o detalhe de cada dia do roteiro.

---

## Padrão de arquitetura

`HomeScreen` é um **composable stateless** — não tem ViewModel próprio. Todo dado chega via parâmetros; toda ação sai via callback.

| Responsabilidade | Onde vive |
|---|---|
| Dados da viagem (dias, hotel) | `TripViewModel` (via `tripData: StateFlow<TripData?>`) |
| Passagem de parâmetros | `AppNavigation` → `MainPagerScreen` → `HomeScreen` |
| Estado do clima | `liveWeather: MutableState` local, dentro de `HomeScreen` |
| Cache e fetch do clima | `WeatherRepository` (singleton `object`) |
| Navegação ao dia | callback `onDayClick(dayId: Int)` → `AppNavigation` |

> **Regra de padrão:** `HomeScreen` não deve receber nem criar um `ViewModel`. Qualquer dado persistido (dias, hotel, coordenadas) vem de `TripViewModel` via `tripData`. Estados efêmeros de UI (clima, scroll) são `remember` locais.

---

## Fluxo de dados

```
TripViewModel.tripData: StateFlow<TripData?>
  └─ MainPagerScreen (collectAsStateWithLifecycle)
       └─ HomeScreen(
            days         = tripData?.days,
            hotelName    = tripData?.trip?.hotelName,
            hotelAddress = tripData?.trip?.hotelAddress,
            hotelPhone   = tripData?.trip?.hotelPhone,
            tripLat      = tripData?.trip?.latitude,
            tripLon      = tripData?.trip?.longitude,
            tripStartDate/tripEndDate
          )
               └─ LaunchedEffect(tripLat, tripLon, ...)
                    └─ WeatherRepository.getWeather(...)
                         └─ liveWeather: Map<String, LiveWeatherDay>?
```

O `LazyColumn` exibe os cards dos dias. Cada `DayCard` recebe `liveWeather?.get(day.date.toString())` — um único `LiveWeatherDay?` correspondente àquela data.

---

## Funcionalidades

### 1. Cards de dia (`DayCard`)

Cada `TravelDay` é renderizado como um `Card` clicável com:

| Elemento | Fonte de dados | Observação |
|---|---|---|
| Dia do mês | `day.date.dayOfMonth` | Número grande em branco, container `GreenMoss` |
| Mês abreviado | `day.date` formatado `"MMM"` pt-BR | Em maiúsculas, alpha 0.8f |
| Dia da semana | `day.dayOfWeek` | Em maiúsculas, espaçamento de letra 2sp, cor `GreenMoss` |
| Badge "HOJE" | `day.isToday` | Composable `HojeBadge` (pill `GreenMoss`) |
| Título | `day.title` | `titleMedium`, `TextPrimary` |
| Emoji do clima | `liveWeather.emoji` | 34sp, visível apenas se `hasWeather` |
| Temperatura | `liveWeather.minTemp` / `maxTemp` | Cor `AmberPrimary`, formato `"X°  ~  Y°C"` |
| Condição | `liveWeather.condition` | `bodySmall`, `TextSecondary` |
| Indicador "● ao vivo" | — | 9sp, `GreenMoss`, `SemiBold`, letterSpacing 0.5sp |
| Contagem de atividades | `day.activities.size` | `"X atividades · toque para ver o roteiro"`, `GreenSage` |

**Border do dia atual:**
```kotlin
border = BorderStroke(
    width = if (day.isToday) 2.dp else 1.dp,
    color = if (day.isToday) GreenMoss else CardBorder
)
```

---

### 2. Clima ao vivo — fetch e cache

**Fonte:** `WeatherRepository.getWeather()` — singleton `object` em `data/weather/WeatherRepository.kt`.

**Trigger:** `LaunchedEffect(tripLat, tripLon, tripStartDate, tripEndDate)` — re-executa se qualquer coordenada ou data mudar (ex: quando `tripData` carrega do banco após a primeira composição com valores nulos).

```kotlin
LaunchedEffect(tripLat, tripLon, tripStartDate, tripEndDate) {
    weatherLoading = true
    try {
        liveWeather = WeatherRepository.getWeather(context, tripLat, tripLon, tripStartDate, tripEndDate)
    } finally {
        weatherLoading = false
    }
}
```

**Tipo retornado:** `Map<String, LiveWeatherDay>?` — chave é a data ISO (`"yyyy-MM-dd"`). Nulo se coordenadas ausentes ou erro de rede sem cache.

**Cache:** dupla camada — memória (`memoryCacheMap`) + disco (`SharedPreferences`). TTL de 3h. Stale cache é usado como fallback em caso de erro de rede.

**Condição `hasWeather`:**
```kotlin
val hasWeather = liveWeather != null && !(liveWeather.minTemp == 0 && liveWeather.maxTemp == 0)
```
Temperatura `0/0` é tratada como dado inválido (ausente), não como temperatura real. Nesses casos a UI exibe "Temperatura indisponível".

**Limite da API:** Open-Meteo `/v1/forecast` suporta até 16 dias a partir de hoje. `WeatherRepository` clipa `endDate` para `hoje + 15 dias`. Viagens além do alcance de previsão (futuras ou passadas) mostram "Temperatura indisponível" para todos os dias.

**Estados de UI no `DayCard`:**

| Estado | Condição | Exibição |
|---|---|---|
| Carregando | `isLoading == true` | `CircularProgressIndicator` (14dp, AmberPrimary) |
| Indisponível | `!hasWeather` | Texto "Temperatura indisponível" (`TextSecondary`) |
| Disponível | `hasWeather` | Temperatura + condição + "● ao vivo" |

---

### 3. Badge "HOJE" e auto-scroll

**Badge:** composable `HojeBadge` — pill `GreenMoss` com texto "HOJE" (9sp, bold, letterSpacing 2sp). Exibido na `Row` do cabeçalho do `DayCard` somente quando `day.isToday == true`.

**`day.isToday`:** propriedade calculada em `TravelDay` comparando `day.date` com `LocalDate.now()`.

**Auto-scroll:**
```kotlin
val todayIdx = remember(days) { days.indexOfFirst { it.isToday } }
var scrolledToToday by remember { mutableStateOf(false) }

LaunchedEffect(todayIdx) {
    if (todayIdx >= 0 && !scrolledToToday) {
        listState.animateScrollToItem(index = todayIdx)
        scrolledToToday = true
    }
}
```

- `todayIdx` é calculado via `remember(days)` — recalcula apenas quando `days` muda.
- `scrolledToToday` é um flag booleano simples (`remember`, não `rememberSaveable`) — reinicia se a tela for recriada, o que é intencional (volta a rolar ao abrir a viagem novamente).
- `animateScrollToItem` — scroll suave. Se `todayIdx < 0` (nenhum dia é hoje), não ocorre scroll.

> **Regra de padrão:** `scrolledToToday` usa `remember` simples, não `rememberSaveable`. Isso é intencional: o auto-scroll deve acontecer ao abrir a tela, mas não deve impedir o usuário de rolar manualmente depois de uma recomposição.

---

### 4. Card do hotel (`HotelCard`)

Renderizado apenas se `hotelName.isNotBlank()`. Se o campo estiver vazio, nenhum card é inserido na `LazyColumn`.

**Aparência:** `Card` com `containerColor = GreenMoss` (fundo verde escuro), texto em branco.

**Conteúdo:**
- Emoji 🏨 (28sp)
- Nome do hotel (`titleMedium`, `SemiBold`, branco)
- Endereço (`bodySmall`, branco 70% alpha)
- Telefone (`bodySmall`, branco 70% alpha) — exibido apenas se `hotelPhone.isNotBlank()`

**Botões de ação:**

| Botão | Cor container | Deep link |
|---|---|---|
| Maps | branco 15% alpha | `geo:0,0?q=<Uri.encode(hotelAddress)>` |
| Uber | `AmberPrimary` | `https://m.uber.com/ul/?action=setPickup&pickup=my_location&dropoff[formatted_address]=<encoded>&dropoff[nickname]=<encoded>` |
| Ligar | branco 15% alpha | `tel:<hotelPhone.filter { it.isDigit() || it == '+' }>` |

O botão "Ligar" só é renderizado se `hotelPhone.isNotBlank()`.

---

### 5. Layout adaptativo dos botões do hotel (`BoxWithConstraints`)

Os botões do hotel se adaptam ao espaço disponível:

```kotlin
BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val buttonCount = if (hotelPhone.isNotBlank()) 3 else 2
    val spacing     = 8.dp * (buttonCount - 1)
    val buttonWidth = (maxWidth - spacing) / buttonCount
    val showText    = buttonWidth >= 82.dp
    val iconSize    = if (showText) 16.dp else 22.dp
    val btnPadding  = if (showText) ButtonDefaults.ContentPadding
                      else PaddingValues(vertical = 12.dp)
    ...
}
```

- **`buttonWidth >= 82.dp` → mostra ícone + texto** (`Maps`, `Uber`, `Ligar`)
- **`buttonWidth < 82.dp` → só ícone** (ícone maior 22dp, padding vertical reduzido)

Isso garante que com 2 botões (sem telefone) sempre mostre texto, e com 3 botões em telas estreitas mostre apenas o ícone.

> **Regra de padrão:** Para adicionar um quarto botão, incremente `buttonCount` e adicione o `Button` na `Row`. O threshold de 82dp pode precisar de ajuste manual dependendo do rótulo do novo botão.

---

### 6. Navegação para o detalhe do dia

Ao tocar em um `DayCard`, é chamado `onDayClick(day.id)`. O callback é wired em `AppNavigation`:

```kotlin
HomeScreen(
    ...
    onDayClick = { dayId ->
        navController.navigate(Screen.DayDetail.createRoute(tripId, dayId))
    }
)
```

`HomeScreen` não importa `NavController`. A tela é completamente agnóstica de navegação.

---

## Assinatura completa do composable

```kotlin
@Composable
fun HomeScreen(
    days: List<TravelDay>,
    hotelName: String = "",
    hotelAddress: String = "",
    hotelPhone: String = "",
    tripLat: Double? = null,
    tripLon: Double? = null,
    tripStartDate: String? = null,
    tripEndDate: String? = null,
    contentPadding: PaddingValues = PaddingValues(),
    onDayClick: (Int) -> Unit
)
```

`contentPadding` vem do scaffold da `MainPagerScreen` (absorve a altura da `BottomNavigationBar`).

---

## Composables privados (resumo)

| Composable | Parâmetros relevantes | Responsabilidade |
|---|---|---|
| `DayCard` | `day`, `liveWeather`, `isLoading`, `onClick` | Card clicável com data, título, clima e CTA |
| `HotelCard` | `hotelName`, `hotelAddress`, `hotelPhone` | Card fixo no rodapé com nome, endereço e botões de ação |
| `HojeBadge` | — | Pill "HOJE" para marcar o dia atual |

---

## Checklist para futuras modificações

- **Novo campo no card de dia:** adicionar campo em `TravelDay` (e `TravelDayEntity` + migration) → ler em `DayCard`. Se for campo de clima, expor via `LiveWeatherDay` em `WeatherRepository`.
- **Novo botão no hotel:** adicionar `Button` na `Row` dentro de `BoxWithConstraints` → incrementar `buttonCount` → ajustar threshold 82dp se necessário.
- **Adicionar ViewModel:** se algum estado que hoje é `remember` local precisar sobreviver a mudanças de configuração (rotação), mova para `TripViewModel`. Não crie um `HomeViewModel` separado — o dado já existe em `TripViewModel.tripData`.
- **Alterar comportamento de scroll:** modificar o `LaunchedEffect(todayIdx)` em `HomeScreen`. Se quiser que o scroll persista após rotação, trocar `remember` por `rememberSaveable` no flag `scrolledToToday`.
- **Refresh manual do clima:** chamar `WeatherRepository.refresh(...)` (limpa cache antes de buscar) em vez de `getWeather(...)`. O `DayDetailScreen` já implementa esse padrão com um botão de refresh.
- **Tratar clima de dias passados:** dias antes de hoje não têm previsão disponível (Open-Meteo só retorna dados futuros). A condição `hasWeather` já trata isso — dias passados mostrarão "Temperatura indisponível".
