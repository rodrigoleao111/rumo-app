# Rumo — App de Roteiros de Viagem

App Android nativo para organizar roteiros de viagem com previsão do tempo ao vivo, geração de itinerário por IA e compartilhamento entre dispositivos.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navegação | Navigation Compose |
| Arquitetura | MVVM (ViewModel + StateFlow) |
| Banco de dados | Room (SQLite) v12 |
| Clima | Open-Meteo API |
| IA | Gemini 2.0 Flash (Google AI) |
| minSdk | 26 (Android 8.0+) |
| targetSdk | 34 |

## Funcionalidades

- **Múltiplas viagens** — crie, edite e exclua viagens; badge de status automático (countdown / Em curso / Concluída)
- **Roteiro diário** — timeline de atividades com horário, emoji, descrição, badges e paradas de caminhada
- **Clima ao vivo** — previsão Open-Meteo por dia de viagem, com cache de 3h e refresh manual
- **Geração de roteiro por IA** — chat com Gemini ou importação de JSON gerado por qualquer IA
- **Compartilhamento `.travel`** — exporta toda a viagem (roteiro, documentos, vouchers) como arquivo ZIP renomeado; importação com um toque
- **Integração com Maps e Uber** — deep links direto de qualquer atividade
- **Documentos por dia** — anexe PDFs ou imagens a cada dia do roteiro
- **Vouchers** — cards com acento colorido por tipo, drag-to-reorder, toggle "Usado", agrupamento por categoria/pessoa/dia (preferência salva por viagem)
- **Configurações** — toggle para abrir automaticamente a viagem em curso ao iniciar o app (quando há exatamente uma ativa)
- **Passagens** — suporte a qualquer tipo de transporte (avião, trem, ônibus, navio); card adaptativo com ícone, labels e campos conforme o tipo; anexo de arquivo ou link da passagem; campo de observações; portão de embarque editável (somente voos)

## Setup

### Pré-requisitos

- Android Studio Hedgehog ou superior
- JDK 17 (incluso no Android Studio)
- Dispositivo ou emulador Android 8.0+
- Conta no [Google AI Studio](https://aistudio.google.com) com acesso ao `gemini-2.0-flash` (requer plano pago)

### Configuração

1. Abra a pasta `GramadoApp/` no Android Studio
2. Crie o arquivo `local.properties` na raiz do projeto (se não existir) e adicione:
   ```
   GEMINI_API_KEY=sua_chave_aqui
   ```
   > `local.properties` **não deve ser versionado** — já está no `.gitignore`

3. Copie os assets da viagem (se aplicável):
   - PDFs de vouchers → `app/src/main/assets/vouchers/` (mantendo subpastas)
   - Imagem de voo → `app/src/main/assets/vouchers/voo.jpeg`
   - Mapa do Bustour → `app/src/main/assets/images/mapa_rotas_bustour.webp`

4. Sync Gradle e execute

### Comandos

```bash
# Build
./gradlew assembleDebug

# Instalar no dispositivo conectado
./gradlew installDebug

# Testes
./gradlew test
```

## Estrutura de pastas

```
app/src/main/
├── java/com/rodrigoleao/gramado2026/
│   ├── MainActivity.kt
│   ├── data/
│   │   ├── model/Models.kt              ← data classes centrais
│   │   ├── ai/ItineraryGenerator.kt     ← Gemini: chat, geração e parse JSON
│   │   ├── db/                          ← Room: database, DAOs, entities, mappers
│   │   ├── export/TravelExporter.kt     ← gera arquivo .travel (ZIP)
│   │   ├── import/TravelImporter.kt     ← importa arquivo .travel
│   │   ├── preferences/SettingsRepository.kt ← SharedPreferences: configurações do app
│   │   ├── repository/                  ← TripRepository, RoteiroRepository
│   │   └── weather/WeatherRepository.kt ← Open-Meteo API + cache
│   ├── navigation/AppNavigation.kt
│   └── ui/
│       ├── splash/SplashScreen.kt
│       ├── trips/                        ← lista, criação e wizard de IA
│       ├── home/HomeScreen.kt
│       ├── day/DayDetailScreen.kt
│       ├── edit/                         ← edição de viagem, dia e atividade
│       ├── share_trip/                   ← compartilhamento .travel
│       ├── import_trip/                  ← importação .travel
│       ├── settings/                     ← SettingsScreen, SettingsViewModel
│       ├── contacts/ContactsScreen.kt
│       ├── vouchers/VouchersScreen.kt
│       ├── components/BadgeChip.kt
│       └── theme/                        ← Color, Type, Theme
├── assets/
│   ├── vouchers/
│   └── images/
└── res/
    └── xml/file_paths.xml               ← FileProvider paths
```

## Formato `.travel`

Arquivo ZIP renomeado com extensão `.travel`. Contém:

```
trip.json          ← roteiro completo (schema v1)
documents/         ← documentos anexados aos dias
vouchers/          ← vouchers e ingressos
boarding/          ← cartões de embarque
```

Veja `docs/travel-export-schema.md` para o schema completo do `trip.json`.

## Banco de dados

Room versão 14. Migrations explícitas em `TravelDatabase.kt` — **nunca usar `fallbackToDestructiveMigration()`**.

Para adicionar campos: crie `MIGRATION_N_(N+1)`, incremente `version` no `@Database` e registre em `.addMigrations(...)`.

## Paleta de cores

| Token | Uso |
|---|---|
| `GreenMoss` | Primary, TopAppBar, badges de data, hotel card |
| `AmberPrimary` | Accent, snackbar, temperatura, botões "Próximo" |
| `GreenLight` | Background geral |
| `SurfaceWhite` | Cards |

Botões de ação principal: `containerColor = GreenMoss`, ícone/texto `AmberPrimary`.

## Documentação adicional

| Arquivo | Conteúdo |
|---|---|
| `docs/travel-export-schema.md` | Schema do `trip.json`, estrutura do ZIP, regras de import/export |
| `docs/ai-itinerary-schema.md` | Schema JSON para IA, prompt gerado pelo app, modos chat e importar |
