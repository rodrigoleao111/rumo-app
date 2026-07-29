# Inventário de Ícones — Pipa

Levantamento de **todos os ícones** usados hoje no app e os que provavelmente usaremos,
para produção da nova identidade (set de ícones de linha da marca).

Legenda de status:
- ✅ **Pronto** — já produzido/integrado.
- 🎨 **Você produz** — asset de marca a ser gerado.
- 💻 **Eu implemento** — troca/wire no código depois que o asset existir.
- ⚙️ **Decisão** — precisa de uma escolha antes de produzir.

Prioridade: **P0** (essencial p/ publicar) · **P1** (identidade coesa) · **P2** (refinamento).

---

## A. Ícones de marca (launcher / splash / notificação)

| Ícone | Onde | Formato | Status |
|---|---|---|---|
| **Ícone do app (pipa)** — adaptive: fundo verde `#1B4332` + pipa | Launcher, Play Store | PNG 512 (fg/mono) + mipmaps + 1024 master | ✅ Pronto |
| **Monochrome (silhueta da pipa)** — ícones temáticos Android 13+ | Launcher (tema) | PNG 512 preto/transparente | ✅ Pronto |
| **Play Store 512** | Google Play Console | PNG 512 RGB | ✅ Pronto |
| **Feature graphic** 1024×500 | Play Store (banner da ficha) | PNG/JPG | 🎨 P0 |
| **Ícone do splash do sistema** (`ic_splash_transparent`) | Splash nativo (Android 12+) | Vetor/PNG transparente, ~240dp, fundo `#1B4332` | 🎨 P1 — hoje é um vetor **vazio** (placeholder), não a arte final |
| **Ilustração de splash tela cheia** (`splash_background.png`) | `SplashScreen.kt` (Compose) | PNG tela cheia | 🎨 P1 — hoje é a arte antiga |
| **Ícone de notificação** (`setSmallIcon`) | Notificação de check-in | Vetor monocromático (silhueta, canal alfa) — **regra do Android: só branco+alfa** | 🎨 P1 — hoje reaproveita o foreground colorido (aparece como quadrado branco na barra) |

> ⚠️ O ícone de notificação **precisa** ser um vetor só-silhueta (branco/transparente). Hoje aponta
> para `ic_launcher_foreground` (colorido), o que o Android renderiza como um quadrado sólido na
> status bar. Candidato natural: a própria silhueta da pipa (podemos reusar o monochrome como vetor).

---

## B. Ícones de interface (set de linha próprio da marca)

> ✅ **Migração concluída.** O app já usa um **set vetorial próprio** (`res/drawable/ic_*.xml`, ~45 ícones), com espessura de traço, cantos e grid 24dp consistentes. Os **Material Icons** sobraram em apenas 5 usos residuais: `Icons.Filled.Menu`, `Icons.Filled.MoreVert` e `Icons.Filled.Add` (TripsListScreen) e `Icons.Outlined.ZoomIn`, `Icons.Outlined.Close` (seletor de capas — TripCovers). As tabelas abaixo mapeiam cada ícone ao recurso `ic_*.xml` atualmente em uso.

### B1. Navegação inferior (5) — **P0, mais visíveis** · ✅ em uso
Ligados em `TAB_ICON_RES` (AppNavigation).

| Função | Ícone atual (`ic_*.xml`) | Observação |
|---|---|---|
| Início | `ic_home` | aba Home |
| Vouchers | `ic_ticket` | aba Vouchers |
| Embarque | `ic_boarding` | aba Cartões de embarque |
| Contatos | `ic_contacts` | aba Contatos |
| Notas | `ic_notes_nav` | aba Notas |

### B2. Ações da barra de topo / gerais · ✅ em uso
`ic_arrow_back` (voltar) · `ic_check` (salvar/confirmar) · `ic_close` (fechar) · `ic_add` (adicionar) ·
`ic_edit` (editar) · `ic_delete` (excluir) · `ic_share` (compartilhar) · `ic_settings` (config) ·
`ic_sort` (ordenar) · `ic_help` (ajuda) · `ic_refresh` (atualizar) · `ic_send` (enviar).
Resíduos Material: `Icons.Filled.MoreVert` e `Icons.Filled.Add` (menu/FAB em TripsListScreen).

### B3. Conteúdo / campos · ✅ em uso
`ic_location` (destino/local) · `ic_calendar` (datas) · `ic_schedule` (hora) ·
`ic_phone` (telefone) · `ic_map` (mapa) · `ic_car` (Uber/carro) · `ic_link` (link) ·
`ic_attach` (anexo) · `ic_copy` (copiar) · `ic_note_text` (nota texto) · `ic_title` (bloco título).

### B4. Estados / seleção · ✅ em uso
`ic_check_circle` + `ic_radio_unchecked` (seleção) · `ic_checkbox_checked` + `ic_checkbox_blank` (checklist) ·
`ic_star` + `ic_star_border` (favorito) · `ic_notification` + `ic_notification_off` (lembrete on/off) ·
`ic_chevron_down` + `ic_chevron_up` (expandir/recolher) · `ic_drag` (arrastar p/ reordenar).

### B5. Import / IA · ✅ em uso
`ic_file_upload` / `ic_upload` / `ic_import` / `ic_download` (importar/exportar arquivo) ·
`ic_auto_awesome` (gerar roteiro com IA).

> ✅ Migração feita: os 5 da bottom nav e o restante do set de interface já são vetores próprios
> (`ic_*.xml`). Padrão dos SVGs de origem: **24×24, traço único, sem preenchimento** — a cor vem
> do código (tint), convertidos para `VectorDrawable`.

---

## C. Emojis usados como ícones

O app usa emojis em vários pontos. Dividem-se em **fixos por função** (candidatos a virar ícones/
ilustrações da marca) e **conteúdo dinâmico** (escolhidos pelo usuário/IA — mantêm emoji).

### C1. Clima — set fixo · ✅ implementado (decisão tomada)
✅ **Decisão feita:** virou **ilustração própria da marca**. O clima usa 10 imagens `weather_*.png`
(`res/drawable-nodpi/`), mapeadas por código WMO em `weatherCodeDrawable()` (`WeatherIcon.kt`).
Os emojis `☀️ 🌤️ ⛅ ☁️ 🌫️ 🌦️ 🌧️ ❄️ 🌨️ ⛈️ 🌡️` ficaram apenas como fallback.

### C2. Transporte / embarque — set fixo (5) · ✅ implementado
✅ **Decisão feita:** virou ilustração própria. Usa `transport_flight/train/bus/ship/ticket.png`
(`res/drawable-nodpi/`), mapeadas por `transportDrawable()` em `BoardingPassScreen.kt`.
Emojis `✈️ 🚂 🚌 🚢 🎫` ficaram como fallback.

### C3. Emojis de seção/UI fixos · ⚙️ P2
`📋` (roteiro vazio) · `🔗` (links) · `📎` (anexos) · `📝` (notas) · `⚠️` (aviso) ·
`🎫` "LEVAR HOJE" · `🗺️` Maps · `🚗` Uber · `🏨` (home) · `🎟️` (vouchers/embarque) ·
`📅` (data) · `🔔` (ativar lembrete) · `👤` (pessoa) · `📂` (grupo) · `💡` (dica).
Decisão: alguns destes ficariam melhores como ícones de linha da marca (ex.: aviso, links, anexo).

### C4. Emojis de conteúdo — **dinâmicos, mantêm emoji**
Capa de viagem, atividades do dia, paradas de caminhada, vouchers: o emoji é escolhido pelo
usuário/IA (defaults: `📍` atividade, `🎫`/`🎟️` voucher, `✈️` capa). **Não são assets de marca** —
no máximo poderíamos curar uma **paleta sugerida de emojis** no seletor.

---

## D. Resumo do que produzir (por prioridade)

**P0 (publicação):**
- 🎨 Feature graphic 1024×500 (Play Store) — *pendente*
- ✅ 5 ícones de linha da bottom nav (Início, Vouchers, Embarque, Contatos, Notas) — **produzidos e em uso**

**P1 (identidade coesa):**
- 🎨 Ícone de notificação (silhueta branca/alfa) — *pendente* (hoje reaproveita o foreground colorido)
- 🎨 Ícone do splash do sistema + ilustração de splash tela cheia — *pendente* (`ic_splash_transparent` é vetor vazio)
- ✅ Set de linha da interface — topo/ações (B2), conteúdo (B3), estados (B4) — **em uso** (`ic_*.xml`)

**P2 (refinamento):**
- ✅ Ícones de clima (10) e transporte (5) — **implementados** como `weather_*.png` / `transport_*.png`
- ⚙️ Ícones de seção fixos (C3) — *ainda emoji*
- ⚙️ Paleta curada de emojis para o seletor de conteúdo — *pendente*

---

### Especificação técnica dos SVGs de interface
- Grid **24×24 dp**, área de traço dentro de 20×20 (margem 2dp).
- **Traço único**, espessura consistente (~2 dp), sem preenchimento sólido.
- Cantos e terminações arredondados (combina com o tom da marca).
- Entregar como **SVG** (converto para `VectorDrawable`/`ImageVector`); a cor vem do código (tint),
  então o SVG pode ser preto em fundo transparente.
