# Roadmap de Novas Funcionalidades

**Status:** Especificação — aguardando implementação

---

## Sumário

### Seções de referência rápida
- [Resumo de ferramentas externas, custos e alternativas](#resumo-de-ferramentas-externas-custos-e-alternativas)
- [Roadmap de entrega — fases e cronologia](#roadmap-de-entrega--fases-e-cronologia)
- [Ordem de implementação recomendada](#ordem-de-implementação-recomendada)
- [Questões em aberto](#questões-em-aberto)

### Funcionalidades (F1–F17)

| # | Funcionalidade | Trilha | Firebase | Esforço |
|---|---|---|---|---|
| F1 ✅ | Identificador de viagem e detecção de duplicatas | A | Não | Pequeno |
| F2 | Cadastro e autenticação de usuários | B | Auth + Firestore | Grande |
| F3 | Adicionar pessoas à viagem | B | Firestore | Grande |
| F4 ✅ | Tela de notas | A | Não | Médio |
| F5 | Controle de orçamento | A | Não | Médio |
| F6 | Divisão de custos entre participantes | A/B | Não | Médio |
| F7 | O que tem perto de mim? | A | Não | Médio |
| F8 | Assistente de viagem (IA) | A | Não | Pequeno |
| F9 | Adicionar amigos / seguir perfis | B | Firestore | Médio |
| F10 | Área da comunidade | B | Firestore + Storage | Grande |
| F11 | Modo agência | C | Firestore | Grande |
| F12 | Geração de PDF do roteiro | A | Não | Médio |
| F13 | Notificações push e lembretes | A/B | FCM (parte social) | Médio |
| F14 | Widget de tela inicial | A | Não | Médio |
| F15 | Backup em nuvem de todas as viagens | B | Storage | Médio |
| F16 | Templates de roteiro | C | Firestore | Médio |
| F17 | Avaliação de atividades | A/B | Não (local) | Pequeno |

### Marcos (M1–M9)

| # | Marco | Pré-requisito | Esforço |
|---|---|---|---|
| M1 | Publicação na Google Play Store | Fase 1 + M3 + M4 + M5 | Grande |
| ~~M2~~ | ~~Compatibilização para iOS~~ | — | CANCELADO |
| M3 | CI/CD com GitHub Actions | — | Pequeno |
| M4 | Analytics e monitoramento de erros | — | Pequeno |
| M5 | Acessibilidade (a11y) | — | Médio |
| M6 | Internacionalização (i18n) | — | Médio |
| M7 | Monetização (Freemium + Play Billing) | F2 + base de usuários | Grande |
| M8 | Site de marketing e landing page | — | Médio |
| M9 | Programa de parceria com agências | F11 estável | Processo |

---

**Dependências entre funcionalidades:**

```
F1 — Identificador de viagem    (independente — pode ser implementada agora)
F2 — Cadastro de usuários       (pré-requisito para F3 e para a parte colaborativa de F6)
F3 — Pessoas na viagem          (depende de F1 + F2)
F4 — Tela de notas              (independente — pode ser implementada agora)
F5 — Controle de orçamento      (independente — pode ser implementada agora)
F6 — Divisão de custos          (independente para uso local; depende de F3 para vincular a participantes)
F7 — O que tem perto de mim?    (independente — requer GPS + internet + chave Foursquare)
F8 — Assistente de viagem (IA)  (independente — reutiliza Gemini SDK já presente no projeto)
F9 — Adicionar amigos           (depende de F2)
F10 — Área da comunidade        (depende de F2 + F9; conecta ao sistema de import de F1)
F11 — Modo agência              (depende de F2 + F3 + F10)
F12 — Geração de PDF            (independente — biblioteca PDF Android)
F13 — Notificações e lembretes  (independente para lembretes locais; depende de F2 para notificações sociais)
F14 — Widget de tela inicial    (independente — Jetpack Glance)
F15 — Backup em nuvem           (depende de F2)
F16 — Templates de roteiro      (depende de F1 + F10; agências dependem de F11)
F17 — Avaliação de atividades   (independente)
```

---

## Resumo de ferramentas externas, custos e alternativas

A tabela abaixo lista todas as APIs, SDKs e serviços externos referenciados neste roadmap. Use-a como referência rápida antes de iniciar qualquer funcionalidade.

| Ferramenta | Usado em | Tier gratuito | Quando começa a custar | Alternativa gratuita |
|---|---|---|---|---|
| **Firebase Auth** | F2, F3, F9, F10 | Ilimitado para e-mail/Google Sign-In | Apenas Phone Auth tem limite (10k/mês grátis); e-mail/Google são gratuitos em qualquer escala | Supabase Auth (open source, self-host gratuito) |
| **Firebase Firestore** | F2, F3, F9, F10, F11, F15, F16 | 1 GiB storage, 50k leituras/dia, 20k escritas/dia, 20k deleções/dia (Spark plan) | Ao ultrapassar qualquer limite → migrar para plano Blaze (pay-as-you-go): ~USD 0,06/100k leituras | Supabase (PostgreSQL gerenciado, tier grátis generoso) |
| **Firebase Storage** | F10, F15 | 5 GB armazenamento, 1 GB/dia download (Spark plan) | USD 0,026/GB além dos 5 GB | Supabase Storage; Backblaze B2 (10 GB gratuitos) |
| **Firebase Analytics** | M4 | **Gratuito sem limites** | — | PostHog (open source, self-host) |
| **Firebase Crashlytics** | M1, M4 | **Gratuito sem limites** | — | Sentry (tier gratuito: 5k erros/mês) |
| **Firebase Performance** | M4 | 500k traces/dia gratuitos | Pago além de 500k/dia | — |
| **Firebase Cloud Messaging (FCM)** | F13 | **Gratuito sem limites** | — | — |
| **Firebase Functions** | M7 | Requer plano Blaze para deploy (pago) | A partir do primeiro deploy | — sem alternativa direta integrada ao Firebase |
| **Google Sign-In** (`play-services-auth`) | F2 | **Gratuito** | — | — |
| **Gemini AI** (`gemini-2.0-flash`) | F8, wizard existente | 15 req/min, 1.500 req/dia (free tier via AI Studio) | USD 0,075/1M tokens de entrada; USD 0,30/1M de saída (Paid tier) | Ollama (local, gratuito); OpenAI GPT-4o-mini (USD 0,15/1M tokens); Groq (free tier generoso) |
| **Foursquare Places API v3** | F7 | 99.000 chamadas/mês | Plano pago via portal Foursquare — preço por chamada além do limite (consultar pricing.foursquare.com) | OpenTripMap (gratuito, tem fotos limitadas); Yelp Fusion (500 chamadas/dia grátis) |
| **Nominatim (OpenStreetMap)** | F8 | Gratuito, mas **uso comercial vedado** na API pública | Não oferece plano pago — exige self-host ou troca de serviço | **Geoapify**: 3.000 req/dia grátis; **MapTiler**: 100k req/mês grátis; **Here Geocoding**: 250k req/mês grátis |
| **Open-Meteo** (clima, geocoding) | Existente | **Gratuito e open source** para uso não-comercial | Plano comercial disponível (USD 10–50/mês) a partir do uso comercial intensivo | — (é open source; self-host disponível) |
| **Google Play Developer** | M1 | — | **USD 25 taxa única** para criar conta de desenvolvedor | — |
| **Google Play Billing** | M7 | Biblioteca gratuita | Google retém **30% de comissão** sobre cada transação (15% após 1 ano de assinatura; 15% para apps com faturamento < USD 1M/ano via programa reduzido) | — sem alternativa para apps na Play Store |
| **GitHub Actions** | M3 | 2.000 minutos/mês em repositórios privados; **ilimitado** em repositórios públicos | USD 0,008/minuto extra em repos privados | GitLab CI (400 minutos/mês grátis); Bitbucket Pipelines (50 min/mês grátis) |
| **Jetpack Glance** | F14 | **Gratuito** (biblioteca Android oficial) | — | — |
| **WorkManager** | F13, F14, F15 | **Gratuito** (biblioteca Android oficial) | — | — |
| **`android.graphics.pdf.PdfDocument`** | F12 | **Gratuito** (API nativa Android) | — | PdfBox-Android (Apache 2.0, gratuito); iText (AGPL — gratuito apenas para OSS; pago para uso comercial fechado) |
| **Coil** | F7, F10 | **Gratuito** (Apache 2.0) | — | Glide (Apache 2.0, gratuito) |
| **OkHttp / Gson** | F7 | **Gratuito** (Apache 2.0) | — | Ktor Client + kotlinx.serialization (Kotlin-nativo) |
| **Vico** (gráficos) | F5 | **Gratuito** (Apache 2.0) | — | MPAndroidChart (gratuito, sem suporte nativo a Compose) |

> **Regra geral — Firebase:** o Spark Plan (gratuito) é suficiente para desenvolvimento e para um app com poucos centenas de usuários. Ao crescer para Trilha B e Trilha C (funcionalidades sociais), antecipar a migração para o plano **Blaze (pay-as-you-go)** — especialmente por causa do Firestore (leituras de feed em F10) e do Storage (fotos de posts e backup em F15). O plano Blaze não tem custo fixo: paga-se apenas pelo uso além dos limites gratuitos.

> **Atenção — Firebase Functions em M7:** validação de compras no backend requer Firebase Functions, que **não está disponível no Spark Plan**. Ao implementar M7, a conta Firebase precisa estar no plano Blaze. Alternativa: validar a compra apenas no cliente (menos seguro, sujeito a fraude) ou usar um servidor próprio simples.

> **Atenção — Nominatim em F8:** a API pública do Nominatim proíbe uso comercial e exige menos de 1 requisição/segundo. Para um app publicado na Play Store (M1), usar **Geoapify** (3.000 req/dia grátis, uso comercial permitido) ou **MapTiler Geocoding** (100k req/mês grátis) para geocodificação reversa no assistente de viagem.

---

## F1 — Identificador de viagem e detecção de duplicatas

> **Status: ✅ Implementada.** `tripUuid` + `lastEditedAt` no schema (Migration 17), `trip.json` v2, detecção de duplicata na importação com diálogo de conflito e `overwriteImport` atômico. Cobertura por testes de migração e de round-trip/detecção. Notas de implementação que divergem desta spec: o `touchLastEditedAt` é disparado na **camada ViewModel** (a importação não deve tocá-lo); o `overwriteImport` importa **antes** de deletar a antiga (mais seguro que delete-then-insert com arquivos); os botões do diálogo foram unificados em "Manter local" / "Importar". O teste de UI Compose ficou pendente por incompatibilidade do emulador API 37 com o toolchain de instrumentação.

### Descrição

Hoje, importar a mesma viagem duas vezes cria duas viagens separadas no banco — sem nenhuma detecção de duplicata. Isso acontece porque não existe nenhum identificador único que persista entre export e import.

Esta funcionalidade adiciona um **UUID de viagem** gerado uma única vez na criação e propagado em todo export/import. Quando o usuário tenta importar um `.travel` cujo UUID já existe no banco, o app compara os timestamps de última edição e oferece ao usuário a escolha de sobrescrever ou cancelar.

### Mudanças de schema

#### Banco de dados — Migration 17

```sql
-- trips: novo UUID + timestamp de última edição
ALTER TABLE trips ADD COLUMN trip_uuid TEXT NOT NULL DEFAULT '';
ALTER TABLE trips ADD COLUMN last_edited_at INTEGER NOT NULL DEFAULT 0;
```

**Geração do UUID:** `java.util.UUID.randomUUID().toString()` no momento de `TripRepository.createTrip()`.  
**Atualização do timestamp:** toda operação de escrita que altere conteúdo da viagem deve chamar `TripRepository.touchLastEditedAt(tripId)`. Isso inclui: salvar viagem, salvar dia, salvar atividade, salvar contato, salvar voucher, salvar passagem, salvar roteiro da IA.

#### Schema do `trip.json` — versão 2

Campos adicionados ao objeto raiz:

| Campo | Tipo | Descrição |
|---|---|---|
| `schemaVersion` | `Int` → **2** | Incrementado para sinalizar presença dos novos campos |
| `tripUuid` | `String` | UUID da viagem, gerado uma única vez na criação |
| `lastEditedAt` | `Long` | Unix timestamp (ms) da última edição registrada |

Arquivos a atualizar: `TravelExporter.kt`, `TravelImporter.kt`, `docs/travel-export-schema.md`.

#### Modelo de domínio

```kotlin
data class Trip(
    // ... campos existentes ...
    val tripUuid: String,       // novo
    val lastEditedAt: Long      // novo — unix timestamp ms
)
```

### Fluxo de importação com detecção de duplicata

```
TravelImporter.import(uri)
  ├─ parseZip(uri) → ExportedTrip (com tripUuid + lastEditedAt)
  ├─ db.tripDao().findByUuid(tripUuid) → TripEntity?
  │
  ├─ [UUID NÃO ENCONTRADO] → fluxo normal de importação (sem mudança)
  │
  └─ [UUID ENCONTRADO] → lança DuplicateTripException(
         localTrip = TripEntity existente,
         incomingLastEditedAt = Long,
         incomingTripName = String
     )

ImportTripViewModel.startImport(uri)
  ├─ catch DuplicateTripException → phase = Duplicate(info)
  └─ ImportTripScreen exibe dialog de conflito

Dialog de conflito:
  ├─ Título: "Viagem já importada"
  ├─ Corpo: mostra nome da viagem, data local vs. data importada
  ├─ [Manter local]  → dismissError() → Idle
  ├─ [Importar]      → vm.overwriteImport(uri, existingTripId) → sobreescreve
  └─ [Cancelar]      → dismissError() → Idle

TravelImporter.overwriteImport(uri, existingTripId)
  ├─ Deleta viagem existente (CASCADE no banco remove tudo relacionado)
  ├─ Remove arquivos de filesDir (Arquivos/, Vouchers/, Passagens/) do trip antigo
  └─ Roda import normal — insere tudo do zero
```

### Estados adicionais no `ImportPhase`

```kotlin
sealed class ImportPhase {
    // estados existentes: Idle, Importing, Done, Error
    data class Duplicate(
        val existingTripName: String,
        val existingLastEditedAt: Long,   // para exibir "Versão local: DD/MM/YYYY HH:mm"
        val incomingLastEditedAt: Long,   // para exibir "Versão importada: DD/MM/YYYY HH:mm"
        val pendingUri: Uri               // guardado para reuso no overwriteImport()
    ) : ImportPhase()
}
```

### Casos de uso

| # | Cenário | Comportamento esperado |
|---|---|---|
| UC-F1-01 | Importar viagem pela primeira vez (sem UUID no banco) | Importação normal, sem dialog |
| UC-F1-02 | Importar arquivo `.travel` de versão antiga do app (sem `tripUuid`) | `tripUuid` ausente → tratar como "sem UUID" → importação normal sem detecção |
| UC-F1-03 | Importar o mesmo arquivo duas vezes sem edições entre as importações | Detecta UUID, timestamps iguais → dialog informa que são idênticas, sugere manter local |
| UC-F1-04 | Importar versão mais nova (timestamp importado > local) | Dialog destaca que a versão importada é mais recente; botão "Importar" em destaque |
| UC-F1-05 | Importar versão mais antiga (timestamp importado < local) | Dialog avisa que a versão local é mais recente e alerta sobre perda de dados |
| UC-F1-06 | Usuário escolhe "Manter local" | Nenhuma mudança no banco; fase volta para Idle |
| UC-F1-07 | Usuário escolhe "Importar" (sobreescrever) | Viagem antiga deletada do banco + arquivos de filesDir removidos; nova importada |
| UC-F1-08 | Usuário escolhe "Cancelar" | Nenhuma mudança; fase volta para Idle |
| UC-F1-09 | Sobreescrita bem-sucedida | `ImportPhase.Done(newTripId)` → navega para a viagem recém-importada |
| UC-F1-10 | Erro durante sobreescrita (ex: arquivo corrompido) | `ImportPhase.Error` com mensagem; viagem antiga **não** deletada (operação atômica) |

### Regra de atomicidade na sobreescrita

O delete da viagem antiga e o insert da nova devem ocorrer na **mesma transação Room** (`db.withTransaction { }`). Se o insert falhar, o delete é revertido — o usuário não perde a viagem local.

### UI — Dialog de conflito

```
┌─────────────────────────────────────┐
│  ⚠  Viagem já importada             │
│                                     │
│  "Gramado 2026" já existe no app.   │
│                                     │
│  Versão local:     12/06/2026 14:30 │
│  Versão importada: 15/06/2026 09:00 │
│                                     │
│  A versão importada é mais recente. │
│  Deseja substituir a versão local?  │
│                                     │
│  [Manter local]  [Cancelar]  [Importar ▶]
└─────────────────────────────────────┘
```

- Quando `incomingLastEditedAt > existingLastEditedAt`: "A versão importada é mais recente." — botão "Importar" em `GreenMoss`
- Quando `incomingLastEditedAt < existingLastEditedAt`: "⚠ Atenção: a versão local é mais recente. Importar substituirá dados mais novos." — botão "Importar" em `MaterialTheme.colorScheme.error`
- Quando timestamps iguais: "As versões são idênticas."

### Especificação de testes

| Teste | Tipo | O que verificar |
|---|---|---|
| `tripUuidGeneratedOnCreate` | JVM | `createTrip()` gera UUID não-vazio e único entre chamadas consecutivas |
| `touchLastEditedAtUpdatesTimestamp` | JVM | Chamar `touchLastEditedAt()` atualiza o campo no banco com valor > anterior |
| `importNewTripNoUuid` | JVM (mock) | Trip sem `tripUuid` no JSON → `findByUuid("")` retorna null → importação normal |
| `importDuplicateUuid` | JVM (mock) | Trip com UUID já existente → lança `DuplicateTripException` com campos corretos |
| `overwriteIsAtomic` | JVM | Se insert falhar após delete, viagem original ainda existe no banco |
| `overwriteReplacesFiles` | Instrumented | Arquivos da viagem antiga removidos; arquivos da nova presentes em filesDir |
| `schemaVersion2Parsed` | JVM | `parseTripJson` com `schemaVersion=2` lê `tripUuid` e `lastEditedAt` corretamente |
| `schemaVersion1Backward` | JVM | `parseTripJson` com `schemaVersion=1` (sem uuid/timestamp) não lança exceção |

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Modificar | `data/db/TravelDatabase.kt` — Migration 16→17 |
| Modificar | `data/db/entity/TripEntity.kt` — novos campos |
| Modificar | `data/db/Mappers.kt` — ambas as direções |
| Modificar | `data/repository/TripRepository.kt` — `createTrip()` gera UUID; novo `touchLastEditedAt()` |
| Criar | `data/model/DuplicateTripException.kt` |
| Modificar | `data/export/TravelExporter.kt` — inclui `tripUuid` e `lastEditedAt` no JSON |
| Modificar | `data/import/TravelImporter.kt` — `findByUuid`, lança exceção, `overwriteImport()` |
| Modificar | `ui/import_trip/ImportTripViewModel.kt` — estado `Duplicate`, método `overwriteImport()` |
| Modificar | `ui/import_trip/ImportTripScreen.kt` — dialog de conflito |
| Atualizar | `docs/travel-export-schema.md` |
| Atualizar | `docs/modulo-09-share-import.md` |

---

## F2 — Cadastro e autenticação de usuários

### Descrição

O app é hoje totalmente local e anônimo. Para suportar viagens compartilhadas (F3), é necessário introduzir identidade de usuário. Esta funcionalidade adiciona autenticação via **Firebase Auth** e um perfil de usuário armazenado no **Firestore**, com suporte a login por e-mail/senha e Google Sign-In.

A autenticação é opcional para funcionalidades locais: o app continua funcionando sem conta para quem só quer usar localmente. A conta só é exigida ao tentar adicionar pessoas a uma viagem.

### Stack adicionada

| Componente | Biblioteca |
|---|---|
| Autenticação | `com.google.firebase:firebase-auth-ktx` |
| Google Sign-In | `com.google.android.gms:play-services-auth` |
| Banco de dados de perfis | `com.google.firebase:firebase-firestore-ktx` |
| Armazenamento de fotos | `com.google.firebase:firebase-storage-ktx` |
| Base | `com.google.firebase:firebase-bom` (BOM para versões) |

> **💰 Custo — Firebase:** o plano **Spark (gratuito)** é suficiente para desenvolvimento e para um app com até algumas centenas de usuários ativos. O limite mais restritivo é o Firestore: **50.000 leituras/dia** e **20.000 escritas/dia**. Para a Trilha B (funcionalidades sociais — F9, F10), o feed da comunidade consome muitas leituras por sessão, podendo exceder o Spark rapidamente. **Antecipar a migração para o plano Blaze (pay-as-you-go) antes de publicar funcionalidades sociais em produção.** No plano Blaze, paga-se apenas pelo excedente (USD 0,06/100k leituras após o limite gratuito diário). **Não há custo fixo mensal no Blaze** — é zero se o uso não ultrapassar o gratuito.
>
> **Alternativa open-source:** Supabase (PostgreSQL + Auth + Storage) tem tier gratuito mais generoso e pode ser self-hosted gratuitamente. A migração exigiria substituir Firebase SDK por Supabase Kotlin client — custo de reestruturação elevado se adotado tardiamente.

> **Arquivo `google-services.json`:** após configurar o projeto no Firebase Console, adicionar o arquivo em `app/`. **Não versionar** — adicionar ao `.gitignore`. Sem este arquivo, o build do app falha.

### Modelo de dados — Firestore

**Coleção:** `users/{uid}`

```
users/
  {uid}/
    displayName: String          // nome escolhido pelo usuário
    email: String                // e-mail da conta
    phone: String?               // número com DDI (+55 11 99999-9999)
    photoUrl: String?            // URL no Firebase Storage ou foto do Google
    createdAt: Timestamp
    lastSeenAt: Timestamp        // atualizado no login
```

### Arquitetura — novas camadas

```
data/auth/
  AuthRepository.kt        ← signInWithEmail(), signInWithGoogle(), signOut(),
                              currentUser: Flow<FirebaseUser?>, isLoggedIn: Boolean

data/user/
  UserRepository.kt        ← getProfile(uid), saveProfile(uid, ProfileData),
                              uploadPhoto(uid, uri): String (photoUrl),
                              searchByEmail(email): UserProfile?

data/model/
  UserProfile.kt           ← data class: uid, displayName, email, phone, photoUrl, createdAt

data/di/
  AppModule.kt             ← @Provides para FirebaseAuth, FirebaseFirestore, FirebaseStorage
```

### Fluxo de autenticação

```
App abre
  └─ SplashScreen (2s)
       └─ AuthRepository.isLoggedIn?
            ├─ true  → fluxo normal (TripsListScreen ou viagem ativa)
            └─ false → AuthScreen
                  ├─ "Entrar com Google"   → GoogleSignIn → Firebase Auth → verificar perfil
                  ├─ "Entrar com e-mail"   → LoginScreen
                  ├─ "Criar conta"         → SignUpScreen → Firebase Auth → ProfileSetupScreen
                  └─ "Continuar sem conta" → TripsListScreen (sem acesso a F3)

ProfileSetupScreen (apenas na primeira vez, após criar conta)
  ├─ Campo: Nome completo (obrigatório)
  ├─ Campo: Telefone/WhatsApp (opcional, com seletor de DDI)
  ├─ Foto de perfil: câmera / galeria / manter foto do Google (opcional)
  └─ Botão "Salvar e começar" → UserRepository.saveProfile() → TripsListScreen
```

### Telas novas

| Tela | Arquivo | Descrição |
|---|---|---|
| `AuthScreen` | `ui/auth/AuthScreen.kt` | Hub de autenticação: Google, e-mail, continuar sem conta |
| `LoginScreen` | `ui/auth/LoginScreen.kt` | E-mail + senha, link "Esqueci a senha" |
| `SignUpScreen` | `ui/auth/SignUpScreen.kt` | E-mail + senha + confirmação de senha |
| `ProfileSetupScreen` | `ui/auth/ProfileSetupScreen.kt` | Nome, telefone, foto — exibida após o primeiro login |
| `ProfileScreen` | `ui/profile/ProfileScreen.kt` | Ver e editar perfil; botão de logout |

**Acesso ao perfil:** ícone de avatar no cabeçalho de `TripsListScreen` (substitui ou complementa o ícone de engrenagem).

### Formulário de cadastro — campos e validações

| Campo | Obrigatório | Validação |
|---|---|---|
| Nome | Sim | `isNotBlank()`, mínimo 2 caracteres |
| E-mail | Sim (somente e-mail) | regex de e-mail válido |
| Senha | Sim (somente e-mail) | mínimo 8 caracteres, ao menos 1 número |
| Telefone | Não | formato internacional `+XX XX XXXXX-XXXX` |
| Foto de perfil | Não | JPG/PNG, máximo 5 MB; redimensionada para 512×512 antes do upload |

### "Continuar sem conta"

- Usuário acessa todas as funcionalidades locais normalmente
- Tentativa de usar F3 (adicionar pessoas) → bottom sheet explicando que é necessário criar conta → botão "Criar conta" → AuthScreen
- Preferência "continuar sem conta" persistida em DataStore para não mostrar AuthScreen novamente na próxima abertura

### Casos de uso

| # | Cenário | Comportamento esperado |
|---|---|---|
| UC-F2-01 | Primeiro acesso ao app | SplashScreen → AuthScreen |
| UC-F2-02 | Login com Google — conta nova | Firebase cria usuário → ProfileSetupScreen |
| UC-F2-03 | Login com Google — conta existente | Firebase autentica → TripsListScreen direto |
| UC-F2-04 | Login com e-mail/senha incorretos | Snackbar: "E-mail ou senha incorretos" |
| UC-F2-05 | Cadastro com e-mail já em uso | Snackbar: "Este e-mail já possui uma conta" |
| UC-F2-06 | Senha fraca no cadastro | Validação inline antes de enviar ao Firebase |
| UC-F2-07 | "Esqueci a senha" | Firebase envia e-mail de redefinição; toast de confirmação |
| UC-F2-08 | Editar foto de perfil | Seletor câmera/galeria → crop → upload Firebase Storage → atualiza `photoUrl` |
| UC-F2-09 | Logout | Firebase Auth signOut → DataStore limpa "logado" → AuthScreen |
| UC-F2-10 | "Continuar sem conta" | DataStore marca preferência → TripsListScreen sem AuthScreen futuros |
| UC-F2-11 | Sem conexão no login | Snackbar: "Sem conexão. Verifique sua internet." |
| UC-F2-12 | App fechado e reaberto com sessão ativa | SplashScreen → Firebase restaura sessão → fluxo normal sem AuthScreen |

### Especificação de testes

| Teste | Tipo | O que verificar |
|---|---|---|
| `loginWithValidCredentials` | Instrumented / Firebase Emulator | `signInWithEmail()` retorna `FirebaseUser` não-nulo |
| `loginWithInvalidCredentials` | Instrumented / Firebase Emulator | Lança exceção Firebase; ViewModel emite `ShowSnackbar` |
| `signUpCreatesFirestoreProfile` | Instrumented / Firebase Emulator | Após `signUp()`, documento `users/{uid}` existe no Firestore |
| `profileSaveValidation` | JVM | Nome vazio → `isValid = false`; telefone mal-formatado → `isValid = false` |
| `continueWithoutAccountSkipsAuth` | JVM | DataStore com `skipAuth=true` → `AuthRepository.shouldShowAuth() = false` |
| `logoutClearsSession` | JVM (mock) | Após `signOut()`, `currentUser` emite `null` |
| `photoUploadResizesBeforeUpload` | JVM | Bitmap > 512px é redimensionado antes do upload |

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Criar | `data/auth/AuthRepository.kt` |
| Criar | `data/user/UserRepository.kt` |
| Criar | `data/model/UserProfile.kt` |
| Criar | `ui/auth/AuthScreen.kt` + `AuthViewModel.kt` |
| Criar | `ui/auth/LoginScreen.kt` + `LoginViewModel.kt` |
| Criar | `ui/auth/SignUpScreen.kt` + `SignUpViewModel.kt` |
| Criar | `ui/auth/ProfileSetupScreen.kt` + `ProfileSetupViewModel.kt` |
| Criar | `ui/profile/ProfileScreen.kt` + `ProfileViewModel.kt` |
| Modificar | `data/di/AppModule.kt` — `@Provides` para Firebase |
| Modificar | `navigation/AppNavigation.kt` — rotas de auth + lógica de redirect |
| Modificar | `ui/trips/TripsListScreen.kt` — avatar no cabeçalho |
| Modificar | `data/preferences/SettingsRepository.kt` — `skipAuth: Flow<Boolean>` |
| Atualizar | `docs/modulo-11-navegacao.md` |
| Criar | `docs/modulo-12-autenticacao.md` |

---

## F3 — Adicionar pessoas à viagem

### Descrição

Após um usuário criar ou importar uma viagem, ele pode convidar outros usuários (que possuam conta — F2) para participar dela. O criador da viagem é automaticamente o **administrador** e pode conceder permissão de edição ou somente visualização para cada participante.

Esta funcionalidade exige sincronização em nuvem (Firestore) da estrutura de participantes. O conteúdo da viagem em si continua no Room local — a sincronização de *conteúdo* entre participantes é escopo de uma funcionalidade futura (F4, não especificada aqui).

### Modelo de dados

#### Firestore — coleção de participantes

```
trip_participants/
  {tripUuid}/
    participants/
      {uid}/
        userId: String
        displayName: String    (denormalizado — evita query extra)
        photoUrl: String?
        role: String           ("ADMIN" | "EDITOR" | "VIEWER")
        inviteStatus: String   ("PENDING" | "ACCEPTED" | "DECLINED")
        invitedBy: String      (uid do admin que convidou)
        invitedAt: Timestamp
        respondedAt: Timestamp?
```

**Por que Firestore e não Room para participantes?**  
Participantes são dados compartilhados entre dispositivos de usuários diferentes — não cabem em banco local. O Room continua sendo a fonte de verdade para o *conteúdo* da viagem (dias, atividades, etc.).

#### Room — nenhuma mudança de schema

O vínculo entre viagem local e participantes remotos é feito pelo `tripUuid` (adicionado em F1).

#### Modelo de domínio — novo

```kotlin
enum class ParticipantRole { ADMIN, EDITOR, VIEWER }
enum class InviteStatus    { PENDING, ACCEPTED, DECLINED }

data class TripParticipant(
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val role: ParticipantRole,
    val inviteStatus: InviteStatus,
    val invitedBy: String,
    val invitedAt: Long   // timestamp ms
)
```

### Permissões por papel

| Ação | ADMIN | EDITOR | VIEWER |
|---|---|---|---|
| Ver viagem | ✅ | ✅ | ✅ |
| Editar dias, atividades, contatos, vouchers, passagens | ✅ | ✅ | ❌ |
| Adicionar / remover participantes | ✅ | ❌ | ❌ |
| Alterar papel de outro participante | ✅ | ❌ | ❌ |
| Excluir viagem | ✅ | ❌ | ❌ |
| Exportar (`.travel`) | ✅ | ✅ | ✅ |
| Sair da viagem | ❌ (admin não pode sair; deve excluir) | ✅ | ✅ |

> **Nota:** a verificação de permissão na fase atual é feita no cliente (UI oculta ações não permitidas). Uma verificação server-side via Firebase Security Rules deve ser implementada em paralelo.

### Fluxo de convite

```
Admin abre TripParticipantsScreen
  └─ Botão "Adicionar pessoa"
       └─ AddParticipantScreen
            ├─ Campo de busca: e-mail ou nome
            │    └─ UserRepository.searchByEmail(query) → List<UserProfile>
            ├─ Lista de resultados (avatar + nome + e-mail)
            ├─ Seleção de usuário → seletor de papel (Editor / Somente visualização)
            └─ Botão "Convidar"
                 └─ ParticipantRepository.sendInvite(tripUuid, targetUid, role)
                      └─ Cria documento Firestore com inviteStatus = PENDING

Usuário convidado abre o app
  └─ InvitesScreen (badge no cabeçalho de TripsListScreen quando há convites pendentes)
       ├─ Card por convite: "João convidou você para 'Gramado 2026' como Editor"
       ├─ [Aceitar] → inviteStatus = ACCEPTED → viagem aparece em TripsListScreen (importada via tripUuid)
       └─ [Recusar] → inviteStatus = DECLINED → convite removido
```

### Novas telas

| Tela | Arquivo | Descrição |
|---|---|---|
| `TripParticipantsScreen` | `ui/participants/TripParticipantsScreen.kt` | Lista de participantes da viagem com papéis; acessível pela TopAppBar da viagem |
| `AddParticipantScreen` | `ui/participants/AddParticipantScreen.kt` | Busca por e-mail/nome, seleção de papel, envio de convite |
| `InvitesScreen` | `ui/invites/InvitesScreen.kt` | Convites pendentes recebidos pelo usuário logado |

**Acesso a `TripParticipantsScreen`:** ícone de grupo (`Group`) na TopAppBar da viagem aberta, visível apenas para usuários logados.

**Badge de convites:** indicador numérico no ícone de avatar em `TripsListScreen` quando há convites `PENDING`.

### Arquitetura — novas camadas

```
data/participants/
  ParticipantRepository.kt   ← sendInvite(), getParticipants(tripUuid): Flow<List<TripParticipant>>,
                                respondToInvite(inviteId, accept: Boolean),
                                updateRole(tripUuid, uid, newRole),
                                removeParticipant(tripUuid, uid)

data/invites/
  InviteRepository.kt        ← getPendingInvites(uid): Flow<List<TripInvite>>,
                                acceptInvite(inviteId),
                                declineInvite(inviteId)
```

### Integração com permissões na UI

O `TripViewModel` precisa conhecer o papel do usuário logado na viagem aberta para ocultar/mostrar controles de edição:

```kotlin
// TripViewModel
val currentUserRole: StateFlow<ParticipantRole?> =
    participantRepo.getMyRole(tripUuid).stateIn(viewModelScope, Eagerly, null)

// Na UI (AppNavigation / MainPagerScreen)
val canEdit = role == ADMIN || role == EDITOR
val canManageParticipants = role == ADMIN
```

Ações de edição (FABs, swipe para deletar, drag-to-reorder) são ocultadas quando `canEdit = false`.

### Casos de uso

| # | Cenário | Comportamento esperado |
|---|---|---|
| UC-F3-01 | Usuário sem conta tenta adicionar participante | Bottom sheet: "Crie uma conta para convidar pessoas" |
| UC-F3-02 | Admin busca por e-mail que não tem conta | Resultado vazio: "Nenhum usuário encontrado com este e-mail" |
| UC-F3-03 | Admin convida usuário existente como Editor | Documento Firestore criado com `PENDING`; usuário recebe notificação (push — F futura) |
| UC-F3-04 | Admin tenta convidar usuário já participante | Snackbar: "Esta pessoa já participa da viagem" |
| UC-F3-05 | Admin tenta convidar a si mesmo | Campo de busca oculta o próprio usuário nos resultados |
| UC-F3-06 | Usuário convidado aceita convite | `inviteStatus = ACCEPTED`; viagem aparece em `TripsListScreen` do convidado (marcada como compartilhada) |
| UC-F3-07 | Usuário convidado recusa convite | `inviteStatus = DECLINED`; convite desaparece de `InvitesScreen` |
| UC-F3-08 | Admin promove Editor para Admin | Papel atualizado no Firestore; UI do promovido reflete novas permissões |
| UC-F3-09 | Admin remove um participante | Documento do participante deletado; viagem some do `TripsListScreen` do removido |
| UC-F3-10 | Editor tenta remover um participante | Ação não disponível na UI; requisição bloqueada por Firebase Security Rules |
| UC-F3-11 | Viewer tenta editar uma atividade | Botão de editar oculto; FAB oculto |
| UC-F3-12 | Admin exclui a viagem | Todos os participantes perdem acesso; documentos Firestore limpos; trip deletada do Room |
| UC-F3-13 | Editor/Viewer sai da viagem | Documento do participante deletado; viagem removida do `TripsListScreen` local |
| UC-F3-14 | Sem conexão ao aceitar convite | Snackbar: "Sem conexão. O convite será aceito quando você reconectar." (operação enfileirada) |

### Especificação de testes

| Teste | Tipo | O que verificar |
|---|---|---|
| `sendInviteCreatesFirestoreDocument` | Instrumented / Emulator | Documento existe em `trip_participants/{uuid}/participants/{uid}` com `PENDING` |
| `acceptInviteUpdatesStatus` | Instrumented / Emulator | `inviteStatus` passa para `ACCEPTED` após `acceptInvite()` |
| `declineInviteUpdatesStatus` | Instrumented / Emulator | `inviteStatus` passa para `DECLINED` |
| `viewerCannotEdit` | JVM | `currentUserRole = VIEWER` → `canEdit = false` |
| `editorCanEdit` | JVM | `currentUserRole = EDITOR` → `canEdit = true` |
| `onlyAdminCanManageParticipants` | JVM | `currentUserRole = EDITOR` → `canManageParticipants = false` |
| `inviteSelfNotAllowed` | JVM | `searchByEmail` filtra o próprio `uid` dos resultados |
| `duplicateInviteBlocked` | JVM (mock) | Tentar convidar uid já participante → `DuplicateParticipantException` |
| `removeParticipantDeletesDocument` | Instrumented / Emulator | Documento Firestore deletado após `removeParticipant()` |

### Firebase Security Rules (esboço)

```javascript
// trip_participants/{tripUuid}/participants/{uid}
match /trip_participants/{tripUuid}/participants/{participantId} {
  // Leitura: apenas quem é participante da viagem
  allow read: if isParticipant(tripUuid);

  // Criar convite: apenas admin da viagem
  allow create: if isAdmin(tripUuid);

  // Atualizar próprio status (aceitar/recusar): apenas o próprio convidado
  allow update: if request.auth.uid == participantId
                && onlyUpdating(['inviteStatus', 'respondedAt']);

  // Atualizar papel de outro: apenas admin
  allow update: if isAdmin(tripUuid) && request.auth.uid != participantId;

  // Remover: admin pode remover qualquer um; participante pode remover a si mesmo
  allow delete: if isAdmin(tripUuid) || request.auth.uid == participantId;
}
```

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Criar | `data/participants/ParticipantRepository.kt` |
| Criar | `data/invites/InviteRepository.kt` |
| Criar | `data/model/TripParticipant.kt` |
| Criar | `ui/participants/TripParticipantsScreen.kt` + `TripParticipantsViewModel.kt` |
| Criar | `ui/participants/AddParticipantScreen.kt` + `AddParticipantViewModel.kt` |
| Criar | `ui/invites/InvitesScreen.kt` + `InvitesViewModel.kt` |
| Modificar | `ui/trips/TripViewModel.kt` — `currentUserRole`, `canEdit`, `canManageParticipants` |
| Modificar | `navigation/AppNavigation.kt` — rotas de participantes e convites |
| Modificar | `ui/trips/TripsListScreen.kt` — badge de convites pendentes no avatar |
| Modificar | `data/di/AppModule.kt` — `@Provides` para novos repos |
| Criar | `docs/modulo-13-participantes.md` |

---

---

## F4 — Tela de notas

> **Status: ✅ Implementada.** Entidades `notes`/`note_blocks`/`checklist_items` (Migration 18), `NoteRepository`, editor completo (`NoteEditorScreen` — blocos texto/checklist/título, drag-to-reorder, toolbar sobre o teclado, foco automático), lista com swipe-delete e reorder, aba "Notas" no pager + notas de dia no `DayDetailScreen`, e export/import no `.travel` (schema v3). Ver `docs/modulo-15-notas.md`. Notas de implementação que divergem desta spec: notas usam `load+refresh` (não `Flow`); as notas gerais são geridas pelo `TripViewModel` (não entram no `TripData`); o doc do módulo é `modulo-15-notas.md` (o `14` já era categorias de contato); o contador "X notas" no card do dia ficou como refinamento. O teste de UI Compose do editor ficou pendente (emulador API 37 incompatível com o toolchain Compose).

### Descrição

Uma área de notas livre dentro de cada viagem, inspirada na organização do Notion: o usuário pode criar blocos de conteúdo variado — texto livre, listas de tarefas (checklists), separadores — tanto em nível geral da viagem quanto vinculadas a um dia específico. O objetivo é centralizar no app tudo que hoje vive em papel, WhatsApp ou outros apps (packing list, coisas para não esquecer, ideias de restaurantes, anotações em viagem).

As notas são locais (Room), ordenadas manualmente, e exportadas junto com a viagem no arquivo `.travel`.

### Tipos de nota

| Tipo | `NoteBlockType` | Descrição |
|---|---|---|
| Texto livre | `TEXT` | Parágrafo editável de texto corrido |
| Checklist | `CHECKLIST` | Lista de itens com checkbox; cada item tem texto + estado marcado/desmarcado |
| Título de seção | `HEADING` | Linha de destaque visual para separar blocos (não é editável como título, só o texto) |

> Escopo inicial deliberadamente simples. Formatação inline (negrito, itálico), imagens e links são escopo de versão futura.

### Acesso e escopo

- **Nota geral da viagem:** acessada pela aba "Notas" no `MainPagerScreen` (nova aba ao lado de Home, Dias, Contatos, etc.)
- **Nota de um dia específico:** acessada por botão "Notas do dia" em `DayDetailScreen`, ou pela tela de edição do dia

O usuário pode criar quantas notas quiser em cada escopo. A ordem é manual (drag-to-reorder) e persistida em `sortOrder`.

### Modelo de dados

#### Room — novas tabelas

**Migration N → N+1** (número exato depende da ordem de implementação: se F1 for implementada antes, o banco estará na versão 17; ajustar conforme o estado do banco no momento da implementação):

```sql
-- Nota: container de blocos
CREATE TABLE IF NOT EXISTS notes (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    trip_id     INTEGER NOT NULL,
    day_id      INTEGER,           -- NULL = nota geral da viagem
    title       TEXT NOT NULL DEFAULT '',
    sort_order  INTEGER NOT NULL DEFAULT 0,
    created_at  INTEGER NOT NULL,
    updated_at  INTEGER NOT NULL,
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE
);

-- Bloco de conteúdo dentro de uma nota
CREATE TABLE IF NOT EXISTS note_blocks (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    note_id     INTEGER NOT NULL,
    type        TEXT NOT NULL,     -- 'TEXT' | 'CHECKLIST' | 'HEADING'
    content     TEXT NOT NULL DEFAULT '',
    sort_order  INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
);

-- Item de checklist (filho de um bloco CHECKLIST)
CREATE TABLE IF NOT EXISTS checklist_items (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    block_id    INTEGER NOT NULL,
    text        TEXT NOT NULL DEFAULT '',
    is_checked  INTEGER NOT NULL DEFAULT 0,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (block_id) REFERENCES note_blocks(id) ON DELETE CASCADE
);
```

#### Modelo de domínio

```kotlin
data class Note(
    val id: Int,
    val tripId: Long,
    val dayId: Int?,       // null = geral da viagem
    val title: String,
    val blocks: List<NoteBlock>,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long
)

sealed class NoteBlock {
    abstract val id: Int
    abstract val sortOrder: Int

    data class TextBlock(
        override val id: Int,
        val content: String,
        override val sortOrder: Int
    ) : NoteBlock()

    data class ChecklistBlock(
        override val id: Int,
        val items: List<ChecklistItem>,
        override val sortOrder: Int
    ) : NoteBlock()

    data class HeadingBlock(
        override val id: Int,
        val content: String,
        override val sortOrder: Int
    ) : NoteBlock()
}

data class ChecklistItem(
    val id: Int,
    val text: String,
    val isChecked: Boolean,
    val sortOrder: Int
)
```

### Arquitetura

```
data/repository/
  NoteRepository.kt     ← getNotes(tripId, dayId?): Flow<List<Note>>,
                           createNote(tripId, dayId?): Long,
                           updateNoteTitle(id, title),
                           deleteNote(id),
                           reorderNotes(tripId, dayId?, ids: List<Int>),
                           upsertBlock(noteId, block),
                           deleteBlock(id),
                           reorderBlocks(noteId, ids: List<Int>),
                           toggleChecklistItem(itemId),
                           upsertChecklistItem(blockId, item),
                           deleteChecklistItem(id),
                           reorderChecklistItems(blockId, ids: List<Int>)

ui/notes/
  NotesListScreen.kt         ← lista de notas da viagem ou do dia; FAB para criar
  NotesListViewModel.kt
  NoteEditorScreen.kt        ← editor de blocos com toolbar de inserção
  NoteEditorViewModel.kt
```

### UI — NotesListScreen

```
TopAppBar: "Notas" (geral) ou "Notas — [Nome do Dia]"
  └─ FAB (+) → cria nota vazia e abre NoteEditorScreen

Lista de notas (LazyColumn):
  ├─ Card por nota
  │    ├─ Título da nota (editável inline ou via editor)
  │    ├─ Preview: primeiros 2 blocos resumidos (texto truncado / "X itens, Y marcados")
  │    ├─ Data de última edição
  │    └─ Swipe esquerdo: deletar com confirmação
  └─ Drag-to-reorder por long press (mesmo padrão de vouchers e contatos)
```

### UI — NoteEditorScreen

```
TopAppBar: título da nota (campo editável no topo) + botão Voltar + botão "..." (opções)

Área de blocos (LazyColumn editável):
  ├─ Cada bloco renderizado conforme tipo:
  │    ├─ TextBlock: TextField multilinha sem borda, placeholder "Escreva algo..."
  │    ├─ ChecklistBlock: lista de itens com Checkbox + TextField; botão "+" para novo item
  │    └─ HeadingBlock: TextField com texto grande/bold, placeholder "Título da seção"
  └─ Drag-to-reorder entre blocos por handle (ícone ⠿ à esquerda)

Toolbar de inserção (fixada no rodapé, acima do teclado):
  [Aa Texto]  [☑ Checklist]  [H Título]  [🗑 Excluir bloco]

Comportamento do teclado:
  - Toolbar sobe junto com o teclado (WindowInsets.ime)
  - Foco navega automaticamente para o bloco recém-adicionado
```

### Integração com export/import (`.travel`)

Notas exportadas como array `notes` no `trip.json`:

```json
"notes": [
  {
    "id": 1,
    "dayId": null,
    "title": "Packing list",
    "sortOrder": 0,
    "createdAt": 1234567890000,
    "updatedAt": 1234567890000,
    "blocks": [
      {
        "type": "CHECKLIST",
        "sortOrder": 0,
        "items": [
          { "text": "Passaporte", "isChecked": true, "sortOrder": 0 },
          { "text": "Carregador", "isChecked": false, "sortOrder": 1 }
        ]
      }
    ]
  }
]
```

### Casos de uso

| # | Cenário | Comportamento esperado |
|---|---|---|
| UC-F4-01 | Criar nota geral da viagem | FAB na aba Notas → nota vazia criada → abre NoteEditorScreen com foco no título |
| UC-F4-02 | Criar nota de um dia | Botão "Notas do dia" em DayDetailScreen → NotesListScreen filtrado → FAB → editor |
| UC-F4-03 | Adicionar bloco de texto | Toolbar → "Aa Texto" → TextBlock adicionado ao final da nota; foco automático |
| UC-F4-04 | Adicionar checklist | Toolbar → "☑ Checklist" → ChecklistBlock com 1 item vazio; foco no item |
| UC-F4-05 | Marcar item de checklist | Tap no Checkbox → `isChecked` togglado; item visualmente riscado |
| UC-F4-06 | Reordenar blocos | Long press no handle ⠿ → drag → soltar → nova ordem salva |
| UC-F4-07 | Reordenar notas na lista | Long press no card → drag → soltar → `sortOrder` atualizado |
| UC-F4-08 | Deletar nota | Swipe esquerdo → dialog de confirmação → nota e todos os blocos/itens deletados |
| UC-F4-09 | Deletar bloco | Foco no bloco → ícone 🗑 na toolbar → bloco removido; nota permanece |
| UC-F4-10 | Exportar viagem com notas | Notas incluídas no `trip.json`; importar em outro dispositivo restaura tudo |
| UC-F4-11 | Nota de dia em DayDetailScreen | Preview do número de notas exibido no card do dia ("3 notas") |
| UC-F4-12 | Viagem sem notas | Lista vazia com ilustração e texto "Nenhuma anotação ainda" + hint do FAB |

### Especificação de testes

| Teste | Tipo | O que verificar |
|---|---|---|
| `createNoteReturnsId` | JVM | `NoteRepository.createNote()` retorna id > 0; nota existe no banco |
| `deleteNoteCascadesBlocks` | JVM | Deletar nota → todos `note_blocks` e `checklist_items` com `note_id` são removidos |
| `toggleChecklistItemPersists` | JVM | `toggleChecklistItem(id)` inverte `is_checked` e persiste |
| `reorderNotesUpdatesSortOrder` | JVM | Lista reordenada → `sortOrder` de cada nota reflete nova posição |
| `dayNoteFilteredByDayId` | JVM | `getNotes(tripId, dayId=3)` retorna apenas notas com `day_id = 3` |
| `generalNoteFilteredByNullDayId` | JVM | `getNotes(tripId, dayId=null)` retorna apenas notas com `day_id IS NULL` |
| `exportIncludesNotes` | JVM | `TravelExporter.export()` produz JSON com array `notes` não-vazio |
| `importRestoresNotes` | JVM | `TravelImporter.import()` recria notas, blocos e itens no banco |

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Modificar | `data/db/TravelDatabase.kt` — novas entidades + migration |
| Criar | `data/db/entity/NoteEntity.kt`, `NoteBlockEntity.kt`, `ChecklistItemEntity.kt` |
| Criar | `data/db/dao/NoteDao.kt` |
| Modificar | `data/db/Mappers.kt` — mappers para Note, NoteBlock, ChecklistItem |
| Criar | `data/model/Note.kt` (NoteBlock sealed class + ChecklistItem) |
| Criar | `data/repository/NoteRepository.kt` |
| Criar | `ui/notes/NotesListScreen.kt` + `NotesListViewModel.kt` |
| Criar | `ui/notes/NoteEditorScreen.kt` + `NoteEditorViewModel.kt` |
| Modificar | `navigation/AppNavigation.kt` — rotas de notas |
| Modificar | `ui/home/MainPagerScreen.kt` — nova aba "Notas" |
| Modificar | `ui/day/DayDetailScreen.kt` — botão "Notas do dia" + preview de quantidade |
| Modificar | `data/export/TravelExporter.kt` — serializar notas |
| Modificar | `data/import/TravelImporter.kt` — deserializar e salvar notas |
| Atualizar | `docs/travel-export-schema.md` |
| Criar | `docs/modulo-15-notas.md` (o `14` já era categorias de contato) |

---

## F5 — Controle de orçamento de viagem

### Descrição

O usuário define um orçamento total para a viagem e vai registrando os gastos ao longo dos dias. O app categoriza automaticamente as despesas, calcula o saldo restante, exibe gráficos de distribuição por categoria e alerta quando o orçamento estiver próximo do limite. O controle é por viagem, em moeda configurável.

Funcionalidade totalmente local (Room), sem necessidade de backend.

### Modelo de dados

#### Room — novas tabelas

```sql
-- Orçamento da viagem (1 por viagem)
ALTER TABLE trips ADD COLUMN budget_total REAL NOT NULL DEFAULT 0;
ALTER TABLE trips ADD COLUMN budget_currency TEXT NOT NULL DEFAULT 'BRL';

-- Registro de gasto
CREATE TABLE IF NOT EXISTS expenses (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    trip_id         INTEGER NOT NULL,
    day_id          INTEGER,               -- NULL = gasto sem dia específico
    amount          REAL NOT NULL,
    description     TEXT NOT NULL DEFAULT '',
    category        TEXT NOT NULL,         -- ver enum ExpenseCategory
    paid_by         TEXT NOT NULL DEFAULT '',   -- nome livre (ou uid futuramente)
    notes           TEXT NOT NULL DEFAULT '',
    created_at      INTEGER NOT NULL,
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE
);
```

**Migration:** incrementar versão do banco ao implementar — ajustar número conforme quantas migrations já foram aplicadas (F1, F4 e F6 também adicionam migrations). Verificar a versão atual em `TravelDatabase.kt` antes de implementar.

#### Categorias de gasto

```kotlin
enum class ExpenseCategory {
    ACCOMMODATION,   // Hospedagem
    FOOD,            // Alimentação
    TRANSPORT,       // Transporte
    ACTIVITIES,      // Passeios e ingressos
    SHOPPING,        // Compras e souvenirs
    HEALTH,          // Saúde e farmácia
    OTHER            // Outros
}
```

Cada categoria tem: emoji de exibição, cor associada (usada nos gráficos e cards), e label em português.

#### Modelo de domínio

```kotlin
data class Expense(
    val id: Int,
    val tripId: Long,
    val dayId: Int?,
    val amount: Double,
    val description: String,
    val category: ExpenseCategory,
    val paidBy: String,
    val notes: String,
    val createdAt: Long
)

data class BudgetSummary(
    val totalBudget: Double,
    val totalSpent: Double,
    val remaining: Double,                        // totalBudget - totalSpent
    val percentUsed: Float,                       // (totalSpent / totalBudget) * 100
    val byCategory: Map<ExpenseCategory, Double>, // soma por categoria
    val byDay: Map<Int, Double>,                  // soma por dayId
    val currency: String                          // ex: "BRL", "USD", "EUR"
)
```

### Arquitetura

```
data/repository/
  BudgetRepository.kt    ← setBudget(tripId, total, currency),
                            getBudgetSummary(tripId): Flow<BudgetSummary>,
                            getExpenses(tripId, dayId?): Flow<List<Expense>>,
                            addExpense(tripId, expense),
                            updateExpense(expense),
                            deleteExpense(id)

ui/budget/
  BudgetScreen.kt           ← visão geral do orçamento: resumo + gráfico + lista de gastos
  BudgetViewModel.kt
  AddExpenseScreen.kt       ← formulário de novo gasto
  AddExpenseViewModel.kt
  EditExpenseScreen.kt      ← editar gasto existente (mesmo formulário)
```

### UI — BudgetScreen

```
TopAppBar: "Orçamento" + ícone de editar orçamento total

Seção de resumo (card fixo no topo):
  ┌────────────────────────────────────────────┐
  │  Orçamento: R$ 5.000,00                    │
  │  ████████████░░░░░░░░  68% utilizado        │
  │  Gasto: R$ 3.400,00   Restante: R$ 1.600,00│
  └────────────────────────────────────────────┘

Gráfico de pizza por categoria (horizontal, com legenda):
  🏨 Hospedagem  R$ 1.200  35%
  🍽 Alimentação R$   800  24%
  🎡 Passeios    R$   600  18%
  ...

Filtro de período:  [Todos os dias ▾]  [Por categoria ▾]

Lista de gastos (LazyColumn, agrupados por dia):
  "Dia 1 — Sex, 09 Jun"        Total: R$ 420,00
  ├─ Card: 🍽 Almoço no Lago   R$ 180,00   [editar] [deletar]
  └─ Card: 🎡 Bondinho         R$ 240,00   [editar] [deletar]

FAB (+) → AddExpenseScreen
```

**Alerta de orçamento:** quando `percentUsed >= 80%`, o card de resumo exibe faixa amarela com "⚠ 80% do orçamento utilizado". Quando `percentUsed >= 100%`, faixa vermelha com "⛔ Orçamento excedido".

### UI — AddExpenseScreen / EditExpenseScreen

```
Scaffold (TopAppBar "Novo gasto" / "Editar gasto" + botão Voltar)
  └─ Column
       ├─ Campo: Valor (teclado numérico, aceita vírgula decimal)
       ├─ Campo: Descrição (ex: "Almoço no Café Colonial")
       ├─ Seletor: Categoria (chips horizontais com emoji + label)
       ├─ Seletor: Dia da viagem (dropdown; "Sem dia específico" como opção)
       ├─ Campo: Pago por (texto livre; padrão = nome do usuário logado se F2 disponível)
       ├─ Campo: Observações (multilinha, opcional)
       └─ Botão "Salvar" (GreenMoss + AmberPrimary)
```

### Acesso à tela de orçamento

- Aba "Orçamento" em `MainPagerScreen` (nova aba, ao lado de Notas)
- Card de resumo rápido em `HomeScreen`: "R$ 3.400 de R$ 5.000 gastos — 68%", com link "Ver detalhes"

### Integração com export/import (`.travel`)

Gastos exportados como array `expenses` e `budget` no `trip.json`:

```json
"budget": {
  "total": 5000.00,
  "currency": "BRL"
},
"expenses": [
  {
    "dayId": 1,
    "amount": 180.00,
    "description": "Almoço no Lago",
    "category": "FOOD",
    "paidBy": "Rodrigo",
    "notes": "",
    "createdAt": 1234567890000
  }
]
```

### Casos de uso

| # | Cenário | Comportamento esperado |
|---|---|---|
| UC-F5-01 | Configurar orçamento pela primeira vez | Botão "Definir orçamento" em BudgetScreen → campo de valor + seletor de moeda → salva na tabela `trips` |
| UC-F5-02 | Editar orçamento | Ícone de editar no cabeçalho → mesmo formulário preenchido → salva; `BudgetSummary` recalculado |
| UC-F5-03 | Adicionar gasto | FAB → preencher formulário → salvar → gasto aparece na lista agrupado pelo dia |
| UC-F5-04 | Gasto sem dia associado | `dayId = null` → aparece no grupo "Sem dia específico" no rodapé da lista |
| UC-F5-05 | Editar gasto | Ícone de editar no card → EditExpenseScreen preenchido → salvar → lista atualizada |
| UC-F5-06 | Deletar gasto | Botão de deletar no card → confirmação → gasto removido; totais recalculados |
| UC-F5-07 | Orçamento atingindo 80% | Faixa de alerta amarela no card de resumo |
| UC-F5-08 | Orçamento excedido | Faixa de alerta vermelha; `remaining` exibido como negativo em vermelho |
| UC-F5-09 | Filtrar por categoria | Seletor de categoria → lista filtrada; gráfico de pizza não muda (exibe sempre o total) |
| UC-F5-10 | Viagem sem orçamento definido | BudgetScreen mostra estado vazio: "Defina um orçamento para começar" |
| UC-F5-11 | Exportar com gastos | `trip.json` inclui `budget` e `expenses`; importar em outro dispositivo restaura tudo |
| UC-F5-12 | Card resumo em HomeScreen | Exibido somente se `budgetTotal > 0`; clique navega para BudgetScreen |

### Especificação de testes

| Teste | Tipo | O que verificar |
|---|---|---|
| `budgetSummaryCalculatedCorrectly` | JVM | `remaining = total - spent`; `percentUsed` correto com 3 gastos de categorias diferentes |
| `byCategoryAggregatesCorrectly` | JVM | 2 gastos FOOD + 1 TRANSPORT → `byCategory[FOOD]` = soma dos dois |
| `addExpenseUpdatesSummaryFlow` | JVM | Inserir expense → `getBudgetSummary()` Flow emite novo valor |
| `deleteExpenseUpdatesSummaryFlow` | JVM | Deletar expense → `getBudgetSummary()` Flow emite valor reduzido |
| `expenseWithoutDayIdGroupedCorrectly` | JVM | `getExpenses(tripId, dayId=null)` retorna só gastos sem dia |
| `budgetAlertThresholdAt80` | JVM | `percentUsed = 80f` → `BudgetSummary` com flag `isWarning = true` |
| `budgetAlertThresholdAt100` | JVM | `percentUsed >= 100f` → `BudgetSummary` com flag `isExceeded = true` |
| `exportIncludesBudgetAndExpenses` | JVM | JSON exportado tem `budget.total` e array `expenses` com itens corretos |

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Modificar | `data/db/TravelDatabase.kt` — nova tabela `expenses` + migration |
| Modificar | `data/db/entity/TripEntity.kt` — campos `budgetTotal`, `budgetCurrency` |
| Criar | `data/db/entity/ExpenseEntity.kt` |
| Criar | `data/db/dao/ExpenseDao.kt` |
| Modificar | `data/db/Mappers.kt` — mappers para Expense; Trip incluindo budget |
| Criar | `data/model/Expense.kt`, `BudgetSummary.kt`, `ExpenseCategory.kt` |
| Criar | `data/repository/BudgetRepository.kt` |
| Criar | `ui/budget/BudgetScreen.kt` + `BudgetViewModel.kt` |
| Criar | `ui/budget/AddExpenseScreen.kt` + `AddExpenseViewModel.kt` |
| Modificar | `navigation/AppNavigation.kt` — rotas de orçamento |
| Modificar | `ui/home/MainPagerScreen.kt` — nova aba "Orçamento" |
| Modificar | `ui/home/HomeScreen.kt` — card de resumo rápido do orçamento |
| Modificar | `data/export/TravelExporter.kt` — serializar budget e expenses |
| Modificar | `data/import/TravelImporter.kt` — deserializar e salvar budget e expenses |
| Atualizar | `docs/travel-export-schema.md` |
| Criar | `docs/modulo-15-orcamento.md` |

### Dependências a adicionar

```kotlin
// Gráficos Compose-nativos — Vico (Apache 2.0, gratuito)
implementation("com.patrykandpatrick.vico:compose-m3:2.x.x")
```

> **💰 Custo:** Vico é open source (Apache 2.0) — **gratuito**. Alternativa nativa seria desenhar o gráfico de pizza com `Canvas` do Compose (zero dependência, mas mais trabalhoso). Evitar MPAndroidChart em novos projetos Compose — não tem suporte nativo.

---

## F6 — Divisão de custos

### Descrição

Calculadora integrada ao app para dividir despesas entre pessoas — seja do grupo de viagem (vinculado a F3) ou por uma lista de nomes definida pelo próprio usuário, sem necessidade de conta. Inspirado no Splitwise, mas focado no contexto de viagem: o usuário registra quem pagou, quanto, e para quem a despesa se aplica; o app calcula os saldos e sugere os repasses mais simples possíveis para quitar as dívidas.

**Dois modos de uso:**
- **Modo local (sem F3):** o usuário digita os nomes das pessoas manualmente. Funciona sem conta.
- **Modo conectado (com F3):** os participantes da viagem são importados automaticamente; os saldos podem ser exibidos a cada participante quando F3 estiver implementado.

### Modelo de dados

#### Room — novas tabelas

```sql
-- Grupo de divisão (1 por viagem, criado na primeira vez que a tela é aberta)
CREATE TABLE IF NOT EXISTS split_groups (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    trip_id     INTEGER NOT NULL UNIQUE,
    currency    TEXT NOT NULL DEFAULT 'BRL',
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE
);

-- Pessoa participante da divisão (nome livre ou vinculado a uid)
CREATE TABLE IF NOT EXISTS split_members (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    group_id    INTEGER NOT NULL,
    name        TEXT NOT NULL,
    user_uid    TEXT,          -- NULL se modo local; uid do participante se modo F3
    FOREIGN KEY (group_id) REFERENCES split_groups(id) ON DELETE CASCADE
);

-- Despesa compartilhada
CREATE TABLE IF NOT EXISTS split_expenses (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    group_id        INTEGER NOT NULL,
    description     TEXT NOT NULL,
    amount          REAL NOT NULL,
    paid_by_id      INTEGER NOT NULL,  -- FK para split_members.id
    created_at      INTEGER NOT NULL,
    FOREIGN KEY (group_id)    REFERENCES split_groups(id)   ON DELETE CASCADE,
    FOREIGN KEY (paid_by_id)  REFERENCES split_members(id)  ON DELETE CASCADE
);

-- Participação de cada membro em uma despesa (quem deve pagar e quanto)
CREATE TABLE IF NOT EXISTS split_shares (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    expense_id      INTEGER NOT NULL,
    member_id       INTEGER NOT NULL,
    share_amount    REAL NOT NULL,     -- valor que este membro deve ao pagador
    FOREIGN KEY (expense_id) REFERENCES split_expenses(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id)  REFERENCES split_members(id)  ON DELETE CASCADE
);
```

#### Modelo de domínio

```kotlin
data class SplitMember(
    val id: Int,
    val name: String,
    val userUid: String?   // null = modo local
)

data class SplitExpense(
    val id: Int,
    val description: String,
    val amount: Double,
    val paidBy: SplitMember,
    val shares: List<SplitShare>,
    val createdAt: Long
)

data class SplitShare(
    val member: SplitMember,
    val shareAmount: Double
)

data class MemberBalance(
    val member: SplitMember,
    val totalPaid: Double,     // soma do que este membro pagou
    val totalOwes: Double,     // soma do que deve aos outros
    val net: Double            // totalPaid - totalOwes (positivo = a receber; negativo = a pagar)
)

data class SplitSettlement(
    val from: SplitMember,    // quem deve pagar
    val to: SplitMember,      // quem deve receber
    val amount: Double
)
```

### Algoritmo de liquidação

O algoritmo minimiza o número de transações necessárias para quitar todos os débitos:

```
1. Calcular net de cada membro: totalPaid - totalOwes
2. Separar em dois grupos: credores (net > 0) e devedores (net < 0)
3. Ordenar ambos por valor absoluto decrescente
4. Iterar: maior devedor → maior credor, transferindo o mínimo entre os dois saldos
5. Repetir até todos os saldos = 0
```

Esse algoritmo produz no máximo `n - 1` transações para `n` pessoas, geralmente menos.

### Modos de divisão por despesa

| Modo | Descrição |
|---|---|
| **Igualitário** | Valor dividido em partes iguais entre todos os membros selecionados |
| **Proporcional (%)** | Cada membro recebe uma porcentagem; soma deve = 100% |
| **Valores exatos** | Cada membro recebe um valor fixo; soma deve = total da despesa |
| **Apenas alguns** | Divisão igualitária, mas só entre membros selecionados (não todos) |

### Arquitetura

```
data/repository/
  SplitRepository.kt    ← getOrCreateGroup(tripId): SplitGroup,
                           getMembers(groupId): Flow<List<SplitMember>>,
                           addMember(groupId, name, uid?),
                           removeMember(id),
                           getExpenses(groupId): Flow<List<SplitExpense>>,
                           addExpense(groupId, expense, shares),
                           deleteExpense(id),
                           calculateBalances(groupId): Flow<List<MemberBalance>>,
                           calculateSettlements(groupId): Flow<List<SplitSettlement>>

ui/split/
  SplitScreen.kt              ← hub principal: membros, saldos, botão de adicionar despesa
  SplitViewModel.kt
  AddSplitExpenseScreen.kt    ← formulário: valor, quem pagou, modo de divisão, participantes
  AddSplitExpenseViewModel.kt
  SplitSettlementsSheet.kt    ← ModalBottomSheet com a lista de repasses sugeridos
```

### UI — SplitScreen

```
TopAppBar: "Divisão de custos"

Seção "Pessoas":
  ├─ Chips horizontais (LazyRow) com nome de cada membro
  ├─ Chip "+" para adicionar membro (dialog com campo de nome)
  └─ Long press em chip existente → dialog "Renomear / Remover"

Seção "Saldos":
  ├─ Card por membro:
  │    ├─ Nome + avatar (inicial ou foto se F2)
  │    ├─ "Pagou: R$ 300"  "Deve: R$ 120"
  │    └─ Net: "+R$ 180 a receber" (verde) ou "-R$ 40 a pagar" (vermelho)
  └─ Botão "Ver repasses sugeridos" → SplitSettlementsSheet

Seção "Despesas":
  └─ Lista de despesas (LazyColumn):
       Card: "🍽 Jantar no Bela Vista — R$ 360"
             "Pago por: Ana | Dividido entre: todos"
             Swipe para deletar

FAB (+) → AddSplitExpenseScreen
```

### UI — AddSplitExpenseScreen

```
Campo: Descrição
Campo: Valor total (teclado numérico)
Seletor: "Pago por" (dropdown com membros)
Seletor: Modo de divisão (chips: Igualitário / Proporcional / Valores exatos)
Participantes: checkboxes com nome de cada membro (todos marcados por padrão)

[Se Proporcional]: campo de % por membro (total exibido em tempo real)
[Se Valores exatos]: campo de R$ por membro (total exibido em tempo real)

Botão "Salvar" — habilitado somente quando soma bate com o total
```

### UI — SplitSettlementsSheet (ModalBottomSheet)

```
"Repasses sugeridos para quitar tudo"

Card por repasse:
  "Ana deve pagar R$ 40,00 para Rodrigo"
  [Copiar]  [WhatsApp]

Botão "Fechar"
```

O botão WhatsApp monta a mensagem: *"Oi Rodrigo! Você pode me pagar R$ 40,00 referente à divisão de custos da nossa viagem? 😊"* e abre `https://wa.me/<número>` se o membro tiver telefone cadastrado (modo F3).

### Integração com F5 (orçamento)

Opção em `AddSplitExpenseScreen`: "Registrar também no orçamento" — ao salvar, cria automaticamente um registro em `expenses` com o mesmo valor, descrição e categoria selecionada. Evita duplicar o trabalho de lançar o gasto em dois lugares.

### Acesso à tela

- Nova aba "Divisão" em `MainPagerScreen` (ou ícone na TopAppBar da viagem)
- Atalho no card de resumo de orçamento em `HomeScreen`: "Dividir custos →"

### Casos de uso

| # | Cenário | Comportamento esperado |
|---|---|---|
| UC-F6-01 | Abrir divisão pela primeira vez | Tela vazia com prompt "Adicione as pessoas do grupo" |
| UC-F6-02 | Adicionar membros manualmente | Dialog com campo de nome → chip adicionado; sem necessidade de conta |
| UC-F6-03 | Importar participantes de F3 | Botão "Importar participantes da viagem" → membros criados com `userUid` preenchido |
| UC-F6-04 | Adicionar despesa igualitária entre todos | R$ 300 entre 3 pessoas → R$ 100 de share por pessoa |
| UC-F6-05 | Adicionar despesa só para alguns membros | Desmarcar 1 pessoa → divisão igualitária entre os selecionados |
| UC-F6-06 | Divisão por valores exatos com soma errada | Botão "Salvar" desabilitado; alerta "Soma dos valores (R$ 280) ≠ total (R$ 300)" |
| UC-F6-07 | Ver saldos após 3 despesas | Cada membro com net calculado corretamente |
| UC-F6-08 | Ver repasses sugeridos | ModalBottomSheet com mínimo de transações para quitar tudo |
| UC-F6-09 | Compartilhar repasse via WhatsApp | Abre WhatsApp com mensagem pré-formatada se número disponível |
| UC-F6-10 | Deletar despesa | Swipe → confirmação → despesa removida; saldos recalculados |
| UC-F6-11 | Integração com orçamento | Checkbox "Registrar no orçamento" → gasto salvo também em `expenses` |
| UC-F6-12 | Todos os saldos zerados | Seção de saldos exibe "Está tudo quitado! 🎉" |

### Especificação de testes

| Teste | Tipo | O que verificar |
|---|---|---|
| `equalSplitCalculatesCorrectly` | JVM | R$ 300 / 3 pessoas → `shareAmount = 100.0` cada |
| `proportionalSplitCalculatesCorrectly` | JVM | 50% + 30% + 20% de R$ 200 → R$ 100, R$ 60, R$ 40 |
| `exactSplitValidatesSum` | JVM | Shares somando R$ 280 quando total é R$ 300 → `isValid = false` |
| `balanceCalculationCorrect` | JVM | 3 despesas com pagadores diferentes → net de cada membro correto |
| `settlementAlgorithmMinimizesTransactions` | JVM | 4 pessoas com saldos variados → no máximo 3 transações |
| `settlementAmountsZeroAllBalances` | JVM | Aplicar todos os repasses → todos os nets = 0 |
| `deleteExpenseUpdatesBalances` | JVM | Remover despesa → Flow de saldos emite novo cálculo |
| `addMemberAppearsInExpenseForm` | JVM | `addMember()` → `getMembers()` retorna lista atualizada |

### Arquivos a criar/modificar (F6)

| Ação | Arquivo |
|---|---|
| Modificar | `data/db/TravelDatabase.kt` — novas tabelas + migration |
| Criar | `data/db/entity/SplitGroupEntity.kt`, `SplitMemberEntity.kt`, `SplitExpenseEntity.kt`, `SplitShareEntity.kt` |
| Criar | `data/db/dao/SplitDao.kt` |
| Modificar | `data/db/Mappers.kt` — mappers para entidades de divisão |
| Criar | `data/model/SplitMember.kt`, `SplitExpense.kt`, `MemberBalance.kt`, `SplitSettlement.kt` |
| Criar | `data/repository/SplitRepository.kt` |
| Criar | `ui/split/SplitScreen.kt` + `SplitViewModel.kt` |
| Criar | `ui/split/AddSplitExpenseScreen.kt` + `AddSplitExpenseViewModel.kt` |
| Criar | `ui/split/SplitSettlementsSheet.kt` (composable de BottomSheet) |
| Modificar | `navigation/AppNavigation.kt` — rotas de divisão |
| Modificar | `ui/home/MainPagerScreen.kt` — nova aba "Divisão" |
| Criar | `docs/modulo-16-divisao-custos.md` |

---

---

## F7 — O que tem perto de mim?

### Descrição

Funcionalidade de descoberta de locais ao vivo: o usuário abre uma tela dentro da viagem, o app obtém a localização atual via GPS e consulta a **Google Places API (Nearby Search)** para exibir restaurantes, atrações turísticas, pontos de interesse, lojas e outros estabelecimentos nas redondezas. O objetivo é responder à pergunta espontânea de quem está em trânsito — "o que posso fazer ou comer aqui perto agora?".

Funciona somente com conexão à internet. Sem conexão, exibe mensagem informativa. Não persiste dados localmente (cada consulta é ao vivo).

### Stack de APIs — gratuita com fotos, avaliações e nível de preço

| API | Endpoint base | O que fornece |
|---|---|---|
| **Foursquare Places API v3** | `https://api.foursquare.com/v3/places/nearby` | Busca por categoria + raio, fotos, avaliação (0–10), nível de preço (1–4), horário, telefone, website |
| **Android FusedLocationProviderClient** | — | Coordenada GPS atual (latitude/longitude) |

**Por que Foursquare?**

| Critério | Foursquare Places v3 | Google Places | Overpass (OSM) |
|---|---|---|---|
| Custo | **Gratuito** até 99.000 chamadas/mês | Pago por requisição | Gratuito |
| Fotos | ✅ | ✅ | ❌ |
| Avaliações | ✅ (escala 0–10) | ✅ | ❌ |
| Nível de preço | ✅ (1–4) | ✅ | ❌ |
| Horário de funcionamento | ✅ | ✅ | Parcial |
| Chave de API | Sim (cadastro gratuito) | Sim (requer faturamento) | Não |
| Cobertura no Brasil | Boa em destinos turísticos | Excelente | Variável |

**Cota gratuita:** 99.000 chamadas/mês, sem necessidade de cartão de crédito para registrar. O cadastro é feito em `foursquare.com/developer`.

> **💰 Custo:** Foursquare Places API v3 é **gratuita até 99.000 chamadas/mês** (combinado: `/places/nearby` + `/places/{id}/photos`). Acima desse limite, a API retorna HTTP 429 (rate limit). O plano pago é calculado por chamada — consultar `pricing.foursquare.com` para valores atuais. Para uma base pequena de usuários o limite gratuito é mais do que suficiente (cada sessão do usuário gera ~2–5 chamadas: 1 de busca + até 4 de fotos). A requisição de fotos sendo lazy (só ao ficar visível) e cacheada em memória mitiga o consumo. Implementar fallback explícito para HTTP 429: estado `UiState.QuotaExceeded` com mensagem "Serviço temporariamente indisponível — tente mais tarde".
>
> **Alternativa gratuita sem fotos/avaliações:** Overpass API (OpenStreetMap) — dados ricos de localização, sem fotos nem ratings.

**Autenticação:** header `Authorization: {FSQ_API_KEY}` em cada requisição. A chave começa com `fsq_`.

### Categorias de busca

Mapeamento de categoria para IDs de categoria do Foursquare:

| Categoria (UI) | Category IDs Foursquare |
|---|---|
| 🍽 Restaurantes | `13065` (Restaurant), `13032` (Café), `13003` (Bar), `13040` (Fast Food) |
| 🎡 Atrações | `16000` (Arts & Entertainment), `16032` (Museum), `19014` (Tourist Attraction), `10000` (Outdoors) |
| 🛍 Compras | `17000` (Retail), `17069` (Shopping Mall), `17114` (Souvenir Shop) |
| 🌿 Natureza | `16020` (Park), `16043` (Nature Preserve), `16035` (Mountain) |
| ⛽ Serviços | `12076` (Gas Station), `15014` (Pharmacy), `12058` (ATM), `11000` (Bank) |
| 🔍 Tudo | sem filtro de categoria |

### Modelo de dados — apenas em memória (sem Room)

```kotlin
data class NearbyPlace(
    val fsqId: String,             // ID do local no Foursquare
    val name: String,
    val categoryLabel: String,     // categoria primária (ex: "Restaurante Italiano")
    val categoryEmoji: String,     // emoji derivado da categoria pai
    val distanceMeters: Int,       // retornado diretamente pela API no campo `distance`
    val address: String,           // formatted_address
    val latitude: Double,
    val longitude: Double,
    val rating: Float?,            // 0.0–10.0; null se sem avaliações suficientes
    val priceLevel: Int?,          // 1 ($) a 4 ($$$$); null se desconhecido
    val isOpen: Boolean?,          // hours.open_now; null se horário não cadastrado
    val openingHoursDisplay: String?, // ex: "Seg–Sex 11h–23h" (formato legível)
    val phone: String?,
    val website: String?,
    val photoUrl: String?          // URL da foto principal (prefixo + dimensão + sufixo)
)
```

### Arquitetura

```
data/nearby/
  NearbyPlacesRepository.kt   ← getNearbyPlaces(lat, lng, radiusMeters, categoryIds): Result<List<NearbyPlace>>
                                 getPlacePhotos(fsqId): Result<String?>  (URL da primeira foto)
                                 parsePlaces(json): List<NearbyPlace>

data/location/
  LocationProvider.kt         ← getCurrentLocation(): Result<LatLng>
                                 (wrapper de FusedLocationProviderClient com coroutines)

ui/nearby/
  NearbyScreen.kt             ← tela principal: lista de resultados + controles
  NearbyViewModel.kt          ← estado: Loading / Success(places) / Error / NoPermission / Offline
```

### Chave de API — configuração

```
# local.properties
FSQ_API_KEY=fsq_...
```

Exposta via `BuildConfig.FSQ_API_KEY` (mesmo padrão da `GEMINI_API_KEY` em `build.gradle.kts`). A chave não requer restrição de package Android como o Google — mas não versionar o `local.properties`.

### Requisição — Nearby Search

```http
GET https://api.foursquare.com/v3/places/nearby
Authorization: fsq_...
Accept: application/json

Query params:
  ll=<lat>,<lng>          (coordenada GPS atual)
  radius=<metros>         (raio de busca: 500–5000)
  categories=<ids>        (IDs separados por vírgula; omitir para "Tudo")
  fields=fsq_id,name,categories,distance,location,rating,price,hours,tel,website
  limit=30                (máximo por requisição)
```

### Requisição — Fotos do local

A busca `/nearby` retorna dados do local mas **não inclui fotos** diretamente para evitar respostas muito grandes. Buscar a foto da primeira tela via endpoint separado, disparado por demanda (lazy) quando o card ficar visível:

```http
GET https://api.foursquare.com/v3/places/{fsq_id}/photos
Authorization: fsq_...

Query params:
  limit=1
  sort=POPULAR
```

Resposta: array de fotos com `prefix`, `suffix`, `width`, `height`. Montar URL: `{prefix}300x200{suffix}`.

**Estratégia de carregamento:** disparar a requisição de foto individualmente por card, com cache em memória (`Map<fsqId, photoUrl>` no ViewModel) para não repetir a chamada ao rolar a lista.

### UI — NearbyScreen

```
TopAppBar: "O que tem perto de mim?" + ícone de refresh

Estado: aguardando GPS / carregando
  └─ CircularProgressIndicator + "Buscando locais próximos..."

Estado: resultados disponíveis
  ┌─────────────────────────────────────────────┐
  │  📍 Gramado, RS                             │
  │  Raio: [1 km ▾]                             │
  └─────────────────────────────────────────────┘

  Chips (LazyRow):
  [🔍 Tudo] [🍽 Restaurantes] [🎡 Atrações] [🛍 Compras] [🌿 Natureza] [⛽ Serviços]

  LazyColumn de resultados:
  ┌─────────────────────────────────────────────┐
  │ [Foto 300×150]                              │
  │  🍽 Restaurante do Lago          350m       │
  │  ⭐ 8.4  •  💰💰  •  🟢 Aberto agora      │
  │  Rua das Hortênsias, 120                   │
  │  [Abrir no Maps]  [Como chegar]  [Uber]    │
  └─────────────────────────────────────────────┘

Estado: sem conexão / NoPermission / Empty / Error
  (mensagens específicas por estado — mesmo padrão dos demais)
```

### Card de resultado

Cada card exibe:
- Foto do local (carregada lazy via Foursquare Photos; placeholder com emoji da categoria enquanto carrega ou se indisponível)
- Nome do local + distância
- Avaliação com estrelas (convertida de 0–10 para 0–5 para exibição, ou exibir o valor original `/10`)
- Nível de preço: `$` / `$$` / `$$$` / `$$$$` (quando disponível)
- Badge "🟢 Aberto agora" / "🔴 Fechado" / sem badge se desconhecido
- Endereço resumido
- Botões: "Abrir no Maps" (`geo:` Intent), "Como chegar" (Maps directions), "Uber" (deep link)

### Controles de busca

**Raio de busca:** dropdown 500 m / 1 km / 2 km / 5 km. Padrão: 1 km. Mudança dispara nova busca.

**Filtro de categoria:** chips horizontais, seleção exclusiva. Trocar categoria refaz a requisição com os `categories` correspondentes.

**Refresh manual:** ícone de reload na TopAppBar — relê GPS e refaz a busca.

**Ordenação:** o campo `distance` já vem da API; exibir por distância crescente sem reordenação local.

**Limite:** `limit=30` por requisição (suficiente para uso em tela; Foursquare retorna no máximo 50).

### Integração com o restante do app

- **Acesso:** ícone "📍" ou "Explorar" na TopAppBar de `HomeScreen` ou `DayDetailScreen`
- **Adicionar como atividade:** botão "Adicionar ao roteiro" no card — abre `EditActivityScreen` com nome, endereço e emoji da categoria pré-preenchidos

### Casos de uso

| # | Cenário | Comportamento esperado |
|---|---|---|
| UC-F7-01 | Abrir tela com GPS ativo e internet disponível | GPS obtido → requisição Foursquare → lista exibida; fotos carregadas progressivamente |
| UC-F7-02 | Filtrar por "Restaurantes" | Nova requisição com `categories=13065,13032,13003,13040` → lista atualizada |
| UC-F7-03 | Mudar raio de 1 km para 5 km | Nova requisição com `radius=5000` → mais resultados |
| UC-F7-04 | Card fica visível na tela | Requisição lazy de foto (`/photos`) disparada → foto carregada via `AsyncImage` |
| UC-F7-05 | Local sem foto cadastrada | Placeholder com emoji grande da categoria |
| UC-F7-06 | Local sem avaliação suficiente | Campo de rating oculto (não exibir "0.0") |
| UC-F7-07 | Local sem nível de preço | Campo de preço oculto |
| UC-F7-08 | Tap em "Abrir no Maps" | `Intent(ACTION_VIEW, "geo:lat,lng?q=nome")` → Google Maps |
| UC-F7-09 | Tap em "Uber" | Deep link Uber com destino preenchido |
| UC-F7-10 | Tap em "Adicionar ao roteiro" | `EditActivityScreen` com nome e endereço pré-preenchidos |
| UC-F7-11 | Sem conexão à internet | Estado `Offline`; sem requisição |
| UC-F7-12 | Permissão de GPS negada | Estado `NoPermission` + botão "Abrir configurações" |
| UC-F7-13 | Permissão negada permanentemente | Botão leva para `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` |
| UC-F7-14 | Nenhum resultado na categoria | Estado `Empty` com sugestão de ampliar raio ou mudar categoria |
| UC-F7-15 | Erro HTTP (timeout, 429 quota) | Estado `Error` + "Tentar novamente" |
| UC-F7-16 | Usuário se moveu e quer atualizar | Refresh → nova leitura de GPS → nova requisição |

### Especificação de testes

| Teste | Tipo | O que verificar |
|---|---|---|
| `foursquareResponseParsedCorrectly` | JVM | JSON de resposta → `List<NearbyPlace>` com `name`, `rating`, `priceLevel`, `address` corretos |
| `photoUrlBuiltCorrectly` | JVM | `prefix + "300x200" + suffix` → URL válida |
| `ratingNullWhenAbsent` | JVM | Campo `rating` ausente no JSON → `NearbyPlace.rating = null` |
| `priceLevelNullWhenAbsent` | JVM | Campo `price` ausente → `NearbyPlace.priceLevel = null` |
| `placesOrderedByDistance` | JVM | Lista desordenada → após sort, `distanceMeters` cresce monotonicamente |
| `categoryFilterBuildsCorrectQueryParam` | JVM | Categoria "Restaurantes" → `categories` contém `13065` |
| `offlineStateEmitted` | JVM (mock) | `IOException` no repositório → `UiState.Offline` |
| `noPermissionStateEmitted` | JVM (mock) | `SecurityException` em `LocationProvider` → `UiState.NoPermission` |
| `emptyResultsStateEmitted` | JVM (mock) | API retorna `results: []` → `UiState.Empty` |
| `apiErrorEmitsErrorState` | JVM (mock) | HTTP 429 → `UiState.Error` |
| `photoCachedAfterFirstLoad` | JVM | Segunda chamada com mesmo `fsqId` → repositório não dispara nova requisição HTTP |

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Criar | `data/nearby/NearbyPlacesRepository.kt` |
| Criar | `data/location/LocationProvider.kt` |
| Criar | `data/model/NearbyPlace.kt` |
| Criar | `ui/nearby/NearbyScreen.kt` + `NearbyViewModel.kt` |
| Modificar | `navigation/AppNavigation.kt` — rota para `NearbyScreen` |
| Modificar | `ui/home/HomeScreen.kt` ou `DayDetailScreen.kt` — botão de acesso |
| Modificar | `AndroidManifest.xml` — permissões de localização |
| Modificar | `data/di/AppModule.kt` — `@Provides` para `FusedLocationProviderClient` |
| Modificar | `build.gradle.kts` — `FSQ_API_KEY` via `BuildConfig` |
| Modificar | `local.properties` — `FSQ_API_KEY=fsq_...` |
| Criar | `docs/modulo-17-nearby.md` |

### Dependências a adicionar

```kotlin
// Localização GPS
implementation("com.google.android.gms:play-services-location:21.x.x")

// HTTP client para Foursquare API
implementation("com.squareup.okhttp3:okhttp:4.x.x")
implementation("com.google.code.gson:gson:2.x.x")

// Carregamento de fotos (lazy, com cache e placeholder)
implementation("io.coil-kt:coil-compose:2.x.x")
```

---

## F8 — Assistente de viagem (IA)

### Descrição

Uma tela de chat dentro da viagem onde o usuário conversa livremente com uma LLM que já conhece o contexto completo da viagem e a localização atual do usuário. A IA atua como assistente de viagem — responde dúvidas, sugere alternativas de passeio, explica horários de funcionamento, recomenda o que comer, ajuda a resolver imprevistos e dá orientações práticas no momento em que o usuário precisa.

A diferença em relação ao chat de criação de roteiro (já existente em `CreateTripScreen`) é o propósito e o momento: aquele é usado **antes** da viagem para planejar; este é usado **durante** a viagem para apoio em tempo real, com localização GPS e contexto enriquecido de tudo que está acontecendo naquele dia.

**SDK:** reutiliza o `com.google.ai.client.generativeai` (Gemini) já presente no projeto com a `GEMINI_API_KEY` existente. Nenhuma dependência nova é necessária.

> **💰 Custo — Gemini API:** o tier gratuito do Google AI Studio permite **15 req/min e 1.500 req/dia** para `gemini-2.0-flash`. Para um app com poucos usuários simultâneos isso é suficiente. Com escala, o custo é **USD 0,075/1M tokens de entrada** e **USD 0,30/1M tokens de saída** (Paid tier). O system prompt desta funcionalidade tem ~500–2.000 tokens; cada resposta tipicamente ~200–500 tokens. Custo estimado por conversa: < USD 0,001. Implementar tratamento explícito de HTTP 429 (quota excedida): `UiState.Error` com mensagem "Assistente temporariamente indisponível — limite de uso atingido. Tente novamente em alguns minutos."
>
> **Alternativa gratuita:** Groq oferece tier gratuito generoso (6.000 req/dia para Llama 3.1/Mixtral). Requer substituição do SDK Gemini por chamada HTTP direta ao endpoint Groq — sem biblioteca específica para Android.

### Contexto injetado no system prompt de cada sessão

A cada nova sessão de chat, o app monta um system prompt com as seguintes informações:

```
Você é um assistente de viagem pessoal do app Rumo. O usuário está em viagem e pode
fazer perguntas sobre o que fazer, onde comer, como se locomover, curiosidades da
região e o que mais precisar.

=== CONTEXTO DA VIAGEM ===
Destino: {trip.destination}
Período: {startDate} a {endDate}
Hospedagem: {hotelName}, {hotelAddress}

=== DIA ATUAL ===
Hoje é {dayOfWeek}, {date} — Dia {N} de {totalDays}
Título do dia: {day.title}
{se day.dayAlert} Alerta do dia: {day.dayAlert}

Atividades previstas para hoje:
{lista de atividades: horário + emoji + nome + endereço}

=== LOCALIZAÇÃO ATUAL ===
{se GPS disponível}
Latitude: {lat}, Longitude: {lng}
Endereço aproximado: {reverseGeocode via Nominatim}
{senão}
Localização não disponível no momento.

=== DIAS RESTANTES DA VIAGEM ===
{para cada dia restante (não passado):}
Dia {N} — {date}: {day.title}
  {lista resumida de atividades}

=== CONTATOS SALVOS ===
{lista: nome — telefone (categoria)}

=== INSTRUÇÕES ===
- Responda sempre em português brasileiro, de forma amigável e prática.
- Use os dados da viagem como base principal; complemente com seu conhecimento geral.
- Se o usuário perguntar sobre um local próximo, considere a localização atual fornecida.
- Não invente telefones, preços ou horários — diga que não tem essa informação e sugira verificar no local.
- Respostas curtas e diretas ao ponto; evite parágrafos longos desnecessários.
```

O prompt é montado em `TravelAssistantRepository.buildSystemPrompt(tripId, location)` a cada nova sessão — nunca cacheado, para garantir dados atualizados de GPS e dia atual.

### Modelo de dados — sessão em memória (sem Room)

As mensagens da sessão atual ficam em memória no ViewModel (`List<ChatMessage>`). Não há persistência de histórico de conversa entre sessões — cada abertura da tela inicia uma conversa nova com o mesmo system prompt atualizado.

```kotlin
data class ChatMessage(
    val role: Role,
    val text: String,
    val timestamp: Long,
    val isLoading: Boolean = false   // true no placeholder enquanto a IA responde
)

enum class Role { USER, MODEL }
```

> **Por que não persistir o histórico?** O contexto da viagem (especialmente localização e dia atual) muda a cada sessão. Persistir conversas antigas criaria ruído — o usuário abriria o chat e veria respostas sobre "ontem" sem contexto. A ausência de persistência é intencional para manter o assistente sempre no tempo presente.

### Arquitetura

```
data/ai/
  TravelAssistantRepository.kt   ← buildSystemPrompt(tripId, location): String
                                    startSession(systemPrompt): ChatSession  (Gemini SDK)
                                    sendMessage(session, text): Flow<String> (streaming)

ui/assistant/
  TravelAssistantScreen.kt       ← tela de chat: lista de mensagens + campo de input
  TravelAssistantViewModel.kt    ← inicia sessão ao entrar; chama LocationProvider para GPS
```

`TravelAssistantRepository` instancia `GenerativeModel("gemini-2.0-flash", apiKey = BuildConfig.GEMINI_API_KEY)` com `systemInstruction = content { text(systemPrompt) }` — mesmo padrão de `ItineraryGenerator.kt`. Reutilizar o `LocationProvider` já criado para F7 (se F7 for implementada antes) ou implementar inline.

### Estados do ViewModel

```kotlin
sealed class AssistantUiState {
    object InitializingSession : AssistantUiState()  // montando contexto + GPS
    data class Ready(
        val messages: List<ChatMessage>,
        val showQuickSuggestions: Boolean
    ) : AssistantUiState()
    data class Streaming(
        val messages: List<ChatMessage>
    ) : AssistantUiState()
    data class Error(val message: String) : AssistantUiState()
}
```

### Streaming de resposta

Usar `sendMessageStream()` do SDK Gemini para exibir a resposta progressivamente (token a token):

```kotlin
viewModelScope.launch {
    session.sendMessageStream(userText).collect { chunk ->
        _uiState.update { appendToLastModelMessage(it, chunk.text ?: "") }
    }
}
```

Enquanto a resposta chega: placeholder com `isLoading = true` é substituído progressivamente pelo texto gerado. Ao finalizar o stream: `isLoading = false`.

### UI — TravelAssistantScreen

```
TopAppBar: "Assistente de viagem" + subtítulo "{trip.destination}" + botão Voltar

LazyColumn (mensagens, scroll automático para o fim):
  ├─ Bubble USUÁRIO (alinhado à direita, fundo GreenMoss, texto branco)
  └─ Bubble IA      (alinhado à esquerda, fundo SurfaceWhite, borda GreenLight)
       ├─ [isLoading] três pontos animados pulsando
       └─ [streaming] texto parcial aparecendo progressivamente

[Chips de sugestão — visíveis apenas antes da primeira mensagem]
  [O que fazer hoje?]  [Onde almoçar perto?]  [Como chegar à hospedagem?]  ...

Barra inferior (sobe com o teclado — WindowInsets.ime):
  ┌─────────────────────────────────────────────┬───┐
  │  Escreva sua dúvida...                      │ ▶ │
  └─────────────────────────────────────────────┴───┘
  - Botão ▶ desabilitado quando campo vazio OU IA ainda respondendo
  - Ao enviar: campo limpa; scroll desce para o fim
```

### Sugestões rápidas pré-definidas

Chips exibidos acima do campo de texto na primeira abertura, para reduzir a fricção inicial. Somem após a primeira mensagem enviada.

```kotlin
val quickSuggestions = listOf(
    "O que posso fazer hoje?",
    "Onde almoçar perto daqui?",
    "Como chegar à hospedagem?",
    "Tem algo imperdível fora do meu roteiro?",
    "Quais são os próximos dias da viagem?"
)
```

### Geocodificação reversa da localização

> **⚠ Atenção — Nominatim:** a API pública do Nominatim (`nominatim.openstreetmap.org`) **proíbe uso comercial** em seus Termos de Serviço e impõe limite de 1 requisição/segundo. Para um app publicado na Play Store (M1), usar a API pública do Nominatim é uma violação de ToS e pode resultar em bloqueio do IP.
>
> **Alternativas gratuitas para uso comercial:**
> - **Geoapify Reverse Geocoding** — 3.000 req/dia grátis, uso comercial permitido; endpoint: `https://api.geoapify.com/v1/geocode/reverse?lat={lat}&lon={lon}&apiKey={key}`
> - **MapTiler Geocoding** — 100.000 req/mês grátis; endpoint similar ao Geoapify
> - **Open-Meteo já implementado** no projeto para geocoding de destinos — verificar se o endpoint `/v1/search` suporta reverse geocoding (não suporta diretamente, mas pode-se usar a coordenada sem endereço textual no prompt)
>
> **Recomendação:** usar **Geoapify** (cadastro gratuito em `geoapify.com`). Chave em `local.properties` como `GEOAPIFY_API_KEY`. Fallback: se a chamada falhar, o prompt inclui apenas as coordenadas brutas ("lat: X, lng: Y") — a IA consegue interpretar coordenadas e contextualizar a localização.

Endpoint Geoapify:
```
GET https://api.geoapify.com/v1/geocode/reverse
    ?lat={lat}&lon={lng}&format=json&apiKey={GEOAPIFY_API_KEY}
```

Retorna `formatted` (ex: "Rua das Hortênsias, Gramado, Rio Grande do Sul"). Se a chamada falhar ou GPS indisponível, incluir apenas "Localização não disponível" no prompt — sem bloquear a abertura do chat.

### Controle de contexto — o que incluir e omitir

| Dado | Incluir? | Motivo |
|---|---|---|
| Destino e período | ✅ | Contexto fundamental |
| Hospedagem (nome e endereço) | ✅ | "Como voltar ao hotel?" |
| Atividades do dia atual | ✅ | Perguntas sobre o que foi planejado |
| Dias futuros (resumo) | ✅ | "O que farei amanhã?" |
| Dias passados | ❌ | Irrelevante para apoio em tempo real |
| Localização GPS atual | ✅ | Perguntas sobre "aqui perto" |
| Contatos salvos | ✅ | "Qual o telefone do hotel?" |
| Vouchers | ❌ | Nomes de arquivo sem utilidade para a IA |
| Notas (F4) | Opcional | Útil se o usuário anotou coisas relevantes; avaliar tamanho do prompt |
| Gastos (F5) | ❌ | Privado; fora do escopo do assistente |

### Limites e boas práticas

**Tamanho do prompt:** Gemini Flash suporta até 1 milhão de tokens — o prompt de viagem (tipicamente 500–2.000 tokens) não representa risco. Mesmo assim, omitir dias passados e limitar atividades a 10 por dia para manter o prompt enxuto.

**Privacidade:** o system prompt inclui dados pessoais (nome do hotel, telefones, localização). Nenhum dado trafega além da API Gemini (Google). Incluir nota na política de privacidade do app (M1) sobre uso da API Gemini para o assistente.

### Acesso à tela

- Ícone de chat (balão de fala) na TopAppBar de `HomeScreen`
- Ou nova aba "IA" no `MainPagerScreen` da viagem

### Casos de uso

| # | Cenário | Comportamento esperado |
|---|---|---|
| UC-F8-01 | Abrir chat com GPS disponível | GPS obtido → geocodificação reversa → system prompt montado → sessão Gemini iniciada → chips de sugestão exibidos |
| UC-F8-02 | Abrir chat sem GPS | Prompt montado sem coordenadas; chat funciona normalmente; IA informa que não tem localização |
| UC-F8-03 | Tocar em chip de sugestão | Mensagem enviada automaticamente; chips somem |
| UC-F8-04 | Enviar mensagem | Bubble USER aparece; placeholder de loading exibido; resposta streamed token a token |
| UC-F8-05 | IA respondendo via streaming | Texto aparece progressivamente; botão ▶ desabilitado durante o stream |
| UC-F8-06 | "O que posso fazer hoje?" | IA lista atividades do dia atual do contexto injetado |
| UC-F8-07 | "Onde almoçar perto daqui?" | IA usa localização GPS + conhecimento geral para sugerir opções |
| UC-F8-08 | "Qual o telefone do hotel?" | IA responde com o número da hospedagem do contexto |
| UC-F8-09 | Fechar e reabrir o chat | Nova sessão do zero; system prompt fresco com GPS e hora atuais; histórico anterior não exibido |
| UC-F8-10 | Erro na API Gemini | `AssistantUiState.Error` com mensagem + botão "Tentar novamente" |
| UC-F8-11 | Resposta muito longa | LazyColumn scrolla automaticamente para o fim à medida que o texto chega |
| UC-F8-12 | Tentar enviar enquanto IA responde | Botão ▶ desabilitado; TextField ainda editável para preparar a próxima mensagem |

### Especificação de testes

| Teste | Tipo | O que verificar |
|---|---|---|
| `systemPromptContainsTripDestination` | JVM | `buildSystemPrompt()` com trip mockado → string contém `trip.destination` |
| `systemPromptContainsTodayActivities` | JVM | Dia atual com 3 atividades → prompt contém os 3 nomes |
| `systemPromptExcludesPastDays` | JVM | Viagem de 5 dias, hoje é dia 3 → dias 1 e 2 ausentes do prompt |
| `systemPromptIncludesLocationWhenAvailable` | JVM | GPS mockado → prompt contém as coordenadas |
| `systemPromptHandlesMissingLocation` | JVM | `LocationProvider` retorna erro → prompt contém "Localização não disponível" |
| `chatMessageAppendedOnSend` | JVM | `sendMessage("Oi")` → `messages` contém bubble USER com texto "Oi" |
| `loadingPlaceholderShownWhileStreaming` | JVM | Após envio, stream não completado → último `ChatMessage` tem `isLoading = true` |
| `streamingUpdatesLastModelMessage` | JVM | 3 chunks recebidos → `ChatMessage` MODEL contém a concatenação dos 3 |
| `errorStateOnApiFailure` | JVM (mock) | `sendMessageStream()` lança exceção → `AssistantUiState.Error` |
| `quickSuggestionsHiddenAfterFirstMessage` | JVM | Lista com 1+ mensagem → `showQuickSuggestions = false` |

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Criar | `data/ai/TravelAssistantRepository.kt` |
| Criar | `ui/assistant/TravelAssistantScreen.kt` + `TravelAssistantViewModel.kt` |
| Criar | `data/model/ChatMessage.kt` |
| Modificar | `navigation/AppNavigation.kt` — rota para `TravelAssistantScreen` com `tripId` |
| Modificar | `ui/home/HomeScreen.kt` ou `ui/home/MainPagerScreen.kt` — botão/aba de acesso |
| Criar | `docs/modulo-18-assistente-ia.md` |

### Dependências a adicionar

Nenhuma. O projeto já possui tudo necessário:

```kotlin
// já presente — sem alteração
implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
// GEMINI_API_KEY já em local.properties e BuildConfig
```

---

## F9 — Adicionar amigos (seguir perfis)

### Descrição

Após criar uma conta (F2), o usuário pode procurar outros perfis pelo app e seguí-los. O relacionamento é unidirecional (estilo Instagram/Twitter): seguir alguém não exige aprovação e não significa reciprocidade. A lista de quem o usuário segue é a base do filtro "Seguindo" na comunidade (F10).

**Depende de:** F2 (cadastro de usuários).

### Modelo de dados — Firestore

O grafo social é armazenado como duas subcoleções espelhadas em cada perfil de usuário, para facilitar queries em ambas as direções sem índices complexos:

```
users/{uid}/
  following/{followedUid}/
    followedAt: Timestamp

  followers/{followerUid}/
    followedAt: Timestamp
```

Ao seguir: gravar em `users/{myUid}/following/{theirUid}` **e** em `users/{theirUid}/followers/{myUid}` na mesma operação batch. Ao deixar de seguir: deletar ambos os documentos.

Os contadores `followingCount` e `followersCount` ficam desnormalizados no documento `users/{uid}` e são incrementados/decrementados via `FieldValue.increment(±1)` na mesma batch — evita query de contagem em toda operação de UI.

### Modelo de domínio

```kotlin
data class UserSummary(
    val uid: String,
    val displayName: String,
    val photoUrl: String?,
    val followersCount: Int,
    val followingCount: Int,
    val isFollowedByMe: Boolean   // calculado no momento da query
)
```

### Arquitetura

```
data/social/
  SocialRepository.kt   ← followUser(targetUid),
                           unfollowUser(targetUid),
                           isFollowing(targetUid): Flow<Boolean>,
                           getFollowing(uid): Flow<List<UserSummary>>,
                           getFollowers(uid): Flow<List<UserSummary>>,
                           searchUsers(query): List<UserSummary>

ui/social/
  UserSearchScreen.kt         ← busca por nome/e-mail + resultado com botão seguir/deixar de seguir
  UserSearchViewModel.kt
  UserProfileScreen.kt        ← perfil público: foto, nome, contadores, botão seguir, posts da comunidade
  UserProfileViewModel.kt
  FollowListScreen.kt         ← lista de seguidores ou seguindo (reutilizável por parâmetro)
  FollowListViewModel.kt
```

### UI — UserSearchScreen

```
TopAppBar: "Encontrar pessoas" + botão Voltar

Campo de busca (SearchBar):
  - Busca em tempo real com debounce de 400 ms
  - Pesquisa por nome (prefixo) ou e-mail exato
  - Oculta o próprio usuário dos resultados

Lista de resultados (LazyColumn):
  ┌───────────────────────────────────────────┐
  │ [Avatar]  João Silva                      │
  │           @joaosilva  •  42 seguidores    │
  │                           [Seguindo ✓]   │  ← já segue
  │                           [Seguir]       │  ← não segue
  └───────────────────────────────────────────┘

Tap no card → UserProfileScreen
```

### UI — UserProfileScreen

```
TopAppBar: nome do usuário + botão Voltar

Cabeçalho:
  [Foto grande]   Nome completo
  Bio (se houver — campo a adicionar em F2)
  [42 seguidores]  [18 seguindo]     ← clicáveis → FollowListScreen
  [Seguir] / [Seguindo ✓] / [Editar perfil] (se for o próprio)

Seção "Posts na comunidade":
  Grid 2 colunas com thumbnails dos posts desta pessoa → tap abre PostDetailScreen (F10)

Estado vazio:
  "Nenhum post compartilhado ainda."
```

### UI — FollowListScreen

Tela genérica usada tanto para "seguidores" quanto para "seguindo" (controlado por parâmetro de navegação):

```
TopAppBar: "Seguidores" ou "Seguindo" + botão Voltar

LazyColumn:
  Card por usuário: avatar + nome + botão Seguir/Seguindo
  Tap no card → UserProfileScreen
```

### Busca de usuários — estratégia no Firestore

O Firestore não suporta busca full-text nativa. Estratégias viáveis:

**Opção A (simples, sem custo extra):** armazenar `displayNameLower` (nome em minúsculas) no perfil e usar query `where("displayNameLower", isGreaterThanOrEqualTo, query).where("displayNameLower", isLessThan, query + "")` — funciona como busca por prefixo. Busca por e-mail exato é `where("email", isEqualTo, query)`.

**Opção B (mais poderosa, custo adicional):** integrar Algolia ou Typesense para busca full-text. Fora do escopo inicial.

Adotar **Opção A** na implementação inicial.

### Firebase Security Rules (esboço)

```javascript
// Leitura pública de perfis
match /users/{uid} {
  allow read: if true;  // perfis são públicos
  allow write: if request.auth.uid == uid;  // só o próprio edita
}

// Seguir: autenticado pode criar/deletar no próprio grafo
match /users/{uid}/following/{followedUid} {
  allow read: if true;
  allow write: if request.auth.uid == uid;
}
match /users/{uid}/followers/{followerUid} {
  allow read: if true;
  allow write: if request.auth.uid == followerUid;  // quem segue escreve no meu followers
}
```

### Casos de uso

| # | Cenário | Comportamento esperado |
|---|---|---|
| UC-F9-01 | Buscar por nome existente | Resultados aparecem após 400 ms de debounce; próprio usuário não aparece |
| UC-F9-02 | Buscar por e-mail exato | Resultado único retornado se e-mail existir |
| UC-F9-03 | Busca sem resultados | Estado vazio: "Nenhum usuário encontrado" |
| UC-F9-04 | Seguir um usuário | Documentos criados em batch; botão muda para "Seguindo ✓"; contadores incrementados |
| UC-F9-05 | Deixar de seguir | Documentos deletados em batch; botão volta para "Seguir"; contadores decrementados |
| UC-F9-06 | Ver perfil de outro usuário | `UserProfileScreen` com contadores, botão seguir e grid de posts |
| UC-F9-07 | Ver lista de seguidores | `FollowListScreen` com todos os seguidores; botão seguir/deixar de seguir por card |
| UC-F9-08 | Ver lista de seguindo | `FollowListScreen` com todos que o usuário segue |
| UC-F9-09 | Usuário não cadastrado tenta seguir | Não tem acesso à funcionalidade (acesso exige F2) |
| UC-F9-10 | Usuário bloqueia outro (escopo futuro) | Não especificado nesta versão; anotar como Q futura |

### Especificação de testes

| Teste | Tipo | O que verificar |
|---|---|---|
| `followCreatesDocumentsInBothDirections` | Instrumented / Emulator | `followUser(B)` → documento em `users/A/following/B` e `users/B/followers/A` |
| `unfollowDeletesBothDocuments` | Instrumented / Emulator | `unfollowUser(B)` → ambos os documentos deletados |
| `followIncrementsCounters` | Instrumented / Emulator | `followingCount` de A e `followersCount` de B incrementados em 1 |
| `unfollowDecrementsCounters` | Instrumented / Emulator | Contadores decrementados ao deixar de seguir |
| `searchByPrefixReturnsCorrectResults` | Instrumented / Emulator | Busca "Joã" → retorna "João Silva" mas não "Carlos" |
| `searchExcludesCurrentUser` | Instrumented / Emulator | Resultado nunca contém o próprio uid |
| `isFollowingFlowUpdatesOnChange` | Instrumented / Emulator | `isFollowing(B)` emite `false` → `followUser(B)` → emite `true` |

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Criar | `data/social/SocialRepository.kt` |
| Criar | `ui/social/UserSearchScreen.kt` + `UserSearchViewModel.kt` |
| Criar | `ui/social/UserProfileScreen.kt` + `UserProfileViewModel.kt` |
| Criar | `ui/social/FollowListScreen.kt` + `FollowListViewModel.kt` |
| Modificar | `data/user/UserRepository.kt` — adicionar `displayNameLower` ao salvar perfil |
| Modificar | `navigation/AppNavigation.kt` — rotas para busca, perfil e listas de follow |
| Modificar | `ui/profile/ProfileScreen.kt` — contadores de seguidores/seguindo clicáveis |
| Modificar | `data/di/AppModule.kt` — `@Provides` para `SocialRepository` |
| Criar | `docs/modulo-19-social.md` |

---

## F10 — Área da comunidade

### Descrição

Um feed social dentro do app onde usuários cadastrados compartilham suas experiências de viagem: roteiro exportável (no formato `trip.json` já especificado), fotos, título e texto descritivo com observações, dicas e informações sobre a viagem realizada. Outros usuários podem ler, curtir, comentar e — o mais importante — **importar o roteiro diretamente para o app** com um toque, usando a mesma infraestrutura de `TravelImporter` já implementada.

O feed tem três modos de visualização: **Todos** (todos os posts públicos), **Seguindo** (posts de quem o usuário segue — requer F9) e **Por destino** (filtrar por nome de cidade/região).

**Depende de:** F2 (conta de usuário para criar posts), F9 (filtro "Seguindo"), F1 (`tripUuid` para evitar duplicatas ao importar roteiros da comunidade).

### Modelo de dados — Firestore

```
community_posts/
  {postId}/
    authorId: String
    authorName: String          (desnormalizado)
    authorPhotoUrl: String?     (desnormalizado)
    destination: String         (ex: "Gramado, RS") — usado no filtro por destino
    destinationLower: String    (lowercase para query de prefixo)
    title: String
    description: String         (texto livre com observações do autor)
    photoUrls: List<String>     (URLs no Firebase Storage — até 10 fotos)
    tripJsonUrl: String?        (URL do trip.json no Firebase Storage; null se não compartilhou roteiro)
    tripUuid: String?           (copiado do trip.json para detecção de duplicata no import)
    tripDestination: String?    (nome do destino do roteiro, para exibição no card)
    tripDaysCount: Int?         (quantidade de dias do roteiro)
    likesCount: Int             (desnormalizado)
    commentsCount: Int          (desnormalizado)
    createdAt: Timestamp
    updatedAt: Timestamp
    isPublic: Boolean           (padrão true; futuro: posts privados para só seguidores)

    likes/
      {uid}/
        likedAt: Timestamp

    comments/
      {commentId}/
        authorId: String
        authorName: String
        authorPhotoUrl: String?
        text: String
        createdAt: Timestamp
```

**Firebase Storage:**
```
community/
  {postId}/
    trip.json           ← roteiro exportável (mesma estrutura do TravelExporter)
    photos/
      0.jpg
      1.jpg
      ...               ← até 10 fotos, máx. 5 MB cada (redimensionadas para 1280×960 antes do upload)
```

### Modelo de domínio

```kotlin
data class CommunityPost(
    val postId: String,
    val author: UserSummary,
    val destination: String,
    val title: String,
    val description: String,
    val photoUrls: List<String>,
    val hasTripItinerary: Boolean,    // tripJsonUrl != null
    val tripUuid: String?,
    val tripDestination: String?,
    val tripDaysCount: Int?,
    val likesCount: Int,
    val commentsCount: Int,
    val isLikedByMe: Boolean,         // calculado no momento da query
    val createdAt: Long
)

data class PostComment(
    val commentId: String,
    val author: UserSummary,
    val text: String,
    val createdAt: Long
)
```

### Arquitetura

```
data/community/
  CommunityRepository.kt      ← getFeed(filter, lastDoc?): Flow<List<CommunityPost>>  (paginado)
                                 getPost(postId): Flow<CommunityPost>
                                 createPost(draft): String (postId)
                                 deletePost(postId)
                                 likePost(postId) / unlikePost(postId)
                                 getComments(postId): Flow<List<PostComment>>
                                 addComment(postId, text)
                                 deleteComment(postId, commentId)
                                 reportPost(postId, reason)
                                 downloadTripJson(postId): Uri  (arquivo temporário para importação)

ui/community/
  CommunityFeedScreen.kt      ← feed paginado com filtros
  CommunityFeedViewModel.kt
  PostDetailScreen.kt         ← post completo: fotos, texto, roteiro, comentários
  PostDetailViewModel.kt
  CreatePostScreen.kt         ← formulário de criação de post
  CreatePostViewModel.kt
```

### UI — CommunityFeedScreen

```
TopAppBar: "Comunidade" + ícone de criar post (+)

Filtros (TabRow fixo abaixo da TopAppBar):
  [Todos]  [Seguindo]  [Por destino]

[Se "Por destino" selecionado]:
  SearchBar: "Buscar por destino..." — filtra posts pelo campo `destination`

Feed (LazyColumn paginado — carregar 15 posts por vez, Infinite Scroll):
  ┌─────────────────────────────────────────────┐
  │ [Avatar]  João Silva  •  Gramado, RS        │
  │           há 2 dias                         │
  │                                             │
  │ "Serra Gaúcha em família — 5 dias"          │
  │                                             │
  │ [Foto 1] [Foto 2] [Foto 3...]  (carrossel)  │
  │                                             │
  │ "Uma viagem incrível! O destaque foi o      │
  │  Natal Luz. Vale cada centavo..."           │
  │  [Ver mais]                                 │
  │                                             │
  │ 🗓 Roteiro disponível — 5 dias em Gramado   │
  │ [📥 Importar roteiro]                       │
  │                                             │
  │ ❤ 24   💬 8   [Compartilhar]               │
  └─────────────────────────────────────────────┘

Paginação: ao rolar até o fim, carregar mais 15 posts (cursor-based com `startAfter(lastDoc)`)
```

**Aba "Seguindo" sem conexões:** se o usuário não segue ninguém ainda, exibir estado vazio com call-to-action: "Encontre pessoas para seguir" → abre `UserSearchScreen`.

### UI — PostDetailScreen

```
TopAppBar: nome do autor + botão Voltar + ícone "..." (denunciar / deletar se autor)

Carrossel de fotos (HorizontalPager com indicadores de página)

Cabeçalho do autor: avatar + nome + destino + data

Seção "Roteiro":
  ┌─────────────────────────────────────────────┐
  │ 🗓  Roteiro: Gramado, RS — 5 dias           │
  │     Inclui dias, atividades e informações   │
  │  [📥 Importar roteiro para o app]           │
  └─────────────────────────────────────────────┘

Texto de descrição completo (sem truncagem)

Ações: [❤ Curtir (24)]  [💬 Comentar (8)]

Seção "Comentários":
  Campo de texto: "Escreva um comentário..." + botão Enviar
  LazyColumn de comentários:
    Card: avatar + nome + texto + data + ícone deletar (se for o autor do comentário)
```

### UI — CreatePostScreen

```
TopAppBar: "Novo post" + botão Voltar + botão "Publicar" (GreenMoss + AmberPrimary)

Formulário (LazyColumn):
  Campo: Destino da viagem (TextField com autocomplete via Open-Meteo geocoding — já implementado)
  Campo: Título (obrigatório, máx. 100 chars)
  Campo: Descrição / Observações (multilinha, máx. 2.000 chars, contador de caracteres)

  Seção "Fotos":
    Grid de miniaturas das fotos selecionadas
    Botão "+ Adicionar fotos" (galeria / câmera; máx. 10)
    Cada miniatura tem ícone X para remover

  Seção "Roteiro" (opcional):
    ┌─────────────────────────────────────────────┐
    │  Compartilhar roteiro?                      │
    │  Outros usuários poderão importá-lo para    │
    │  o app com um toque.                        │
    │                                             │
    │  [Selecionar viagem]  ← dropdown das viagens│
    │                         do usuário no banco │
    └─────────────────────────────────────────────┘
    Ao selecionar: app exporta silenciosamente via TravelExporter → faz upload do trip.json no Storage

  Toggle: "Post público" (padrão: ativo)
```

### Fluxo de importação de roteiro da comunidade

```
Usuário tap em [📥 Importar roteiro]
  └─ CommunityRepository.downloadTripJson(postId)
       └─ Baixa trip.json do Firebase Storage → salva em cacheDir/community_imports/{postId}.json
            └─ Converte para Uri e passa para TravelImporter.import(uri)
                 ├─ [UUID não existe no banco] → importação normal → navega para a viagem importada
                 └─ [UUID já existe] → DuplicateTripException → dialog de conflito (F1)
                      ├─ [Manter local] → cancela
                      └─ [Importar] → sobreescreve
```

A importação reutiliza **exatamente** a mesma infraestrutura de `TravelImporter` já implementada — nenhuma lógica nova é necessária além de baixar o arquivo e passar a URI.

### Paginação do feed

O Firestore não suporta `OFFSET` — usar cursor-based pagination com `startAfter(lastDocument)`:

```kotlin
// Primeira página
db.collection("community_posts")
  .orderBy("createdAt", DESCENDING)
  .limit(15)
  .get()

// Próximas páginas
db.collection("community_posts")
  .orderBy("createdAt", DESCENDING)
  .startAfter(lastDocumentSnapshot)
  .limit(15)
  .get()
```

O ViewModel mantém `lastDocumentSnapshot` e uma flag `hasMore: Boolean`. Ao detectar que o usuário chegou perto do fim da lista (`LazyListState.firstVisibleItemIndex + visibleItemsCount >= totalItems - 3`), dispara `loadMore()`.

### Filtro "Seguindo" — estratégia

O Firestore não permite fazer um `JOIN` entre `community_posts` e a lista de seguindo. Estratégia:

1. Buscar os UIDs de quem o usuário segue: `users/{myUid}/following` → lista de IDs
2. Consultar posts onde `authorId IN [lista de ids]` — Firestore suporta `whereIn` com até 30 valores
3. Se o usuário seguir mais de 30 pessoas: fazer múltiplas queries em paralelo (chunks de 30) e mesclar os resultados localmente, ordenando por `createdAt` desc

Essa limitação deve ser documentada na tela ("Feed dos usuários que você segue").

### Firebase Security Rules (esboço)

```javascript
match /community_posts/{postId} {
  // Leitura pública para posts públicos
  allow read: if resource.data.isPublic == true || request.auth.uid == resource.data.authorId;

  // Criar: autenticado, author = próprio uid
  allow create: if request.auth != null
                && request.resource.data.authorId == request.auth.uid;

  // Deletar: apenas o autor
  allow delete: if request.auth.uid == resource.data.authorId;

  // Atualizar: autor pode editar; qualquer autenticado pode atualizar likesCount/commentsCount
  allow update: if request.auth.uid == resource.data.authorId
                || onlyUpdating(['likesCount', 'commentsCount']);

  match /likes/{uid} {
    allow read: if true;
    allow write: if request.auth.uid == uid;
  }

  match /comments/{commentId} {
    allow read: if true;
    allow create: if request.auth != null;
    allow delete: if request.auth.uid == resource.data.authorId
                  || request.auth.uid == get(/databases/$(database)/documents/community_posts/$(postId)).data.authorId;
  }
}

// Storage rules — usar regras baseadas em metadata, não em convenção de postId
// (postId é auto-gerado pelo Firestore e não segue nenhuma convenção de prefixo)
match /community/{postId}/{allPaths=**} {
  allow read: if request.auth != null;
  // Escrever: apenas quem criou o post (verificar no Firestore)
  allow write: if request.auth != null
               && exists(/databases/$(database)/documents/community_posts/$(postId))
               && get(/databases/$(database)/documents/community_posts/$(postId)).data.authorId == request.auth.uid;
  // Criar arquivos antes do post existir no Firestore (upload de foto antes de criar o doc):
  // usar uma coleção temporária ou realizar o upload após a criação do documento.
}
```

### Moderação e denúncias

- Botão "Denunciar" no menu "..." de cada post
- Formulário simples: categoria (Conteúdo inapropriado / Spam / Informação falsa / Outro) + texto opcional
- Gravado em coleção `reports/{reportId}` no Firestore
- Moderação manual em primeira instância (sem automação nesta versão)

### Casos de uso

| # | Cenário | Comportamento esperado |
|---|---|---|
| UC-F10-01 | Abrir feed "Todos" | 15 posts mais recentes carregados; scroll infinito carrega mais |
| UC-F10-02 | Abrir feed "Seguindo" sem seguir ninguém | Estado vazio com CTA "Encontrar pessoas" |
| UC-F10-03 | Abrir feed "Seguindo" com 5 pessoas seguidas | Posts dos 5 usuários seguidos, ordenados por data |
| UC-F10-04 | Filtrar por destino "Gramado" | Posts com `destination` começando por "gramado" (busca insensível a maiúsculas) |
| UC-F10-05 | Curtir um post | `likesCount` incrementado; ícone ❤ preenchido; operação otimista na UI |
| UC-F10-06 | Curtir post já curtido | Descurtir; `likesCount` decrementado |
| UC-F10-07 | Comentar em um post | Comentário aparece na lista; `commentsCount` incrementado |
| UC-F10-08 | Importar roteiro de um post | Download do trip.json → `TravelImporter` → detecção de duplicata (F1) → viagem importada |
| UC-F10-09 | Importar roteiro já existente | Dialog de conflito (F1): "Esta viagem já está no app" → opções manter/importar |
| UC-F10-10 | Criar post sem fotos e sem roteiro | Apenas título + texto; publicado normalmente |
| UC-F10-11 | Criar post com roteiro selecionado | `TravelExporter` roda silenciosamente → trip.json subido para Storage → `tripJsonUrl` salvo |
| UC-F10-12 | Criar post com 10 fotos | Todas redimensionadas e subidas; carrossel exibido no feed |
| UC-F10-13 | Tentar criar post sem conta | Não tem acesso (requer F2) |
| UC-F10-14 | Deletar próprio post | Confirmação → post deletado do Firestore + arquivos removidos do Storage |
| UC-F10-15 | Denunciar post | Formulário de denúncia → gravado em `reports/` |
| UC-F10-16 | Feed offline | Firestore cache offline exibe últimos posts carregados; banner "Você está offline" |
| UC-F10-17 | Post sem roteiro | Card não exibe seção de roteiro; botão "Importar" não aparece |
| UC-F10-18 | Autor edita descrição | Post atualizado no Firestore; feed reflete a mudança em tempo real |

### Especificação de testes

| Teste | Tipo | O que verificar |
|---|---|---|
| `feedLoadsPaginatedPosts` | Instrumented / Emulator | Primeira carga retorna 15 posts; `loadMore()` retorna próximos 15 |
| `feedFollowingFiltersCorrectly` | Instrumented / Emulator | Feed "Seguindo" retorna só posts dos UIDs na lista `following` |
| `feedByDestinationFiltersCorrectly` | Instrumented / Emulator | Busca "gramado" → retorna posts com `destinationLower` começando por "gramado" |
| `likeIncreasesCount` | Instrumented / Emulator | `likePost(id)` → `likesCount` +1 no Firestore |
| `unlikeDecreasesCount` | Instrumented / Emulator | `unlikePost(id)` após like → `likesCount` -1 |
| `addCommentIncreasesCount` | Instrumented / Emulator | `addComment(id, text)` → `commentsCount` +1 |
| `downloadTripJsonReturnsValidUri` | Instrumented / Emulator | `downloadTripJson(postId)` → Uri aponta para arquivo com JSON válido |
| `importFromCommunityUsesExistingImporter` | JVM (mock) | `downloadTripJson()` → Uri passada para `TravelImporter.import()` |
| `createPostUploadsPhotosAndJson` | Instrumented / Emulator | `createPost()` → arquivos presentes no Storage; `photoUrls` e `tripJsonUrl` no documento |
| `deletePostRemovesStorageFiles` | Instrumented / Emulator | `deletePost()` → arquivos de Storage deletados junto com o documento |
| `nonAuthorCannotDeletePost` | Instrumented / Emulator | Usuário B tenta deletar post do usuário A → Firestore rejeita (Security Rules) |

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Criar | `data/community/CommunityRepository.kt` |
| Criar | `data/model/CommunityPost.kt`, `PostComment.kt` |
| Criar | `ui/community/CommunityFeedScreen.kt` + `CommunityFeedViewModel.kt` |
| Criar | `ui/community/PostDetailScreen.kt` + `PostDetailViewModel.kt` |
| Criar | `ui/community/CreatePostScreen.kt` + `CreatePostViewModel.kt` |
| Modificar | `navigation/AppNavigation.kt` — rotas do feed, detalhe e criação de post |
| Modificar | `MainActivity.kt` ou `TripsListScreen.kt` — ícone/aba de acesso à comunidade |
| Modificar | `data/di/AppModule.kt` — `@Provides` para `CommunityRepository` |
| Criar | `docs/modulo-20-comunidade.md` |

### Dependências a adicionar

Nenhuma nova dependência além das já listadas para F2/F3 (Firebase Auth, Firestore, Storage). Coil já especificado em F7 para carregamento de imagens.

> **💰 Custo — Firebase Storage para fotos da comunidade:** cada post pode ter até 10 fotos de até 5 MB (redimensionadas para 1280×960). Estimando ~1 MB por foto após redimensionamento: 10 fotos × 1 MB = 10 MB/post. Com 500 posts, isso ocupa ~5 GB — **atingindo o limite do Spark Plan**. Migrar para Blaze ao começar a crescer a comunidade (USD 0,026/GB além dos 5 GB gratuitos). Estratégia de mitigação: reduzir limite para 5 fotos por post ou comprimir mais agressivamente (JPEG 80% a 640×480) na primeira versão.

> **⚠ Impacto na arquitetura de upload:** o fluxo de criação de post requer atenção à ordem de operações:
> 1. Criar documento no Firestore primeiro (com `photoUrls` vazio e sem `tripJsonUrl`)
> 2. Fazer upload dos arquivos no Storage (usando o `postId` obtido no passo 1)
> 3. Atualizar o documento com as URLs dos arquivos
> Essa sequência garante que a Security Rule do Storage funcione (o documento já existe antes do upload). Se o upload falhar, deletar o documento criado no passo 1 (operação de cleanup no `catch`).

---

## F11 — Modo agência

### Descrição

Perfil especial para agências de viagem que permite criar templates de roteiro, gerenciar múltiplos clientes e distribuir roteiros em lote. Uma conta marcada como agência tem acesso a uma seção exclusiva "Minha Agência" com painel de clientes, biblioteca de templates e estatísticas básicas de uso.

### Modelo de dados

**Firestore (`agencies/{agencyId}`):**
```
name: String
ownerUid: String
logoUrl: String?
contactEmail: String
plan: String           // "free" | "pro" | "enterprise"
createdAt: Timestamp
```

**Firestore (`agencies/{agencyId}/clients/{clientId}`):**
```
clientUid: String?     // uid Firebase do cliente (se tiver conta)
clientEmail: String
clientName: String
assignedTripIds: List<String>  // tripUuid das viagens enviadas
addedAt: Timestamp
```

**Room — campo adicional em `TripEntity`:**
```
agencyId: String?      // null se criada pelo próprio usuário
```
Requer Migration N → N+1 (verificar versão atual do banco em `TravelDatabase.kt` — F1, F4, F5 e F6 também adicionam migrations antes de F11).

### Arquitetura

| Camada | Arquivo | Responsabilidade |
|---|---|---|
| Repositório | `data/repository/AgencyRepository.kt` | CRUD de agência, clientes, envio de roteiros |
| ViewModel | `ui/agency/AgencyDashboardViewModel.kt` | Painel de clientes e estatísticas |
| ViewModel | `ui/agency/AgencyTemplateViewModel.kt` | Gerenciar biblioteca de templates |
| UseCase | `data/usecase/SendItineraryToClientUseCase.kt` | Exporta `.travel` + envia link/arquivo para cliente |

### Telas

| Tela | Conteúdo |
|---|---|
| `AgencyDashboardScreen` | Lista de clientes, total de roteiros enviados, busca rápida |
| `AgencyClientDetailScreen` | Viagens atribuídas ao cliente, botão "Enviar novo roteiro" |
| `AgencyTemplateLibraryScreen` | Templates salvos, filtro por destino/duração |
| `AgencyProfileScreen` | Dados da agência, logo, e-mail de contato, plano atual |

### Casos de uso

| Ação | Comportamento |
|---|---|
| Marcar conta como agência | Toggle em `SettingsScreen` ou prompt no primeiro uso — grava `isAgency: true` no perfil Firestore |
| Criar template | Cria viagem com flag `isTemplate = true`; não aparece na lista principal de viagens do usuário |
| Enviar roteiro a cliente | `SendItineraryToClientUseCase` gera arquivo `.travel` → compartilha via `ACTION_SEND` ou envia link de download no Firestore |
| Registrar cliente | Adiciona documento em `agencies/{id}/clients/` com e-mail e nome |
| Associar viagem a cliente | Adiciona `tripUuid` em `assignedTripIds` do cliente |

### Especificação de testes

| Teste | Tipo | O que verificar |
|---|---|---|
| `agencyCreatedInFirestore` | Instrumented / Emulator | `AgencyRepository.createAgency()` → documento em `agencies/{agencyId}` com `ownerUid` correto |
| `clientAddedToAgency` | Instrumented / Emulator | `addClient()` → documento em `agencies/{id}/clients/{clientId}` com `clientEmail` e `clientName` |
| `templateTripHiddenFromMainList` | JVM | Viagem com `isTemplate = true` → `TripRepository.getTrips()` não a retorna |
| `sendItineraryGeneratesTravel` | JVM (mock) | `SendItineraryToClientUseCase(tripId)` → `TravelExporter.export()` chamado; Uri retornada não-nula |
| `agencyIdPersistedInTripEntity` | JVM | Salvar trip com `agencyId = "abc"` → recarregar do banco → `agencyId == "abc"` |
| `nonAgencyUserCannotAccessDashboard` | JVM | `isAgency = false` → `AgencyDashboardViewModel` emite `UiState.NotAgency` |

### Impacto no app existente

- **`TripEntity`**: novo campo `agencyId` (nullable) — migration obrigatória + ambas as direções nos Mappers
- **`TripsListScreen`**: viagens com `isTemplate = true` ficam ocultas da lista principal — lógica no `TripRepository.getTrips()`
- **`MainPagerScreen`** / **`TopAppBar` da viagem**: ícone de acesso ao painel da agência visível apenas quando `isAgency = true`
- **`SettingsScreen`**: novo item "Modo agência" — adicionar à seção existente, seguindo padrão DataStore

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Criar | `data/repository/AgencyRepository.kt` |
| Criar | `data/usecase/SendItineraryToClientUseCase.kt` |
| Criar | `ui/agency/AgencyDashboardScreen.kt` + `AgencyDashboardViewModel.kt` |
| Criar | `ui/agency/AgencyClientDetailScreen.kt` |
| Criar | `ui/agency/AgencyTemplateLibraryScreen.kt` |
| Criar | `ui/agency/AgencyProfileScreen.kt` + `AgencyTemplateViewModel.kt` |
| Modificar | `data/db/TravelDatabase.kt` — Migration N→N+1, campo `agencyId` em `TripEntity` |
| Modificar | `data/db/Mappers.kt` — ambas as direções |
| Modificar | `data/repository/TripRepository.kt` — filtrar `isTemplate = true` em `getTrips()` |
| Modificar | `navigation/AppNavigation.kt` — rotas do módulo agência |
| Modificar | `ui/settings/SettingsScreen.kt` — opção "Modo agência" |
| Criar | `docs/modulo-21-agencia.md` |

### Dependências a adicionar

Nenhuma nova além do Firebase Auth + Firestore (F2).

---

## F12 — Geração de PDF do roteiro

### Descrição

Exportar o roteiro completo de uma viagem em formato PDF, formatado de forma profissional, pronto para impressão ou envio por e-mail. O PDF inclui: capa com nome e destino, cronograma dia a dia com horários e atividades, informações de hospedagem e contatos. Não requer internet.

### Arquitetura

| Camada | Arquivo | Responsabilidade |
|---|---|---|
| Repositório | `data/export/PdfExporter.kt` | Gera `ByteArray` do PDF via `android.graphics.pdf.PdfDocument` |
| ViewModel | `ui/share_trip/ShareTripViewModel.kt` | Adiciona ação `exportPdf()` ao ViewModel já existente |

**Biblioteca:** `android.graphics.pdf.PdfDocument` (nativa Android — zero dependência nova). Para layouts mais ricos, alternativa é `iText` (MIT / AGPL — verificar licença) ou `PdfBox-Android` (Apache 2.0).

### Estrutura do PDF

```
Página 1 — Capa
  Nome da viagem + emoji
  Destino
  Período (data início → data fim)
  Hotel e telefone

Páginas 2..N — Um bloco por dia
  Título do dia + data + dia da semana
  Alerta do dia (se houver)
  Lista de atividades:
    [HH:MM] [emoji] Nome da atividade
              Descrição
              Badges

Página final — Contatos
  Lista de contatos agrupados por categoria
```

### Telas

Nenhuma tela nova — o botão "Gerar PDF" é adicionado à `ShareTripScreen` existente, ao lado do botão de compartilhar `.travel`.

Fluxo: botão → `ShareTripViewModel.exportPdf()` → dialog de loading → salvar em `cacheDir/exports/<nome>.pdf` → `ACTION_SEND` com MIME `application/pdf`.

### Casos de uso

| Ação | Comportamento |
|---|---|
| Gerar PDF simples | Layout sem imagens — rápido, funciona offline, compatível com qualquer impressora |
| Compartilhar | `ACTION_SEND` com `application/pdf` — usuário escolhe app (Gmail, WhatsApp, Drive, etc.) |
| Uso por agência | Agência envia PDF do roteiro ao cliente junto com (ou no lugar de) o arquivo `.travel` |

### Especificação de testes

| Teste | Tipo | O que verificar |
|---|---|---|
| `pdfGeneratedSuccessfully` | JVM (Instrumented) | Viagem com 5 dias e 3 atividades cada → `PdfExporter.export()` retorna `ByteArray` não-vazio, número de páginas ≥ 6 |
| `pdfWithNoContacts` | JVM | Viagem sem contatos → página de contatos ausente ou com "Nenhum contato cadastrado" |
| `pdfWithSpecialCharacters` | JVM | Nome da viagem com acentos, emojis e caracteres especiais → sem exceção de renderização |
| `pdfWithLongActivityDescription` | JVM | Atividade com descrição de 1.000 chars → conteúdo paginado corretamente, sem conteúdo cortado |
| `pdfCachedInCacheDir` | Instrumented | Arquivo gerado em `cacheDir/exports/*.pdf` → URI válida retornada via FileProvider |
| `pdfShareIntent` | Instrumented | `ACTION_SEND` disparado com MIME `application/pdf` e URI não-nula |

### Impacto no app existente

- **`ShareTripScreen`**: adição de botão "Gerar PDF" ao lado do botão de compartilhar `.travel` — avaliar layout para não sobrecarregar a tela; considerar usar `DropdownMenu` agrupando as duas opções de export
- **`ShareTripViewModel`**: novo estado `ExportingPdf` e método `exportPdf()` seguindo mesmo padrão do `export()` existente (`Idle → Exporting → Ready → Error`)
- **`PdfExporter`**: geração é síncrona e pesada — executar em `Dispatchers.IO` dentro do ViewModel via `viewModelScope.launch(Dispatchers.IO)`

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Criar | `data/export/PdfExporter.kt` |
| Modificar | `ui/share_trip/ShareTripScreen.kt` — botão "Gerar PDF" |
| Modificar | `ui/share_trip/ShareTripViewModel.kt` — método `exportPdf()` e estado `ExportingPdf` |
| Criar | `docs/modulo-22-pdf.md` |

### Dependências a adicionar

Nenhuma (usa `android.graphics.pdf.PdfDocument` nativo).

> **💰 Custo — bibliotecas PDF:** `android.graphics.pdf.PdfDocument` é **nativo e gratuito**, mas requer layout manual (sem suporte a HTML ou wrap de texto automático). `PdfBox-Android` (Apache 2.0) é gratuito e facilita o layout. **Evitar iText 7** em builds fechados: a licença AGPL exige que o código-fonte do app seja publicado; a licença comercial é paga (centenas de USD/ano).

---

## F13 — Notificações push e lembretes

### Descrição

Notificações locais agendadas (WorkManager + AlarmManager) que lembram o usuário das atividades do dia em curso. Opcionalmente, notificações remotas (Firebase Cloud Messaging) para eventos sociais como convites de viagem (F3) e novos posts de amigos (F9/F10).

### Tipos de notificação

| Tipo | Canal | Trigger | Conteúdo |
|---|---|---|---|
| Lembrete de atividade | Local (WorkManager) | X minutos antes do horário da atividade | "[emoji] [nome] em X min — [descrição]" |
| Início de viagem | Local (WorkManager) | Manhã do primeiro dia | "Hoje começa [nome da viagem] 🎉 — Primeiro destino: [atividade]" |
| Check-in de voo | Local (WorkManager) | 24h antes do voo | "Check-in disponível para o voo [número]" |
| Convite para viagem | Remota (FCM) | Quando alguém adiciona o usuário em F3 | "[Nome] te convidou para a viagem [nome]" |
| Novo post de amigo | Remota (FCM) | Opcional (configurável) | "[Nome] publicou um roteiro para [destino]" |

### Arquitetura

```
WorkManager
  └─ ItineraryReminderWorker      ← agenda notificações para cada atividade com horário
  └─ TripStartWorker              ← notificação na manhã do primeiro dia

Firebase Cloud Messaging
  └─ RumoFirebaseMessagingService ← recebe tokens + processa mensagens remotas

NotificationHelper.kt            ← cria canais, constrói e dispara notificações
```

**Canais de notificação (obrigatório API 26+):**
```kotlin
NotificationChannel("rumo_reminders",  "Lembretes de atividade", IMPORTANCE_DEFAULT)
NotificationChannel("rumo_social",     "Notificações sociais",   IMPORTANCE_LOW)
NotificationChannel("rumo_trip_start", "Início de viagem",       IMPORTANCE_HIGH)
```

### Configuração do usuário

Toggle em `SettingsScreen` (DataStore):
- "Lembretes de atividade" — habilita/desabilita `ItineraryReminderWorker`
- "Notificações de amigos" — habilita/desabilita canal `rumo_social`
- "Antecedência do lembrete" — seletor: 15 min / 30 min / 1h (salvo em DataStore como `Int`)

### Permissões

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

Solicitação em runtime (Android 13+): `Manifest.permission.POST_NOTIFICATIONS`.

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Criar | `data/notification/NotificationHelper.kt` |
| Criar | `data/notification/ItineraryReminderWorker.kt` |
| Criar | `data/notification/TripStartWorker.kt` |
| Criar | `data/notification/RumoFirebaseMessagingService.kt` (para notificações remotas) |
| Modificar | `ui/settings/SettingsScreen.kt` — seção de notificações |
| Modificar | `data/preferences/SettingsRepository.kt` — novos campos DataStore |
| Modificar | `AndroidManifest.xml` — permissões + registro do Service FCM |
| Modificar | `data/di/AppModule.kt` — WorkManager binding |
| Criar | `docs/modulo-23-notificacoes.md` |

### Especificação de testes

| Teste | Tipo | O que verificar |
|---|---|---|
| `reminderWorkerScheduledOnActivitySave` | JVM | Salvar atividade com horário → `WorkManager.enqueueUniqueWork()` chamado com tag `activity_{id}` |
| `reminderWorkerCancelledOnActivityEdit` | JVM | Editar horário de atividade → trabalho antigo cancelado (por tag) antes de agendar novo |
| `reminderWorkerCancelledOnActivityDelete` | JVM | Deletar atividade → `WorkManager.cancelUniqueWork("activity_{id}")` chamado |
| `notificationChannelsCreated` | Instrumented | Após `NotificationHelper.createChannels()`, os 3 canais existem em `NotificationManager` |
| `tripStartWorkerFiredOnCorrectDate` | JVM (mock) | `TripStartWorker` agendado para `startDate - hoje` dias a partir de agora |
| `settingsDisablingRemindersSkipsScheduling` | JVM | Toggle "Lembretes" desativado → `ItineraryReminderWorker` não agendado ao salvar atividade |

> **💰 Custo:** WorkManager é **gratuito** (biblioteca Android oficial). Firebase Cloud Messaging é **gratuito sem limites** — não há custo por mensagem enviada, independentemente do volume.

> **⚠ Impacto — permissão `SCHEDULE_EXACT_ALARM` (Android 12+):** em Android 12 (API 31) e superior, a permissão `SCHEDULE_EXACT_ALARM` requer que o usuário acesse `Configurações → Gerenciar alarmes e lembretes` para conceder. A UX recomendada é detectar se a permissão foi concedida antes de tentar agendar, e — se negada — mostrar um `AlertDialog` explicando a importância e com botão "Abrir configurações" que leva diretamente à tela correta via `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. Considerar usar `setInexactRepeating()` (que não exige a permissão) como fallback para dispositivos que não concederem.

### Dependências a adicionar

```kotlin
implementation("androidx.work:work-runtime-ktx:2.9.x")
// FCM (somente se notificações remotas):
implementation("com.google.firebase:firebase-messaging-ktx")
```

---

## F14 — Widget de tela inicial

### Descrição

Widget Android (4×2 células) que exibe a próxima atividade da viagem em curso diretamente na tela inicial do dispositivo. Atualiza a cada 30 minutos ou quando o usuário toca. Requer Jetpack Glance (API oficial para widgets Compose).

### Conteúdo do widget

```
┌─────────────────────────────────────┐
│ ✈ RUMO              [nome viagem]   │
│─────────────────────────────────────│
│  14:30  🍽 Almoço no Colméia        │
│         Próxima atividade           │
│─────────────────────────────────────│
│  16:00  🎡 Parque Bondinho Planaltina│
└─────────────────────────────────────┘
```

- Fundo: `GreenMoss`
- Texto: branco + `AmberPrimary` para horários
- Toque no widget: abre `DayDetailScreen` do dia atual
- Estado "sem viagem ativa": mensagem "Nenhuma viagem em curso"
- Estado "dia concluído": "Todas as atividades de hoje foram concluídas 🎉"

### Arquitetura

```
GlanceAppWidget (RumoWidget.kt)
  └─ GlanceAppWidgetReceiver (RumoWidgetReceiver.kt)
  └─ WorkManager (WidgetUpdateWorker) — atualização periódica a cada 30min
```

O widget acessa o banco Room diretamente via repositório injetado (Hilt não é suportado em Glance diretamente — usar `EntryPointAccessors` para injetar dependência no `GlanceAppWidget`).

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Criar | `ui/widget/RumoWidget.kt` — layout Glance |
| Criar | `ui/widget/RumoWidgetReceiver.kt` — receptor AppWidget |
| Criar | `ui/widget/WidgetUpdateWorker.kt` — WorkManager |
| Criar | `res/xml/rumo_widget_info.xml` — metadados do widget (dimensões, período de update) |
| Criar | `res/drawable/widget_preview.png` — preview na galeria de widgets |
| Modificar | `AndroidManifest.xml` — receiver + intent-filter `APPWIDGET_UPDATE` |
| Criar | `docs/modulo-24-widget.md` |

### Especificação de testes

> **Nota:** widgets Glance são difíceis de testar de forma unitária pura — o `GlanceAppWidget` não é instanciável fora de contexto Android. Usar testes instrumentados ou screenshots de composables extraídos.

| Teste | Tipo | O que verificar |
|---|---|---|
| `widgetShowsNextActivity` | Instrumented | Viagem ativa com atividade futura → widget exibe horário e nome da próxima atividade |
| `widgetShowsNoTripState` | Instrumented | Nenhuma viagem ativa → widget exibe "Nenhuma viagem em curso" |
| `widgetShowsAllDoneState` | Instrumented | Todas as atividades do dia passadas → widget exibe mensagem de conclusão |
| `widgetUpdaterWorkerReschedules` | JVM | `WidgetUpdateWorker.doWork()` retorna `Result.success()` e `AppWidgetManager.updateAppWidget()` é chamado |
| `widgetTapOpensDayDetail` | Instrumented | Toque no widget → `DayDetailScreen` aberta com `tripId` correto |

### Impacto no app existente

- **`EntryPointAccessors`**: injeção de dependência em widgets Glance exige `@InstallIn(SingletonComponent::class)` + `@EntryPoint` — padrão diferente dos `@HiltViewModel` do resto do app; documentar claramente
- **`EditActivityViewModel`**: ao salvar uma atividade, disparar `AppWidgetManager.updateAppWidget()` para forçar refresh imediato do widget (além do refresh periódico de 30 min do WorkManager)
- **`AndroidManifest.xml`**: declarar o receiver com `<intent-filter>` para `APPWIDGET_UPDATE` e referência ao `rumo_widget_info.xml`

### Dependências a adicionar

```kotlin
implementation("androidx.glance:glance-appwidget:1.1.x")
implementation("androidx.glance:glance-material3:1.1.x")
// WorkManager já adicionado em F13
```

> **💰 Custo:** Jetpack Glance é **gratuito** (biblioteca Android oficial, Apache 2.0).

---

## F15 — Backup em nuvem de todas as viagens

### Descrição

Sincronização automática de todas as viagens do usuário com Firebase Storage, permitindo recuperação em caso de troca de aparelho ou desinstalação do app. Requer conta de usuário (F2). O backup é incremental (apenas viagens modificadas desde o último backup) e silencioso (sem interação do usuário para o backup automático).

### Arquitetura

```
BackupRepository.kt
  ├─ backup(tripId)    ← exporta .travel via TravelExporter → faz upload para Storage
  ├─ restoreAll()      ← lista arquivos em Storage → baixa → importa via TravelImporter
  └─ scheduleAutoBackup() ← WorkManager diário
```

**Firebase Storage path:**
```
users/{uid}/backups/{tripUuid}.travel
```

A deduplicação por `tripUuid` (F1) garante que restaurar um backup não cria viagens duplicadas.

### Configuração do usuário

Seção "Backup" em `SettingsScreen`:
- "Backup automático diário" — toggle (DataStore)
- "Fazer backup agora" — botão que dispara backup imediato com progress dialog
- "Restaurar viagens" — baixa todos os `.travel` do Storage e importa (pula duplicatas via tripUuid)
- "Último backup: [data/hora]" — exibido abaixo do toggle

### Casos de uso

| Ação | Comportamento |
|---|---|
| Backup automático | WorkManager diário → `BackupRepository.backupAll()` → upload silencioso |
| Backup manual | Botão "Fazer backup agora" → progress dialog → snackbar de confirmação |
| Restaurar | Lista arquivos em Storage → download → `TravelImporter.import()` para cada um → relatório final ("X viagens restauradas, Y já existiam") |
| Troca de aparelho | Usuário instala app novo, faz login → toca "Restaurar viagens" → todas as viagens voltam |

### Especificação de testes

| Teste | Tipo | O que verificar |
|---|---|---|
| `backupUploadsToCorrectStoragePath` | Instrumented / Emulator | `BackupRepository.backup(tripId)` → arquivo em `users/{uid}/backups/{tripUuid}.travel` no Storage |
| `restoreSkipsDuplicateTrips` | JVM (mock) | Trip com UUID já no banco local → `TravelImporter.import()` detecta duplicata; contagem de "já existiam" incrementada |
| `restoreCreatesNewTrip` | Instrumented / Emulator | Banco local vazio → `restoreAll()` importa 1 arquivo → viagem aparece no banco |
| `backupWorkerRunsOnce` | JVM | `BackupWorker` com `ExistingPeriodicWorkPolicy.KEEP` → segunda chamada não cria novo trabalho |
| `lastBackupAtUpdatedAfterSuccess` | JVM (mock) | `backup()` bem-sucedido → `SettingsRepository.lastBackupAt` atualizado com timestamp |
| `backupFailsGracefullyWithNoInternet` | JVM (mock) | Upload lança `IOException` → `BackupRepository` retorna `Result.failure()`; WorkManager agenda retry |

### Impacto no app existente

- **`TravelExporter`** e **`TravelImporter`**: reutilizados integralmente pelo `BackupRepository` — zero duplicação de lógica de serialização
- **`F1 (tripUuid)`**: o backup **depende** de F1 para funcionar corretamente — sem UUID, restaurar geraria duplicatas inevitáveis. F15 deve ser implementada após F1.
- **`SettingsScreen`**: nova seção "Backup" com toggle, botão e timestamp — verificar impacto de layout em telas pequenas (adicionar divider e scroll se necessário)
- **Permissão de internet**: sem mudança de permissão (INTERNET já declarado no manifesto pelo Firebase)

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Criar | `data/repository/BackupRepository.kt` |
| Criar | `data/notification/BackupWorker.kt` — WorkManager diário |
| Modificar | `ui/settings/SettingsScreen.kt` — seção "Backup" |
| Modificar | `data/preferences/SettingsRepository.kt` — campos `autoBackupEnabled`, `lastBackupAt` |
| Modificar | `data/di/AppModule.kt` — `@Provides` para `BackupRepository` |
| Criar | `docs/modulo-25-backup.md` |

### Dependências a adicionar

Firebase Storage já listado em F2. WorkManager já listado em F13.

> **💰 Custo — Firebase Storage para backups:** cada arquivo `.travel` tem tipicamente 50 KB a 2 MB (dependendo do número de dias, atividades e arquivos anexados). Com 100 usuários e 5 viagens cada = 500 arquivos × 1 MB médio = ~0,5 GB. O Spark Plan tem 5 GB gratuitos, então atingirá o limite com ~5.000 arquivos. No plano Blaze: USD 0,026/GB — custo baixo mesmo em escala. Incluir no plano Pro (M7), não no plano Free.

---

## F16 — Templates de roteiro

### Descrição

Biblioteca de roteiros prontos curados pelo time Rumo, pela comunidade (F10) e por agências (F11). O usuário pode navegar por templates filtrados por destino, duração e tipo de viagem (família, aventura, romântico, etc.), visualizar o roteiro completo e importar como base para uma nova viagem com um toque.

### Fontes de templates

| Fonte | Como é criado | Exibido em |
|---|---|---|
| Curado (equipe Rumo) | Documento Firestore criado manualmente com flag `isOfficial: true` | Seção "Em destaque" |
| Comunidade | Post em F10 com flag `isTemplate: true` (autor marca ao publicar) | Seção "Da comunidade" |
| Agência | Viagem criada em F11 com flag `isTemplate: true` (somente assinantes) | Seção "De agências" |

### Modelo de dados (Firestore)

**`templates/{templateId}`:**
```
title: String
destination: String
durationDays: Int
tags: List<String>           // ["família", "montanha", "inverno"]
source: String               // "official" | "community" | "agency"
authorUid: String?
agencyId: String?
tripJson: String             // JSON no formato do TravelExporter — reutiliza TravelImporter
coverEmoji: String
likesCount: Int
importCount: Int
createdAt: Timestamp
```

### Arquitetura

```
TemplateRepository.kt
  ├─ getTemplates(filter)     ← query Firestore com paginação
  ├─ getTemplateDetail(id)    ← download do tripJson
  └─ importTemplate(id)       ← TravelImporter.import() com novo tripUuid gerado
```

**Reutiliza `TravelImporter` diretamente** — o `tripJson` armazenado no Firestore segue o mesmo schema do `trip.json` interno do `.travel`. Importar um template = criar uma nova viagem a partir dele, com novo `tripUuid`.

### Telas

| Tela | Conteúdo |
|---|---|
| `TemplateLibraryScreen` | Lista com seções (Em destaque / Comunidade / Agências), filtros por tag e destino |
| `TemplateDetailScreen` | Preview do roteiro dia a dia, botão "Usar este roteiro" |

Acessível por botão "Ver templates" em `CreateTripScreen` (passo 4) e por aba na `TripsListScreen`.

### Casos de uso

| # | Cenário | Comportamento esperado |
|---|---|---|
| UC-F16-01 | Acessar biblioteca de templates sem estar logado | Seção "Em destaque" (oficial) disponível; seções "Comunidade" e "Agências" exibem CTA para criar conta |
| UC-F16-02 | Filtrar por destino "Gramado" | Templates com `destination` contendo "gramado" (busca insensível) filtrados |
| UC-F16-03 | Filtrar por tag "família" | Templates com `tags` contendo "família" exibidos |
| UC-F16-04 | Visualizar preview do roteiro | `TemplateDetailScreen` lista dias e atividades sem importar |
| UC-F16-05 | Importar template | `TravelImporter.import()` chamado com o `tripJson` do template → nova viagem criada com novo `tripUuid` |
| UC-F16-06 | Importar template já importado antes | `tripUuid` gerado é novo (cada importação gera UUID novo) — sem detecção de duplicata |
| UC-F16-07 | Agência publica template | Requer plano Agência (M7) — botão oculto para usuários sem plano |

### Especificação de testes

| Teste | Tipo | O que verificar |
|---|---|---|
| `templatesLoadedFromFirestore` | Instrumented / Emulator | `getTemplates(filter)` retorna lista com documentos corretos da coleção `templates/` |
| `importTemplateCreatesNewTrip` | JVM (mock) | `importTemplate(id)` → `TravelImporter.import()` chamado; trip com novo UUID no banco |
| `importedTripHasDifferentUuid` | JVM | Importar o mesmo template duas vezes → dois `tripUuid` distintos no banco |
| `officialTemplatesVisibleWithoutLogin` | JVM | `getTemplates(source = "official")` não requer `AuthRepository.isLoggedIn()` |

### Impacto no app existente

- **`CreateTripScreen` (passo 4)**: adição de botão "Ver templates" cria um terceiro ponto de entrada para a tela (além de chat IA e importar JSON) — revisar o layout do passo 4 para acomodar sem sobrecarregar
- **`TripsListScreen`**: nova aba ou botão de acesso à biblioteca — avaliar se merece aba própria ou acesso via menu
- **`TravelImporter`**: uso indireto via `TemplateRepository` — sem modificações necessárias no importer

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Criar | `data/repository/TemplateRepository.kt` |
| Criar | `ui/templates/TemplateLibraryScreen.kt` + `TemplateLibraryViewModel.kt` |
| Criar | `ui/templates/TemplateDetailScreen.kt` + `TemplateDetailViewModel.kt` |
| Modificar | `ui/trips/CreateTripScreen.kt` — botão "Ver templates" no passo 4 |
| Modificar | `navigation/AppNavigation.kt` — rotas de templates |
| Criar | `docs/modulo-26-templates.md` |

---

## F17 — Avaliação de atividades

### Descrição

Após o término de uma viagem, o usuário pode avaliar cada atividade com 👍 ou 👎 (ou escala 1–5 estrelas). As avaliações ficam salvas localmente e, opcionalmente, contribuem para a avaliação agregada da atividade na comunidade (F10), ajudando outros usuários a escolherem roteiros melhores.

### Modelo de dados

**Room — campo adicional em `ActivityEntity`:**
```
rating: Int?       // null = não avaliada; 1-5 = avaliação
ratingNote: String // observação opcional do usuário
```
Requer Migration 18 → 19 (ou N → N+1 conforme a última migration vigente).

**Firestore (opcional, para avaliação agregada):**
```
activities/{activityId}/ratings/{uid}
  rating: Int
  note: String
  tripUuid: String
  createdAt: Timestamp
```

### UI

A avaliação é exibida ao final da viagem (quando `endDate < hoje`):
- Em `DayDetailScreen`: cada card de atividade exibe estrelas clicáveis ou ícones 👍/👎 no rodapé do card
- Em `HomeScreen`: banner "Como foi sua viagem? Avalie as atividades" no topo quando a viagem terminou há menos de 7 dias
- Avaliações positivas (⭐⭐⭐⭐⭐) exibem uma sugestão: "Quer compartilhar este roteiro com a comunidade?" (link para F10)

### Casos de uso

| Ação | Comportamento |
|---|---|
| Avaliar atividade | `ActivityRepository.updateRating(id, rating, note)` — gravação local imediata |
| Contribuir para comunidade | Upload da avaliação para Firestore (opcional, com opt-in explícito) |
| Ver resumo de avaliações | Card de resumo em `HomeScreen` após fim da viagem: X atividades ótimas, Y regulares |

### Especificação de testes

| Teste | Tipo | O que verificar |
|---|---|---|
| `ratingPersistedCorrectly` | JVM | `ActivityRepository.updateRating(id, 4, "Ótimo!")` → recarregar do banco → `rating == 4`, `ratingNote == "Ótimo!"` |
| `ratingNullByDefault` | JVM | Atividade criada sem avaliação → `rating == null` |
| `updateRatingDoesNotAffectOtherFields` | JVM | Após `updateRating()`, nome, horário e badges da atividade permanecem intactos |
| `ratingUiVisibleOnlyAfterTripEnd` | JVM | `endDate` no futuro → `showRatingControls = false`; `endDate` no passado → `showRatingControls = true` |
| `postTripBannerShownWithin7Days` | JVM | `endDate` = hoje − 3 dias → `HomeScreen` emite `showRatingBanner = true` |
| `postTripBannerHiddenAfter7Days` | JVM | `endDate` = hoje − 8 dias → `showRatingBanner = false` |
| `communityUploadOptIn` | JVM (mock) | Sem opt-in explícito → `ActivityRepository.uploadRatingToCommunity()` não chamado |

### Impacto no app existente

- **`ActivityEntity` e `Mappers.kt`**: novos campos `rating` (nullable Int) e `ratingNote` (String default "") — migration obrigatória
- **`DayDetailScreen`**: UI de avaliação é condicional (`endDate < today`) — sem impacto visual em viagens futuras ou em andamento
- **`HomeScreen`**: banner de pós-viagem ocupa espaço no topo — verificar interação com card de clima e card de hotel; garantir que aparece apenas quando a viagem está nos últimos 7 dias pós-conclusão
- **`TripViewModel`**: nova propriedade computada `isPostTrip: Boolean` derivada de `endDate`

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Modificar | `data/db/TravelDatabase.kt` — Migration N→N+1, campos `rating`, `ratingNote` em `ActivityEntity` |
| Modificar | `data/db/Mappers.kt` — ambas as direções |
| Modificar | `data/repository/ActivityRepository.kt` — método `updateRating()` |
| Modificar | `ui/day/DayDetailScreen.kt` — estrelas/👍👎 no rodapé de cada atividade (visível só após fim da viagem) |
| Modificar | `ui/home/HomeScreen.kt` — banner de avaliação pós-viagem |
| Modificar | `ui/trips/TripViewModel.kt` — `isPostTrip: Boolean` |
| Criar | `docs/modulo-27-avaliacoes.md` |

---

## Roadmap de entrega — fases e cronologia

Este roadmap organiza funcionalidades e marcos em fases de entrega, considerando dependências técnicas, valor para o usuário e risco de implementação. A sequência é uma recomendação — ajuste conforme disponibilidade e prioridades de negócio.

### Visão geral das fases

```
FASE 0 (contínua)          FASE 1                  MARCO M1         FASE 2                  FASE 3
Preparação técnica    →    Funcionalidades     →    Play Store   →   Social + Nuvem      →   Escala e receita
M3 · M4 · M5               offline (Trilha A)       (lançamento)     (Trilha B)               (Trilha C + M7)
Sem produto entregável     F1 F4 F12 F5 F6          M6 · M8                                   F11 F16 M9
                           F7 F8 F13 F14 F17        ↑ pré-requisito  F2 F3 F9 F10 F15
                                                    para tudo abaixo F13(FCM) F6+ F17+
```

### Critérios de priorização

| Critério | Peso | Justificativa |
|---|---|---|
| Valor imediato para o usuário | Alto | Funcionalidades offline entregam valor desde o dia 1, sem backend |
| Independência técnica | Alto | Sem Firebase = menos risco, menos custo, APK menor |
| Ser pré-requisito de outras | Alto | F1 desbloqueiam F15/F10/F11; F2 desbloqueiam toda a Trilha B |
| Impacto no APK size | Médio | Adicionar Firebase Auth/Firestore aumenta o APK em ~5 MB |
| Custo de infraestrutura | Médio | Funcionalidades Firebase geram custo variável de operação |

---

### Fase 0 — Preparação técnica (imediata, contínua)

*Objetivo: estabelecer a base de qualidade antes de qualquer release público. Nenhum entregável de produto — o resultado é a confiabilidade do processo de desenvolvimento.*

| Item | Esforço | Quando configurar | Por quê agora |
|---|---|---|---|
| **M3** CI/CD com GitHub Actions | Pequeno | Antes do 1º merge relevante | Garante que testes passam antes de cada merge; evita regressões silenciosas |
| **M4** Crashlytics | Pequeno | Antes do 1º release externo | Impossível depurar crashes de produção sem stack trace |
| **M4** Analytics (eventos básicos) | Pequeno | Antes do 1º release externo | Entender como o app é usado desde o início; dados históricos são valiosos |
| **M5** Auditoria de acessibilidade | Médio | Antes de M1 | Exigência da Play Store; muito mais difícil corrigir após o lançamento |

> **Nota:** M3 e M4 são independentes — podem ser configurados em paralelo.

---

### Fase 1 — Funcionalidades offline (Trilha A)

*Objetivo: publicar o app na Play Store com um conjunto rico de funcionalidades locais, sem nenhuma dependência de Firebase. Custo de infraestrutura = zero.*

**Resultado esperado ao final da fase:** app completo e publicável com 10 funcionalidades independentes.

| # | Funcionalidade | Esforço | Depende de | Valor principal entregue |
|---|---|---|---|---|
| **F1** | Identificador de viagem | Pequeno | — | Elimina duplicatas de import; `tripUuid` base para F15, F10, F11 |
| **F4** | Tela de notas | Médio | — | Organização; substitui papel e anotações no WhatsApp |
| **F12** | Geração de PDF | Médio | — | Distribuição profissional de roteiros; especialmente útil para agências |
| **F5** | Controle de orçamento | Médio | — | Controle financeiro integrado à viagem |
| **F6** | Divisão de custos (local) | Médio | F5 (recomendado) | Resolve "quem deve quanto" sem precisar de servidor |
| **F7** | O que tem perto | Médio | Chave Foursquare | Descoberta espontânea de locais durante a viagem |
| **F8** | Assistente IA | Pequeno | Gemini (SDK já presente) | Suporte contextual em tempo real; quase zero de trabalho novo |
| **F13** | Notificações locais | Médio | — | Lembretes de atividades; alerta de check-in de voo |
| **F14** | Widget | Médio | F13 (WorkManager) | Acesso ao dia atual sem abrir o app |
| **F17** | Avaliação de atividades (local) | Pequeno | F1 (recomendado) | Feedback pós-viagem; base para avaliações comunitárias em F10 |

**Sequência recomendada dentro da Fase 1:**

```
F1 → F4 → F12 → F5 → F6 → F7 → F8 → F13 → F14 → F17
 ↑           ↑         ↑                ↑     ↑
base      scope    APIs externas    WorkManager  ordem
de tudo   menor;   (configurar      antes de     natural
           entrega  chaves antes)   F14
           rápida
```

- **F1 primeiro:** `tripUuid` é campo pequeno mas desbloqueante — F6, F15, F10 e F11 dependem dele para deduplicação e identificação cross-device.
- **F4 e F12 antes de F5/F6:** escopo menor, entregam valor rápido enquanto a mente ainda está "aquecendo".
- **F7 e F8 no meio:** dependem de configuração de chaves de API externas (Foursquare, Geoapify); é melhor ter tempo para configurar contas e testar quotas antes de precisar delas.
- **F13 antes de F14:** F14 (widget) usa WorkManager, que é introduzido em F13.
- **F17 no final:** pequeno escopo, beneficia de F1 estar pronto para associar avaliações por `tripUuid`.

---

### Marco M1 — Publicação na Google Play Store

*Gate: Fase 1 completa (ou ao menos F1 + F4 + F8 como MVP mínimo) + M3 + M4 + M5.*

| Ação de suporte | Timing | Obrigatório? |
|---|---|---|
| **M6** Internacionalização | Antes do lançamento internacional; opcional para V1 em pt-BR | Não para V1 |
| **M8** Site de marketing | Em paralelo com M1; ideal lançar no mesmo dia | Não (mas recomendado) |
| Política de privacidade | Antes de qualquer release com analytics | **Sim** |
| Ícone final + screenshots | Antes de submeter para revisão | **Sim** |
| `google-services.json` no `.gitignore` | Antes do 1º commit com Firebase | **Sim** |

> **Estratégia de rollout:** Internal Testing (até 100 e-mails) → Open Testing (beta pública) → Produção com rollout de 10% → 50% → 100%. Cada etapa revela crashes que a anterior não encontrou.

---

### Fase 2 — Funcionalidades sociais e nuvem (Trilha B)

*Objetivo: adicionar identidade de usuário, colaboração e comunidade após ter usuários reais e dados de uso.*

**Pré-requisito:** M1 publicado, conta Firebase no plano Blaze (necessário para escala e para Firebase Functions em M7).  
**Resultado esperado:** usuários criam conta, compartilham roteiros, fazem backup na nuvem e descobrem roteiros de outros.

| # | Funcionalidade | Esforço | Depende de | Firebase envolvido |
|---|---|---|---|---|
| **F2** | Cadastro de usuários | Grande | — | Auth + Firestore + Storage |
| **F3** | Pessoas na viagem | Grande | F1 + F2 | Firestore |
| **F9** | Seguir amigos | Médio | F2 | Firestore |
| **F10** | Área da comunidade | Grande | F2 + F9 | Firestore + Storage |
| **F15** | Backup em nuvem | Médio | F1 + F2 | Storage (usa TravelExporter existente) |
| **F6+** | Vincular divisão a participantes | Pequeno | F3 | — (Room) |
| **F13+** | Notificações sociais (FCM) | Pequeno | F2 | FCM (gratuito) |
| **F17+** | Avaliações comunitárias | Pequeno | F2 + F10 | Firestore |

**Sequência obrigatória na Fase 2:**

```
F2
├── F3 ──────────────────────────── F6+ (upgrade local → participantes)
│    └─ (libera F11 na Fase 3)
├── F9 ──── F10 ─────────────────── F17+ (avaliações comunitárias)
│            └─ (libera F16, F11 na Fase 3)
└── F15     F13+ (FCM)

F3 e F9 podem rodar em paralelo após F2.
F15, F6+, F13+, F17+ podem rodar em paralelo após seus pré-requisitos.
```

> **Atenção Firebase:** ao iniciar F2, migrar a conta para o plano Blaze. O plano não tem custo fixo — cobra apenas o que exceder os limites gratuitos (50k leituras Firestore/dia, 5 GB Storage etc.). Monitorar o console Firebase ativamente nas primeiras semanas.

---

### Fase 3 — Escala, agências e monetização (Trilha C)

*Objetivo: transformar o app em produto com receita sustentável, com segmento B2B.*

**Pré-requisito:** Fase 2 com usuários ativos e métricas de retenção positivas. F10 com comunidade suficientemente ativa para justificar o modo agência.

| # | Funcionalidade / Marco | Esforço | Depende de | Resultado de negócio |
|---|---|---|---|---|
| **M7** | Monetização (Play Billing) | Grande | F2 + base de usuários | Receita recorrente; planos Free / Pro / Agência |
| **F11** | Modo agência | Grande | F2 + F3 + F10 + M7 | Segmento B2B; receita do plano Agência |
| **F16** | Templates de roteiro | Médio | F1 + F10 (+ F11 para templates de agências) | Descoberta de roteiros; onboarding mais rápido |
| **M9** | Programa de parceria | Processo | F11 estável | 20 agências parceiras em 6 meses |

**Sequência recomendada na Fase 3:**

```
M7 → F11 → F16 → M9
 ↑     ↑     ↑     ↑
billing  enforcement  templates  vendas
Play     de plano     de agência B2B
```

> **Nota M7:** Firebase Functions (necessário para validação segura de assinaturas) requer o plano Blaze — que já estará ativo desde a Fase 2. Sem Functions, usar `BillingClient.queryPurchasesAsync()` client-side como fallback temporário (menos seguro, mas funcional para fase inicial).

---

### Cronologia indicativa

*Assume um desenvolvedor solo em tempo parcial (~10h/semana). Ajustar conforme disponibilidade real.*

| Período | Fase | Entregáveis |
|---|---|---|
| Meses 1–2 | Fase 0 + início Fase 1 | CI/CD configurado; Crashlytics ativo; F1, F4, F12 implementados |
| Meses 3–4 | Fase 1 (meio) | F5, F6, F7 implementados; primeiros testes internos com conhecidos |
| Meses 5–6 | Fase 1 (fim) + M1 | F8, F13, F14, F17 implementados; M5 auditado; app na Play Store |
| Meses 7–8 | Fase 2 (início) | F2 (auth); M8 (site); M6 (se necessário para expansão) |
| Meses 9–10 | Fase 2 (meio) | F3 (participantes); F9 (seguir); F10 (comunidade) |
| Meses 11–12 | Fase 2 (fim) | F15, F13 FCM, F6+, F17+; versão social completa |
| Ano 2 | Fase 3 | M7, F11, F16, M9 conforme crescimento da base de usuários |

> **Calibragem de esforço:** F8 (Gemini SDK já existe no projeto) e F12 (biblioteca nativa Android) podem ser concluídas em poucos dias. F10 (comunidade) e F11 (agência) são as mais complexas e podem levar mais de um mês cada. F1 (campo UUID + migration) é o menor trabalho com o maior impacto estratégico.

---

### Mapa de dependências críticas

```
F1 (tripUuid)
├── F15  — sem UUID o backup gera duplicatas em import
├── F10  — import da comunidade usa UUID para deduplicar viagens
└── F11  — campo agencyId referencia a viagem por tripUuid

F2 (autenticação)
├── F3   — participantes requerem identidade
│    └── F6+  — vincular despesas a participantes
│    └── F11  — modo agência gerencia trips de terceiros
├── F9   — seguir requer perfil de usuário
│    └── F10  — feed filtrado por "Seguindo"
│         └── F16  — templates da comunidade
│         └── F11  — plataforma de agências com comunidade ativa
├── F15  — backup armazenado em Storage por uid
└── M7   — planos de assinatura vinculados ao uid

M3 (CI/CD) ─────────────────────────────────────── M1 (release automatizado)
M4 (Crashlytics) ───────────────────────────────── M1 (debug em produção)
M5 (acessibilidade) ────────────────────────────── M1 (Play Store policy)
M7 (Billing + planos) ──────────────────────────── F11 (enforcement do plano Agência)
F10 (comunidade ativa) ─────────────────────────── F11 (proposta de valor do modo agência)
```

---

## Ordem de implementação recomendada

```
Trilha A — Funcionalidades independentes (Room puro / APIs sem Firebase):
  F1  — Identificador de viagem    (escopo pequeno; elimina duplicatas de import)
  F4  — Tela de notas              (valor imediato para organização)
  F5  — Controle de orçamento      (valor imediato para controle financeiro)
  F6  — Divisão de custos          (complementa F5; modo local sem dependências)
  F7  — O que tem perto de mim?    (GPS + Foursquare; gratuita; independente de tudo)
  F8  — Assistente de viagem (IA)  (Gemini já disponível; zero dependências novas)
  F12 — Geração de PDF             (biblioteca nativa Android; sem dependências novas)
  F13 — Notificações locais        (WorkManager; parte remota opcional após F2)
  F14 — Widget de tela inicial     (Jetpack Glance; sem dependências novas)
  F17 — Avaliação de atividades    (Room local; contribuição comunitária opcional após F10)

Trilha B — Funcionalidades sociais (exigem Firebase; depois da Trilha A):
  F2  — Cadastro de usuários       (base de identidade para todo o restante da Trilha B)
  F3  — Pessoas na viagem          (depende de F1 + F2; Security Rules em paralelo)
  F9  — Adicionar amigos           (depende de F2; base do filtro "Seguindo" em F10)
  F10 — Área da comunidade         (depende de F2 + F9; conecta ao import de F1)
  F15 — Backup em nuvem            (depende de F2; reutiliza TravelExporter + TravelImporter)
  F6  conectado                    (upgrade: vincular membros de divisão a participantes de F3)
  F13 remoto                       (FCM para notificações sociais — convites, novos posts)
  F17 comunitário                  (contribuição de avaliações para Firestore)

Trilha C — Funcionalidades para escala e agências (após Trilha B estar estável):
  F11 — Modo agência               (depende de F2 + F3 + F10; segmento B2B)
  F16 — Templates de roteiro       (depende de F1 + F10; agências conectam via F11)
```

**Sequência sugerida dentro da Trilha A:** F1 → F4 → F12 → F5 → F6 → F7 → F8 → F13 → F14 → F17.

**Sequência obrigatória na Trilha B:** F2 → (F3 e F9 em paralelo) → F10 → (F15, F13 remoto, F17 comunitário em paralelo).

**Sequência da Trilha C:** F11 → F16. Ambas exigem que F10 já esteja publicado e com usuários ativos.

**Marcos de infraestrutura (recomendado antes ou durante as trilhas):**
```
M3 — CI/CD                    (configurar antes de qualquer release público)
M4 — Analytics e monitoramento (configurar antes de M1)
M5 — Acessibilidade / a11y    (revisão antes de M1)
M6 — Internacionalização      (antes de expansão internacional)
M1 — Publicação Play Store    (após Trilha A completa + M3 + M4 + M5)
M7 — Monetização              (após base de usuários estabelecida pós-M1)
M8 — Site de marketing        (em paralelo com M1)
M9 — Programa de parceria     (após F11 estável e primeiros clientes agência)
```

> **Pré-requisito transversal:** antes de publicar na Play Store (M1), garantir que todas as funcionalidades implementadas passaram pela lista de verificação de M1.

---

## M1 — Publicação na Google Play Store

### Descrição

Publicar o app **Rumo** na Google Play Store como app de acesso público (ou closed testing inicialmente), permitindo que qualquer pessoa com Android instale, atualize e avalie o app. Não é uma funcionalidade de produto, mas um marco de infraestrutura que exige preparação técnica, editorial e de processo.

### Pré-requisitos de conta e configuração

| Item | Detalhe |
|---|---|
| Conta Google Play Developer | Taxa única de US$ 25; associada a um e-mail Google; cadastro em [play.google.com/console](https://play.google.com/console) |
| Aceite dos termos de desenvolvedor | Feito na criação da conta |
| Informações de contato públicas | E-mail de suporte obrigatório na página do app na Store |
| Conta de faturamento | Necessária mesmo para apps gratuitos (para compras futuras, se houver) |

### Preparação técnica do app

#### 1. Application ID e nome definitivos

O package atual é `com.rodrigoleao.gramado2026`. Para um app genérico de viagens chamado **Rumo**, o ideal é mudar para algo como `com.rodrigoleao.rumo` antes da primeira publicação — **impossível mudar depois sem perder todos os usuários e histórico de avaliações**.

Passos:
- Alterar `applicationId` em `build.gradle.kts`
- Renomear o pacote base em todos os arquivos Kotlin (`refactor → rename` no Android Studio)
- Atualizar `AndroidManifest.xml`, `file_paths.xml` (authority do FileProvider) e qualquer string hardcoded com o package name

#### 2. Assinatura do APK (release keystore)

A Play Store exige APKs/AABs assinados com uma chave de release (diferente da debug key).

```bash
# Gerar keystore (fazer uma vez; guardar em local seguro — NÃO versionar)
keytool -genkey -v -keystore rumo-release.jks \
        -alias rumo -keyalg RSA -keysize 2048 -validity 10000

# build.gradle.kts — configurar signing config
android {
    signingConfigs {
        create("release") {
            storeFile     = file("rumo-release.jks")
            storePassword = providers.gradleProperty("STORE_PASSWORD").get()
            keyAlias      = "rumo"
            keyPassword   = providers.gradleProperty("KEY_PASSWORD").get()
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

Senhas em `~/.gradle/gradle.properties` (fora do repositório):
```
STORE_PASSWORD=...
KEY_PASSWORD=...
```

**Google Play App Signing (recomendado):** fazer upload do keystore para o Google gerenciar. O Google re-assina cada APK distribuído; o desenvolvedor mantém uma upload key separada. Protege contra perda do keystore.

#### 3. Formato de distribuição: AAB em vez de APK

A Play Store exige **Android App Bundle (`.aab`)** desde agosto 2021 para novos apps.

```bash
./gradlew bundleRelease
# Saída: app/build/outputs/bundle/release/app-release.aab
```

#### 4. ProGuard / R8

Ativar minificação e ofuscação no build de release:

```kotlin
buildTypes {
    release {
        isMinifyEnabled   = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

Regras a adicionar em `proguard-rules.pro`:
- Manter classes Room (entities, DAOs): `-keep class * extends androidx.room.*`
- Manter classes Gson/Moshi se usadas para serialização
- Manter classes de modelo de dados usadas com reflexão
- Manter `BuildConfig`

Verificar: rodar o AAB de release em dispositivo/emulador antes de fazer upload, pois ProGuard pode quebrar reflexão.

#### 5. Versionamento

Estabelecer convenção de `versionCode` e `versionName` em `build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        versionCode = 1          // inteiro incremental; nunca pode diminuir na Store
        versionName = "1.0.0"   // exibido ao usuário; formato semver sugerido
    }
}
```

A cada atualização publicada: incrementar `versionCode` (obrigatório) e atualizar `versionName` (recomendado seguir semver: `MAJOR.MINOR.PATCH`).

#### 6. Chaves de API — não expor no build

Verificar que nenhuma chave secreta (`GEMINI_API_KEY` etc.) está hardcoded no código ou no `AndroidManifest.xml`. O padrão atual via `local.properties` → `BuildConfig` é correto; confirmar que `local.properties` está no `.gitignore`.

Para CI/CD (se configurado futuramente): injetar segredos via variáveis de ambiente do GitHub Actions/GitLab CI, não via arquivo commitado.

### Conteúdo obrigatório na Play Console

#### Ficha do app (Store Listing)

| Campo | Obrigatório | Recomendação |
|---|---|---|
| Nome do app | Sim | "Rumo — Roteiros de Viagem" (máx. 30 chars) |
| Descrição curta | Sim | Até 80 chars; aparece nos resultados de busca |
| Descrição longa | Sim | Até 4.000 chars; incluir funcionalidades principais e palavras-chave |
| Ícone | Sim | 512×512 px, PNG, sem transparência |
| Capturas de tela | Sim | Mínimo 2 por tipo de dispositivo (phone obrigatório); recomendado 4–6 |
| Gráfico de recurso (feature graphic) | Sim | 1024×500 px; exibido no topo da ficha |
| Categoria | Sim | Viagens e informações locais |
| Tags | Não | viagem, roteiro, planejamento, itinerário |
| E-mail de suporte | Sim | E-mail público exibido na ficha |
| Política de privacidade | Sim | URL de página pública (ver abaixo) |

#### Política de privacidade

Obrigatória para qualquer app na Play Store. Deve descrever:
- Quais dados o app coleta (após F2: e-mail, nome, telefone, foto)
- Como são armazenados (local / Firebase)
- Com quem são compartilhados
- Como o usuário pode solicitar exclusão dos dados

Para a versão inicial (sem F2), o app é local e não coleta dados pessoais — a política pode ser simples. Após F2, deve ser atualizada para cobrir Firebase Auth e Firestore.

Opções: criar uma página estática no GitHub Pages, Notion público, ou Google Sites.

#### Classificação de conteúdo (IARC)

Preencher questionário de classificação etária na Play Console. Para um app de planejamento de viagens sem conteúdo sensível, a classificação esperada é **Livre** (todos os públicos).

#### Declaração de privacidade de dados (Data Safety)

Seção obrigatória desde 2022. Declarar:
- Se o app coleta dados
- Se os dados são compartilhados com terceiros
- Se o usuário pode solicitar exclusão

Para versão inicial sem F2: declarar que nenhum dado é coletado. Atualizar ao implementar F2.

### Processo de publicação — passo a passo

```
1. Criar conta Google Play Developer
   └─ play.google.com/console → pagar taxa → verificar identidade

2. Criar app na Play Console
   ├─ Tipo: App (não jogo)
   ├─ Distribuição: gratuito (não pode mudar para pago depois)
   └─ País principal: Brasil

3. Preparar o AAB de release
   └─ ./gradlew bundleRelease → testar em dispositivo físico

4. Preencher toda a ficha do app
   ├─ Store Listing (textos, ícone, screenshots)
   ├─ Classificação de conteúdo (questionário IARC)
   └─ Data Safety (declaração de dados)

5. Configurar trilha de teste (recomendado antes do lançamento público)
   ├─ Internal testing (até 100 e-mails; aprovação imediata; ideal para testes iniciais)
   ├─ Closed testing / Alpha (grupo maior; aprovação em horas)
   └─ Open testing / Beta (qualquer usuário; aprovação em horas)

6. Fazer upload do AAB na trilha escolhida
   └─ Play Console → Release → selecionar trilha → Create release → Upload AAB

7. Revisão do Google
   ├─ Internal testing: aprovação geralmente em minutos/horas
   └─ Produção: revisão pode levar de horas a poucos dias (primeira publicação costuma ser mais longa)

8. Lançamento em produção
   └─ Após aprovação na beta, promover release para produção (100% dos usuários ou rollout gradual)
```

### Estratégia de rollout recomendada

```
Fase 1 — Internal testing (sem revisão demorada)
  Convidar até 100 testadores via e-mail
  Coletar feedback de usabilidade e bugs
  Duração: 1–2 semanas

Fase 2 — Open testing / Beta pública
  Qualquer usuário pode optar por ser testador
  Link público para instalar a beta
  Coletar avaliações e relatórios de crash (via Firebase Crashlytics — adicionar)
  Duração: 2–4 semanas

Fase 3 — Produção com rollout gradual
  Publicar para 10% dos usuários → monitorar métricas → expandir para 100%
  Rollout gradual permite reverter sem afetar todos os usuários
```

### Monitoramento pós-publicação

| Ferramenta | O que monitorar |
|---|---|
| Play Console → Android Vitals | ANRs (travamentos), crash rate, jank rate |
| Play Console → Reviews | Avaliações e comentários de usuários |
| Firebase Crashlytics (adicionar) | Stack traces de crashes em produção, com versão e dispositivo |
| Firebase Analytics (opcional) | Funis de uso, telas mais visitadas, retenção |

**Firebase Crashlytics** é fortemente recomendado antes de publicar — crashes em produção sem stack trace são impossíveis de depurar. Adicionar:

```kotlin
implementation("com.google.firebase:firebase-crashlytics-ktx")
// + apply plugin: "com.google.firebase.crashlytics" no build.gradle.kts
```

### Atualizações futuras

Cada atualização segue o mesmo fluxo: incrementar `versionCode` → gerar AAB → upload na Play Console → aguardar revisão → publicar. O tempo de revisão de atualizações costuma ser bem menor que o da publicação inicial.

Considerar configurar **CI/CD** (GitHub Actions) para automatizar o build e upload do AAB ao fazer merge na branch `main`:
- Plugin Gradle `com.github.triplet.play` (Gradle Play Publisher) automatiza o upload via API da Play Console
- Segredos (keystore, senhas, `google-services.json`) injetados como GitHub Actions secrets

### Lista de verificação antes da publicação

- [ ] `applicationId` definitivo (`com.rodrigoleao.rumo` ou similar)
- [ ] `versionCode = 1`, `versionName = "1.0.0"`
- [ ] Keystore de release gerado e guardado em local seguro (fora do repositório)
- [ ] Google Play App Signing configurado na Play Console
- [ ] Build de release testado em dispositivo físico (não só emulador)
- [ ] ProGuard ativado; app funciona corretamente com minificação
- [ ] Nenhuma chave de API exposta no código ou no manifesto
- [ ] `local.properties` no `.gitignore`
- [ ] Ícone 512×512 preparado
- [ ] Capturas de tela (mínimo 2, recomendado 4–6)
- [ ] Feature graphic 1024×500
- [ ] Textos de descrição curta e longa escritos
- [ ] Política de privacidade publicada em URL pública
- [ ] Questionário IARC preenchido na Play Console
- [ ] Declaração Data Safety preenchida na Play Console
- [ ] Firebase Crashlytics integrado
- [ ] Internal testing concluído sem crashes críticos

---

## Dependências externas a adicionar (F2 + F3)

```kotlin
// build.gradle.kts (projeto)
classpath("com.google.gms:google-services:4.4.x")
classpath("com.google.firebase:firebase-bom:33.x.x")

// build.gradle.kts (app)
apply plugin: "com.google.gms.google-services"

implementation(platform("com.google.firebase:firebase-bom:33.x.x"))
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-storage-ktx")
implementation("com.google.android.gms:play-services-auth:21.x.x")
```

Arquivo `google-services.json` deve ser adicionado em `app/` após configurar o projeto no Firebase Console. **Não versionar** — adicionar ao `.gitignore`.

## Questões em aberto

| # | Questão | Impacto |
|---|---|---|
| Q1 | Sincronização de *conteúdo* da viagem entre participantes (dias, atividades, etc.) — Firestore em tempo real vs. export/import manual? | F3 não sincroniza conteúdo — cada participante tem sua cópia local. Escopo de funcionalidade futura. |
| Q2 | O que acontece com a viagem local de um Editor/Viewer quando o Admin deleta a viagem? | Precisa de listener Firestore para detectar deleção e notificar o usuário. |
| Q3 | Limite de participantes por viagem? | Sem limite definido. Considerar impacto de custo no Firestore. |
| Q4 | Notificações push para convites? | Requer Firebase Cloud Messaging (FCM) — escopo de funcionalidade futura. |
| Q5 | Comportamento offline na F3 | Firestore tem suporte a cache offline nativo — avaliar se é suficiente. |
| Q6 | F4 — Formatação inline nas notas (negrito, itálico, links)? | Requer `AnnotatedString` ou biblioteca de rich text. Não está no escopo inicial. |
| Q7 | F5 — Suporte a múltiplas moedas dentro da mesma viagem? | Hoje a moeda é por viagem. Conversão de câmbio exigiria API externa. |
| Q8 | F5/F6 — Integração automática entre gasto registrado em F6 e orçamento de F5? | Especificada como opt-in (checkbox "Registrar no orçamento"). Avaliar se deve ser o padrão. |
| Q9 | F6 — Histórico de repasses já realizados (marcar como pago)? | Hoje os repasses são sugestões sem estado. Um "marcar como pago" persistiria o settlement. |
| Q10 | F4 — Notas aparecem no export `.travel` e são restauradas na importação? | Sim, especificado. Verificar impacto no tamanho do `trip.json` para viagens com muitas notas. |
| Q11 | F7 — Cobertura Foursquare em cidades menores do Brasil? | Destinos turísticos (Gramado, Canela, Floripa, etc.) têm boa cobertura. Cidades pequenas podem ter poucos locais cadastrados — informar o usuário com estado "Poucos resultados encontrados nesta região". |
| Q12 | F7 — Salvar histórico de buscas ou locais favoritados pelo usuário? | Não está no escopo inicial. Poderia ser adicionado como extensão: "Salvar local" → cria atividade no dia atual. |
| Q13 | F7 — Exibir resultados em mapa (MapView) além da lista? | Fora do escopo inicial por complexidade (Maps SDK adiciona tamanho ao APK). Lista com distância é suficiente para a primeira versão. |
| Q14 | F8 — Persistir histórico de conversas entre sessões? | Descartado intencionalmente: o contexto (localização, dia atual) muda a cada abertura. Histórico antigo geraria ruído. Reavaliar se usuários sentirem falta. |
| Q15 | F8 — Incluir notas (F4) no system prompt do assistente? | Depende do volume de notas. Se o usuário tiver muitas, pode inflar o prompt. Implementar como opt-in ou truncar a 500 chars de notas por dia. |
| Q16 | F8 — Usar streaming ou resposta completa? | Streaming especificado (`sendMessageStream`) para melhor UX. Se o SDK tiver problemas com streaming em determinadas versões Android, fallback para `sendMessage` como plano B. |
| Q17 | F9 — Bloqueio de usuários? | Não especificado nesta versão. Um usuário bloqueado deixaria de ver os posts do bloqueador e vice-versa. Escopo de iteração futura. |
| Q18 | F9 — Notificação quando alguém começa a seguir? | Requer Firebase Cloud Messaging (FCM) — não especificado ainda. Escopo de funcionalidade futura. |
| Q19 | F10 — Filtro "Seguindo" com mais de 30 seguidos requer múltiplas queries Firestore (`whereIn` limitado a 30 itens). | Especificado como chunks de 30 + merge local. Monitorar performance conforme a base de usuários cresce. |
| Q20 | F10 — Moderação automática de conteúdo impróprio nas fotos? | Não especificada. Firebase Extensions tem um moderador de imagens via Cloud Vision API. Considerar ao escalar. |
| Q21 | F10 — Edição de posts após publicação? | Especificada como possível (autor pode atualizar `description` e `title`). Fotos e roteiro editáveis aumentam a complexidade do Storage — avaliar na implementação. |
| Q22 | F10 — Feed em tempo real (Firestore `addSnapshotListener`) ou on-demand? | On-demand especificado (pull-to-refresh) para evitar consumo excessivo de leituras Firestore em um feed público de escala indeterminada. |
| Q23 | F11 — Quantos clientes uma agência free pode ter? | Não definido. Considerar limite (ex.: 10 clientes no plano free) para incentivar upgrade ao plano pro. |
| Q24 | F11 — Como a agência entrega o roteiro ao cliente que não tem o app? | PDF (F12) como alternativa ao `.travel`. Ou link de download gerado pelo app. Definir no escopo da implementação. |
| Q25 | F12 — Incluir fotos das atividades no PDF? | Atividades atualmente não têm fotos. Se F7 (lugares próximos) for integrado, pode-se incluir foto do local. Não está no escopo inicial. |
| Q26 | F12 — Paginação automática se o roteiro for muito longo? | `android.graphics.pdf.PdfDocument` não faz wrap automático. Implementar lógica de nova página quando o conteúdo excede a altura da página. Complexidade média. |
| Q27 | F13 — Como reagendar lembretes quando o usuário edita o horário de uma atividade? | `EditActivityScreen` deve cancelar o `WorkRequest` antigo (por tag `activity_{id}`) e criar um novo. Usar tag única por atividade como identificador. |
| Q28 | F13 — Comportamento quando o dispositivo é reiniciado? | WorkManager e `BOOT_COMPLETED` garantem reagendamento. Lembretes agendados por `AlarmManager.setExact` exigem `BroadcastReceiver` para `BOOT_COMPLETED`. Definir abordagem na implementação. |
| Q29 | F14 — Widget funciona sem viagem ativa? | Sim — exibe estado "Nenhuma viagem em curso" com sugestão de criar uma nova. |
| Q30 | F14 — Widget atualiza automaticamente ao editar uma atividade? | WorkManager pode ser disparado imediatamente após salvar a edição. Alternativa: `AppWidgetManager.updateAppWidget()` chamado no `EditActivityViewModel`. |
| Q31 | F15 — Backup inclui arquivos anexados (PDFs de vouchers, documentos de dias)? | TravelExporter já inclui — o backup herda esse comportamento. Limite de tamanho por arquivo no Firebase Storage gratuito: 1 GB total. |
| Q32 | F15 — O que acontece se o usuário tiver 2 aparelhos e editar a mesma viagem nos dois? | Último backup gravado vence (Storage sobrescreve por `tripUuid`). Sem resolução de conflito na V1 — informar o usuário. |
| Q33 | F16 — Quem aprova templates da comunidade antes de entrar na seção "Em destaque"? | Moderação manual pela equipe Rumo ou automática por número de importações (ex.: >50 importações → promovido para "Em destaque"). Definir política. |
| Q34 | F16 — Template pode ser atualizado pelo autor após publicação? | Sim, mas importações anteriores não são afetadas (cada importação gera uma cópia local). |
| Q35 | F17 — Avaliação de 👍/👎 ou escala 1–5? | Escala 1–5 dá mais granularidade mas exige mais esforço do usuário. Considerar 👍/👎 para V1 e expandir para estrelas depois. |
| Q36 | F17 — Avaliações influenciam o ranking de templates em F16? | Sim, intencionalmente — templates com mais atividades bem avaliadas sobem no ranking. Definir fórmula de scoring. |
| Q37 | M3 — CI/CD dispara build a cada push ou apenas em tags de release? | Recomendado: build de debug a cada push (validação), AAB assinado apenas em tags `v*.*.*`. |
| Q38 | M4 — Quais eventos do Firebase Analytics capturar por padrão? | Mínimo sugerido: `trip_created`, `trip_imported`, `activity_added`, `pdf_generated`, `template_imported`, `community_post_created`. |
| Q39 | M5 — Acessibilidade exige redesign de componentes existentes? | Principalmente: `contentDescription` ausentes em ícones, tamanho mínimo de toque (48dp), contraste de texto sobre `GreenMoss`. Auditoria com TalkBack antes de M1. |
| Q40 | M6 — Internacionalização inclui datas e moedas no formato local? | Sim — usar `DateTimeFormatter` com `Locale` do sistema; `NumberFormat.getCurrencyInstance()` para valores monetários. |
| Q41 | M7 — Modelo freemium: quais funcionalidades ficam no plano pago? | Sugestão: plano free = até 3 viagens simultâneas + comunidade; plano pro = viagens ilimitadas + backup em nuvem + widget; plano agência = F11 completo. Validar com usuários antes de implementar. |
| Q42 | M9 — Parceria com agências requer contrato formal ou apenas cadastro no app? | Para V1: formulário de cadastro no site (M8) + e-mail de confirmação. Contrato formal apenas para plano enterprise. |
---

## ~~M2 — Compatibilização e publicação para iOS~~ — CANCELADO

> **Decisão:** suporte a iOS cancelado. O app permanece exclusivamente Android. Nenhuma mudança arquitetural de compatibilização (Room → SQLDelight, Hilt → Koin, expect/actual) deve ser realizada com base neste marco.

---

## M3 — CI/CD com GitHub Actions

### Descrição

Automatizar o processo de build, teste e deploy do app usando GitHub Actions e Gradle Play Publisher. Garante que cada push passe pelos testes antes de ser mesclado, e que releases sejam publicadas na Play Store sem intervenção manual.

### Pipeline

```
Push para qualquer branch:
  ├─ ./gradlew test                    ← testes unitários JVM
  └─ ./gradlew lint                    ← análise estática

Tag v*.*.* (release):
  ├─ ./gradlew test
  ├─ ./gradlew bundleRelease           ← gera AAB assinado
  └─ gradle-play-publisher             ← upload para Play Store (faixa internal)
```

### Configuração de secrets no GitHub

| Secret | Valor |
|---|---|
| `KEYSTORE_BASE64` | Keystore de release codificado em Base64 |
| `KEY_ALIAS` | Alias da chave |
| `KEY_PASSWORD` | Senha da chave |
| `STORE_PASSWORD` | Senha do keystore |
| `GEMINI_API_KEY` | Chave da API Gemini (para testes que precisem dela) |
| `GOOGLE_PLAY_JSON` | JSON de service account do Google Play |

### Arquivo de workflow

```yaml
# .github/workflows/ci.yml
name: CI
on:
  push:
    branches: ["**"]
    tags: ["v*.*.*"]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: "17", distribution: "temurin" }
      - run: ./gradlew test lint
  release:
    if: startsWith(github.ref, 'refs/tags/v')
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: "17", distribution: "temurin" }
      - name: Decode keystore
        run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > app/rumo-release.jks
      - run: ./gradlew bundleRelease
        env:
          STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
      - uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.GOOGLE_PLAY_JSON }}
          packageName: com.rodrigoleao.rumo
          releaseFiles: app/build/outputs/bundle/release/*.aab
          track: internal
```

### Impacto no processo de desenvolvimento

- O pipeline de CI bloqueia merges com testes falhando — a cultura de manter `./gradlew test` passando (já existente) torna-se obrigatória e automatizada
- O build de release no CI requer que `GEMINI_API_KEY` e `FSQ_API_KEY` estejam nos GitHub Secrets para testes que dependem dessas APIs; caso os testes sejam mocados (recomendado), as chaves reais não são necessárias no CI
- **`local.properties` nunca deve ser commitado** — verificar `.gitignore` antes de configurar o CI (o CI monta o `local.properties` a partir de secrets)

### Arquivos a criar

| Arquivo | Conteúdo |
|---|---|
| `.github/workflows/ci.yml` | Pipeline de CI/CD completo |
| `.github/workflows/pr-check.yml` | Check rápido em PRs (somente testes) |
| `CHANGELOG.md` | Histórico de versões (alimenta release notes da Play Store) |

> **💰 Custo — GitHub Actions:** **gratuito ilimitado** para repositórios públicos. Para repositórios privados: 2.000 minutos/mês gratuitos (suficiente para dezenas de builds). Cada build de teste demora ~3–5 min; cada build de release ~8–12 min. Acima de 2.000 min/mês: USD 0,008/min. Para referência: 50 pushes por semana × 4 min = 800 min/mês — dentro do gratuito. **Recomendação:** manter o repositório privado e monitorar o uso de minutos na aba "Billing" do GitHub.
>
> **Alternativa gratuita ilimitada:** GitLab CI/CD com repositório no GitLab.com (runners compartilhados, 400 min/mês grátis em repos privados; ilimitado em público).

---

## M4 — Analytics e monitoramento

### Descrição

Instrumentar o app com Firebase Analytics (comportamento do usuário), Firebase Crashlytics (rastreamento de crashes em produção) e Firebase Performance Monitoring (latência de operações críticas). Deve ser configurado antes da publicação em M1.

### Eventos Firebase Analytics a capturar

| Evento | Parâmetros | Trigger |
|---|---|---|
| `trip_created` | `destination`, `duration_days`, `has_hotel` | Ao salvar nova viagem |
| `trip_imported` | `source` ("file" / "community") | Ao concluir importação |
| `itinerary_generated` | `mode` ("chat" / "import"), `days_count` | Ao salvar roteiro gerado por IA |
| `activity_added` | `has_emoji`, `has_address`, `badge_count` | Ao salvar nova atividade |
| `pdf_generated` | `days_count`, `activity_count` | Ao gerar PDF (F12) |
| `template_imported` | `source` ("official" / "community" / "agency") | Ao usar template de F16 |
| `community_post_created` | — | Ao publicar roteiro em F10 |
| `widget_opened` | — | Ao tocar no widget (F14) |
| `nearby_search` | `category`, `results_count` | Ao buscar em F7 |
| `ai_chat_message_sent` | `trip_context_included` | Ao enviar mensagem ao assistente em F8 |

### Firebase Crashlytics

- Inicializado automaticamente via `google-services.json`
- Adicionar `userId` ao crash report após login (F2): `FirebaseCrashlytics.getInstance().setUserId(uid)`
- Logar erros não fatais em operações críticas: `crashlytics.recordException(e)` nos `catch` de `TravelImporter` e `BackupRepository`

### Firebase Performance

Monitorar latência de operações críticas com traces personalizados:

```kotlin
val trace = Firebase.performance.newTrace("import_travel_file")
trace.start()
// ... TravelImporter.import(uri)
trace.stop()
```

Traces sugeridos: `import_travel_file`, `export_travel_file`, `generate_itinerary_ai`, `backup_all_trips`, `foursquare_search`.

### Dependências a adicionar

```kotlin
implementation(platform("com.google.firebase:firebase-bom:33.x.x"))
implementation("com.google.firebase:firebase-analytics-ktx")
implementation("com.google.firebase:firebase-crashlytics-ktx")
implementation("com.google.firebase:firebase-perf-ktx")
```

Plugins em `build.gradle.kts`:
```kotlin
id("com.google.firebase.crashlytics")
id("com.google.firebase.firebase-perf")
```

---

## M5 — Acessibilidade (a11y)

### Descrição

Garantir que o app Rumo seja utilizável por pessoas com deficiência visual, motora ou cognitiva. Foco nos requisitos mínimos: suporte a TalkBack (leitor de tela), escala de fonte do sistema, e contraste de cores conforme WCAG AA.

### Checklist de implementação

**TalkBack (leitor de tela):**
- Todos os ícones clicáveis têm `contentDescription` preenchido (não nulo, não vazio)
- `IconButton` sem texto visível: `contentDescription = "Editar atividade"`
- Imagens decorativas: `contentDescription = null` (ignora o leitor)
- Reordenamento por drag-and-drop: alternativa acessível via menu contextual (segure → mover para cima / baixo)
- Cards de atividade colapsáveis: `semantics { heading() }` no título

**Toque:**
- Tamanho mínimo de toque: 48×48dp para todos os botões e ícones interativos
- `Modifier.minimumInteractiveComponentSize()` nos ícones pequenos (estrela de favorito, badge chips)

**Escala de fonte:**
- Testar com fonte em 200% (`Configurações → Acessibilidade → Tamanho da fonte`)
- Usar `sp` para textos, nunca `dp` para tamanhos de fonte
- Verificar truncamentos em textos que devem ser completos (nome da viagem, nome da atividade)

**Contraste:**
- Texto branco sobre `GreenMoss` (#1B4332): ✅ passa WCAG AA (contraste 8.5:1)
- Texto `AmberPrimary` (#F59E0B) sobre `GreenMoss`: verificar (pode falhar em texto pequeno)
- Texto secundário (`TextSecondary`) sobre `SurfaceWhite`: verificar contraste mínimo 4.5:1

**Ferramenta de auditoria:** `Accessibility Scanner` (app Google) + `Layout Inspector` no Android Studio.

### Arquivos potencialmente afetados

Todos os arquivos de tela em `ui/` — a revisão é transversal. Priorizar: `DayDetailScreen`, `EditActivityScreen`, `VouchersScreen`, `ContactsScreen` (mais densos em ícones).

---

## M6 — Internacionalização (i18n)

### Descrição

Preparar o app para suporte a múltiplos idiomas, com prioridade para **português do Brasil (pt-BR)** já implementado e **inglês (en-US)** como segunda língua. A internacionalização amplia o alcance do app para viajantes e agências internacionais.

### Passos de implementação

**1. Externalizar todas as strings hardcoded:**
- Mover strings em código Kotlin para `res/values/strings.xml` (pt-BR) e `res/values-en/strings.xml` (en-US)
- Usar `stringResource(R.string.key)` em todos os composables
- Strings de formatação de data: usar `DateTimeFormatter` com `Locale.getDefault()` em vez de formato fixo

**2. Plurais:**
```xml
<plurals name="days_count">
    <item quantity="one">%d dia</item>
    <item quantity="other">%d dias</item>
</plurals>
```
Usar `pluralStringResource(R.plurals.days_count, count, count)` no Compose.

**3. Formatos de data e hora:**
- Datas: `DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())`
- Moedas: `NumberFormat.getCurrencyInstance(Locale.getDefault())`

**4. Imagens e ícones regionais:**
- Nenhum ícone atual é culturalmente específico — sem necessidade de variantes por locale.

**5. Textos gerados pela IA (F8):**
- Passar o idioma do dispositivo no system prompt: `"Responda em ${Locale.getDefault().language}"`

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Criar | `res/values/strings.xml` — todas as strings em pt-BR |
| Criar | `res/values-en/strings.xml` — traduções em en-US |
| Modificar | Todos os arquivos de tela em `ui/` — substituir strings inline por `stringResource()` |

---

## M7 — Monetização

### Descrição

Implementar modelo de receita sustentável para o app Rumo, equilibrando acesso gratuito (crescimento da base de usuários) com funcionalidades premium (receita). Usar Google Play Billing para compras in-app e assinaturas.

### Modelo freemium proposto

| Plano | Preço | Funcionalidades |
|---|---|---|
| **Free** | Gratuito | Até 3 viagens simultâneas, todas as funcionalidades base (F1–F8, F12–F14, F17), comunidade (leitura), templates oficiais |
| **Pro** | ~R$ 14,90/mês ou R$ 99/ano | Viagens ilimitadas, backup em nuvem (F15), widget (F14 sem limite), templates da comunidade, sem anúncios |
| **Agência** | ~R$ 49,90/mês | Tudo do Pro + Modo agência (F11), biblioteca de templates privados, painel de clientes ilimitado |

### Implementação técnica

**Google Play Billing:**
```kotlin
implementation("com.android.billingclient:billing-ktx:7.x.x")
```

**Fluxo:**
1. `BillingClient.startConnection()` na inicialização do app
2. `queryProductDetailsAsync()` para buscar planos disponíveis
3. `launchBillingFlow()` ao usuário tocar em "Assinar"
4. `PurchasesUpdatedListener` → validar compra no backend (Firebase Functions recomendado) → gravar plano em Firestore
5. `SubscriptionRepository.kt` expõe `Flow<Plan>` para a UI

> **💰 Custo — Google Play Billing:** a biblioteca é gratuita. Porém, o Google retém **30% de comissão sobre cada transação** (recorrente para assinaturas). Após o 1º ano de assinatura do mesmo usuário, a comissão cai para **15%**. Apps com receita anual < USD 1 milhão podem se inscrever no programa de taxa reduzida para pagar 15% desde o início. Isso afeta diretamente o preço dos planos: para receber R$ 10/mês líquido, cobrar R$ 14,90 (30% vai para o Google). Os preços sugeridos na tabela acima já consideram essa margem.
>
> **💰 Custo — Firebase Functions para validação de compras:** Firebase Functions **não está disponível no Spark Plan** — requer o **plano Blaze (pay-as-you-go)**. O plano Blaze tem um nível gratuito generoso para Functions (2 milhões de chamadas/mês grátis), mas exige a configuração de um método de pagamento. **Ao implementar M7, a conta Firebase já deve estar no plano Blaze** (o que provavelmente já ocorreu para F10/F15). Alternativa mais simples: verificar a compra apenas no cliente via `BillingClient.queryPurchasesAsync()` (menos seguro, mas funcional para apps de baixo risco de fraude).

**Enforcement:**
- Verificar plano antes de permitir criação da 4ª viagem (Free)
- Verificar plano antes de acessar `BackupRepository` (Pro)
- Verificar plano antes de acessar módulo Agência (Agency)

### Arquivos a criar/modificar

| Ação | Arquivo |
|---|---|
| Criar | `data/repository/SubscriptionRepository.kt` |
| Criar | `ui/paywall/PaywallScreen.kt` — tela de upgrade |
| Modificar | `data/repository/TripRepository.kt` — checar limite de viagens |
| Modificar | `data/di/AppModule.kt` — `@Provides` para `BillingClient` |
| Criar | `docs/modulo-28-monetizacao.md` |

---

## M8 — Site de marketing e landing page

### Descrição

Criar uma página de apresentação do app Rumo com CTA para download na Play Store, voltada tanto para usuários individuais quanto para agências de viagem. O site serve como ponto de entrada para SEO, divulgação em redes sociais e parceria com agências (M9).

### Conteúdo da landing page

| Seção | Conteúdo |
|---|---|
| Hero | Nome do app + tagline + screenshot do app + botão "Baixar na Play Store" |
| Funcionalidades | Grid com 6–8 funcionalidades principais (ícone + título + 1 frase) |
| Para agências | Seção separada com benefícios do plano Agência + CTA "Quero ser parceiro" |
| Depoimentos | (após ter usuários) Avaliações da Play Store ou testemunhos de clientes |
| Preços | Tabela dos 3 planos (Free / Pro / Agência) |
| Rodapé | Links: Política de Privacidade, Termos de Uso, e-mail de contato, redes sociais |

### Requisitos técnicos

- **Hospedagem:** GitHub Pages (gratuito) ou Vercel (gratuito no plano hobby)
- **Tecnologia:** HTML/CSS estático ou Next.js (Vercel) para SEO melhor
- **Domínio:** `rumoapp.com.br` ou similar (registrar no registro.br)
- **Política de Privacidade:** obrigatória pela Play Store — deve listar quais dados são coletados (Firebase Analytics, Auth) e como são usados
- **Termos de Uso:** recomendado, especialmente para o plano Agência

### Arquivos/repos

Manter o site em repositório separado (`rumo-site`) para não poluir o repo do app.

---

## M9 — Programa de parceria com agências

### Descrição

Processo formal de onboarding para agências de viagem que desejam usar o plano Agência do Rumo para distribuir roteiros a seus clientes. Inclui formulário de cadastro, contrato de uso, suporte dedicado e co-marketing (logo da agência parceira no site M8).

### Fluxo de onboarding

```
1. Agência preenche formulário no site (M8) → "Quero ser parceiro"
2. E-mail automático de boas-vindas com instruções de ativação
3. Equipe Rumo valida a agência (verificar CNPJ, redes sociais)
4. Agência recebe código de cupom para 30 dias grátis do plano Agência
5. Agência cria conta no app → insere cupom → acessa módulo F11
6. Call de onboarding (30min) com a equipe Rumo
7. Agência listada como "Parceira Oficial" no site (M8) com logo
```

### Materiais de suporte

| Material | Formato |
|---|---|
| Guia de início rápido para agências | PDF + página no site |
| Tutorial em vídeo: criar e enviar roteiro a cliente | Vídeo (YouTube / Loom) |
| Template de roteiro exclusivo para parceiros | Disponível em F16 com tag "Parceiro" |
| Canal de suporte dedicado | WhatsApp Business ou e-mail prioritário |

### Métricas de sucesso do programa

| Métrica | Meta (6 meses pós-lançamento) |
|---|---|
| Agências cadastradas | 20 |
| Roteiros enviados a clientes | 200 |
| Taxa de conversão free → agência | 5% das agências que testaram |
| NPS de agências parceiras | ≥ 70 |
