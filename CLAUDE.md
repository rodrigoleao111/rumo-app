# CLAUDE.md — rumo-app

Guia para agentes trabalhando neste repositório. Complementa o `README.md` (visão de produto) com convenções, comandos e armadilhas do dia a dia.

## O que é

App Android nativo de roteiros de viagem. Package `com.rodrigoleao.pipa` (o nome `gramado2026` é histórico — o produto é o **Pipa**). Kotlin + Jetpack Compose + Material 3, MVVM, Hilt, Room, DataStore. minSdk 26, compileSdk/targetSdk 36, JDK 17.

## Build & run (Windows)

Sempre defina o `JAVA_HOME` do JBR do Android Studio antes de chamar o Gradle no PowerShell/terminal:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
```

Comandos:

```bash
./gradlew :app:compileDebugKotlin   # checagem rápida de compilação (use após editar Kotlin)
./gradlew :app:installDebug         # build + instala no emulador/dispositivo conectado
./gradlew test                      # testes unitários (JVM)
./gradlew connectedAndroidTest      # testes instrumentados (migrations, DAOs) — requer device
./gradlew assembleRelease           # APK release (R8/minify) — ver "Assinatura" abaixo
```

- **adb / emulador:** `adb` fica em `C:\Users\rasantos\AppData\Local\Android\Sdk\platform-tools\adb.exe` (não está no PATH — invoque pelo caminho completo). O build **debug** usa `applicationId` com sufixo `.debug`, então debug e release convivem como apps separados no aparelho.
- **KSP travado** (`kspDebugKotlin` com erro interno após grandes refactors): rode `./gradlew :app:kspDebugKotlin --rerun-tasks` e recompile.

## Arquitetura

- **Camadas:** `ui/<tela>/` (Screen + ViewModel) → `data/repository/` → `data/db/` (Room) / `data/preferences/` (DataStore) / `data/weather` (Open-Meteo) / `data/ai` (Gemini).
- **DI (Hilt):** ViewModels são `@HiltViewModel` e obtidos via `hiltViewModel()`. Dependências (DB, repositórios) são providas em `data/di/AppModule.kt`. `PipaApplication` é `@HiltAndroidApp`. **Não** instancie DB/repos manualmente em telas.
- **Estado:** `StateFlow` no ViewModel, coletado com `collectAsStateWithLifecycle()`. Para derivar de várias preferências, combine os flows no ViewModel (ver `TripsListViewModel` combinando `allTrips` + ordenação + ocultação).
- **Navegação:** `navigation/AppNavigation.kt` — rotas em `sealed class Screen`, pager de **5 abas** (Início, Vouchers, Embarque, Contatos, Notas). Refresh após telas de edição via `previousBackStackEntry.savedStateHandle["refresh"]`.

## Room — regras invioláveis

- **Nunca** `fallbackToDestructiveMigration()`. Migrations são explícitas em `TravelDatabase.kt`.
- Versão atual: `TravelDatabase.CURRENT_VERSION` (**20**) — fonte única de verdade (anotação `@Database` + testes).
- Para alterar o schema: nova `MIGRATION_N_(N+1)` → incrementa `CURRENT_VERSION` → registra em `ALL_MIGRATIONS` → teste de migração com `MigrationTestHelper`.
- `app/schemas/*.json` são **versionados no git — nunca apagar** (histórico usado pelos testes de migração).

## Identidade visual (não regredir)

- **Paleta:** `ui/theme/Color.kt`. Fundo geral do app = `Sand` (`#F4EDE1`); superfície de cards = `SurfaceWhite`; primary = `GreenMoss` (`#1B4332`); accent = `AmberPrimary` (`#E9B43C`). O token creme chama-se `Cream` (não existe mais `GreenLight`).
- **Ícones:** use o set vetorial da marca em `res/drawable/ic_*.xml` via `ImageVector.vectorResource(R.drawable.ic_*)`. Evite `Icons.Filled.*` (Material) — só restam uns poucos usos residuais (Menu/MoreVert/Add, ZoomIn/Close).
- **Ilustrações:** capas `cover_*.webp`, clima `weather_*.png`, transporte `transport_*.png` em `res/drawable-nodpi/`.
- **Tipografia:** Plus Jakarta Sans (`res/font/`), já ligada no tema.
- **VectorDrawable — armadilha:** `viewportWidth/Height` DEVE bater com a escala das coordenadas do `pathData`. Ícone invisível/gigante geralmente é viewport errado (ex.: pathData em escala 512 com viewport 24).

## Internacionalização (i18n)

App traduzido para **pt (padrão), en, es**. Textos de UI ficam em `res/values{,-en,-es}/strings.xml` — **as três com o mesmo conjunto de chaves**. Nada de string hardcoded na UI.

- **Adicionar texto novo:** crie a chave nas **três** `strings.xml` e use `stringResource(R.string.x)` (composable) ou `context.getString(...)` (ViewModel/Worker — VMs Hilt recebem `@ApplicationContext appContext: Context` no construtor). Chaves `snake_case` com prefixo por módulo; reutilize o glossário `common_*` (Salvar/Cancelar/OK/Voltar…).
- **Não traduzir:** identificadores/valores persistidos (`"BY_CATEGORY"`), nomes de rota, chaves de `savedStateHandle`, nomes de recurso/drawable, e o prompt do Gemini.
- **Seleção de idioma:** `locale/LocaleHelper.kt` (`SharedPreferences` `pipa_locale`) + `MainActivity.attachBaseContext`; troca via `LanguageSettingRow` em Configurações → `Activity.recreate()`. `"system"` segue o aparelho. Detalhes em `docs/arquitetura-geral.md` e `docs/modulo-10-settings.md`.
- **Verificação:** garanta paridade de chaves entre os 3 arquivos; chave referenciada e não definida = erro de compilação. `assembleRelease`/`compileDebugKotlin` acusam faltas.

## Firebase (Crashlytics + Analytics)

Projeto Firebase `gen-lang-client-0481952640` (o mesmo GCP do Gemini). Detalhes em `docs/modulo-17-firebase.md`.

- **Plugins/SDKs:** plugins `com.google.gms.google-services` e `com.google.firebase.crashlytics` (root `apply false` + aplicados no `app`); SDKs via **Firebase BoM** (`firebase-common` + `firebase-crashlytics` + `firebase-analytics`). `FirebaseApp` auto-inicializa (sem código).
- **`google-services.json`:** fica em **`app/`** (não na raiz) e está no **`.gitignore`** (contém a API key). Tem **2 clients** — `com.rodrigoleao.pipa` e `com.rodrigoleao.pipa.debug` — reusando o mesmo `mobilesdk_app_id`, porque o build debug tem sufixo `.debug` (senão o plugin falha com *"No matching client found"*). **Efeito colateral:** telemetria de debug cai no mesmo app do release. Restringir a chave no Google Cloud Console é recomendação pendente.
- **Crashlytics:** auto-captura crashes; no release o `mapping.txt` sobe sozinho (desofuscação). Coleta em debug **ligada** (para testar).
- **Analytics:** todo evento passa pelo **`AnalyticsService`** (`data/analytics/`, nomes/parâmetros em constantes, `snake_case`, **sem PII**). ViewModels recebem por `@Inject`; composables usam o `AnalyticsViewModel` (a `AppNavigation` loga `screen_view` manualmente — Compose não gera automático). User property `app_language` fatiando os relatórios. Ao criar evento novo, centralize no `AnalyticsService`.
- **DebugView:** `adb shell setprop debug.firebase.analytics.app com.rodrigoleao.pipa.debug` → Console → Analytics → DebugView.

## Armadilhas conhecidas

- **Edge-to-edge:** `MainActivity` chama `enableEdgeToEdge()`, o que desativa o resize automático da janela pelo teclado. Telas com formulário precisam de `.imePadding()` antes do scroll para o campo focado subir acima do teclado virtual (padrão já aplicado em `Edit*`, `CreateTrip` e no editor de notas).
- **Seed de exemplo é debug-only:** `DatabaseSeeder.seedIfEmpty` só roda dentro de `if (BuildConfig.DEBUG)` no `MainActivity`. Release nasce sem viagens. Para screenshots/testes com dados, use o build debug.
- **Assinatura release:** o build type `release` **usa** o `signingConfig("release")` (linha `signingConfig = signingConfigs.getByName("release")` no `buildTypes.release`). Ele lê a **upload key** em `keystore/gramado2026.jks` (gitignored via `*.jks`) + as props `STORE_PASSWORD`/`KEY_ALIAS`(=`gramado`)/`KEY_PASSWORD`, que ficam no **`~/.gradle/gradle.properties` global** (fora do repo — nunca comitar senha). Com isso, `bundleRelease` gera um **AAB assinado** direto; se a keystore/props faltarem, o build **falha** (em vez de gerar artefato não assinado silenciosamente). O app usa **Play App Signing** (o Google detém a app signing key; nós só assinamos o upload). Verificar o AAB com `jarsigner -verify <aab>` (espera-se `jar verified` + um Warning de "certificate chain is invalid" — **normal** para cert autoassinado). Para APK, `apksigner verify` — e **não** faça pipe para `Select-Object` (fecha o stdout do exe e retorna exit 255 falso).

## Git

Repo pessoal no GitHub (`rodrigoleao111/rumo-app`), identidade **Rodrigo Leão `<rasantos@informa.com.br>`**. Operações de leitura são livres; **commit** local pode ser feito mostrando a mensagem antes; **nunca** `push` sem aprovação explícita do usuário. Trailer de co-autoria nos commits quando aplicável.

## Documentação

`docs/` tem visão arquitetural (`arquitetura-geral.md`), design system (`design-system.md`), guia de testes (`guia-testes.md`), schemas (`travel-export-schema.md`, `ai-itinerary-schema.md`) e um doc por módulo (`modulo-01`..`modulo-17`). Ao mudar comportamento de uma tela, atualize o `modulo-*` correspondente. Screenshots em `docs/screenshots/` são capturados do build debug.
