# Schema — Arquivo `.travel` (exportação/importação de viagem)

Um arquivo `.travel` é um ZIP renomeado. O nome do arquivo usa o nome da viagem com caracteres especiais substituídos por `_`.

```
viagem.travel  (ZIP)
├── trip.json               ← dados completos da viagem
├── documents/              ← documentos anexados aos dias
│   └── <nome-do-arquivo>
└── vouchers/               ← arquivos de vouchers/ingressos
    └── <subpasta>/<nome-do-arquivo>   (espelha o assetPath)
```

> Boarding passes **não** geram arquivos no ZIP — seus dados ficam inteiramente em `trip.json`.

---

## `trip.json` — estrutura completa

```json
{
  "schemaVersion": 2,
  "exportedAt": "2026-06-17T14:30:00",
  "trip": {
    "tripUuid": "3f2a…-…-…",
    "lastEditedAt": 1718637000000,
    "name": "Gramado 2026",
    "destination": "Gramado, RS",
    "coverEmoji": "🏔️",
    "startDate": "2026-06-09",
    "endDate": "2026-06-13",
    "latitude": -29.3781,
    "longitude": -50.8728,
    "voucherSortMode": "BY_CATEGORY",
    "hotel": {
      "name": "Serra Azul Resort",
      "address": "Rua dos Pinheiros, 100, Gramado, RS",
      "phone": "+55 54 99999-0000"
    },
    "days": [...],
    "contacts": [...],
    "vouchers": [...],
    "boardingPasses": [...]
  }
}
```

| Campo | Tipo | Notas |
|---|---|---|
| `schemaVersion` | `int` | Versão do schema. Atual: `2` (F1 adicionou `tripUuid` + `lastEditedAt`) |
| `exportedAt` | `string` | ISO 8601 — informativo, não usado na importação |
| `trip.tripUuid` | `string` | **F1** — UUID estável da viagem, gerado na criação e preservado em todo export/import. Ausente/vazio em arquivos v1 → tratado como "sem UUID" (importação normal, gera novo) |
| `trip.lastEditedAt` | `long` | **F1** — unix ms da última edição de conteúdo. Usado na detecção de duplicata para comparar "versão local" × "versão importada" |
| `trip.latitude` / `trip.longitude` | `number \| null` | Coordenadas do destino para clima |
| `trip.voucherSortMode` | `string` | `BY_CATEGORY \| BY_PERSON \| BY_DAY` — preferência de agrupamento restaurada na importação |

---

## `days[]`

```json
{
  "dayNumber": 1,
  "date": "2026-06-09",
  "dayOfWeek": "Terça-feira",
  "title": "Chegada e primeiros passeios",
  "dayAlert": "Check-in a partir das 14h",
  "linkUrl": "https://maps.google.com/...",
  "linkLabel": "Ver mapa do centro",
  "documentName": "ingresso-teatro.pdf",
  "activities": [...]
}
```

| Campo | Tipo | Notas |
|---|---|---|
| `dayNumber` | `int` | Sequencial a partir de 1; usado para localizar o dia no banco na importação |
| `date` | `string` | ISO 8601 — `"YYYY-MM-DD"` |
| `dayOfWeek` | `string` | Ex: `"Terça-feira"` — informativo |
| `title` | `string` | Título do dia |
| `dayAlert` | `string \| null` | Alerta exibido no topo do dia |
| `linkUrl` | `string \| null` | Link externo do dia |
| `linkLabel` | `string` | Rótulo do link; string vazia se sem link |
| `documentName` | `string \| null` | Nome do arquivo em `documents/`; `null` se não houver |

> Na importação: o documento é copiado para `filesDir/Arquivos/<documentName>`.

---

## `activities[]`

```json
{
  "position": 0,
  "time": "14h00",
  "emoji": "🏨",
  "name": "Check-in no hotel",
  "detail": "Deixe as malas e descanse um pouco antes de sair.",
  "address": "Rua dos Pinheiros, 100, Gramado, RS",
  "badges": [
    { "type": "BOOKED", "label": "Reservado", "color": null },
    { "type": "CUSTOM", "label": "Pacote", "color": "#7B4FBF" }
  ],
  "walkStops": [...]
}
```

| Campo | Tipo | Notas |
|---|---|---|
| `position` | `int` | Ordem de exibição dentro do dia; atividades são inseridas nessa ordem |
| `time` | `string` | Formato livre — ex: `"09h00"`, `"14h30"` |
| `emoji` | `string` | Um único emoji |
| `name` | `string` | Nome da atividade |
| `detail` | `string` | Descrição; pode ser string vazia |
| `address` | `string \| null` | Endereço para Maps e Uber; preenche `mapQuery` e `uberDestination` |
| `badges` | `Badge[]` | Pode ser `[]` |
| `walkStops` | `WalkStop[]` | Pode ser `[]` |

### `badges[]`

```json
{ "type": "BOOKED", "label": "Reservado", "color": null }
```

| Campo | Tipo | Notas |
|---|---|---|
| `type` | `string` | `FREE \| PAID \| BOOKED \| INCLUDED \| UBER \| WALKING \| CUSTOM` |
| `label` | `string` | Texto exibido no badge |
| `color` | `string \| null` | Hex — ex: `"#7B4FBF"`. Obrigatório para `CUSTOM`; `null` para os demais |

> Na importação: `type` é convertido via `BadgeType.valueOf()`. Valores desconhecidos são ignorados silenciosamente.

### `walkStops[]`

```json
{ "order": 1, "name": "Praça Central", "detail": "Ponto de encontro", "emoji": "📍", "isLast": false }
```

| Campo | Tipo | Notas |
|---|---|---|
| `order` | `int` | Ordem na rota a pé; começa em 1 |
| `name` | `string` | Nome da parada |
| `detail` | `string` | Descrição; pode ser string vazia |
| `emoji` | `string` | Emoji da parada |
| `isLast` | `boolean` | `true` na última parada da rota |

---

## `contacts[]`

```json
{
  "name": "Guia turístico João",
  "role": "Guia local",
  "phone": "+55 54 98888-1111",
  "type": "AGENCY",
  "hasWhatsApp": true,
  "isEmergency": false
}
```

| Campo | Tipo | Notas |
|---|---|---|
| `name` | `string` | Nome do contato |
| `role` | `string` | Função/papel |
| `phone` | `string \| null` | Telefone com DDD |
| `type` | `string` | `AGENCY \| HOTEL \| RESTAURANT \| TRANSPORT \| EMERGENCY \| OTHER` |
| `hasWhatsApp` | `boolean` | |
| `isEmergency` | `boolean` | Destacado no app se `true` |

> Na importação: `type` é convertido via `ContactType.valueOf()`. Valor padrão se desconhecido: `AGENCY`.

---

## `vouchers[]`

```json
{
  "emoji": "🎢",
  "groupName": "Parque Bondinho",
  "name": "Adulto — dia 09/06",
  "person": "Rodrigo",
  "assetPath": "bondinhos/voucher-adulto.pdf",
  "dayId": 1,
  "sortOrder": 0,
  "isUsed": false
}
```

| Campo | Tipo | Notas |
|---|---|---|
| `emoji` | `string` | Emoji do voucher |
| `groupName` | `string` | Nome do grupo/estabelecimento |
| `name` | `string` | Descrição do voucher |
| `person` | `string \| null` | Nome do portador |
| `assetPath` | `string` | Caminho relativo dentro de `vouchers/` no ZIP |
| `dayId` | `int \| null` | `dayNumber` do dia relacionado; `null` se não vinculado |
| `sortOrder` | `int` | Posição do voucher dentro do grupo para preservar ordem manual |
| `isUsed` | `boolean` | `true` se o voucher foi marcado como já utilizado |

> **Exportação:** o exporter busca o arquivo do voucher em três locais, nesta ordem: (1) caminho absoluto em `filesDir` (vouchers importados de outro aparelho), (2) `filesDir/Vouchers/<assetPath>`, (3) `assets/<assetPath>` do APK. Se não encontrado em nenhum, o registro JSON é mantido mas o arquivo é omitido do ZIP.  
> **Importação:** se o arquivo existir no ZIP em `vouchers/<assetPath>`, é salvo em `filesDir/Vouchers/<assetPath>` e `assetPath` é substituído pelo caminho absoluto. Se não existir, o `assetPath` original é mantido (aponta para o asset do APK).

---

## `boardingPasses[]`

```json
{
  "origin": "GRU",
  "originCity": "São Paulo",
  "destination": "CXJ",
  "destinationCity": "Caxias do Sul",
  "flightNumber": "LA3456",
  "date": "09/06/2026",
  "boardingTime": "06h30",
  "passenger": "Rodrigo Santos",
  "walletUrl": null
}
```

| Campo | Tipo | Notas |
|---|---|---|
| `origin` / `destination` | `string` | Código IATA do aeroporto |
| `originCity` / `destinationCity` | `string` | Nome da cidade |
| `flightNumber` | `string` | Código do voo |
| `date` | `string` | Data do voo — formato livre (ex: `"09/06/2026"`) |
| `boardingTime` | `string` | Horário de embarque — formato livre (ex: `"06h30"`) |
| `passenger` | `string` | Nome do passageiro |
| `walletUrl` | `string \| null` | URL para carteira digital (Apple/Google Wallet) |

---

## Regras gerais

- `schemaVersion` permite migrações futuras. O importador rejeita versões superiores ao `SUPPORTED_SCHEMA_VERSION` atual (`2`) com mensagem de erro clara
- Campos opcionais ausentes ou `null` devem ser JSON nativo (`null`), nunca a string `"null"`
- `exportedAt` é apenas informativo — não é usado em nenhuma lógica de importação
- **Detecção de duplicata (F1):** se o `tripUuid` importado já existir no banco, a importação **não** cria uma nova viagem — lança `DuplicateTripException` e o usuário decide entre manter a local ou substituí-la (`overwriteImport`). UUID vazio (arquivos v1) nunca casa → importação normal, com novo UUID gerado
- IDs internos do banco (`dbId`, `id`) não são exportados — novos IDs são gerados na importação
- O weather **não é exportado** — sempre buscado ao vivo pelo Open-Meteo após a importação
- Dias ausentes no banco (por `dayNumber`) são silenciosamente ignorados na importação
- Atividades existentes no dia não são removidas antes da inserção

---

## Arquivos relevantes

| Arquivo | Responsabilidade |
|---|---|
| `data/export/TravelExporter.kt` | Serializa todos os dados + compacta → `.travel` |
| `data/import/TravelImporter.kt` | Descompacta + parseia `trip.json` + salva no banco |
| `ui/share_trip/ShareTripScreen.kt` | Tela intermediária antes do `ACTION_SEND` |
| `ui/share_trip/ShareTripViewModel.kt` | Estados `Idle / Exporting / Ready / Error` |
| `ui/import_trip/ImportTripScreen.kt` | Seletor de arquivo + feedback de importação |
| `ui/import_trip/ImportTripViewModel.kt` | Estados `Idle / Importing / Done / Error / Duplicate` |
| `data/model/DuplicateTripException.kt` | Sinaliza UUID já existente (F1) |
| `data/db/entity/` | Entidades Room usadas na importação |
