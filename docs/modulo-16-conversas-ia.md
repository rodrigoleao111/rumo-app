# Módulo 16 — Minhas conversas com a IA

**Telas:** `AiConversationsScreen` (lista) + `AiConversationDetailScreen` (detalhe)
**Arquivos:** `ui/aiconversations/`
**ViewModels:** `AiConversationsViewModel`, `AiConversationDetailViewModel`
**Entry point:** item **"Minhas conversas com a IA"** no drawer da tela inicial (`TripsListScreen`)

---

## Visão geral

Lista as conversas passadas com a IA (feitas no wizard de criação, Módulo 04). Cada card mostra **título da viagem**, **local** (destino) e **data** (período da viagem ou, na falta, a data da conversa). Ao tocar um card, abre o detalhe com as bolhas da conversa e um botão **"Copiar conversa"** que copia toda a transcrição.

---

## Persistência (Room)

As conversas **não existiam** em disco antes deste módulo — viviam só na memória do `CreateTripViewModel`. Foi adicionada persistência:

| Peça | Arquivo | Observação |
|---|---|---|
| Entidade | `data/db/entity/AiConversationEntity.kt` | Tabela `ai_conversations`. **Sem ForeignKey** — é um *snapshot independente* (guarda nome/destino/datas), então sobrevive à exclusão da viagem. |
| DAO | `data/db/dao/AiConversationDao.kt` | `observeAll()` (Flow, DESC por `createdAt`), `getById`, `insert`, `updateMessages`, `delete`. |
| Migração | `TravelDatabase` `MIGRATION_19_20` | `CREATE TABLE ai_conversations`. Versão **19 → 20**. |
| Repositório | `data/repository/AiConversationRepository.kt` | Mapeia entidade ↔ modelo e (de)serializa as mensagens. |
| Modelo | `data/model/AiConversation.kt` | `AiConversation` + `AiChatMessage(fromUser, text)`. |

**Mensagens:** serializadas como **JSON** numa única coluna `messagesJson` — `[{"role":"USER|AI","text":"..."}]` (via `org.json`, sem tabela filha nem lib externa).

**Regra Room (inviolável):** migração explícita + `CURRENT_VERSION` incrementado + registro em `ALL_MIGRATIONS` + `app/schemas/20.json` versionado. O SQL de `MIGRATION_19_20` bate exatamente com o `createSql` do `20.json`.

---

## Gravação da conversa (`CreateTripViewModel`)

`persistConversation()` é chamado **após cada resposta** da IA em `sendChatMessage()` (upsert):
- Só grava se houver ≥ 1 mensagem do usuário (abrir e só ler a saudação não cria conversa).
- 1ª vez → `insert` e guarda `currentConversationId`; depois → `updateMessages`.
- Guarda o snapshot da viagem (`_form`: nome, destino, datas) + `tripId` + `createdAt`.
- `currentConversationId` reinicia a cada nova conversa (`startChat`).

Inclui a saudação inicial, as mensagens do usuário/IA e o *nudge* (se houver) — tudo que o usuário viu.

---

## Telas

### Lista (`AiConversationsScreen`)
- `conversations: StateFlow<List<AiConversation>?>` (null = carregando) via `repo.conversations`.
- `TopAppBar` verde + back; `LazyColumn` de cards; estado vazio (`aiconv_empty_*`).
- **Card:** ícone ✨ (`ic_auto_awesome`), título (`tripName`, com fallback destino → `aiconv_untitled`), `📍 destino`, e a data (`conversationDateLabel`: período da viagem ou `createdAt`).

### Detalhe (`AiConversationDetailScreen`)
- `AiConversationDetailViewModel` recebe `conversationId` via `SavedStateHandle`, carrega com `repo.getById`.
- Bolhas iguais às do chat (usuário = `GreenMoss`/branco; IA = `SurfaceWhite` + **`MarkdownText`**).
- `bottomBar` com **"Copiar conversa"** → copia `buildTranscript()` (linhas `Você:`/`Assistente:`, rótulos `aiconv_you`/`aiconv_assistant`) para o clipboard + snackbar `create_copied`.
- Ação de **excluir** (`ic_delete`) na `TopAppBar` → `viewModel.delete(onBack)` (exclui e volta).

### Excluir conversa
Padrão do app (igual à exclusão de viagem): ação/ícone → `AlertDialog` de confirmação (vermelho `#D32F2F`, `aiconv_delete_title`/`aiconv_delete_msg`) → `viewModel.delete(...)` → `repo.delete(id)` → o `Flow` de `observeAll` atualiza a lista sozinho. Disponível em **dois lugares**: ícone de lixeira em cada card da lista e ação na barra do detalhe.

> **`MarkdownText`** foi extraído para `ui/components/MarkdownText.kt` (antes era privado no `CreateTripScreen`) para ser reusado aqui e no chat.

---

## Navegação e drawer

- Rotas em `navigation/AppNavigation.kt`: `Screen.AiConversations` (`ai_conversations`) e `Screen.AiConversationDetail` (`ai_conversation/{conversationId}`, `LongType`).
- Drawer (`TripsListScreen.TripsDrawerContent`): item "Minhas conversas com a IA" (`aiconv_title`, ícone `ic_auto_awesome`) acima de Configurações. Wiring: `TripsListScreen(onAiConversationsClick = …)`.

---

## i18n

Chaves `aiconv_*` nos três `strings.xml` (pt/en/es): `aiconv_title`, `aiconv_untitled`, `aiconv_empty_title`, `aiconv_empty_msg`, `aiconv_you`, `aiconv_assistant`, `aiconv_not_found`. Botão/feedback de cópia reutilizam `create_copy_conversation` / `create_copied`.

---

## Checklist para futuras modificações

- **Vincular ao abrir a viagem:** o card guarda `tripId` (pode ser null); dá para navegar à viagem se ela ainda existir.
- **Campo novo no card:** adicionar coluna → nova `MIGRATION_20_21` → `CURRENT_VERSION` 21 → `AiConversation`/repo.
