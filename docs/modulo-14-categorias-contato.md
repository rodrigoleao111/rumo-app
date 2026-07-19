# Módulo 14 — Categorias de contato personalizadas

**Arquivo:** `data/preferences/ContactCategoryRepository.kt`
**Consumidor:** `ui/edit/EditContactViewModel.kt`
**Injeção:** Hilt (`AppModule.provideContactCategoryRepository`)

---

## Visão geral

Além das categorias fixas de contato (agência, hotel, atrações, família, emergência), o usuário pode criar **categorias personalizadas** ao cadastrar/editar um contato. Essa lista de categorias custom é global ao app (não por viagem) e persiste em **SharedPreferences** como um array JSON simples.

Ver `docs/modulo-07-contacts.md` para como as categorias (fixas + custom) são usadas no agrupamento da `ContactsScreen`. O formulário que consome/cria categorias é o `EditContactScreen` (par `EditContactViewModel`).

---

## `ContactCategoryRepository`

```kotlin
class ContactCategoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("contact_categories", Context.MODE_PRIVATE)

    fun getCustomCategories(): List<String> {
        val json = prefs.getString(KEY, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { arr.getString(it) }
        }.getOrDefault(emptyList())          // parse defensivo → lista vazia em caso de JSON inválido
    }

    fun addCategory(name: String) {
        val current = getCustomCategories().toMutableList()
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && !current.contains(trimmed)) {   // ignora vazio e duplicado
            current.add(trimmed)
            prefs.edit { putString(KEY, JSONArray().apply { current.forEach { put(it) } }.toString()) }
        }
    }

    companion object { private const val KEY = "custom_categories" }
}
```

| Método | Retorno | Comportamento |
|---|---|---|
| `getCustomCategories()` | `List<String>` | Lê o array JSON da chave `custom_categories`; parse tolerante (`runCatching → emptyList` se corrompido). |
| `addCategory(name)` | — | `trim` no nome; grava só se não for vazio **e** ainda não existir (dedup case-sensitive). |

**Persistência:** `SharedPreferences("contact_categories")`, chave `custom_categories` → string JSON (ex.: `["Guias","Passeios"]`).

**Por que não Room:** é uma lista pequena de strings, global e sem necessidade de consulta/relacionamento/export — encaixa na regra do projeto de usar `SharedPreferences` para dados chave-valor simples (ver `docs/arquitetura-geral.md`, "Três tipos de persistência").

**API síncrona:** os métodos são síncronos (não `suspend`). São chamados de `EditContactViewModel` dentro do `viewModelScope`/`init`; o volume é ínfimo, então o custo em thread principal é desprezível.

---

## Uso em `EditContactViewModel`

Injetado via Hilt junto com o `ContactRepository`:

```kotlin
@HiltViewModel
class EditContactViewModel @Inject constructor(
    private val repo: ContactRepository,
    private val categoryRepo: ContactCategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    init {
        val customCategories = categoryRepo.getCustomCategories()   // carrega no init
        // … popula EditContactState.customCategories
    }

    fun addCategory(name: String) {
        categoryRepo.addCategory(name)
        _state.value = _state.value.copy(customCategories = categoryRepo.getCustomCategories())  // relê e atualiza o estado
    }
}
```

- No `init`, as categorias custom entram no `EditContactState` para popular o seletor.
- Ao criar uma categoria nova, o ViewModel grava e **relê** a lista para atualizar o estado (não há `Flow` — a leitura é pontual).

---

## Injeção (Hilt)

```kotlin
// AppModule.kt
@Provides @Singleton
fun provideContactCategoryRepository(@ApplicationContext ctx: Context): ContactCategoryRepository =
    ContactCategoryRepository(ctx)
```

Singleton de escopo de aplicação, como os demais repositórios de preferências.

---

## Checklist para futuras modificações

- **Reatividade (atualizar seletor sem relê manual):** migrar para **DataStore** expondo `Flow<List<String>>` — mesmo caminho da melhoria #9 do `SettingsRepository`. Hoje a atualização é por releitura explícita após `addCategory`.
- **Remover/renomear categoria custom:** adicionar `removeCategory(name)` / `renameCategory(old, new)` no repositório (não existem hoje — só é possível adicionar).
- **Categorias por viagem (não globais):** exigiria mover para Room com FK `tripId` + migration — atualmente são globais a todas as viagens.
- **Dedup case-insensitive:** o `contains` atual é sensível a maiúsculas ("Guias" ≠ "guias"); normalizar se for indesejado.
