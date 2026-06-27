# Design System — Rumo

---

## Identidade visual

O app usa uma paleta **Forest Dark**: verde floresta profundo como cor primária, âmbar dourado como acento, e um fundo linho-esverdeado muito claro. A combinação remete a natureza e aventura — coerente com o contexto de viagens ao sul do Brasil — sem cair no visual genérico de apps de turismo.

O design é deliberadamente **contido**: poucos tokens de cor, formas arredondadas consistentes, tipografia sem serifa padrão do sistema. O resultado é um app de aparência premium sem depender de fontes customizadas ou bibliotecas de componentes de terceiros.

---

## Paleta de cores

### Tokens principais

| Token | Hex | Uso |
|---|---|---|
| `GreenMoss` | `#1B4332` | Primary — TopAppBar, cabeçalhos de card, badges de data, container de emoji |
| `GreenSage` | `#40916C` | Secondary — botão compartilhar no swipe, loading indicators, datas |
| `GreenLight` | `#F2F5F0` | Background geral — linho esverdeado muito claro |
| `GreenForest` | `#D5E8DC` | Navigation bar, top bars de sistema, cards de fundo suave |
| `GreenMist` | `#2E4039` | Texto secundário esverdeado (uso pontual) |
| `GreenWarm` | `#C2D9CB` | Container suave, hover states |
| `AmberPrimary` | `#D4A017` | Accent — badges de status planning, snackbar, FAB, botões "Próximo", pill nav ativo |
| `AmberLight` | `#FFF8E1` | Fundo de badge amber |
| `TextPrimary` | `#0D1F16` | Quase-preto com tonalidade verde — títulos e texto principal |
| `TextSecondary` | `#3A5045` | Verde-cinza escuro — subtítulos, labels, texto auxiliar |
| `SurfaceWhite` | `#F8FAF8` | Off-white levemente esverdeado — superfície de cards |
| `CardBorder` | `#1B4332` 31% alpha | Borda semitransparente em cards |
| `DividerColor` | `#1B4332` 9% alpha | Divisórias sutis entre seções |

### Tokens de badge

| Token | Hex | Contexto |
|---|---|---|
| `BadgeFreeText / Bg` | `#1B4332` / `#1B4332` 10% | Badge "Grátis" |
| `BadgePaidText / Bg` | `#7A5000` / `#D4A017` 10% | Badge "Pago" |
| `BadgeBookedText / Bg` | `#1A3A8A` / `#2850A0` 10% | Badge "Reservado" |
| `BadgeUberText / Bg` | `#444444` / `#000000` 6% | Badge "Uber" |

**Padrão de badge:** fundo = cor-texto a 10–15% de alpha; borda = cor-texto a 40% de alpha. Isso garante legibilidade em qualquer fundo sem hard-code de cores específicas por tema.

### Hierarquia cromática

```
GreenMoss  ←─ autoridade, estrutura, ações confirmadas
AmberPrimary ←─ atenção, acento, ações primárias de avanço
GreenSage  ←─ secundário, datas, ações de suporte
Vermelho (#D32F2F) ←─ exclusão, perigo (nunca como token nomeado — hardcoded nos dois pontos de uso)
```

A cor vermelha não tem token porque é usada em exatamente dois contextos (botão Excluir no dialog e fundo do swipe de deleção) e não faz parte do vocabulário recorrente do app.

---

## Material Theme

O app usa `MaterialTheme` do Material 3 com `lightColorScheme` mapeando os tokens customizados para os papéis do sistema:

```kotlin
primary          = GreenMoss       // botões primários, TopAppBar
secondary        = AmberPrimary    // botões secundários, FAB
background       = GreenLight      // fundo da Activity
surface          = SurfaceWhite    // Cards, Sheets
surfaceVariant   = GreenForest     // fundos alternativos
outline          = CardBorder      // bordas de TextField, chips
```

**Status bar:** `GreenLight` (mesmo que o fundo) com `isAppearanceLightStatusBars = true` — ícones escuros. **Navigation bar:** `GreenForest`. Ambos configurados via `SideEffect` em `GramadoTheme`.

O tema nunca usa modo escuro — `lightColorScheme` sem contraparte `darkColorScheme`. O app não responde a `uiMode` do sistema.

---

## Tipografia

Fonte do sistema (padrão Android — Roboto). Sem fontes importadas.

| Estilo | Tamanho | Peso | Uso principal |
|---|---|---|---|
| `headlineLarge` | 28sp / lh 34sp | Bold | — (reservado) |
| `headlineMedium` | 20sp / lh 26sp | SemiBold | Títulos de seção grandes |
| `titleLarge` | 18sp / lh 24sp | SemiBold | Títulos de card principal |
| `titleMedium` | 16sp / lh 22sp | SemiBold | Nome de viagem, títulos de card |
| `bodyLarge` | 16sp / lh 24sp | Normal | Texto de formulário, labels de toggle |
| `bodyMedium` | 14sp / lh 22sp | Normal `TextSecondary` | Texto auxiliar, descrições |
| `bodySmall` | 12sp / lh 18sp | Normal `TextSecondary` | Subtítulos, hints, labels menores |
| `labelMedium` | 11sp / lh 16sp / ls 0.5sp | Medium | Labels de informação |
| `labelSmall` | 10sp / lh 14sp / ls 1sp | Medium | Datas, labels uppercase em caps |

**Usos recorrentes fora do tema:**
- **9sp + letterSpacing 1.5sp** — labels de `InfoBlock` nos boarding passes
- **10sp + letterSpacing 2sp uppercase** — cabeçalhos de grupo em `VouchersScreen` e `ContactsScreen`
- **UPPERCASE** é aplicado via `.uppercase()` no código — nunca via `TextStyle`

---

## Formas (bordas arredondadas)

O app usa um único vocabulário de raio:

| Raio | Contexto |
|---|---|
| `RoundedCornerShape(100.dp)` — pill completo | Badges de status, chips de badge de atividade, bottom nav pill, `StatusBadge` |
| `RoundedCornerShape(32.dp)` | Bottom nav container |
| `RoundedCornerShape(16.dp)` | Cards principais (`TripCard`, `ActionCard`, `VoucherCard`), FAB, botões de formulário |
| `RoundedCornerShape(14.dp)` | Botão primário de salvar |
| `RoundedCornerShape(12.dp)` | Container de emoji, `TimePickerField`, `PillNavItem` pill interno, `EmojiPickerDialog` células |
| `RoundedCornerShape(10.dp)` | Células do `EmojiPickerDialog` |
| `CircleShape` | Pontos de rota de caminhada (`WalkRouteSection`), color picker de badges |

**Regra geral:** cards → 16dp; botões e chips → pill (100dp) ou 14–16dp; containers pequenos → 12dp.

---

## Elevação e sombra

O app usa elevação de forma conservadora — apenas onde há hierarquia real a comunicar.

| Elevação | Contexto |
|---|---|
| `2dp` | `TripCard`, `BoardingPassCard` — card padrão |
| `1dp` | `ActionCard` (importar/nova viagem) |
| `0dp` | Cards de voucher, contato, dia — sem elevação no estado estático |
| `10dp` | Cards durante drag (animado via `animateDpAsState`) |
| `20dp` via `Modifier.shadow` | Bottom nav pill — sombra colorida `GreenMoss 25–35% alpha` |

**Sombra colorida no bottom nav:**
```kotlin
.shadow(
    elevation    = 20.dp,
    shape        = RoundedCornerShape(32.dp),
    ambientColor = GreenMoss.copy(alpha = 0.25f),
    spotColor    = GreenMoss.copy(alpha = 0.35f)
)
```
A sombra usa a cor primária em vez de preto — reforça a identidade visual e evita a aparência genérica de sombra cinza.

---

## Componentes recorrentes

### Card padrão

```
Card (SurfaceWhite, borda CardBorder 1dp, raio 16dp, elevação 2dp)
```

Borda `CardBorder` (GreenMoss 31% alpha) presente em todos os cards — substitui a sombra como delimitador de superfície. Bordas são preferidas a sombras porque preservam o visual limpo no fundo `GreenLight`.

**Card de viagem ativa:** borda `GreenMoss` 2dp sólido em vez de `CardBorder` — sinaliza o estado especial sem precisar de cor de fundo diferente.

### Cabeçalho colorido de card (`ContactCard`, `BoardingPassCard`)

```
Box/Row (GreenMoss, padding 12–16dp, conteúdo branco)
 └─ texto branco + ícones brancos
```

Fundo `GreenMoss` sólido no topo do card cria hierarquia visual clara entre cabeçalho e corpo. Em contatos de emergência, o cabeçalho usa `#B71C1C` (vermelho escuro) — mantendo a gramática estrutural mas sinalizando categoria crítica.

### Acento lateral de voucher

```
Box (4dp de largura, altura total do card, cor da paleta do tipo)
```

Faixa colorida de 4dp na borda esquerda do card identifica o tipo de voucher (PDF/Imagem/Link/Arquivo) à primeira vista, sem precisar de badge ou ícone adicional. A cor varia por tipo via `VoucherPalette`.

### Container de emoji

```
Surface (GreenMoss, RoundedCornerShape(12dp), padding 10dp)
 └─ Text (emoji, 28sp)
```

Padrão consistente em `TripCard`, `ContactCard`, `ActionCard` e `VouchersScreen`. O fundo `GreenMoss` cria contraste garantido para qualquer emoji, independente da cor do app.

### `BadgeChip`

Pill com fundo = cor-texto 10–15% alpha + borda = cor-texto 40% alpha + texto em uppercase 10sp letterSpacing 1sp. Padrão idêntico para badges padrão e customizados — a única diferença é a cor base.

```kotlin
Surface(
    shape  = RoundedCornerShape(100.dp),
    color  = bgColor,
    border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.4f))
) {
    Text(badge.label.uppercase(), fontSize = 10.sp, letterSpacing = 1.sp)
}
```

### `SectionLabel` / cabeçalho de grupo

Padrão recorrente em formulários e listas agrupadas:
```
Text (10sp, GreenMoss, letterSpacing 2sp, uppercase)
```
Delimitador leve entre seções sem usar `HorizontalDivider`. Aparece em `CreateTripScreen`, `VouchersScreen`, `ContactsScreen`, `EditActivityScreen`.

### `InfoBlock` (boarding passes)

```
Column (CenterHorizontally)
 ├─ Text (label, 9sp, TextSecondary, letterSpacing 1.5sp)
 └─ Text (valor, titleMedium, SemiBold)
```

Par label/valor com hierarquia clara — label diminuto uppercase abaixo do valor proeminente. Padrão repetido em DATA, EMBARQUE, VOO e PORTÃO.

---

## Botões — hierarquia e cores

O app não usa os estilos padrão do Material 3 para botões primários — define cores explicitamente em cada contexto.

### Ação principal (salvar, criar, gerar)

```kotlin
Button(colors = ButtonDefaults.buttonColors(containerColor = GreenMoss)) {
    Icon(tint = AmberPrimary)
    Text(color = Color.White ou AmberPrimary)
}
```
`GreenMoss` + conteúdo `AmberPrimary` — combinação usada em "Salvar", "Adicionar", "Gerar roteiro", "Compartilhar viagem". O âmbar sobre verde escuro é o par de maior contraste da paleta.

### Avanço de etapa (wizard)

```kotlin
Button(colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)) {
    Text("Próximo →", color = Color.White)
}
```
`AmberPrimary` sólido — exclusivo para botões de progressão no wizard. Diferencia visualmente "avançar no fluxo" de "confirmar uma ação".

### Ação destrutiva

```kotlin
Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))) {
    Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
}
```
Vermelho sólido. Sempre dentro de `AlertDialog` de confirmação — nunca exposto diretamente na tela principal.

### Ação secundária

```kotlin
TextButton(onClick = ...) { Text("Cancelar") }
OutlinedButton(border = BorderStroke(1.dp, AmberPrimary)) { Text(...) }
```
`TextButton` para cancelar/voltar. `OutlinedButton` com borda `AmberPrimary` para ações opcionais de destaque médio (ex: "Adicionar link").

### Switch

```
thumb: GreenMoss (sempre)
track ativo: AmberPrimary
track inativo: #9E9E9E
```
`Modifier.scale(0.80f)` no wrapper — reduz o Switch sem afetar o layout do `Row`. Padrão consistente em `SettingsScreen`.

---

## Bottom nav pill

Não usa `NavigationBar` do Material 3. Implementação customizada:

```
Row (GreenMoss, RoundedCornerShape(32dp), shadow colorida 20dp)
 └─ PillNavItem × 4
      ├─ [se selecionado] Box (46×32dp, AmberPrimary 20% alpha, raio 16dp) — pill de fundo
      └─ Icon (AmberPrimary se ativo, Color.White 50% se inativo, 22dp)
```

O pill interno usa `AmberPrimary` a 20% de alpha — visível o suficiente para indicar seleção sem competir com o ícone. A barra inteira fica flutuante com `padding` lateral de 16dp, separada da tela.

---

## TopAppBar

`enterAlwaysScrollBehavior` em todas as telas com lista longa — colapsa ao rolar para baixo, reaparece ao rolar para cima. Cores:

```
containerColor = GreenMoss
scrolledContainerColor = GreenMoss   // mantém GreenMoss mesmo ao colapsar
titleContentColor = Color.White
actionIconContentColor = Color.White
```

Título em SemiBold, branco. Ícone de back e ações em branco. Sem `subtitle`.

**Adaptação por aba em `MainPagerScreen`:** o título muda conforme a aba ativa (`"Gramado  •  Vouchers"`, etc.). O botão de back só aparece na aba 0.

---

## Snackbar

```kotlin
Snackbar(
    containerColor = AmberPrimary,
    contentColor   = Color.White
)
```

Fundo `AmberPrimary` em todas as snackbars do app. Sem botão de ação. Duração padrão (`SHORT`). Exibida após operações de escrita bem-sucedidas ("Alterações salvas ✓").

---

## FAB

```kotlin
FloatingActionButton(
    containerColor = AmberPrimary,
    contentColor   = Color.White,
    shape          = RoundedCornerShape(16dp)
) { Icon(Add) }
```

Forma quadrada arredondada (`RoundedCornerShape(16dp)`) em vez do pill padrão do Material 3. Aparece apenas nas abas Vouchers, Embarque e Contatos — ausente na aba Home.

---

## Estado vazio (empty state)

Padrão consistente em todas as telas com lista opcional:

```
Column (CenterHorizontally, padding top 60dp)
 ├─ Text (emoji, 36–56sp)
 ├─ Text (título, titleMedium ou bodyMedium, Bold, TextPrimary)
 └─ Text (CTA, bodySmall ou labelSmall, TextSecondary)
```

Emoji grande como âncora visual, título descritivo, CTA suave. Sem ilustrações vetoriais ou imagens — emoji é suficiente e evita assets adicionais.

---

## Interatividade e feedback

### Swipe to reveal (TripsListScreen)

Implementação customizada com `Animatable<Float>` + `Modifier.draggable`. Snap automático ao meio (50%) ou por velocidade (`> 600f`). Botões revelados com bordas arredondadas apenas nas extremidades — canto esquerdo do primeiro botão, canto direito do último.

### Swipe to dismiss (ContactsScreen)

`SwipeToDismissBox` do Material 3 com `confirmValueChange` sempre retornando `false` — nunca remove automaticamente. Abre dialog de confirmação. Fundo vermelho com gradação via `animateColorAsState(targetValue)`.

### Drag-to-reorder (VouchersScreen, ContactsScreen)

`sh.calvin.reorderable` — `ReorderableItem` + `longPressDraggableHandle`. Sombra animada durante o drag via `animateDpAsState(if (isDragging) 10.dp else 0.dp)`. Handle de drag sempre visível (ícone `DragHandle` sobre o cabeçalho do card).

### `animateContentSize`

`Modifier.animateContentSize(tween(260, FastOutSlowInEasing))` nas atividades colapsáveis de `DayDetailScreen` — transição suave de altura ao expandir/colapsar.

### Loading states

`CircularProgressIndicator(color = GreenSage)` para carregamento de dados. `Box(Modifier.scale(0.80f)) { CircularProgressIndicator }` inline para botões de salvar no estado `isSaving`. Sem skeleton screens.

---

## Dialogs

Dois tipos:

**`AlertDialog` (confirmação):** `onDismissRequest` fecha. Botão de cancelar como `TextButton`, botão de confirmar como `Button` ou `TextButton` colorido. Ícone opcional (lixeira vermelha para exclusão).

**`Dialog` não cancelável (loading):** `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)`. Card branco centralizado com `CircularProgressIndicator` + texto. Usado em exportação e importação de `.travel`.

---

## Tokens de espaçamento recorrentes

| Valor | Uso |
|---|---|
| `4.dp` | Padding mínimo, espaçamento entre labels |
| `8.dp` | Espaçamento entre chips, padding interno de badge |
| `12.dp` | Gap entre cards na lista, espaçamento entre elementos de formulário |
| `14.dp` | Padding interno de card (horizontal + vertical) |
| `16.dp` | Padding horizontal de lista (`LazyColumn contentPadding`), padding de tela |
| `20.dp` | Padding horizontal de formulário |

---

## Princípios implícitos do design

1. **Verde é estrutura, âmbar é ação.** `GreenMoss` delimita e organiza; `AmberPrimary` chama para agir. O usuário aprende rapidamente que âmbar = "toque aqui para avançar".

2. **Borda em vez de sombra.** `CardBorder` (GreenMoss 31% alpha) delimita cards no fundo `GreenLight` sem sombra. Sombra só aparece quando há elevação real a comunicar (drag, bottom nav).

3. **Emoji como ícone de identidade.** Viagens têm `coverEmoji`; atividades têm emoji próprio; grupos têm emoji de categoria. Elimina a necessidade de um sistema de ícones customizado.

4. **Uppercase + letter-spacing para labels.** Labels de seção e cabeçalhos de grupo usam 10sp + 2sp de letter-spacing + uppercase. Cria hierarquia tipográfica sem mudar a fonte ou o peso.

5. **Estado desabilitado por alpha, não por cor alternativa.** Botões desabilitados usam `AmberPrimary.copy(alpha = 0.35f)` ou `GreenMoss.copy(alpha = 0.4f)` — mantêm a forma e a cor, apenas reduzem a opacidade.

6. **Feedback imediato via estado local, persistência via callback.** Drag, toggle "Usado" e favoritar atualizam a UI instantaneamente via `localList` ou `_tripData.copy(...)` antes da operação no banco confirmar.

---

## Checklist para novos componentes

- Usar `SurfaceWhite` como fundo de card, `GreenLight` como fundo de tela
- Bordas de card: `BorderStroke(1.dp, CardBorder)` — nunca omitir em fundo `GreenLight`
- Raio de card: `RoundedCornerShape(16.dp)`
- Raio de pill: `RoundedCornerShape(100.dp)`
- Container de emoji: `Surface(GreenMoss, RoundedCornerShape(12.dp), padding 10dp)`
- Botão de ação principal: `containerColor = GreenMoss`, conteúdo `AmberPrimary`
- Snackbar: `containerColor = AmberPrimary, contentColor = Color.White`
- Label de seção: `Text(10sp, GreenMoss, letterSpacing 2sp, uppercase)`
- Loading: `CircularProgressIndicator(color = GreenSage)`
- Empty state: emoji grande + título `TextPrimary` + CTA `TextSecondary`
- Dialogs de loading: `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)`
