# Inventário de Ícones — Rumo

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
| **Ícone do splash do sistema** (`ic_splash_transparent`) | Splash nativo (Android 12+) | Vetor/PNG transparente, ~240dp, fundo `#1B4332` | 🎨 P1 — hoje é o logo antigo |
| **Ilustração de splash tela cheia** (`splash_background.png`) | `SplashScreen.kt` (Compose) | PNG tela cheia | 🎨 P1 — hoje é a arte antiga |
| **Ícone de notificação** (`setSmallIcon`) | Notificação de check-in | Vetor monocromático (silhueta, canal alfa) — **regra do Android: só branco+alfa** | 🎨 P1 — hoje reaproveita o foreground colorido (aparece como quadrado branco na barra) |

> ⚠️ O ícone de notificação **precisa** ser um vetor só-silhueta (branco/transparente). Hoje aponta
> para `ic_launcher_foreground` (colorido), o que o Android renderiza como um quadrado sólido na
> status bar. Candidato natural: a própria silhueta da pipa (podemos reusar o monochrome como vetor).

---

## B. Ícones de interface (Material Icons → set de linha da marca)

Atualmente são os **Material Icons** padrão (Roboto-style). Para a identidade nova, o ideal é um
**set de linha coeso** (mesma espessura de traço ~2px, cantos, grid 24dp). Lista deduplicada do que
o app usa hoje (≈40 ícones):

### B1. Navegação inferior (5) — **P0, mais visíveis**
| Função | Ícone atual | Observação |
|---|---|---|
| Início | `Home` | aba Home |
| Vouchers | `ConfirmationNumber` | aba Vouchers |
| Embarque | `FlightTakeoff` | aba Cartões de embarque |
| Contatos | `Call` | aba Contatos |
| Notas | `EventNote` | aba Notas |

### B2. Ações da barra de topo / gerais (P1)
`ArrowBack` (voltar) · `Check` (salvar/confirmar) · `Close` (fechar) · `Add` (adicionar) ·
`Edit` (editar) · `Delete` (excluir) · `Share` (compartilhar) · `Settings` (config) ·
`Sort` (ordenar) · `HelpOutline` (ajuda) · `Refresh` (atualizar) · `Send` (enviar) ·
`MoreVert`? (não usado hoje, mas provável).

### B3. Conteúdo / campos (P1)
`LocationOn` (destino/local) · `CalendarMonth` / `DateRange` (datas) · `Schedule` (hora) ·
`Phone` (telefone) · `Map` (mapa) · `DirectionsCar` (Uber/carro) · `Link` (link) ·
`AttachFile` (anexo) · `ContentCopy` (copiar) · `Notes` (nota texto) · `Title` (bloco título).

### B4. Estados / seleção (P1)
`CheckCircle` + `RadioButtonUnchecked` (seleção) · `CheckBox` + `CheckBoxOutlineBlank` (checklist) ·
`Star` + `StarBorder` (favorito) · `Notifications` + `NotificationsOff` (lembrete on/off) ·
`ExpandMore` + `ExpandLess` (expandir/recolher) · `DragHandle` (arrastar p/ reordenar).

### B5. Import / IA (P2)
`FileUpload` / `Upload` / `FileOpen` / `Download` (importar/exportar arquivo) ·
`AutoAwesome` (gerar roteiro com IA).

> 💻 Podemos migrar por partes: primeiro os **5 da bottom nav** (P0), depois topo/conteúdo.
> Formato de entrega ideal: **SVG 24×24, traço único, sem preenchimento** (converto para
> `ImageVector`/`VectorDrawable`).

---

## C. Emojis usados como ícones

O app usa emojis em vários pontos. Dividem-se em **fixos por função** (candidatos a virar ícones/
ilustrações da marca) e **conteúdo dinâmico** (escolhidos pelo usuário/IA — mantêm emoji).

### C1. Clima — set fixo (11 estados) · ⚙️ P2
`☀️ 🌤️ ⛅ ☁️ 🌫️ 🌦️ 🌧️ ❄️ 🌨️ ⛈️ 🌡️` (mapeados por código WMO em `LiveWeatherDay.kt`).
Decisão: manter emoji (simples, colorido) **ou** criar mini-ícones de clima da marca.

### C2. Transporte / embarque — set fixo (5) · ⚙️ P2
`✈️ Voo · 🚂 Trem · 🚌 Ônibus · 🚢 Navio · 🎫 (outro)` (em `BoardingPassScreen.kt`).

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
- 🎨 Feature graphic 1024×500 (Play Store)
- 🎨 5 ícones de linha da bottom nav (Início, Vouchers, Embarque, Contatos, Notas)

**P1 (identidade coesa):**
- 🎨 Ícone de notificação (silhueta branca/alfa)
- 🎨 Ícone do splash do sistema + ilustração de splash tela cheia
- 🎨 Set de linha da interface — topo/ações (B2), conteúdo (B3), estados (B4)

**P2 (refinamento):**
- ⚙️ Ícones de clima (11) e transporte (5), se quiser sair dos emojis
- ⚙️ Ícones de seção fixos (C3)
- ⚙️ Paleta curada de emojis para o seletor de conteúdo

---

### Especificação técnica dos SVGs de interface
- Grid **24×24 dp**, área de traço dentro de 20×20 (margem 2dp).
- **Traço único**, espessura consistente (~2 dp), sem preenchimento sólido.
- Cantos e terminações arredondados (combina com o tom da marca).
- Entregar como **SVG** (converto para `VectorDrawable`/`ImageVector`); a cor vem do código (tint),
  então o SVG pode ser preto em fundo transparente.
