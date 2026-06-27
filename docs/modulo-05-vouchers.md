# Módulo 05 — Vouchers

**Tela:** `VouchersScreen`  
**Arquivo:** `ui/vouchers/VouchersScreen.kt`  
**ViewModel:** nenhum — tela **stateless**  
**Entry point de navegação:** aba "Vouchers" dentro de `MainPagerScreen` (rota `trip_main/{tripId}`)

---

## Visão geral

Exibe todos os vouchers de uma viagem agrupados em seções. Suporta drag-to-reorder por long press dentro do grupo, toggle "Usado", compartilhamento por arquivo ou texto, e exclusão com confirmação. O agrupamento é configurável por categoria, pessoa ou dia, com a preferência persistida por viagem.

---

## Padrão de arquitetura

`VouchersScreen` é um **composable stateless** — sem ViewModel próprio. Todo dado chega via parâmetro `vouchers: List<Voucher>`; toda ação sai via callbacks.

| Responsabilidade | Onde vive |
|---|---|
| Lista de vouchers | `TripViewModel.tripData.vouchers` |
| Modo de agrupamento atual | `TripViewModel` → `tripData.voucherSortMode` |
| Persistir reordenação | callback `onReorderVouchers` → `TripViewModel.reorderVouchers()` → `TripRepository` |
| Persistir toggle "Usado" | callback `onToggleUsed` → `TripViewModel.toggleVoucherUsed()` → `TripRepository` |
| Excluir voucher | callback `onDeleteVoucher` → `TripViewModel.deleteVoucher()` (reindexação inclusa) |
| Alternar modo de agrupamento | FAB/menu em `MainPagerScreen` → `TripViewModel.setVoucherSortMode()` |
| Estado de drag local | `localVouchers: MutableState<List<Voucher>>` — cópia local para animações |

> **Regra de padrão:** `VouchersScreen` mantém `localVouchers` apenas para feedback visual imediato durante o drag. A persistência real ocorre via `onReorderVouchers` → `TripViewModel.reorderVouchers()`. Nunca acesse o banco diretamente da tela.

---

## Fluxo de dados

```
TripViewModel.tripData.vouchers: List<Voucher>
  └─ VouchersScreen(
       vouchers   = vouchers,
       sortMode   = tripData.voucherSortMode,
       onReorder  = vm::reorderVouchers,
       onDelete   = vm::deleteVoucher,
       onToggle   = vm::toggleVoucherUsed,
       onEdit     = { id -> navController.navigate(...) }
     )
          └─ localVouchers: MutableState (cópia para drag)
               └─ flatList: List<VoucherListItem> (computed via remember)
                    └─ LazyColumn com ReorderableItem por voucher
```

**`localVouchers`** é reinicializado via `remember(vouchers)` sempre que `vouchers` muda externamente (ex: após exclusão, reload do banco). Isso mantém a lista local em sincronia sem perder o estado de drag intermediário.

---

## Tipos e paletas (`VoucherType` + `VoucherPalette`)

Cada voucher é classificado pelo campo `assetPath`:

| Tipo | Condição de `assetPath` | Acento lateral | Badge |
|---|---|---|---|
| `LINK` | começa com `"http"` | `AmberPrimary` | âmbar |
| `PDF` | termina com `.pdf` | `GreenMoss` | verde |
| `IMAGE` | termina com `.jpeg`, `.jpg`, `.png`, `.webp` | `GreenSage` | verde claro |
| `FILE` | qualquer outro | `TextSecondary` | cinza |

A paleta é um `VoucherPalette` com 6 tokens de cor (`accent`, `emojiBackground`, `badgeBackground`, `badgeText`, `buttonColor`, `label`). Todas as cores do card são derivadas desta paleta — nenhuma cor é hardcoded no `VoucherCard`.

> **Regra de padrão:** Para suportar um novo tipo de arquivo, adicione o valor em `VoucherType`, a condição em `voucherType()` e a paleta em `voucherPalette()`. Nenhuma mudança é necessária em `VoucherCard`.

---

## Estrutura da lista plana (`flatList`)

A `LazyColumn` usa uma lista "achatada" (`List<VoucherListItem>`) que intercala headers e itens:

```kotlin
sealed class VoucherListItem {
    data class Header(val groupName: String, val count: Int) : VoucherListItem()
    data class Item(val voucher: Voucher, val groupName: String) : VoucherListItem()
}
```

Esta estrutura permite que headers e itens coexistam numa única `LazyColumn` com keys estáveis, requisito da biblioteca `sh.calvin.reorderable`.

**Keys dos itens:**
- Header: `"header_${groupName}"`
- Item: `"voucher_${voucher.id}"`

---

## Modos de agrupamento (`VoucherSortMode`)

```kotlin
enum class VoucherSortMode { BY_CATEGORY, BY_PERSON, BY_DAY }
```

| Modo | Chave de grupo | Ordenação dos grupos | Fallback |
|---|---|---|---|
| `BY_CATEGORY` | `voucher.groupName` | Ordem de inserção (mantém `sortOrder`) | `"Sem categoria"` |
| `BY_PERSON` | `voucher.person` | Alfabética; "Sem pessoa" vai ao final (`"￿"`) | `"Sem pessoa"` |
| `BY_DAY` | `"Dia ${voucher.dayId}"` | Numérica por `dayId`; sem dia vai ao final | `"Sem dia"` |

O modo persistido por viagem é salvo no campo `TripEntity.voucherSortMode` (String) via `TripViewModel.setVoucherSortMode()` → `TripRepository.saveVoucherSortMode()`.

**Nota sobre drag:** a reordenação só é significativa em `BY_CATEGORY` (onde `sortOrder` é respeitado). Em `BY_PERSON` e `BY_DAY`, o drag move itens visualmente, mas a ordenação é recalculada pelo agrupamento a cada recomposição — evite habilitar drag nesses modos ou deixe claro ao usuário que a ordenação não persiste.

---

## Drag-to-reorder (`rememberReorderableLazyListState`)

**Biblioteca:** `sh.calvin.reorderable` — `ReorderableItem` + `rememberReorderableLazyListState`.

**`onMove` callback:**
```kotlin
onMove = { from, to ->
    // Rejeita se algum dos índices não é um Item (ex: header)
    val fromItem = flatList.getOrNull(from.index) as? VoucherListItem.Item ?: return@...
    val toItem   = flatList.getOrNull(to.index)   as? VoucherListItem.Item ?: return@...
    // Rejeita se tentar mover entre grupos diferentes
    if (fromItem.groupName != toItem.groupName) return@...
    // Troca de posição na lista local
    val mutable = localVouchers.toMutableList()
    val fromIdx = mutable.indexOfFirst { it.id == fromItem.voucher.id }
    val toIdx   = mutable.indexOfFirst { it.id == toItem.voucher.id }
    if (fromIdx >= 0 && toIdx >= 0) {
        val moved = mutable.removeAt(fromIdx)
        mutable.add(toIdx, moved)
        localVouchers = mutable
    }
}
```

O drag é restrito ao mesmo grupo (`fromItem.groupName != toItem.groupName` → rejeita). Headers são transparentes ao drag — `getOrNull(index) as? Item` retorna `null` para headers, abortando o movimento.

**Persistência pós-drag:**
```kotlin
LaunchedEffect(localVouchers) {
    if (skipNextReorder) { skipNextReorder = false; return@LaunchedEffect }
    if (localVouchers != vouchers) onReorderVouchers(localVouchers)
}
```
`LaunchedEffect(localVouchers)` dispara sempre que a lista local muda. `skipNextReorder` é um flag para suprimir a persistência na exclusão (quando `localVouchers` muda por remoção, não por drag).

**Sombra durante drag:**
```kotlin
val elevation by animateDpAsState(
    targetValue = if (isDragging) 10.dp else 0.dp,
    label       = "dragElevation"
)
VoucherCard(elevation = elevation, ...)
```
`animateDpAsState` anima suavemente a elevação de 0dp para 10dp quando `isDragging == true`. A elevação é passada para `CardDefaults.cardElevation(defaultElevation = elevation)` dentro do `VoucherCard`.

> **Diferença do ContactsScreen:** no `VouchersScreen`, o `VoucherCard` renderiza a sombra via `CardDefaults.cardElevation` porque não há `SwipeToDismissBox` a envolvê-lo (que causaria clipping). No `ContactsScreen`, a sombra é aplicada externamente via `Modifier.shadow()` por esse motivo.

**Handle de drag:** `IconButton` com `Modifier.longPressDraggableHandle()` (extensão da biblioteca `sh.calvin.reorderable`). O drag só começa com long press no handle — o toque normal no card dispara `onOpen`.

---

## Layout do `VoucherCard` (Conceito A)

```
Card (fillMaxWidth)
 ├─ Box 4dp width ← acento lateral colorido por tipo
 └─ Column
     ├─ Row (corpo superior, padding 14dp)
     │    ├─ Box 44dp (emoji em container arredondado, cor emojiBackground)
     │    ├─ Column (nome + pessoa)
     │    └─ dragHandle()
     ├─ HorizontalDivider (0.5dp, CardBorder 20% alpha)
     └─ Row (footer, padding horizontal 14dp, vertical 8dp)
          ├─ Surface (badge de tipo)
          ├─ Spacer(weight(1f))
          ├─ IconButton (toggle "Usado")
          ├─ IconButton (Editar)
          ├─ IconButton (Compartilhar)
          └─ IconButton (Deletar)
```

**Estado "Usado":** `usedAlpha = if (voucher.isUsed) 0.45f else 1f`. Aplicado em:
- `containerColor` do Card: `SurfaceWhite.copy(alpha = 0.6f)` se usado
- Borda: `0.5dp, CardBorder 15% alpha` se usado; `CardBorder 30% alpha` caso contrário
- Acento lateral: `palette.accent.copy(alpha = usedAlpha)`
- Texto nome: `TextPrimary.copy(alpha = usedAlpha)`
- Texto pessoa: `TextSecondary.copy(alpha = 0.7f * usedAlpha)`

**Ícone toggle "Usado":**
- Não usado: `RadioButtonUnchecked`, `TextSecondary` 40% alpha
- Usado: `CheckCircle`, `GreenMoss`

**Border durante drag:** `BorderStroke(1.5.dp, palette.accent.copy(alpha = 0.4f))` — usa a cor de acento do tipo, 40% alpha. Fora do drag: `CardBorder`.

---

## Ação "Abrir" (`openAction`)

A lógica de abertura é determinada pelo `assetPath`:

```kotlin
val openAction: () -> Unit = {
    when {
        voucher.assetPath.startsWith("http") ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(voucher.assetPath)))
        voucher.assetPath.startsWith(context.filesDir.absolutePath) ->
            openInternalFile(context, voucher.assetPath)
        voucher.assetPath.isNotBlank() ->
            openAssetFile(context, voucher.assetPath)
    }
}
```

| Caso | Função | Mecanismo |
|---|---|---|
| URL (`http...`) | direto | `Intent.ACTION_VIEW` com URI |
| Arquivo interno (`filesDir/...`) | `openInternalFile()` | `FileProvider.getUriForFile()` + `Intent.ACTION_VIEW` |
| Asset bundled | `openAssetFile()` | copia asset para `cacheDir` → `FileProvider` → `Intent.ACTION_VIEW` |

---

## Ação "Compartilhar" (`shareVoucher`)

Função top-level privada. Monta o texto de compartilhamento com nome, pessoa, categoria e URL (se link). Trata três casos de arquivo:

| Caso | Compartilhamento |
|---|---|
| Arquivo em `filesDir` | `ACTION_SEND` com URI via `FileProvider` + texto |
| Asset bundled | Copia para `cacheDir` → `FileProvider` → `ACTION_SEND` |
| Link ou sem arquivo | `ACTION_SEND` com `EXTRA_TEXT` apenas |

Em caso de erro ao copiar o asset, faz fallback para compartilhamento de texto.

---

## Exclusão com confirmação

```kotlin
var confirmDeleteId by remember { mutableStateOf<Long?>(null) }
```

Ao tocar "Deletar" no footer do card: `confirmDeleteId = voucher.id`.

`AlertDialog` exibe "Remover voucher?" com botão "Remover" (`#D32F2F`, Bold).

Ao confirmar:
```kotlin
skipNextReorder = true          // evita disparar onReorderVouchers
localVouchers = localVouchers.filter { it.id != deleteId }
onDeleteVoucher(deleteId)
```

`skipNextReorder` previne que o `LaunchedEffect(localVouchers)` dispare `onReorderVouchers` logo após a exclusão — a mudança de `localVouchers` nesse caso é por remoção, não por drag.

**Reindexação no ViewModel:** `TripViewModel.deleteVoucher()` além de chamar `repo.deleteVoucher(id)`, reindexta os `sortOrder` dos vouchers remanescentes por grupo (`groupBy { groupName }.flatMap { items.mapIndexed { i, v -> v.copy(sortOrder = i) } }`), fechando o gap deixado pelo item removido.

---

## Cabeçalho de grupo (`GroupHeader`)

```
Row
 ├─ Text (label uppercase, 10sp, GreenMoss, letterSpacing 2sp)
 └─ Surface (pill, GreenMoss 12% alpha)
      └─ Text (count, 9sp, GreenMoss, SemiBold)
```

O contador de itens (`count`) exibido no pill é calculado no momento em que `flatList` é construída (`items.size` para cada grupo).

---

## Estado vazio

Se `vouchers.isEmpty()`, a tela retorna antecipadamente exibindo:
- Emoji 🎫 (36sp)
- "Nenhum voucher ainda" (`bodyMedium`, `TextSecondary`)
- "Toque em + para adicionar" (`labelSmall`, `TextSecondary` 60% alpha)

O `return` antecipado evita criar `lazyListState` e `reorderState` desnecessariamente.

---

## Composables e funções privadas (resumo)

| Símbolo | Tipo | Responsabilidade |
|---|---|---|
| `VoucherType` | `enum` | Classifica o voucher por tipo de conteúdo |
| `VoucherPalette` | `data class` | Tokens de cor derivados do tipo |
| `voucherType(assetPath)` | função pura | Infere o tipo a partir do caminho |
| `voucherPalette(type)` | função pura | Retorna a paleta para o tipo |
| `VoucherSortMode` | `enum` | Modo de agrupamento da lista |
| `VoucherListItem` | `sealed class` | Item da lista achatada (header ou voucher) |
| `VouchersScreen` | composable | Tela principal — orquestra lista e dialogs |
| `VoucherCard` | composable privado | Card estilo "Conceito A" com acento lateral |
| `GroupHeader` | composable privado | Cabeçalho de grupo com nome e contador |
| `shareVoucher()` | função top-level | Monta e dispara o intent de compartilhamento |

---

## Checklist para futuras modificações

- **Novo tipo de arquivo:** adicionar valor em `VoucherType` → condição em `voucherType()` → paleta em `voucherPalette()`. Nenhuma mudança em `VoucherCard`.
- **Novo modo de agrupamento:** adicionar valor em `VoucherSortMode` → adicionar branch no `when(sortMode)` dentro de `flatList` → atualizar `EditVoucherScreen` para expor a opção ao usuário → mapear o enum para String no `TripRepository.saveVoucherSortMode()`.
- **Novo botão no footer do card:** adicionar `IconButton` na `Row` do footer em `VoucherCard`. Passar o novo callback como parâmetro de `VoucherCard`.
- **Drag entre grupos:** remover a verificação `if (fromItem.groupName != toItem.groupName) return@...` no `onMove`. Cuidado: a persistência de `sortOrder` é intra-grupo — será necessário ajustar o modelo para suportar ordem global.
- **Habilitar drag no modo BY_PERSON ou BY_DAY:** atualmente o drag funciona visualmente mas não persiste de forma significativa nesses modos (o agrupamento recalcula a ordem). Para persistir, seria necessário salvar a ordem global (`sortOrder` global, não intra-grupo) ou reconsiderar o modelo.
- **Alterar comportamento de exclusão:** a lógica de reindexação de `sortOrder` está em `TripViewModel.deleteVoucher()`. Modificar lá para mudar como os sortOrders são recalculados após a remoção.
