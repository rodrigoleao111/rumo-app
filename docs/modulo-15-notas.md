# Módulo 15 — Notas (F4)

**Telas:** `NotesListContent` (aba/lista) · `DayNotesScreen` (notas de dia) · `NoteEditorScreen` (editor)
**ViewModels:** `NotesListViewModel` · `NoteEditorViewModel` · (notas gerais no `TripViewModel`)
**Dados:** `NoteRepository`, `NoteDao`, entidades `NoteEntity`/`NoteBlockEntity`/`ChecklistItemEntity`

---

## Visão geral

Notas livres por viagem, inspiradas no Notion: cada nota é um container de **blocos** de três tipos — texto, checklist e título de seção. As notas existem em dois escopos: **gerais da viagem** (aba "Notas" no pager) e **de um dia** (botão "Notas do dia" no `DayDetailScreen`). São locais (Room), ordenáveis manualmente e exportadas no `.travel` (schema v3).

---

## Modelo de dados

Três tabelas em cadeia com `ON DELETE CASCADE` (trip → note → block → item):

```
notes           (id, tripId→trips, dayId?, title, sortOrder, createdAt, updatedAt)
note_blocks     (id, noteId→notes, type, content, sortOrder)
checklist_items (id, blockId→note_blocks, text, isChecked, sortOrder)
```

- **`dayId`**: `null` = nota geral; inteiro = `dayNumber` (1-N) da nota de dia. Não há FK para `travel_days` — é um filtro lógico, igual a `vouchers.dayNumber`.
- **Migration 18** (`MIGRATION_17_18`) cria as três tabelas; o SQL espelha exatamente o `18.json` gerado pelo Room (sem `DEFAULT` nas colunas).

**Domínio** (`data/model/Note.kt`): `Note` contém `List<NoteBlock>`; `NoteBlock` é uma **sealed class** (`TextBlock`, `ChecklistBlock`, `HeadingBlock`); `ChecklistBlock` contém `List<ChecklistItem>`. Enum `NoteBlockType { TEXT, CHECKLIST, HEADING }` persiste como `.name` na coluna `type`.

---

## `NoteRepository`

`@Inject constructor(db)` — segue o padrão do app: `suspend` que retorna domain models montados **em bloco** (não `Flow`). Monta `Note` completo com **3 queries bulk** (notes → blocks → items) via `groupBy`, sem N+1.

| Método | Uso |
|---|---|
| `getNotes(tripId, dayId?)` | Lista de um escopo (`dayId IS :dayId` cobre geral e por-dia) |
| `getAllNotes(tripId)` | Todas as notas da viagem — usado no export |
| `getNote(id)` | Uma nota completa (editor) |
| `countNotes(tripId, dayId?)` | Contagem por escopo |
| `createNote` / `updateNoteTitle` / `deleteNote` / `reorderNotes` / `touchNote` | CRUD da nota |
| `addBlock` / `updateBlockContent` / `deleteBlock` / `reorderBlocks` | Blocos (checklist nasce com 1 item) |
| `addChecklistItem` / `updateItemText` / `toggleChecklistItem` / `deleteChecklistItem` / `reorderChecklistItems` | Itens |
| `insertImportedNote(tripId, note)` | Inserção completa preservando estrutura/timestamps (import) |

---

## Telas

### Aba "Notas" (pager) e `NotesListContent`

A aba geral (5ª do `MainPagerScreen`) renderiza `NotesListContent` — composable **stateless compartilhado** — com dados do `TripViewModel.generalNotes`. A `DayNotesScreen` reusa o mesmo `NotesListContent` com dados do `NotesListViewModel`.

`NotesListContent`: `LazyColumn` de `NoteCard` (título, preview dos 2 primeiros blocos, "Editada em …"), com **drag-to-reorder** (`sh.calvin.reorderable`) e **swipe-to-delete** com `AlertDialog` de confirmação — mesmo padrão de Vouchers/Contacts. Estado vazio com ilustração.

### `NoteEditorScreen` + `NoteEditorViewModel`

Editor completo. O **estado em memória é a fonte de verdade da UI** (edição de texto fluida, sem recarregar a cada tecla); a persistência é otimista/background. Só recarrega do banco em mudanças estruturais (add bloco/item), quando precisa do id gerado — e emite um sinal de foco.

- **Blocos por tipo:** `TextBlock` (multilinha), `HeadingBlock` (bold), `ChecklistBlock` (itens com checkbox, texto riscado quando marcado, "+ Adicionar item", excluir item).
- **Cada `TextField` tem estado local keyed por id** (`remember(id)`) — o cursor não pula durante a digitação.
- **Drag-to-reorder de blocos** por handle ⠿ (o campo de título fica fora da reordenação).
- **Toolbar de inserção fixada acima do teclado** (`imePadding()`): `[Texto] [Checklist] [Título] [Excluir]`. "Excluir" só habilita com um bloco focado (rastreado via `onFocusChanged`).
- **Foco automático** no bloco/item recém-criado (`FocusRequester` keyed + `focusBlockId`/`focusItemId` consumíveis no ViewModel).

---

## Navegação

| Rota | Tela |
|---|---|
| Aba 4 do `MainPagerScreen` | Notas gerais (`NotesListContent`, dados do `TripViewModel`) |
| `trip/{tripId}/day/{dayNumber}/notes` | `DayNotesScreen` (`NotesListViewModel`) |
| `trip/{tripId}/note/{noteId}` | `NoteEditorScreen` (`NoteEditorViewModel`) |

Criar nota (FAB) chama `createNote { id -> navega para o editor }`. Voltar do editor grava `refresh` no `SavedStateHandle` da entrada anterior → a lista recarrega (padrão do app).

---

## Export / import (`.travel` v3)

`TravelExporter` injeta `NoteRepository`, busca `getAllNotes(tripId)` e serializa o array `notes[]` (ver `docs/travel-export-schema.md`). `TravelImporter` injeta `NoteRepository`, parseia `notes[]` direto para domain models e chama `insertImportedNote` por nota — preservando `dayId`, blocos, itens, ordem e timestamps. `SUPPORTED_SCHEMA_VERSION` subiu para `3`.

---

## Testes

- **`NoteRepositoryTest`** (12, instrumentado) — filtro por `dayId`, CASCADE em cadeia, toggle, reorder, ordenação, checklist com item inicial.
- **`MigrationTest.migracao17Para18_...`** — cria as tabelas de notas e valida contra o `18.json`.
- **`ExportImportRoundTripTest.roundTrip_preservaNotasComBlocosEItens`** — round-trip completo (nota geral com heading+checklist + nota de dia).

> **Editor Compose:** não coberto por teste de UI instrumentado (mesma limitação da F1 — o emulador API 37 é incompatível com o toolchain Compose atual). Validado por smoke test manual no emulador.

---

## Checklist para futuras modificações

- **Formatação inline / imagens / links nos blocos:** escopo de versão futura (spec F4). Hoje os blocos são texto simples.
- **Contador de notas por dia no card do `DayDetailScreen`:** `DayNotesButton` aceita `count`, mas hoje recebe `0` (texto genérico). Ligar exigiria expor contagens por dia (ex.: `Map<Int,Int>` no `TripViewModel` via `countNotes`).
- **Reordenar itens de checklist na UI:** o repositório suporta (`reorderChecklistItems`), mas o editor só expõe reorder de blocos.
- **Novo tipo de bloco:** adicionar ao enum `NoteBlockType`, ao sealed `NoteBlock`, aos mappers (`typeName`/`contentText`/`toDomain`) e ao `BlockEditor` do `NoteEditorScreen`.
