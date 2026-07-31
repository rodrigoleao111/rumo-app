# Módulo 10 — Configurações

**Tela:** `SettingsScreen`  
**Arquivo:** `ui/settings/SettingsScreen.kt`  
**ViewModel:** `ui/settings/SettingsViewModel.kt`  
**Repositório:** `data/preferences/SettingsRepository.kt`  
**Entry point de navegação:** item "Configurações" da gaveta (`ModalNavigationDrawer`, aberta pelo botão ☰) de `TripsListScreen`

---

## Visão geral

Tela de configurações globais do app com **quatro toggles** (persistidos em **DataStore**, `androidx.datastore.preferences`) e um **seletor de idioma** (persistido à parte, via `LocaleHelper`/`SharedPreferences` — ver seção *Idioma*). As preferências afetam o comportamento de toda a aplicação — não de uma viagem específica. A tela não tem scroll: todos os itens cabem em uma coluna simples.

---

## Padrão de arquitetura

Segue **MVVM** com `SettingsRepository` expondo `Flow<Boolean>` via DataStore.

| Camada | Arquivo | Responsabilidade |
|---|---|---|
| **View** | `SettingsScreen.kt` | Stateless — coleta quatro `StateFlow<Boolean>` e emite eventos de toggle |
| **ViewModel** | `SettingsViewModel.kt` | Converte `Flow<Boolean>` do repo em `StateFlow` via `stateIn(Eagerly)` |
| **Repositório** | `SettingsRepository.kt` | Lê e grava em `DataStore<Preferences>` — expõe `Flow<Boolean>` e `suspend fun set*()` |

> **Corrotinas no ViewModel:** os métodos `set*()` são `suspend` — o ViewModel usa `viewModelScope.launch { settings.set*(...) }`. A UI reage reativamente ao `Flow`, sem necessidade de recomposição manual.

---

## `SettingsRepository`

```kotlin
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "pipa_settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val autoOpenActiveTrip: Flow<Boolean> =
        dataStore.data.map { it[KEY_AUTO_OPEN] ?: true }

    val showEmergencyContacts: Flow<Boolean> =
        dataStore.data.map { it[KEY_EMERGENCY_CONTACTS] ?: true }

    val sortTripsByProximity: Flow<Boolean> =
        dataStore.data.map { it[KEY_SORT_BY_PROXIMITY] ?: true }

    val hideCompletedTrips: Flow<Boolean> =
        dataStore.data.map { it[KEY_HIDE_COMPLETED] ?: false }

    suspend fun setAutoOpenActiveTrip(enabled: Boolean)   { dataStore.edit { it[KEY_AUTO_OPEN] = enabled } }
    suspend fun setShowEmergencyContacts(enabled: Boolean) { dataStore.edit { it[KEY_EMERGENCY_CONTACTS] = enabled } }
    suspend fun setSortTripsByProximity(enabled: Boolean)  { dataStore.edit { it[KEY_SORT_BY_PROXIMITY] = enabled } }
    suspend fun setHideCompletedTrips(enabled: Boolean)    { dataStore.edit { it[KEY_HIDE_COMPLETED] = enabled } }

    companion object {
        private val KEY_AUTO_OPEN          = booleanPreferencesKey("auto_open_active_trip")
        private val KEY_EMERGENCY_CONTACTS = booleanPreferencesKey("show_emergency_contacts")
        private val KEY_SORT_BY_PROXIMITY  = booleanPreferencesKey("sort_trips_by_proximity")
        private val KEY_HIDE_COMPLETED     = booleanPreferencesKey("hide_completed_trips")
    }
}
```

**DataStore:** `preferencesDataStore(name = "pipa_settings")` — extensão de propriedade no `Context`. Instância única por processo via delegado Kotlin.

**Chaves:** as quatro `booleanPreferencesKey` ficam no `companion object` da classe.

**Defaults:** `autoOpenActiveTrip`, `showEmergencyContacts` e `sortTripsByProximity` iniciam como `true`; `hideCompletedTrips` inicia como `false`. O `?: <default>` no `.map { }` aplica o default quando a chave ainda não existe.

**Reatividade:** qualquer gravação via `dataStore.edit { }` propaga automaticamente pelo `Flow` para todos os coletores — sem recomposição manual, sem `DisposableEffect`.

---

## `SettingsViewModel`

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository
) : ViewModel() {

    val autoOpenActiveTrip: StateFlow<Boolean> = settings.autoOpenActiveTrip
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showEmergencyContacts: StateFlow<Boolean> = settings.showEmergencyContacts
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val sortTripsByProximity: StateFlow<Boolean> = settings.sortTripsByProximity
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val hideCompletedTrips: StateFlow<Boolean> = settings.hideCompletedTrips
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setAutoOpenActiveTrip(enabled: Boolean)   { viewModelScope.launch { settings.setAutoOpenActiveTrip(enabled) } }
    fun setShowEmergencyContacts(enabled: Boolean) { viewModelScope.launch { settings.setShowEmergencyContacts(enabled) } }
    fun setSortTripsByProximity(enabled: Boolean)  { viewModelScope.launch { settings.setSortTripsByProximity(enabled) } }
    fun setHideCompletedTrips(enabled: Boolean)    { viewModelScope.launch { settings.setHideCompletedTrips(enabled) } }
}
```

**`stateIn(Eagerly)`:** cada `Flow<Boolean>` do repositório é convertido em `StateFlow` com início imediato (`Eagerly`) — o valor já está disponível na primeira composição, sem valor inicial nulo. O valor inicial do `stateIn` casa com o default de cada preferência (`true` para as duas primeiras, `false` para as duas novas).

**Propagação reativa:** a gravação via `dataStore.edit` propaga automaticamente de volta pelo `Flow` para o `StateFlow` — não há atualização manual do estado após `set*()`. O ViewModel não é mais a fonte de verdade: o DataStore é.

---

## `SettingsScreen`

A tela coleta os quatro estados e renderiza um `Row` por configuração:

```
Scaffold (TopAppBar GreenMoss + botão Voltar)
 └─ Column (padding 16dp horizontal, 8dp vertical)
      ├─ Row [toggle 1] — "Abrir viagem em curso automaticamente"
      ├─ HorizontalDivider (Sand)
      ├─ Row [toggle 2] — "Adicionar números de emergência..."
      ├─ HorizontalDivider (Sand)
      ├─ Row [toggle 3] — "Ordenar viagens por proximidade"
      ├─ HorizontalDivider (Sand)
      ├─ Row [toggle 4] — "Ocultar viagens concluídas"
      └─ HorizontalDivider (Sand)
```

**Estrutura de cada `Row`:**
```
Row (SpaceBetween, fillMaxWidth, padding vertical 12dp)
 ├─ Column (weight(1f), padding end 16dp)
 │    ├─ Text (título, bodyLarge, Medium, TextPrimary)
 │    └─ Text (descrição, bodySmall, TextSecondary)
 └─ Box (scale 0.80f)
      └─ Switch (cores customizadas)
```

`Modifier.scale(0.80f)` no `Box` que envolve o `Switch` — reduz o tamanho visual do componente sem afetar o layout do `Row` (o espaço reservado permanece o original do `Switch`).

**Cores do `Switch`:**

| Estado | Thumb | Track | Border |
|---|---|---|---|
| Ativado | `GreenMoss` | `AmberPrimary` | `AmberPrimary` |
| Desativado | `GreenMoss` | `#9E9E9E` (cinza) | `#9E9E9E` (cinza) |

O thumb é sempre `GreenMoss` — o estado é sinalizado apenas pela cor do track.

---

## Comportamento das configurações no app

### Toggle 1 — "Abrir viagem em curso automaticamente"

**Lido em:** `AppNavigation` na composição inicial.

**Lógica:**
```
se autoOpenActiveTrip == true
  && viagens ativas == exatamente 1
     → navega direto para HomeScreen da viagem ativa
     → omite TripsListScreen do backstack
```

A condição `== exatamente 1` é importante: se houver zero ou mais de uma viagem ativa, o comportamento é o padrão (lista de viagens). Isso evita ambiguidade quando o usuário tem múltiplas viagens simultâneas.

**Quem consume:** `AppNavigation.kt` — obtém um `SettingsViewModel` via `hiltViewModel()` e coleta `settingsVm.autoOpenActiveTrip` com `collectAsStateWithLifecycle()` na composição do grafo de navegação (mesmo `SettingsViewModel` usado para `showEmergencyContacts`).

### Toggle 2 — "Adicionar números de emergência nos contatos das viagens"

**Lido em:** `ContactsScreen` via parâmetro `showEmergencyContacts: Boolean`.

**Lógica:** quando `true`, os três contatos builtins (SAMU 192, Bombeiros 193, PM 190) são injetados visualmente no início do grupo "Emergências" em **todas** as viagens. São contatos virtuais — IDs negativos, nunca gravados no banco. Ver `docs/modulo-07-contacts.md` para detalhes do rendering.

**Quem consume:** `AppNavigation` passa o valor para `MainPagerScreen` → `ContactsScreen`.

### Toggle 3 — "Ordenar viagens por proximidade"

**Default:** `true` (ligado — a lista já nasce ordenada por proximidade).

**Lido em:** `TripsListViewModel` — o `Flow` `sortTripsByProximity` entra no `combine` que produz a lista exibida.

**Lógica:** quando `true`, a lista é reordenada por proximidade (viagem em curso primeiro, depois as futuras mais próximas de começar e, por último, as concluídas mais recentes). Quando `false`, mantém a ordem do repositório (`createdAt ASC`). A reordenação é feita pelas funções `sortByProximity`/`proximityKey` — nada é gravado no banco.

### Toggle 4 — "Ocultar viagens concluídas"

**Default:** `false`.

**Lido em:** `TripsListViewModel` — o `Flow` `hideCompletedTrips` entra no mesmo `combine`.

**Lógica:** quando `true`, as viagens já encerradas (`isCompleted`: têm datas válidas e hoje já passou da data final) são filtradas da lista. É apenas exibição — as viagens não são apagadas e reaparecem ao desativar o toggle.

> **Vínculo com a lista:** os toggles 3 e 4 não têm efeito na `SettingsScreen` em si; são consumidos pelo `TripsListViewModel` via `combine(repo.allTrips, sortTripsByProximity, hideCompletedTrips)` (ver `docs/modulo-01-lista-viagens.md`).

### Idioma (seletor de idioma)

Abaixo dos quatro toggles há uma linha **Idioma** (`LanguageSettingRow`, em `ui/settings/LanguageSettingRow.kt`). Ao tocar, abre um diálogo com quatro opções — **Sistema / Português / English / Español**. Ao escolher, a preferência é gravada e a `Activity` é recriada para reaplicar o locale.

- **Fora do DataStore:** o idioma é lido bem cedo (em `MainActivity.attachBaseContext`), antes de qualquer recurso ser resolvido — cedo demais para o DataStore (assíncrono). Por isso fica em `SharedPreferences` (`pipa_locale` / chave `app_language`), acessado por `LocaleHelper` (`locale/LocaleHelper.kt`).
- **Aplicação do locale:** `LocaleHelper.wrap(context)` envolve o contexto com o locale escolhido (`createConfigurationContext`); `"system"` mantém o idioma do aparelho (fallback para os recursos padrão = português).
- **Troca:** `LocaleHelper.setLanguage(...)` grava e `context.findActivity()?.recreate()` reinicia a UI no novo idioma.

Panorama de recursos (`res/values{,-en,-es}/strings.xml`) na seção **Internacionalização** de `docs/arquitetura-geral.md`.

---

## Composables e símbolos (resumo)

| Símbolo | Tipo | Responsabilidade |
|---|---|---|
| `SettingsRepository` | classe | Persistência em `DataStore("pipa_settings")` — expõe quatro `Flow<Boolean>` e quatro `suspend fun set*()` |
| `SettingsViewModel` | `@HiltViewModel` | Quatro `Flow<Boolean>` → `StateFlow` via `stateIn(Eagerly)`; `set*()` via `viewModelScope.launch` |
| `SettingsScreen` | composable | Quatro `Row` com `Switch` + `LanguageSettingRow` + `HorizontalDivider(Sand)`; stateless — coleta e emite via ViewModel. Textos via `stringResource` |
| `LanguageSettingRow` | composable | Linha "Idioma" + diálogo de seleção (Sistema/PT/EN/ES); grava via `LocaleHelper` e chama `Activity.recreate()` |

---

## Checklist para futuras modificações

- **Nova configuração:** adicionar `booleanPreferencesKey` em `SettingsRepository` → adicionar `val x: Flow<Boolean>` + `suspend fun setX()` → adicionar `StateFlow` + `setX()` em `SettingsViewModel` → adicionar `Row` com `Switch` em `SettingsScreen` → passar o valor para o composable que o consome via `AppNavigation`.
- **Default diferente de `true`:** alterar o `?: true` no `.map { }` em `SettingsRepository`. Usuários existentes **não são afetados** — o default só vale se a chave ainda não existir no DataStore.
- **Configuração por viagem (não global):** usar `TripEntity` + migration de banco (campo novo) em vez de DataStore. `SettingsRepository` é apenas para configurações globais do app.
- **Seções no futuro:** se houver muitas configurações, agrupar com `Text` de label de seção (10sp uppercase GreenMoss, mesmo padrão de `SectionLabel` em `CreateTripScreen`) acima de cada grupo de `Row`s.
- **Novo texto de UI:** criar a chave nas **três** `strings.xml` (pt/en/es) e usar `stringResource` — nunca hardcode. Ver *Internacionalização* em `docs/arquitetura-geral.md`.
