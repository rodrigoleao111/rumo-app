# Módulo 10 — Configurações

**Tela:** `SettingsScreen`  
**Arquivo:** `ui/settings/SettingsScreen.kt`  
**ViewModel:** `ui/settings/SettingsViewModel.kt`  
**Repositório:** `data/preferences/SettingsRepository.kt`  
**Entry point de navegação:** ícone de engrenagem no cabeçalho de `TripsListScreen`

---

## Visão geral

Tela de configurações globais do app com dois toggles. As preferências afetam o comportamento de toda a aplicação — não de uma viagem específica — e são persistidas em **DataStore** (`androidx.datastore.preferences`). A tela não tem scroll: todos os itens cabem em uma coluna simples.

---

## Padrão de arquitetura

Segue **MVVM** com `SettingsRepository` expondo `Flow<Boolean>` via DataStore.

| Camada | Arquivo | Responsabilidade |
|---|---|---|
| **View** | `SettingsScreen.kt` | Stateless — coleta dois `StateFlow<Boolean>` e emite eventos de toggle |
| **ViewModel** | `SettingsViewModel.kt` | Converte `Flow<Boolean>` do repo em `StateFlow` via `stateIn(Eagerly)` |
| **Repositório** | `SettingsRepository.kt` | Lê e grava em `DataStore<Preferences>` — expõe `Flow<Boolean>` e `suspend fun set*()` |

> **Corrotinas no ViewModel:** os métodos `set*()` são `suspend` — o ViewModel usa `viewModelScope.launch { settings.set*(...) }`. A UI reage reativamente ao `Flow`, sem necessidade de recomposição manual.

---

## `SettingsRepository`

```kotlin
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "rumo_settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private val KEY_AUTO_OPEN          = booleanPreferencesKey("auto_open_active_trip")
    private val KEY_EMERGENCY_CONTACTS = booleanPreferencesKey("show_emergency_contacts")

    val autoOpenActiveTrip: Flow<Boolean> =
        dataStore.data.map { it[KEY_AUTO_OPEN] ?: true }

    val showEmergencyContacts: Flow<Boolean> =
        dataStore.data.map { it[KEY_EMERGENCY_CONTACTS] ?: true }

    suspend fun setAutoOpenActiveTrip(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_OPEN] = enabled }
    }

    suspend fun setShowEmergencyContacts(enabled: Boolean) {
        dataStore.edit { it[KEY_EMERGENCY_CONTACTS] = enabled }
    }
}
```

**DataStore:** `preferencesDataStore(name = "rumo_settings")` — extensão de propriedade no `Context`. Instância única por processo via delegado Kotlin.

**Defaults:** ambas as configurações iniciam como `true` (ativadas) na primeira execução — o `?: true` no `.map { }` aplica o default quando a chave ainda não existe.

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

    fun setAutoOpenActiveTrip(enabled: Boolean) {
        viewModelScope.launch { settings.setAutoOpenActiveTrip(enabled) }
    }

    fun setShowEmergencyContacts(enabled: Boolean) {
        viewModelScope.launch { settings.setShowEmergencyContacts(enabled) }
    }
}
```

**`stateIn(Eagerly)`:** o `Flow<Boolean>` do repositório é convertido em `StateFlow` com início imediato (`Eagerly`) — o valor já está disponível na primeira composição, sem valor inicial nulo.

**Propagação reativa:** a gravação via `dataStore.edit` propaga automaticamente de volta pelo `Flow` para o `StateFlow` — não há atualização manual do estado após `set*()`. O ViewModel não é mais a fonte de verdade: o DataStore é.

---

## `SettingsScreen`

A tela coleta os dois estados e renderiza um `Row` por configuração:

```
Scaffold (TopAppBar GreenMoss + botão Voltar)
 └─ Column (padding 16dp horizontal, 8dp vertical)
      ├─ Row [toggle 1] — "Abrir viagem em curso automaticamente"
      ├─ HorizontalDivider (GreenLight)
      ├─ Row [toggle 2] — "Adicionar números de emergência..."
      └─ HorizontalDivider (GreenLight)
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

---

## Composables e símbolos (resumo)

| Símbolo | Tipo | Responsabilidade |
|---|---|---|
| `SettingsRepository` | classe | Persistência em `DataStore("rumo_settings")` — expõe `Flow<Boolean>` e `suspend fun set*()` |
| `SettingsViewModel` | `@HiltViewModel` | `Flow<Boolean>` → `StateFlow` via `stateIn(Eagerly)`; `set*()` via `viewModelScope.launch` |
| `SettingsScreen` | composable | Dois `Row` com `Switch`; stateless — coleta e emite via ViewModel |

---

## Checklist para futuras modificações

- **Nova configuração:** adicionar `booleanPreferencesKey` em `SettingsRepository` → adicionar `val x: Flow<Boolean>` + `suspend fun setX()` → adicionar `StateFlow` + `setX()` em `SettingsViewModel` → adicionar `Row` com `Switch` em `SettingsScreen` → passar o valor para o composable que o consome via `AppNavigation`.
- **Default diferente de `true`:** alterar o `?: true` no `.map { }` em `SettingsRepository`. Usuários existentes **não são afetados** — o default só vale se a chave ainda não existir no DataStore.
- **Configuração por viagem (não global):** usar `TripEntity` + migration de banco (campo novo) em vez de DataStore. `SettingsRepository` é apenas para configurações globais do app.
- **Seções no futuro:** se houver muitas configurações, agrupar com `Text` de label de seção (10sp uppercase GreenMoss, mesmo padrão de `SectionLabel` em `CreateTripScreen`) acima de cada grupo de `Row`s.
