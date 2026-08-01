# Módulo 17 — Firebase (Crashlytics + Analytics)

**Escopo:** integração base do Firebase, relatório de falhas (Crashlytics) e telemetria de produto (Analytics).
**Arquivos:** `app/build.gradle.kts`, `build.gradle.kts`, `app/google-services.json`, `data/analytics/AnalyticsService.kt`, `ui/analytics/AnalyticsViewModel.kt`, `PipaApplication.kt`.
**Projeto Firebase:** `gen-lang-client-0481952640` (o mesmo projeto GCP onde o Gemini está habilitado).

---

## 1. Integração base

- **Plugins Gradle:** `com.google.gms.google-services` (4.4.2) e `com.google.firebase.crashlytics` (3.0.2), declarados `apply false` no `build.gradle.kts` raiz e aplicados no `app/build.gradle.kts`.
- **SDKs:** via **Firebase BoM** (`firebase-bom:33.7.0`) — `firebase-common` (inicializa o `FirebaseApp` via `FirebaseInitProvider`, sem código), `firebase-crashlytics` e `firebase-analytics`.
- **Config:** `app/google-services.json` (dentro do módulo `app/`, não na raiz — é onde o plugin procura).

### Armadilha do `.debug` (importante)

O build debug usa `applicationId` `com.rodrigoleao.pipa.debug` (sufixo `.debug`), que **não** existe por padrão no `google-services.json` → o plugin falharia com *"No matching client found"*. Solução adotada: o `google-services.json` tem **dois clients** — `com.rodrigoleao.pipa` (release) e `com.rodrigoleao.pipa.debug` — ambos **reusando o mesmo `mobilesdk_app_id`**. Consequência: **telemetria de debug cai no mesmo app Firebase do release**. Pré-lançamento/solo, é inofensivo.

> Para separar debug e release no futuro: registrar `com.rodrigoleao.pipa.debug` como app Android próprio no Console Firebase (logado na conta pessoal dona do projeto — a conta corporativa do Firebase CLI não tem acesso), baixar o `google-services.json` atualizado (2 clients reais) e substituir em `app/`.

### Segurança da chave

`google-services.json` está no **`.gitignore`** (contém a API key `AIza…`, a mesma chave/projeto do Gemini). Recomendação pendente: **restringir a chave** no Google Cloud Console (restrição de app Android por package + SHA-1 e restrição de API).

---

## 2. Crashlytics

- Auto-inicializa e registra o handler de exceções não tratadas — **nenhum código** de init é necessário. Todo crash (`FATAL`) sobe no **próximo start** do app.
- **Release (R8/minify):** o plugin sobe o `mapping.txt` automaticamente (tarefa `injectCrashlyticsMappingFileId*`), então as stack traces chegam **desofuscadas**. Não há NDK, então não há upload de símbolos nativos.
- **Coleta em debug:** ligada por ora (permite testar). Para desligar em debug: `FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG` no `PipaApplication`.
- **Testar:** forçar um crash (`throw RuntimeException("Test")`), reabrir o app, ver em **Console → Crashlytics**.

---

## 3. Analytics

Não tem plugin Gradle próprio — só o SDK `firebase-analytics`. Eventos automáticos (`first_open`, `session_start`, engajamento) sobem sozinhos.

### `AnalyticsService` — ponto único

`data/analytics/AnalyticsService.kt` (`@Singleton`, `@Inject constructor(FirebaseAnalytics)`) concentra **todos os nomes de evento/parâmetro em constantes** e expõe métodos tipados. Regras: `snake_case` (≤40 chars), sem prefixos reservados (`firebase_`/`google_`/`ga_`) e **sem PII** nos parâmetros (só contagens e enums — nunca nome/destino livre da viagem).

- **Injeção:** ViewModels de dados recebem por `@Inject constructor`. Para **composables** (que não têm construtor injetável), há o `ui/analytics/AnalyticsViewModel.kt` (wrapper fino obtido via `hiltViewModel()`), usado na `AppNavigation`.
- **`FirebaseAnalytics`** é provido em `data/di/AppModule.kt` (`provideFirebaseAnalytics`).

### Eventos instrumentados

| Evento | Parâmetros | Onde dispara |
|---|---|---|
| `trip_created` | `method` (manual/ai/ai_import), `days_count` | `CreateTripViewModel` — `saveItinerary` / `skipItinerary` |
| `trip_imported` | `mode` (new/overwrite) | `ImportTripViewModel` (ao concluir) |
| `ai_itinerary_generated` | `success`, `days_count`, `prompt_tokens`, `output_tokens`, `total_tokens`, `latency_ms` | `CreateTripViewModel.generateItinerary` |
| `ai_chat_message_sent` | `total_tokens`, `message_index` | `CreateTripViewModel.sendChatMessage` |
| `ai_limit_reached` | `type` (token_budget/daily_cap) | `CreateTripViewModel` (trava de token / cap diário) |
| `screen_view` (padrão) | `screen_name` | `AppNavigation` — troca de rota + troca de aba do pager |
| `trip_opened` | `is_active` | `AppNavigation` — TripMain (uma vez por abertura) |
| `share` (recomendado) | `content_type=trip` | `ShareTripViewModel` (export concluído) |
| `content_added` | `type` (voucher/boarding_pass/contact/note/activity) | `Edit*ViewModel` (item novo, id == 0) + `TripViewModel`/`NotesListViewModel` (notas) |
| `language_changed` | `language` (pt/en/es/system) | `SettingsViewModel.onLanguageChanged` |

**Propriedade de usuário `app_language`** — setada no start (`PipaApplication.onCreate`, via `LocaleHelper.getLanguage`) e a cada troca de idioma. Permite fatiar **todos** os relatórios por idioma.

### Detalhes de implementação

- **`screen_view` no Compose** é **manual** (Compose não gera automático): a `AppNavigation` observa `currentBackStackEntryAsState()` e mapeia rota → nome amigável (`screenNameFor`); as 5 abas do pager são logadas em `LaunchedEffect(pagerState.currentPage)` (rota `TripMain` é ignorada no observador de rota para não duplicar).
- **`content_added`** só dispara em **item novo** (`id == 0L`), não em edição.
- **`ai_itinerary_generated`**: o `ItineraryGenerator.generateItinerary()` devolve `GenerationResult` (dias + tokens + latência) — antes só logava internamente.

---

## 4. Como testar (DebugView)

```powershell
& "C:\Users\rasantos\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell setprop debug.firebase.analytics.app com.rodrigoleao.pipa.debug
```

Rodar o app e abrir **Console → Analytics → DebugView**. Eventos e parâmetros aparecem em segundos. Desligar: trocar o valor por `.none.`.

> **Parâmetros nos relatórios agregados:** no DebugView os parâmetros aparecem na hora, mas para fatiar por eles nos relatórios normais é preciso registrá-los uma vez em **Analytics → Custom definitions** (custom dimensions/metrics). O mesmo vale para a user property `app_language`.

---

## Checklist para futuras modificações

- **Novo evento:** adicionar constantes de nome/parâmetro + método tipado no `AnalyticsService`; chamar do ViewModel (via `@Inject`) ou do `AnalyticsViewModel` (Compose). Manter `snake_case`, sem PII.
- **Nova tela:** mapear a rota em `screenNameFor` (`AppNavigation`) para gerar `screen_view`.
- **Custo estimado da IA:** as constantes de preço/câmbio ficam em `ItineraryGenerator` (marcadas como aproximadas) — ajustar conforme o pricing do modelo em uso.
