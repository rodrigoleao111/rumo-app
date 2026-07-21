# Brief de Repaginação Visual — Rumo

> **Objetivo:** elevar a identidade visual do app ao nível da referência de marca (mascote-pipa, "seu companheiro de viagem", visual ilustrado e acolhedor) para deixá-lo pronto para publicação na Play Store.
>
> **O que este documento é:** a lista completa de **tudo que precisa ser produzido/fornecido na parte de design** para que a repaginação possa ser implementada. Cada item traz especificação técnica (formato, tamanho, densidades, nomenclatura), onde aparece no app e prioridade.
>
> **Referências de marca:** os 3 mockups fornecidos (board de identidade + tela "Minhas viagens" + tela "Férias verão").

---

## 1. Como ler este documento

Cada item está marcado com um **responsável** e uma **prioridade**:

| Marcador | Significado |
|---|---|
| 🎨 **VOCÊ produz** | Arte/asset que precisa ser criado (por você, um designer ou IA de geração de imagem) e entregue como arquivo |
| 💻 **EU implemento** | Já resolvo em código a partir dos valores/arquivos — você não precisa produzir arte |
| ⚙️ **Decisão** | Escolha de produto que preciso que você tome antes de eu implementar |

| Prioridade | Significado |
|---|---|
| **P0** | Mínimo indispensável para publicar o app com a nova cara |
| **P1** | Refinamento importante — entra logo depois do P0 |
| **P2** | Encanto / diferencial — pode vir em uma segunda leva |

**Regra de ouro dos formatos:**
- **Ícones e arte de linha simples (1–2 cores, sem textura)** → entregar em **SVG**. Eu converto para *Vector Drawable* (`.xml`) — escala infinita, um único arquivo serve todas as telas, e recolore por tema.
- **Ilustrações complexas (várias cores, gradientes, textura, cenário)** → entregar em **PNG** (ou WebP) em alta resolução. Ver §12 para densidades e nomenclatura.

---

## 2. Estado atual vs. alvo (resumo do gap)

| Dimensão | Hoje no app | Alvo da referência | Quem resolve |
|---|---|---|---|
| **Paleta** | "Forest Dark" (GreenMoss `#1B4332`, GreenSage `#40916C`, Âmbar `#D4A017`) | Quase idêntica; falta o **verde claro `#A7C957`** e o âmbar muda p/ `#E9B43C` | 💻 EU |
| **Tipografia** | Fonte do sistema (Roboto) | **Plus Jakarta Sans** (Regular/Medium/SemiBold/Bold) | 💻 EU (busco a fonte) |
| **Ícone do app** | PNG antigo (`ic_launcher_logo`) | **Pipa** em ícone adaptativo + versão monocromática | 🎨 VOCÊ |
| **Ícones de interface** | Material Icons genéricos | **Conjunto de ícones de linha** próprio | 🎨 VOCÊ (ou fallback Material) |
| **Ilustrações** | Nenhuma — usa **emoji** como identidade | Mascote, cenários, capas de destino, botânicos | 🎨 VOCÊ |
| **Clima** | Emoji (☀️ ⛅ 🌧️) | Ícones de clima ilustrados | ⚙️ Decisão + 🎨 VOCÊ |
| **Estados vazios** | Emoji grande | Spot illustrations | 🎨 VOCÊ |
| **Splash** | Ícone estático | Splash com a pipa | 🎨 VOCÊ (usa o mesmo asset do ícone) |
| **Modo escuro** | Não existe | — (a definir) | ⚙️ Decisão |

---

## 3. 💻 O que EU já faço em código (você NÃO precisa produzir)

Para você não gastar esforço à toa, isto **já sai de código** a partir dos valores da referência — não precisa de asset:

- **Tokens de cor** (toda a paleta, incluindo o novo verde claro e o âmbar ajustado).
- **Tipografia** — o wiring do tema para Plus Jakarta Sans (a fonte em si, ver §5).
- **Componentes de UI**: botões (primário/secundário/ação), chips/badges ("Em breve", "Concluída", "Em X dias"), campos de busca e dropdowns, cards, bottom nav, top bars. Tudo isso é desenho em Compose.
- **Formas, sombras, espaçamentos, cantos arredondados.**
- **Motivos geométricos simples** (ex.: o "sublinhado" âmbar dos títulos, a linha pontilhada do aviãozinho) — consigo fazer como vetor em código se forem simples; se você quiser a versão ilustrada caprichada, aí vira asset (ver §9).

> Ou seja: **cor, tipografia e componentes não são sua tarefa.** Sua tarefa é a **arte** — ícone do app, ícones de linha, mascote e ilustrações.

---

## 4. ⚙️ Decisões de produto que preciso de você

Antes de implementar, preciso que você decida (pode responder junto ao aprovar este brief):

1. **Capas de viagem/dia** — o modelo de ilustração (ver §8). Recomendo **biblioteca de arquétipos** (praia, montanha, cidade, etc.) em vez de arte sob medida por viagem. **Qual caminho?**
2. **Ícones de clima** — manter emoji (custo zero) ou produzir conjunto ilustrado próprio? Recomendo **manter emoji no P0** e produzir set custom como P2.
3. **Modo escuro** — entra agora, depois, ou não entra? (Se entrar, dobra o trabalho de definir cores, mas ilustrações podem ser as mesmas.)
4. **Nome do pacote / identidade de loja** — o `applicationId` hoje é `com.rodrigoleao.gramado2026` (nome herdado do projeto original). Para publicar como "Rumo", o ideal é renomear para algo como `com.rodrigoleao.rumo`. **É código (eu faço)**, mas afeta a identidade da loja e é irreversível depois de publicado — precisa da sua decisão.

---

## 5. Tipografia — Plus Jakarta Sans

**💻 EU implemento / 🎨 arquivos triviais.** A fonte é **open source (SIL Open Font License)** e está no Google Fonts — pode ser embarcada no app sem custo nem restrição.

**Preciso de 4 arquivos `.ttf`** (ou posso baixá-los eu mesmo do Google Fonts, se você preferir):

| Peso | Arquivo esperado | Uso no app |
|---|---|---|
| Regular (400) | `PlusJakartaSans-Regular.ttf` | Corpo de texto |
| Medium (500) | `PlusJakartaSans-Medium.ttf` | Labels, botões |
| SemiBold (600) | `PlusJakartaSans-SemiBold.ttf` | Títulos de card, subtítulos |
| Bold (700) | `PlusJakartaSans-Bold.ttf` | Títulos de tela ("Minhas viagens", "Férias verão") |

- **Destino no projeto:** `app/src/main/res/font/`
- **Prioridade: P0** (a fonte é o que mais muda a "cara" percebida, junto com o ícone).

> Se você não fornecer, **eu baixo os 4 arquivos do Google Fonts e embarco** — só confirme que pode.

---

## 6. Paleta de cores (referência — 💻 EU implemento)

Você **não precisa produzir nada** aqui; é só a confirmação dos valores que vou codar como tokens:

| Nome | Hex | Papel |
|---|---|---|
| Verde Musgo | `#1B4332` | Primária — top bars, títulos, badges de data |
| Verde Sálvia | `#40916C` | Secundária — datas, acentos de suporte |
| Verde Claro | `#A7C957` | **NOVO** — detalhes, ícones, realces frescos |
| Areia | `#F4EDE1` | Fundo quente alternativo |
| Âmbar | `#E9B43C` | Acento/ação (hoje é `#D4A017`, vou ajustar) |
| Creme | `#FAF7F2` | Fundo geral |
| Cinza Claro | `#E7EDE8` | Superfícies suaves |
| Cinza Médio | `#A8B5A9` | Texto auxiliar, ícones inativos |
| Cinza Escuro | `#2E3D34` | Texto forte alternativo |

⚙️ **Confirme se estes são os hexes finais** (peguei do board de identidade).

---

## 7. Ícone do aplicativo (launcher) — 🎨 VOCÊ · **P0**

Este é o item de maior impacto para a loja. O Android usa **ícone adaptativo** (camadas separadas de frente e fundo) + uma camada **monocromática** (ícones "temáticos" do Android 13+).

**Preciso de 3 entregáveis:**

| # | Asset | Formato | Especificação |
|---|---|---|---|
| 7.1 | **Foreground (a pipa)** | SVG **ou** PNG | Arte da pipa isolada, **sem fundo** (transparente). Canvas **1024×1024 px**; a arte deve caber na **zona segura central de ~66%** (≈672 px) — o Android corta as bordas em máscaras diferentes (círculo, squircle, etc.) |
| 7.2 | **Background** | SVG/PNG ou só a cor | Camada de fundo **1024×1024**. Pode ser o verde musgo sólido `#1B4332` com as colininhas/rio ao fundo (como no board), **ou** só a cor sólida — sua escolha estética |
| 7.3 | **Monocromático** | SVG (preferível) | A silhueta da pipa em **uma cor só** (preto sobre transparente), mesma zona segura. O sistema recolore automaticamente |
| 7.4 | **Ícone da Play Store** | PNG **512×512**, 32-bit | Versão "cheia" do ícone (com fundo), sem transparência, cantos **quadrados** (a loja aplica o arredondamento) |

- ⚙️ **Escolha qual das 4 variações do board** vira o ícone oficial (recomendo a **verde-escura com a pipa clara** — maior contraste na gaveta de apps).
- **Destino:** eu monto o `ic_launcher` adaptativo a partir das camadas; você só entrega a arte.
- Se entregar só **um PNG 1024×1024 da pipa em transparência**, eu já consigo montar 90% disso — o monocromático é o único que idealmente vem separado.

---

## 8. Ilustrações de capa de viagem e de dia — 🎨 VOCÊ · **P0/P1**

**O maior item de arte.** Na referência, cada viagem e cada dia têm uma ilustração temática (montanha, castelo, praia com guarda-sol, palmeira+prancha, igreja histórica, orla, posto de salva-vidas...).

### ⚙️ Decisão de modelo (escolha uma):

- **A) Biblioteca de arquétipos (RECOMENDADO).** Um conjunto fixo de ~10–16 ilustrações por "tipo de lugar". O usuário escolhe (ou o app infere pelo destino) qual arquétipo representa a viagem/dia. **Escalável, bounded, reutilizável** — funciona para qualquer viagem que o usuário criar.
- **B) Arte sob medida por viagem.** Linda, mas **não escala** para um app publicado onde o usuário cria viagens arbitrárias. Serve no máximo para as viagens-semente de demonstração.
- **C) Foto do usuário como capa.** Pragmático, mas quebra o visual ilustrado.

**Se A**, preciso deste conjunto base (**P0 = os 8 primeiros; P1 = o resto**):

| # | Arquétipo | Exemplo de uso |
|---|---|---|
| 1 | Praia tropical (guarda-sol/palmeira) | "Férias verão", "FDS na praia" |
| 2 | Montanha / serra | "Gramado & Canela" |
| 3 | Cidade histórica / arquitetura | "Visita à Letícia" (castelo) |
| 4 | Orla / costa | dias de litoral |
| 5 | Floresta / natureza | trilhas, parques |
| 6 | Campo / interior | viagens rurais |
| 7 | Lago / rio | destinos de água doce |
| 8 | Urbano moderno (skyline) | cidade grande |
| 9 | Deserto / dunas | P1 |
| 10 | Neve / inverno | P1 |
| 11 | Ilha / arquipélago | P1 |
| 12 | Genérico "mala/viagem" (fallback) | quando nada casar |

**Especificação de cada ilustração de capa:**
- **Formato:** PNG ou WebP (têm textura/gradiente → não são vetor).
- **Proporção:** entregar **2 recortes** por arquétipo, OU um master que eu recorto:
  - **Card quadrado** (miniatura no card da lista / badge do dia): master **512×512 px**.
  - **Header largo** (topo da tela de detalhe, atrás do título): master **1080×720 px** (proporção ~3:2), com a **parte superior "respirável"** (céu/espaço) para o título e ícones ficarem legíveis por cima.
- **Paleta:** dentro da identidade (verdes, areia, âmbar, azuis suaves de céu/mar). Evitar cores fora do sistema.
- **Estilo:** flat/ilustrado com sombreamento suave, coerente com os mockups.
- **Nomenclatura:** `il_cover_beach.png`, `il_cover_mountain.png`, `il_header_beach.webp`, etc. (ver §12).

> Se você for gerar por IA, dá pra pedir o conjunto todo com um prompt de estilo fixo para manter consistência. Posso te ajudar a montar os prompts depois que você escolher o modelo.

---

## 9. Mascote, logo e cenário de marca — 🎨 VOCÊ · **P0**

A **pipa** e o **cenário de colinas** são a assinatura da marca. Preciso deles como assets reutilizáveis (splash, cabeçalhos, estados vazios, "sobre").

| # | Asset | Formato | Especificação / uso |
|---|---|---|---|
| 9.1 | **Pipa (mascote) isolada** | SVG + PNG 1024² | A pipa com o rabicho amarelo, sem fundo. Reutilizada em vários lugares. **P0** |
| 9.2 | **Lockup vertical** (pipa + "Rumo" + "seu companheiro de viagem") | SVG/PNG transparente | Splash, tela "sobre". Como no canto superior esquerdo do board. **P0** |
| 9.3 | **Lockup horizontal** (pipa à esquerda + "Rumo") | SVG/PNG | Cabeçalhos, cartão de compartilhamento. **P1** |
| 9.4 | **Wordmark "Rumo" isolado** | SVG | Onde só cabe o texto. **P1** |
| 9.5 | **Cenário/hero de colinas** (as ondas de morros verdes + sol + pássaros) | PNG/WebP largo | Fundo decorativo do topo da Home e dos cabeçalhos. Master **1080×900 px**, transparência opcional. **P0** |
| 9.6 | **Kit botânico** (ramos de folhas para cantos, folhagem, sol, nuvens, pássaros) | SVG **ou** PNGs transparentes avulsos | Elementos soltos que eu posiciono nos cantos das telas (ver rodapé da Home e cabeçalhos). ~6–8 peças. **P1** |
| 9.7 | **Motivo do caminho/aviãozinho pontilhado** | SVG | A linha pontilhada com aviãozinho (card "Em curso"). Simples → posso fazer em código, mas se quiser a versão ilustrada, entregue em SVG. **P2** |
| 9.8 | **Pin de mapa estilizado** (âmbar, como no rodapé da Home) | SVG | **P2** |

- **Versão monocromática/clara da pipa** (para usar sobre fundo verde escuro) — pode ser a mesma SVG recolorida por código; não precisa entregar separado.

---

## 10. Ícones de interface (iconografia de linha) — 🎨 VOCÊ · **P1** (com fallback P0)

A referência tem um **conjunto de ícones de linha** próprio (seção "ICONOGRAFIA"). Hoje o app usa Material Icons.

⚙️ **Decisão:** ou (a) você produz o conjunto custom para consistência total de marca, ou (b) **usamos Material Symbols (estilo Rounded)** como fallback — fica coerente e é **custo zero** para você. **Recomendo Material como P0** e o conjunto custom como P1, para não travar a publicação.

**Se você for produzir**, preciso de cada ícone em **SVG, grid de 24×24 dp, traço ~2 px, cantos arredondados**, monocromático (recoloro por código). Conjunto necessário:

**Navegação principal / seções (do board):**

| Ícone | Uso | Nome sugerido |
|---|---|---|
| Viagens | aba/lista de viagens | `ic_trips` |
| Itinerário | roteiro do dia | `ic_itinerary` |
| Documentos | vouchers/documentos | `ic_documents` |
| Hospedagens | acomodação | `ic_lodging` |
| Check-in | cartões de embarque | `ic_checkin` |
| Transportes | transporte | `ic_transport` |
| Atividades | atividades do dia | `ic_activities` |
| Mapas | mapa | `ic_map` |
| Contatos | contatos | `ic_contacts` |
| Mais | overflow | `ic_more` |
| Início | home (nav) | `ic_home` |
| Notas | notas (nav) | `ic_notes` |

**Ações e utilitários (usados nas telas atuais):**

`ic_back` (voltar), `ic_edit` (editar), `ic_share` (compartilhar), `ic_add` (novo +), `ic_delete` (excluir), `ic_search` (buscar), `ic_settings` (engrenagem), `ic_sort` (ordenar), `ic_close` (fechar), `ic_chevron` (dropdown/avançar), `ic_drag` (handle de reordenar), `ic_send` (enviar/IA), `ic_help` (ajuda).

**Meta/informação (aparecem nos cards):**

`ic_calendar` (datas), `ic_pin` (localização), `ic_clock` (horário), `ic_thermometer` (temperatura), `ic_droplet` (chuva/precipitação), `ic_leaf` (contador de atividades), `ic_check` (concluído), `ic_phone` (ligar), `ic_car` (Uber/transporte).

> Total: ~34 ícones. Se produzir, entregue todos com o **mesmo peso de traço e cantos** — a consistência do traço é o que faz o conjunto parecer profissional. Um único arquivo SVG por ícone.

---

## 11. Ícones de clima — ⚙️ Decisão + 🎨 VOCÊ · **P2**

Hoje o clima vem da API (Open-Meteo) e é exibido como **emoji** derivado do código WMO. A referência mostra ícones **ilustrados** (sol + nuvem + gotas).

⚙️ **Decisão:** manter emoji (custo zero, já funciona) ou produzir set próprio?

**Se produzir**, preciso de ~9 ícones cobrindo os grupos de código WMO (SVG, 48×48 dp, podem ser multicoloridos no estilo da referência):

| Grupo | Condição | Nome sugerido |
|---|---|---|
| 0 | Céu limpo (sol) | `ic_weather_clear` |
| 1–2 | Parcialmente nublado (sol+nuvem) | `ic_weather_partly` |
| 3 | Encoberto (nuvem) | `ic_weather_cloudy` |
| 45–48 | Névoa/neblina | `ic_weather_fog` |
| 51–57 | Garoa | `ic_weather_drizzle` |
| 61–67 | Chuva | `ic_weather_rain` |
| 71–77 | Neve | `ic_weather_snow` |
| 80–82 | Pancadas | `ic_weather_showers` |
| 95–99 | Trovoada | `ic_weather_thunder` |

**Recomendação:** manter emoji no P0/P1 e tratar como P2.

---

## 12. Ilustrações de estado vazio — 🎨 VOCÊ · **P1**

Hoje os estados vazios usam emoji grande. Spot illustrations dão acabamento profissional. Cada uma: **PNG/WebP transparente, master ~600×600 px**.

| # | Estado | Onde | Nome |
|---|---|---|---|
| 12.1 | Nenhuma viagem ainda | Lista de viagens vazia | `il_empty_trips` |
| 12.2 | Sem notas | Tela de notas vazia | `il_empty_notes` |
| 12.3 | Sem contatos | Contatos vazio | `il_empty_contacts` |
| 12.4 | Sem documentos/vouchers | Vouchers vazio | `il_empty_documents` |
| 12.5 | Busca sem resultado | Filtro sem match | `il_empty_search` |
| 12.6 | Erro ao importar `.travel` | Falha de importação | `il_error_import` |

> Podem reaproveitar o mascote-pipa em situações diferentes (pipa presa, pipa procurando, etc.) — dá unidade à marca.

---

## 13. Splash screen — 🎨 (reaproveita) · **P0**

O Android 12+ usa a Splash Screen API. **Não precisa de asset novo** se o ícone da pipa (§7/§9.1) estiver bom:

- **Ícone de splash:** a pipa (§9.1), idealmente como **vetor** para nitidez. Zona segura de ~2/3 do círculo.
- **Cor de fundo do splash:** `#1B4332` (verde musgo) ou `#FAF7F2` (creme) — ⚙️ sua escolha.
- **Branding (opcional):** o lockup (§9.2) na base da splash.

---

## 14. Assets para a Play Store (publicação) — parte 🎨 VOCÊ, parte 💻 EU · **P0**

Para publicar, a loja exige:

| # | Asset | Formato | Quem |
|---|---|---|---|
| 14.1 | Ícone da loja | PNG 512×512 (§7.4) | 🎨 VOCÊ |
| 14.2 | **Feature graphic** (banner do topo da ficha) | PNG/JPG **1024×500** | 🎨 VOCÊ — pode ser o cenário de colinas + pipa + "Rumo · seu companheiro de viagem" |
| 14.3 | **Screenshots de telefone** (mín. 2, ideal 4–8) | PNG, proporção do device | 💻 EU gero a partir do app rodando no emulador (depois da repaginação) |
| 14.4 | Screenshots de tablet (opcional) | PNG | 💻 EU (se quiser suporte a tablet) |
| 14.5 | Texto: título, descrição curta e longa | Texto | ⚙️ VOCÊ decide o conteúdo; eu ajudo a redigir |
| 14.6 | Política de privacidade (URL) | Página web | ⚙️ VOCÊ — obrigatória pela Play Store |

> Os **screenshots (14.3) eu produzo** capturando o app já repaginado — não é tarefa de arte sua.

---

## 15. Animações (encanto) — 🎨 VOCÊ (opcional) · **P2**

Totalmente opcional, mas eleva muito a percepção:

- **Lottie da pipa** (`.json`) voando no splash ou nos estados vazios — animação leve e nítida.
- Microinterações (check animado, pull-to-refresh temático). A maioria eu faço em código; só a pipa animada exigiria um `.json` do Lottie ou After Effects.

---

## 16. Formatos, densidades e nomenclatura (referência técnica)

### Quando é vetor vs. raster
- **SVG → Vector Drawable:** ícones (§7 mono, §10, §11 se simples), mascote de linha, wordmark, motivos. **Prefira sempre que a arte for 1–2 cores sem textura.** Um arquivo serve todas as telas.
- **PNG/WebP:** ilustrações com gradiente/textura/cenário (§8 capas, §9.5 hero, §12 estados vazios). **WebP** é preferível (mesma qualidade, arquivo menor) — mas PNG está ótimo, eu converto.

### Densidades (para assets raster)
Se você entregar **um master em alta resolução**, **eu gero as densidades** — não precisa exportar 5 vezes. Entregue no maior tamanho listado em cada item (equivale a xxxhdpi/4×). Para referência:

| Densidade | Multiplicador | Ex.: ícone base 24dp |
|---|---|---|
| mdpi | 1× | 24 px |
| hdpi | 1.5× | 36 px |
| xhdpi | 2× | 48 px |
| xxhdpi | 3× | 72 px |
| xxxhdpi | 4× | 96 px |

> Ilustrações grandes que preenchem a largura da tela vão em `drawable-nodpi` (um único arquivo escalado pelo container) — para essas, um master largo (ex.: 1080 px) basta.

### Nomenclatura (regra de recurso Android — obrigatória)
- **Só minúsculas, números e `_`.** Nada de espaços, hífens, acentos ou maiúsculas.
- Prefixos:
  - `ic_` → ícones (`ic_trips`, `ic_weather_rain`)
  - `il_` → ilustrações (`il_cover_beach`, `il_empty_trips`)
  - `bg_` → fundos/cenários (`bg_hills_header`)
- Sufixos de variação quando houver: `il_cover_beach_card` / `il_cover_beach_header`.

### Como entregar
Sugiro uma pasta única, ex.: `OneDrive/Rumo/Design/` com subpastas `icone-app/`, `ilustracoes/`, `icones-ui/`, `mascote/`, `fontes/`, `loja/`. Quando estiver pronta, me aponta o caminho que eu importo e organizo dentro do projeto (`app/src/main/res/...`).

---

## 17. Resumo priorizado — checklist de produção

### 🟥 P0 — mínimo para publicar com a nova identidade
- [ ] **Fonte** Plus Jakarta Sans (4 `.ttf`) — *ou autorizar que eu baixe*
- [ ] **Ícone do app**: pipa foreground (PNG/SVG 1024²) + monocromático (SVG) + loja 512² (§7)
- [ ] **Mascote-pipa** isolada + **lockup vertical** (§9.1, §9.2)
- [ ] **Cenário de colinas** (hero) para Home e cabeçalhos (§9.5)
- [ ] **Capas de destino** — 8 arquétipos base (§8, modelo A)
- [ ] **Feature graphic** da loja 1024×500 (§14.2)
- [ ] ⚙️ Decisões §4 (capas, clima, dark mode, nome do pacote) + confirmar hexes §6
- [ ] *(EU) tokens de cor, tema tipográfico, componentes, screenshots da loja*

### 🟨 P1 — refinamento
- [ ] Conjunto completo de **ícones de linha** (§10) — se optar por custom
- [ ] **Kit botânico** e lockup horizontal (§9.6, §9.3)
- [ ] Capas de destino restantes (arquétipos 9–12, §8)
- [ ] **Estados vazios** ilustrados (§12)

### 🟩 P2 — encanto
- [ ] **Ícones de clima** ilustrados (§11)
- [ ] **Lottie** da pipa animada (§15)
- [ ] Motivos ilustrados (caminho/aviãozinho, pin de mapa — §9.7, §9.8)

---

## 18. Próximos passos (fluxo sugerido)

1. Você **revisa este brief** e toma as decisões da §4 (+ confirma hexes da §6).
2. Enquanto você produz os assets P0, **eu adianto o que é código**: tokens de cor novos, wiring da tipografia (busco a fonte), e refino de componentes conforme a referência — sem depender de arte.
3. Você me entrega os assets P0 numa pasta; eu importo, monto o ícone adaptativo, o splash e ligo as ilustrações às telas.
4. Repaginação P0 pronta → **eu capturo os screenshots** no emulador e montamos a ficha da Play Store.
5. Seguimos com P1 e P2 em levas.

> Assim que você aprovar o modelo de capas (§4.1) e o estilo, posso te montar os **prompts de geração de imagem** para produzir a biblioteca de ilustrações de forma consistente, se for esse o caminho.
