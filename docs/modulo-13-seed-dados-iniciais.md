# Módulo 13 — Seed e dados iniciais

**Arquivos:**
- `data/seeder/DatabaseSeeder.kt` — insere a viagem de exemplo se o banco estiver vazio
- `data/repository/RoteiroRepository.kt` — fonte estática dos dados da viagem "Gramado & Canela"

**Entry point:** `MainActivity.onCreate` → `DatabaseSeeder.seedIfEmpty(db)` em background.

---

## Visão geral

Na primeira execução (banco vazio), o app popula uma **viagem de demonstração completa** — "Gramado & Canela" — a partir de dados estáticos declarados em `RoteiroRepository`. Isso dá ao usuário um roteiro navegável de imediato, em vez de uma tela vazia. O seed é **idempotente**: só roda se não houver nenhuma viagem.

`RoteiroRepository` é a herança direta do app de viagem única original — hoje serve exclusivamente como **fixture de seed**, não como repositório de runtime (não acessa o banco; é um `object` com listas literais).

---

## `DatabaseSeeder`

`object` com um único método `suspend`:

```kotlin
suspend fun seedIfEmpty(db: TravelDatabase) {
    if (db.tripDao().count() > 0) return          // idempotente: já há viagem → não faz nada

    val days      = RoteiroRepository.days
    val startDate = days.minOfOrNull { it.date }?.toString()
    val endDate   = days.maxOfOrNull { it.date }?.toString()

    val tripId = db.tripDao().insert(TripEntity(name = "Gramado & Canela", …))

    days.forEach { day ->
        val dayId = db.dayDao().insert(day.toEntity(tripId))
        day.activities.forEachIndexed { idx, activity ->
            val activityId = db.activityDao().insertActivity(activity.toEntity(dayId, idx))
            activity.badges.forEach    { db.activityDao().insertBadge(it.toEntity(activityId)) }
            activity.walkStops.forEachIndexed { i, s -> db.activityDao().insertWalkStop(s.toEntity(activityId, i)) }
        }
    }
    RoteiroRepository.contacts.forEach       { db.contactDao().insert(it.toEntity(tripId)) }
    RoteiroRepository.vouchers.forEach       { db.voucherDao().insert(it.toEntity(tripId)) }
    RoteiroRepository.boardingPasses.forEach { db.boardingPassDao().insert(it.toEntity(tripId)) }
}
```

**Pontos-chave:**
- **Guard de idempotência:** `db.tripDao().count() > 0` → early return. Roda apenas com o banco realmente vazio (não "sem esta viagem específica").
- **Datas derivadas:** `startDate`/`endDate` são o mínimo e o máximo das datas dos dias — não hardcoded na `TripEntity`.
- **Ordem de inserção respeitada:** atividades por índice (`forEachIndexed → position`), walk stops idem — preserva a ordenação usada pela UI.
- **Mappers `domain → entity`:** usa as extensões `toEntity(...)` de `Mappers.kt` (ver `docs/arquitetura-geral.md`).
- **Sem transação:** as inserções não estão em `withTransaction { }`. Como só roda uma vez com o banco vazio e em background, o risco de estado parcial é baixo; ainda assim, envolver em transação seria uma melhoria de robustez.

### Invocação (`MainActivity`)

```kotlin
val db = TravelDatabase.getInstance(this)
lifecycleScope.launch(Dispatchers.IO) {
    DatabaseSeeder.seedIfEmpty(db)
}
```

Roda em `Dispatchers.IO`, fora da thread principal. A UI (`AppNavigation`) observa `TripsListViewModel` reativamente via `Flow`, então a viagem aparece assim que as inserções terminam — sem necessidade de sincronização manual.

---

## `RoteiroRepository`

`object` estático (~687 linhas) que expõe quatro listas literais imutáveis:

| Propriedade | Tipo | Conteúdo |
|---|---|---|
| `days` | `List<TravelDay>` | Dias com atividades, badges e paradas de caminhada aninhadas |
| `contacts` | `List<Contact>` | Contatos da viagem (agência, hotel, atrações, emergência) |
| `boardingPasses` | `List<BoardingPass>` | Passagens aéreas de ida e volta |
| `vouchers` | `List<Voucher>` | Vouchers e ingressos |

**Natureza:** conteúdo 100% específico da viagem Gramado 2026 (nomes de hotéis, restaurantes, horários, telefones, códigos de voucher). Trabalha com **modelos de domínio** (`TravelDay`, `Contact`, etc.), não com entities — a conversão para entity acontece no `DatabaseSeeder` na hora de inserir.

> **Nota de origem.** `RoteiroRepository` e o conteúdo semeado são resquícios do app de viagem única. Menções a "Bustour" e afins aqui são apenas **texto descritivo do roteiro** (conteúdo de dados), não código de recurso — devem ser mantidas como estão. O nome `Repository` é histórico; funcionalmente é um provedor de dados de seed, não um repositório do padrão da camada de dados.

---

## Ciclo de vida do dado semeado

1. **1ª execução:** banco vazio → seed insere a viagem completa.
2. **Execuções seguintes:** `count() > 0` → seed não faz nada; os dados agora são de propriedade do usuário (editáveis, deletáveis).
3. **Usuário deleta todas as viagens:** na próxima abertura o banco volta a estar vazio → **o seed roda de novo** e a viagem de demonstração reaparece. (Comportamento atual — se não for desejado, seria preciso uma flag "already_seeded" persistida.)

---

## Checklist para futuras modificações

- **Trocar/atualizar a viagem de exemplo:** editar as listas em `RoteiroRepository`. Como usa modelos de domínio, não requer migration.
- **Não semear em produção (app "limpo"):** condicionar `seedIfEmpty` a `BuildConfig.DEBUG`, ou substituir a viagem de demonstração por um estado vazio com onboarding.
- **Evitar reseed após o usuário apagar tudo:** persistir uma flag `already_seeded` em DataStore e checá-la além do `count()`.
- **Tornar o seed atômico:** envolver as inserções em `db.withTransaction { }` para garantir tudo-ou-nada.
- **Novo tipo de dado semeado:** adicionar a lista em `RoteiroRepository` + o laço de inserção correspondente em `DatabaseSeeder`, usando o mapper `toEntity` do novo tipo.
