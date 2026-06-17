# Schema JSON — Geração de Roteiro por IA

Documento de referência para o formato JSON usado na geração e importação de roteiros via IA.  
O parse é feito em `ItineraryGenerator.parseJson()` e o salvamento em `TripRepository.saveGeneratedItinerary()`.

---

## Estrutura completa

```json
{
  "days": [
    {
      "dayNumber": 1,
      "title": "Chegada e primeiro contato com a cidade",
      "dayAlert": "Check-in a partir das 14h",
      "activities": [
        {
          "time": "15h00",
          "emoji": "🏨",
          "name": "Check-in no hotel",
          "detail": "Deixe as malas e descanse antes de sair.",
          "address": "Rua dos Navegantes, 363, Boa Viagem, Recife, PE",
          "badges": ["BOOKED"]
        },
        {
          "time": "17h30",
          "emoji": "🎭",
          "name": "Passeio no centro histórico",
          "detail": "Explore o Recife Antigo e a Praça do Marco Zero.",
          "address": "Praça do Marco Zero, Recife Antigo, PE",
          "badges": ["FREE", "WALKING"]
        }
      ]
    }
  ]
}
```

---

## Campos — `days[]`

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `dayNumber` | `int` | ✅ | Sequencial de 1 a N, correspondendo aos dias criados na viagem. Dias sem correspondência no banco são ignorados |
| `title` | `string` | ✅ | Título curto e descritivo do dia |
| `dayAlert` | `string \| null` | — | Alerta exibido em destaque no topo do dia. Use JSON `null` se não houver |

## Campos — `activities[]`

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `time` | `string` | ✅ | Formato `"HHhMM"` — ex: `"09h00"`, `"14h30"` |
| `emoji` | `string` | ✅ | Um único emoji representando a atividade |
| `name` | `string` | ✅ | Nome curto da atividade |
| `detail` | `string` | — | Descrição com dicas práticas. Pode ser string vazia |
| `address` | `string \| null` | — | Endereço completo para Google Maps e Uber. Use `null` se desconhecido |
| `badges` | `string[]` | — | Lista de badges padrão. Pode ser `[]`. Ver tabela abaixo |

---

## Badges válidos

| Valor | Label exibido | Cor | Significado |
|---|---|---|---|
| `FREE` | Grátis | Verde | Entrada ou atividade sem custo |
| `PAID` | Pago | Vermelho | Requer pagamento no local |
| `BOOKED` | Reservado | Azul | Já reservado ou necessário reservar |
| `INCLUDED` | Incluso | Roxo | Incluso no pacote ou hospedagem |
| `WALKING` | A pé | Cinza | Atividade a pé / caminhada |
| `UBER` | Uber | Preto | Recomenda usar Uber para chegar |

Uma atividade pode ter múltiplos badges: `["PAID", "BOOKED"]`.  
O tipo `CUSTOM` (badge com cor personalizada) **não é suportado** na geração por IA — apenas no schema `.travel`. Valores fora da lista são ignorados silenciosamente.

---

## Regras de formatação da resposta

- Retornar **apenas o JSON**, sem texto antes ou depois
- Sem bloco markdown ` ```json ``` ` — o parser tenta extrair o bloco, mas texto adicional pode quebrar o parse
- `null` deve ser JSON nativo (`null`), não a string `"null"`
- Gerar exatamente o número de dias da viagem, com `dayNumber` de 1 a N sem lacunas

---

## Comportamento do app ao salvar (`saveGeneratedItinerary`)

1. Para cada `dayNumber`, busca o `TravelDayEntity` correspondente no banco
2. Atualiza `title` e `dayAlert` do dia
3. Insere cada atividade como `TravelActivityEntity`, com `address` preenchendo `mapQuery` e `uberDestination`
4. Insere os badges como `ActivityBadgeEntity`
5. Dias sem correspondência no banco (por `dayNumber`) são ignorados
6. Atividades existentes no dia **não são removidas** antes da inserção — em geração inicial a viagem está vazia

---

## Modos de uso no app

O wizard de criação (Step 4) oferece dois modos de entrada do JSON:

### Modo Chat (Gemini)

- O app inicia uma sessão de chat com `gemini-2.0-flash` via SDK `com.google.ai.client.generativeai`
- O system prompt já contém o contexto da viagem (destino, datas, hospedagem)
- Após ao menos 1 mensagem, o botão "Gerar roteiro agora" instrui o modelo a produzir o JSON completo
- O JSON é gerado internamente — o usuário não precisa copiar/colar nada

### Modo Importar (JSON externo)

O usuário copia um prompt, gera o JSON em qualquer IA externa (ChatGPT, Gemini web, etc.) e cola ou importa o resultado no app.

**Prompt gerado pelo app (`buildImportPrompt()` em `CreateTripViewModel`):**

```
Você é um especialista em roteiros de viagem.

Gere um roteiro detalhado para a seguinte viagem:

- Destino: [destino]
- Período: [data início] a [data fim] ([N] dias)
- Hospedagem: [nome do hotel], [endereço]

Retorne APENAS o JSON abaixo, sem texto antes ou depois, sem bloco markdown, sem explicações.

{
  "days": [
    {
      "dayNumber": 1,
      "title": "Título curto e descritivo do dia",
      "dayAlert": "Alerta importante para o dia, ou null",
      "activities": [
        {
          "time": "09h00",
          "emoji": "🎯",
          "name": "Nome da atividade",
          "detail": "Descrição com dicas práticas",
          "address": "Endereço completo, Cidade, UF, ou null",
          "badges": ["FREE"]
        }
      ]
    }
  ]
}

Regras obrigatórias:
- Gere exatamente [N] dias, com dayNumber de 1 a [N]
- Cada dia deve ter entre 3 e 6 atividades com horários realistas
- Inclua refeições (café da manhã, almoço, jantar) quando fizer sentido
- "time" no formato HHhMM (ex: "09h00", "14h30")
- "address" deve ser endereço completo para Google Maps — use null se não souber
- Badges válidos: FREE, PAID, BOOKED, INCLUDED, WALKING, UBER
- Uma atividade pode ter múltiplos badges: ["PAID", "BOOKED"]
- "dayAlert" deve ser null (JSON nativo) se não houver alerta
- Retorne apenas JSON válido, nada mais
```

> O prompt é copiado para o clipboard via botão "Copiar prompt para IA". Também cobre dois cenários extras que o usuário pode adicionar manualmente: converter roteiro em texto livre e reformatar JSON de outra IA.

**Entrada do JSON pelo usuário:**
- Campo de texto grande para colar diretamente
- Botão "Importar arquivo" para abrir `.json` ou `.txt` do dispositivo

O parse em ambos os casos usa `ItineraryGenerator.parseJson()`, que extrai o bloco JSON mesmo que o texto contenha markdown ou texto em volta.

---

## Diferença entre este schema e o `.travel`

| Aspecto | AI Schema | `.travel` Schema |
|---|---|---|
| Origem | IA externa ou Gemini | App exportando viagem completa |
| Dados exportados | Dias + atividades + badges | Tudo (dias, contatos, vouchers, docs, boarding) |
| `walkStops` | Não suportado | ✅ Suportado |
| Badge `CUSTOM` | Não suportado | ✅ Suportado |
| `schemaVersion` | Sem versionamento | `schemaVersion: 1` |
| Resultado no app | Preenche roteiro de viagem existente | Cria nova viagem completa |

---

## Arquivos relevantes

| Arquivo | Responsabilidade |
|---|---|
| `data/ai/ItineraryGenerator.kt` | System prompt do Gemini, `parseJson()`, `generateItinerary()` |
| `ui/trips/CreateTripViewModel.kt` | `ChatPhase`, `buildImportPrompt()`, `importFromJson()`, `startChat()` |
| `data/repository/TripRepository.kt` | `saveGeneratedItinerary(tripId, days)` |
| `data/db/entity/TravelDayEntity.kt` | Entidade do dia no banco |
| `data/db/entity/TravelActivityEntity.kt` | Entidade da atividade no banco |
| `data/db/entity/ActivityBadgeEntity.kt` | Entidade dos badges no banco |
