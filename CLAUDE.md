# CLAUDE.md — rumo-app

Guia para agentes trabalhando neste repositório. Complementa o `README.md` (visão de produto) com convenções, comandos e armadilhas do dia a dia.

## O que é

App Android nativo de roteiros de viagem. Package `com.rodrigoleao.pipa` (o nome `gramado2026` é histórico — o produto é o **Pipa**). Kotlin + Jetpack Compose + Material 3, MVVM, Hilt, Room, DataStore. minSdk 26, compileSdk/targetSdk 34, JDK 17.

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
- Versão atual: `TravelDatabase.CURRENT_VERSION` (**19**) — fonte única de verdade (anotação `@Database` + testes).
- Para alterar o schema: nova `MIGRATION_N_(N+1)` → incrementa `CURRENT_VERSION` → registra em `ALL_MIGRATIONS` → teste de migração com `MigrationTestHelper`.
- `app/schemas/*.json` são **versionados no git — nunca apagar** (histórico usado pelos testes de migração).

## Identidade visual (não regredir)

- **Paleta:** `ui/theme/Color.kt`. Fundo geral do app = `Sand` (`#F4EDE1`); superfície de cards = `SurfaceWhite`; primary = `GreenMoss` (`#1B4332`); accent = `AmberPrimary` (`#E9B43C`). O token creme chama-se `Cream` (não existe mais `GreenLight`).
- **Ícones:** use o set vetorial da marca em `res/drawable/ic_*.xml` via `ImageVector.vectorResource(R.drawable.ic_*)`. Evite `Icons.Filled.*` (Material) — só restam uns poucos usos residuais (Menu/MoreVert/Add, ZoomIn/Close).
- **Ilustrações:** capas `cover_*.webp`, clima `weather_*.png`, transporte `transport_*.png` em `res/drawable-nodpi/`.
- **Tipografia:** Plus Jakarta Sans (`res/font/`), já ligada no tema.
- **VectorDrawable — armadilha:** `viewportWidth/Height` DEVE bater com a escala das coordenadas do `pathData`. Ícone invisível/gigante geralmente é viewport errado (ex.: pathData em escala 512 com viewport 24).

## Armadilhas conhecidas

- **Edge-to-edge:** `MainActivity` chama `enableEdgeToEdge()`, o que desativa o resize automático da janela pelo teclado. Telas com formulário precisam de `.imePadding()` antes do scroll para o campo focado subir acima do teclado virtual (padrão já aplicado em `Edit*`, `CreateTrip` e no editor de notas).
- **Seed de exemplo é debug-only:** `DatabaseSeeder.seedIfEmpty` só roda dentro de `if (BuildConfig.DEBUG)` no `MainActivity`. Release nasce sem viagens. Para screenshots/testes com dados, use o build debug.
- **Assinatura release:** o `signingConfig("release")` lê `keystore/gramado2026.jks` + props `STORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD`. Se ausentes, `assembleRelease` gera APK **não assinado** — assine com `apksigner` (build-tools em `...\Android\Sdk\build-tools\<versão>\`). Ao verificar com `apksigner verify`, **não** faça pipe para `Select-Object` (fecha o stdout do exe e retorna exit 255 falso).

## Git

Repo pessoal no GitHub (`rodrigoleao111/rumo-app`), identidade **Rodrigo Leão `<rasantos@informa.com.br>`**. Operações de leitura são livres; **commit** local pode ser feito mostrando a mensagem antes; **nunca** `push` sem aprovação explícita do usuário. Trailer de co-autoria nos commits quando aplicável.

## Documentação

`docs/` tem visão arquitetural (`arquitetura-geral.md`), design system (`design-system.md`), guia de testes (`guia-testes.md`), schemas (`travel-export-schema.md`, `ai-itinerary-schema.md`) e um doc por módulo (`modulo-01`..`modulo-15`). Ao mudar comportamento de uma tela, atualize o `modulo-*` correspondente. Screenshots em `docs/screenshots/` são capturados do build debug.
