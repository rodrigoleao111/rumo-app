# Módulo 18 — Atualização in-app (Play In-App Updates)

**Escopo:** notificar o usuário quando há uma versão nova do app na Google Play e permitir baixar/instalar sem sair do app.
**Arquivos:** `update/InAppUpdateManager.kt`, `MainActivity.kt`, `data/di/AppModule.kt`, `data/analytics/AnalyticsService.kt`, `app/build.gradle.kts`, `res/values{,-en,-es}/strings.xml`.
**Modo:** **FLEXIBLE** (download em segundo plano; o usuário continua usando o app e reinicia quando quiser).

---

## 1. Visão geral do fluxo

1. Em **`onResume`** da `MainActivity`, o app consulta a Play (`requestAppUpdateInfo`).
2. Havendo versão nova e nada em andamento, inicia o fluxo → **a própria Play exibe o diálogo** de confirmação (não há UI custom para "há update disponível").
3. Aceito, o download roda **em segundo plano**; o app segue utilizável.
4. Ao concluir (`InstallStatus.DOWNLOADED`), um **Snackbar global** oferece *"Atualização pronta para instalar · Reiniciar"*.
5. Tocar em **Reiniciar** → `completeUpdate()` → a Play instala e reabre o app.

> Não confundir com o modo **IMMEDIATE** (tela cheia bloqueante), reservado a updates críticos — não usado hoje (ver §6).

---

## 2. Componentes

### `InAppUpdateManager` (`update/InAppUpdateManager.kt`)

Classe que encapsula todo o ciclo. Não é singleton Hilt — é instanciada pela `MainActivity` porque precisa de contexto de Activity (o `ActivityResultLauncher`) e do `lifecycleScope`.

| Membro | Responsabilidade |
|---|---|
| `register()` / `unregister()` | Liga/desliga o `InstallStateUpdatedListener` (chamados em `onCreate`/`onDestroy`). |
| `checkForUpdate()` | Consulta a Play; se já baixado → `onDownloaded()`; senão, se disponível e FLEXIBLE permitido → inicia o fluxo. |
| `onFlowResult(resultCode)` | Recebe o resultado do diálogo da Play e registra no Analytics. |
| `completeUpdate()` | Instala a atualização baixada (reinicia o app). |

- **Guarda `flowStarted`:** o diálogo da Play é oferecido **no máximo 1x por sessão do processo** (evita insistir a cada `onResume`). O aviso de "baixado → reiniciar" continua sendo reexibido sempre que houver download concluído.
- **Listener:** trata `DOWNLOADED` (dispara `onDownloaded`) e `FAILED` (loga no Analytics); estados intermediários (`PENDING`/`DOWNLOADING`/…) são ignorados.
- **API ktx:** usa `appUpdateManager.requestAppUpdateInfo()` (suspend, de `com.google.android.play.core.ktx`) dentro do `scope`.

### `MainActivity`

- **Injeção (Hilt, field):** `@Inject lateinit var appUpdateManager` e `analytics`.
- **Launcher:** `registerForActivityResult(StartIntentSenderForResult())` encaminha o `resultCode` para `inAppUpdate.onFlowResult(...)`.
- **Ciclo:** cria + `register()` em `onCreate`; `checkForUpdate()` em `onResume`; `unregister()` em `onDestroy`.
- **UI (`AppRoot` composable):** envolve a `AppNavigation` num `Box` com um **`SnackbarHost` de nível global** (cores da marca — `GreenMoss` de fundo, `AmberPrimary` na ação). Um `mutableStateOf` (`updateReadyState`) vira `true` no callback `onDownloaded`; um `LaunchedEffect` mostra o Snackbar `Indefinite` e, se a ação for tocada, chama `completeUpdate()`.

### `AppModule`

`provideAppUpdateManager(@ApplicationContext)` → `AppUpdateManagerFactory.create(ctx)` como `@Singleton`.

### Dependência (`app/build.gradle.kts`)

```kotlin
implementation("com.google.android.play:app-update:2.1.0")
implementation("com.google.android.play:app-update-ktx:2.1.0")
```

A biblioteca traz suas próprias regras de ProGuard (consumer rules) — não é preciso `-keep` manual para o release com R8.

---

## 3. Analytics

Eventos centralizados no `AnalyticsService` (ver [módulo 17](modulo-17-firebase.md)):

| Evento | Parâmetros | Quando |
|---|---|---|
| `app_update_available` | — | Update encontrado e fluxo iniciado |
| `app_update_flow` | `result` (`accepted`/`canceled`/`failed`) | Resultado do diálogo da Play |
| `app_update_installed` | — | Usuário tocou em "Reiniciar" (`completeUpdate`) |

---

## 4. i18n

Duas chaves nas três `strings.xml` (pt/en/es):

- `update_downloaded_message` — texto do Snackbar ("Atualização pronta para instalar.")
- `update_downloaded_action` — rótulo da ação ("Reiniciar")

---

## 5. Como testar

⚠️ **Não funciona no build debug** rodado via `installDebug`: o `applicationId` tem sufixo `.debug`, que não existe na Play → a checagem nunca encontra update. Caminhos válidos:

- **Fluxo real ponta a ponta:** publicar num **canal de teste interno** (ou *Internal App Sharing*) com um `versionCode` maior; instalar a versão anterior pela Play; abrir o app → o fluxo dispara.
- **UI isolada (Snackbar/estados):** teste instrumentado com **`FakeAppUpdateManager`** (da própria lib), que simula `UPDATE_AVAILABLE` → `DOWNLOADING` → `DOWNLOADED` sem depender da Play.

> Consequência prática: a funcionalidade entra "adormecida" no **primeiro** upload e passa a agir a partir da **segunda** versão publicada — comportamento esperado.

---

## 6. Extensão futura — modo Immediate por prioridade

Para updates críticos, dá para escolher dinamicamente entre FLEXIBLE e IMMEDIATE lendo a **`updatePriority`** (0–5) definida **por release** no Play Console / Developer API:

```kotlin
val type = if (info.updatePriority() >= 4) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
```

O IMMEDIATE mostra uma tela cheia bloqueante e a Play cuida de instalar/reiniciar (não usa o Snackbar). Exigiria também tratar, em `onResume`, o estado `UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS` para retomar um fluxo interrompido.

---

## Checklist para futuras modificações

- **Trocar/estender o modo:** ajustar `checkForUpdate` em `InAppUpdateManager`; para IMMEDIATE, tratar a retomada em `onResume` (ver §6).
- **Novo texto de UI:** criar a chave nas **três** `strings.xml`.
- **Novo evento:** seguir o padrão do `AnalyticsService` (constante + método tipado, `snake_case`, sem PII).
- **Atualizar a lib:** conferir se a API `startUpdateFlowForResult(info, launcher, options)` e as extensões ktx (`requestAppUpdateInfo`) permanecem estáveis.
