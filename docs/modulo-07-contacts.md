# Módulo 07 — Contatos

**Tela:** `ContactsScreen`  
**Arquivo:** `ui/contacts/ContactsScreen.kt`  
**ViewModel:** nenhum — tela **stateless**  
**Entry point de navegação:** aba "Contatos" dentro de `MainPagerScreen` (rota `trip_main/{tripId}`)

---

## Visão geral

Exibe os contatos de uma viagem agrupados por categoria, com suporte a favoritar (move o contato para o grupo "Favoritos"), drag-to-reorder por long press, swipe esquerdo para deletar com confirmação, e botões de ligar e WhatsApp. Contatos fixos de emergência (SAMU, Bombeiros, PM) são injetados visualmente no grupo Emergências quando a configuração está ativa — sem drag, swipe ou estrela.

---

## Padrão de arquitetura

`ContactsScreen` é um **composable stateless** — sem ViewModel próprio. Segue o mesmo padrão das demais telas de aba.

| Responsabilidade | Onde vive |
|---|---|
| Lista de contatos | `TripViewModel.tripData.contacts` |
| Persistir reordenação | callback `onReorderContacts` → `TripViewModel` → `TripRepository.reorderContacts()` |
| Persistir favorito | callback `onToggleFavoriteContact` → `TripViewModel` → `TripRepository.toggleFavoriteContact()` |
| Excluir contato | callback `onDeleteContact` → `TripViewModel` → `TripRepository.deleteContact()` |
| Configuração emergências | `showEmergencyContacts: Boolean` — vem de `SettingsRepository` via `AppNavigation` |
| Estado de drag local | `localContacts: MutableState<List<Contact>>` — cópia local para animações |

> **Regra de padrão:** `ContactsScreen` não acessa nem cria ViewModel. Toda persistência é delegada via callbacks. `localContacts` existe exclusivamente para feedback visual imediato do drag — não é fonte de verdade.

---

## Fluxo de dados

```
TripViewModel.tripData.contacts: List<Contact>
  └─ ContactsScreen(
       contacts             = contacts,
       showEmergencyContacts = settings.showEmergencyContacts,
       onReorderContacts    = vm::reorderContacts (via TripRepository),
       onDeleteContact      = vm::deleteContact,
       onToggleFavorite     = vm::toggleFavoriteContact,
       onEditContact        = { id -> navController.navigate(...) }
     )
          └─ localContacts: MutableState (cópia para drag)
               └─ listItems: List<ContactListItem> (computed via remember)
                    └─ LazyColumn com ReorderableItem + SwipeToDismissBox
```

---

## Grupos de contatos (`listItems`)

A lista é construída como `List<ContactListItem>` — sealed class com `Header` e `Item` — via `remember(localContacts, showEmergencyContacts)`:

### Grupos fixos (ordem fixa, hardcoded)

| Chave | Label | Emoji | Filtro |
|---|---|---|---|
| `favorites` | Favoritos | ⭐ | `c.isFavorite` |
| `agency` | Agência & Transfers | 📋 | `c.type == AGENCY && !c.isFavorite` |
| `hotel` | Hospedagem | 🏨 | `c.type == HOTEL && !c.isFavorite` |
| `family` | Família | 👨‍👩‍👧 | `c.type == FAMILY && !c.isFavorite` |
| `attraction` | Atrações | 🎡 | `c.type == ATTRACTION && !c.isFavorite` |
| `emergency` | Emergências | 🚨 | `c.type == EMERGENCY && !c.isFavorite` |

Grupos sem itens (nem builtins, nem usuário) não são adicionados à lista — sem header vazio.

### Contatos fixos de emergência (builtins)

Injetados no início do grupo `emergency` quando `showEmergencyContacts == true`:

```kotlin
private val BUILTIN_EMERGENCY_CONTACTS = listOf(
    Contact(id = -1L, name = "SAMU",           phone = "192", ...),
    Contact(id = -2L, name = "Bombeiros",       phone = "193", ...),
    Contact(id = -3L, name = "Polícia Militar", phone = "190", ...),
)
```

IDs negativos (`-1`, `-2`, `-3`) os identificam como virtuais — nunca vêm do banco. A tela usa `contact.id < 0` para distingui-los e desabilitar swipe, drag e estrela.

### Grupos customizados (dinâmicos)

Contatos com `type == ContactType.CUSTOM` e `!isFavorite` são agrupados por `customTypeName`. Os grupos são ordenados alfabeticamente por nome. Header usa emoji 🏷️.

```kotlin
val customContacts = localContacts.filter { it.type == ContactType.CUSTOM && !it.isFavorite }
val customGroups = customContacts
    .groupBy { it.customTypeName.ifBlank { "Outros" } }
    .entries.sortedBy { it.key }
```

**Keys da lista:**
- Header: `"header_${label}"`
- Item: `"contact_${contact.id}"`

---

## Drag-to-reorder

**Biblioteca:** `sh.calvin.reorderable` — mesmo padrão de `VouchersScreen`.

**`onMove`:** rejeita se o índice é um Header (`as? ContactListItem.Item` retorna null) ou se os grupos são diferentes (`fromItem.groupKey != toItem.groupKey`). Troca posições em `localContacts` (lista em memória).

**Persistência:** `LaunchedEffect(localContacts)` — dispara `onReorderContacts(localContacts)` sempre que a lista muda e difere de `contacts`.

```kotlin
LaunchedEffect(localContacts) {
    if (localContacts != contacts) onReorderContacts(localContacts)
}
```

**Sombra durante drag:** `Modifier.shadow(elevation, shape = RoundedCornerShape(12.dp), clip = false)` aplicado no `Box` externo que envolve o `SwipeToDismissBox`:

```kotlin
ReorderableItem(state = reorderState, key = "contact_${contact.id}") { isDragging ->
    val elevation by animateDpAsState(if (isDragging) 10.dp else 0.dp, label = "dragElevation")
    Box(Modifier.shadow(elevation, shape = RoundedCornerShape(12.dp), clip = false)) {
        SwipeToDismissBox(...) {
            ContactCard(isDragging = isDragging, ...)
        }
    }
}
```

> **Por que `Modifier.shadow()` e não `CardDefaults.cardElevation()`:** o `SwipeToDismissBox` tem clipping interno que corta a sombra do Card. Aplicar a sombra no `Box` externo, fora do clipping, resolve o problema. Ver também `docs/modulo-05-vouchers.md` para a comparação com `VouchersScreen`.

**Ausência de `.sortedBy { it.sortOrder }` em `listItems`:** a lista `localContacts` já chega ordenada do banco. Adicionar `.sortedBy` causaria snap-back visual após cada `onMove` (a recomposição usaria `sortOrder` original, antes do DB ser atualizado). Ver detalhe do bug no histórico de sessão.

---

## Swipe para deletar (`SwipeToDismissBox`)

Usa o componente Material 3 `SwipeToDismissBox` (não implementação customizada). Deslizar da direita para a esquerda revela fundo vermelho e ícone `Delete`.

**Configuração:**
```kotlin
val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { value ->
        if (value == SwipeToDismissBoxValue.EndToStart && contactToDelete == null) {
            contactToDelete = contact   // abre o dialog
        }
        false   // nunca confirma o dismiss — o item não sai da lista
    }
)
```

`confirmValueChange` retorna sempre `false` — o `SwipeToDismissBox` **nunca** remove o item automaticamente. Ele apenas abre o `AlertDialog`. A remoção real acontece só após confirmação no dialog.

**Reset do swipe após fechar o dialog:**
```kotlin
LaunchedEffect(contactToDelete) {
    if (contactToDelete == null) dismissState.reset()
}
```
Quando `contactToDelete` volta a `null` (cancelamento ou confirmação), o swipe anima de volta para a posição inicial.

**Background animado:**
```kotlin
val color by animateColorAsState(
    if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
        Color(0xFFE53935) else Color.Transparent,
    label = "swipe_bg"
)
```
Aparece gradualmente conforme o usuário desliza, usando `targetValue` (intenção) e não `currentValue` (posição atual).

**Contatos builtins:** não são envolvidos em `SwipeToDismissBox`. São renderizados diretamente via `ContactCard(showActions = false)`.

---

## Favoritar contato

Ao tocar na estrela no header do card:

```kotlin
onToggleFavorite = {
    val newFav = !contact.isFavorite
    localContacts = localContacts.map {
        if (it.id == contact.id) it.copy(isFavorite = newFav) else it
    }
    onToggleFavoriteContact(contact.id, newFav)
}
```

1. **Atualização local imediata:** `localContacts` é atualizado para feedback visual instantâneo — o contato se move para/de "Favoritos" sem esperar o banco
2. **Persistência assíncrona:** `onToggleFavoriteContact` → `TripRepository.toggleFavoriteContact()` → `UPDATE contacts SET isFavorite = ? WHERE id = ?` (direct UPDATE, sem SELECT)

O grupo "Favoritos" aparece/desaparece automaticamente conforme o estado de `localContacts` — sem lógica especial de visibilidade.

---

## Layout do `ContactCard`

```
Card (SurfaceWhite, border GreenMoss ou vermelho para emergência)
 ├─ Row (header, GreenMoss ou #B71C1C)
 │    ├─ dragHandle()        ← {} para builtins
 │    ├─ Text (nome, branco, 17sp SemiBold)
 │    └─ IconButton (estrela) ← ausente se showActions = false
 └─ Column (corpo, padding 14dp horizontal, 10dp vertical)
      ├─ Text (role, bodySmall, TextSecondary)
      ├─ Text (telefone formatado, GreenMoss ou #8A1515 para emergência)
      └─ Row (botões)
           ├─ [Button "🚨 Ligar" vermelho] ← emergência
           │   ou
           ├─ [OutlinedButton "📞 Ligar" GreenMoss] ← normal
           ├─ [OutlinedButton "💬 WhatsApp" verde] ← se hasWhatsApp
           ├─ Spacer(weight(1f))
           └─ [IconButton Edit] ← ausente se showActions = false
```

**`showActions`:** parâmetro Boolean que oculta estrela e botão de editar. Usado exclusivamente para contatos builtins (`id < 0`).

**Header colorido:**
- Contatos normais: `GreenMoss`
- Contatos de emergência (builtins e `isEmergency = true`): `#B71C1C` (vermelho escuro)

**Border:**
- Durante drag: `1.5dp, GreenMoss 40%`
- Emergência: `1dp, #E88888` (vermelho claro)
- Normal: `1dp, GreenMoss`

**Handle de drag:** `Modifier.longPressDraggableHandle()` no `IconButton` de drag. Ícone `DragHandle` em `Color.White` 80% alpha — visível sobre o header verde.

---

## Formatação de telefone (`formatPhone`)

```kotlin
private fun formatPhone(raw: String): String = when (raw.length) {
    3    -> raw                                             // "192", "193", "190"
    10   -> "(${raw.take(2)}) ${raw.drop(2).take(4)}-${raw.drop(6)}"   // fixo
    11   -> "(${raw.take(2)}) ${raw.drop(2).take(5)}-${raw.drop(7)}"   // celular
    else -> raw                                            // formato desconhecido: exibe bruto
}
```

Apenas dígitos são esperados em `raw`. Números de emergência de 3 dígitos são exibidos sem formatação.

---

## Deep links de comunicação

```kotlin
// Ligar
dialPhone(context, phone)    // → Intent(ACTION_DIAL, "tel:<phone>")

// WhatsApp
openWhatsApp(context, phone) // → Intent(ACTION_VIEW, "https://wa.me/55<phone>")
```

Ambas são funções utilitárias em `utils/` — não inlined na tela. `onWhatsAppClick` é `null` se `contact.hasWhatsApp == false` ou se `contact.phone == null`, tornando o botão ausente.

---

## Estado vazio

Se `contacts.isEmpty()`, a tela retorna antecipadamente:
- Emoji 📞 (48sp)
- "Nenhum contato ainda" (`bodyMedium`, Bold, `TextPrimary`)
- "Toque em + para adicionar" (14sp, `TextSecondary`)

O retorno antecipado evita instanciar `lazyListState` e `reorderState` desnecessariamente.

---

## Composables e funções privadas (resumo)

| Símbolo | Tipo | Responsabilidade |
|---|---|---|
| `ContactListItem` | `sealed class` | Item da lista achatada (Header ou Item) |
| `BUILTIN_EMERGENCY_CONTACTS` | `val` | Contatos virtuais de emergência (IDs negativos) |
| `ContactsScreen` | composable | Tela principal — orquestra lista, drag, swipe e dialogs |
| `ContactGroupHeader` | composable privado | Cabeçalho de grupo com emoji e label uppercase |
| `ContactCard` | composable privado | Card com header colorido, telefone e botões de ação |
| `formatPhone(raw)` | função pura | Formata string de dígitos em formato visual |

---

## Checklist para futuras modificações

- **Novo tipo de contato fixo:** adicionar valor em `ContactType` → adicionar entrada em `fixedGroups` (chave, label, emoji, filtro) na construção de `listItems` → atualizar `EditContactScreen` para expor o tipo.
- **Novo grupo fixo:** adicionar entrada no `fixedGroups` na ordem desejada. A ordem da lista é determinada pela ordem do `fixedGroups` — não há lógica de ordenação separada.
- **Novo contato builtin de emergência:** adicionar em `BUILTIN_EMERGENCY_CONTACTS` com ID negativo único. IDs negativos garantem que o contato nunca colida com IDs do banco (que são auto-incrementados positivos).
- **Novo botão de ação no card:** adicionar `OutlinedButton` ou `IconButton` na `Row` de botões do `ContactCard` → adicionar o callback como parâmetro → wiring em `ContactsScreen`.
- **Mover persistência de reordenação para fora do `LaunchedEffect`:** o `LaunchedEffect(localContacts)` dispara a cada mudança de `localContacts`, incluindo mudanças por favoritar. Para separar os dois tipos de mudança, use um flag separado (como `skipNextReorder` em `VouchersScreen`) ou converta para callbacks específicos de drag vs. favoritar.
- **Alterar critério de snap-back do drag:** nunca adicionar `.sortedBy { it.sortOrder }` ao computar `listItems` a partir de `localContacts`. Essa ordenação causa snap-back visual após cada `onMove` porque o `sortOrder` no banco só é atualizado assincronamente. A ordem correta vem da posição em `localContacts`.
