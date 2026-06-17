package com.rodrigoleao.gramado2026.data.repository

import com.rodrigoleao.gramado2026.data.model.*
import java.time.LocalDate

object RoteiroRepository {

    // ── DIAS ─────────────────────────────────────────────────────────────

    val days: List<TravelDay> = listOf(

        // ═══ DIA 1 — 9 Jun (Terça) ═══════════════════════════════════════
        TravelDay(
            id = 1,
            date = LocalDate.of(2026, 6, 9),
            dayOfWeek = "Terça-feira",
            title = "Chegada — Lago Negro & Centro",
            weather = WeatherInfo(9, 15, "Manhã chuvosa (81% · 4mm), tarde nublada", "🌦"),
            vouchers = listOf(
                DayVoucher("📱", "Transfer IN — Brocker"),
                DayVoucher("🪪", "Documento com foto (RG ou CNH)")
            ),
            activities = listOf(
                TravelActivity(
                    time = "12h",
                    emoji = "✈️",
                    name = "Chegada em Gramado",
                    detail = "Transfer contratado do aeroporto até o Hotel San Lucas. Desfazer as malas e descansar.",
                    badges = listOf(Badge(BadgeType.BOOKED, "transfer"))
                ),
                TravelActivity(
                    time = "13h30",
                    emoji = "🍽️",
                    name = "Almoço — Sabores & Temperos",
                    detail = "Buffet caseiro a ~500m do hotel, dá para ir a pé! ⭐ 5.0. Funciona seg–sáb 11h30–14h. ⚠️ Fecha às 14h — chegar até 13h. Alternativa: Coelho Café Colonial ⭐ 4.5 (Av. das Hortênsias, 5433).",
                    badges = listOf(Badge(BadgeType.WALKING, "a pé")),
                    mapQuery = "Sabores e Temperos Av das Hortensias 5809 Gramado RS"
                ),
                TravelActivity(
                    time = "15h",
                    emoji = "🌊",
                    name = "Lago Negro",
                    detail = "Uber do restaurante até o Lago Negro (~R\$15). Tarde inteira no lago — caminhada ao redor, fotos, pedalinhos opcionais.",
                    badges = listOf(Badge(BadgeType.FREE, "grátis"), Badge(BadgeType.UBER, "uber")),
                    mapQuery = "Lago Negro Gramado RS",
                    uberDestination = "Lago Negro, Gramado, RS"
                ),
                TravelActivity(
                    time = "19h",
                    emoji = "🚶",
                    name = "Caminhada turística até a padaria",
                    detail = "~2 km, uns 25–30 min passando pelos principais pontos do centro.",
                    badges = listOf(Badge(BadgeType.WALKING, "a pé")),
                    walkStops = listOf(
                        WalkStop("🌊", "Lago Negro", "ponto de partida"),
                        WalkStop("🌿", "Praça das Etnias", "jardins, casas típicas italianas e portuguesas"),
                        WalkStop("↩️", "Rua Torta", "a famosa rua curva de Gramado, ótima para fotos"),
                        WalkStop("⛪", "Paróquia São Pedro", "bela igreja no coração da cidade"),
                        WalkStop("🌸", "Praça Major Nicoletti", "praça central com fontes e flores"),
                        WalkStop("🥐", "Copetín de Buenos Aires", "destino final — medialunas e empanadas", isLast = true)
                    )
                ),
                TravelActivity(
                    time = "19h20",
                    emoji = "🛍️",
                    name = "Rua Coberta — passeio noturno",
                    detail = "Passeio pela Rua Coberta e Praça Nicoletti — lojas, chocolates, clima noturno de Gramado.",
                    badges = listOf(Badge(BadgeType.FREE, "grátis")),
                    mapQuery = "Rua Coberta Gramado RS"
                ),
                TravelActivity(
                    time = "19h30",
                    emoji = "🥐",
                    name = "Copetín de Buenos Aires",
                    detail = "Padaria argentina ⭐ 5.0 — medialunas e empanadas imperdíveis. Abre a partir das 14h (fecha dom e seg).",
                    badges = listOf(Badge(BadgeType.PAID, "pago")),
                    mapQuery = "Copetin de Buenos Aires Gramado RS"
                ),
                TravelActivity(
                    time = "21h",
                    emoji = "🍝",
                    name = "Jantar — Ristorante Tomasini",
                    detail = "Italiano clássico de Gramado, ⭐ 4.6. A poucos metros da Rua Coberta. Abre a partir das 18h (seg–sex) — recomenda-se reserva.",
                    badges = listOf(Badge(BadgeType.WALKING, "a pé")),
                    mapQuery = "Ristorante Tomasini Gramado RS"
                ),
                TravelActivity(
                    time = "22h30",
                    emoji = "🏨",
                    name = "Retorno ao hotel",
                    detail = "Uber de volta ao Hotel San Lucas.",
                    badges = listOf(Badge(BadgeType.UBER, "uber"))
                )
            )
        ),

        // ═══ DIA 2 — 10 Jun (Quarta) ══════════════════════════════════════
        TravelDay(
            id = 2,
            date = LocalDate.of(2026, 6, 10),
            dayOfWeek = "Quarta-feira",
            title = "Gramado — Lavanda, Cristais & Atrações",
            weather = WeatherInfo(8, 14, "Nublado o dia todo, sem chuva prevista", "⛅"),
            vouchers = listOf(
                DayVoucher("📱", "Mini Mundo — pedido 067454"),
                DayVoucher("📱", "Vouchers vinho quente (2x)")
            ),
            activities = listOf(
                TravelActivity(
                    time = "8h30",
                    emoji = "💎",
                    name = "Cristais de Gramado",
                    detail = "⭐ 4.6 · RS-115, km 36. Fábrica de cristais estilo Murano, entrada gratuita. Demonstrações a cada 15 min. ~45 min. Abre às 8h30.",
                    badges = listOf(Badge(BadgeType.FREE, "grátis"), Badge(BadgeType.UBER, "uber")),
                    mapQuery = "Cristais de Gramado RS",
                    uberDestination = "Cristais de Gramado, RS-115, Gramado, RS"
                ),
                TravelActivity(
                    time = "9h15",
                    emoji = "🏰",
                    name = "Pórtico Normando de Gramado",
                    detail = "⭐ 4.9 · Logo ao lado do Cristais, a pé. O famoso portão de entrada de Gramado, icônico para foto de casal. Visita rápida.",
                    badges = listOf(Badge(BadgeType.FREE, "grátis"), Badge(BadgeType.WALKING, "a pé")),
                    mapQuery = "Portico Normando Gramado RS"
                ),
                TravelActivity(
                    time = "9h30",
                    emoji = "🌸",
                    name = "Le Jardin — Parque de Lavanda",
                    detail = "⭐ 4.7 · RS-115, km 37. Parque de lavanda com vista para a serra. Entrada gratuita, cafezinho e lojinha. ~1h. Abre às 9h30.",
                    badges = listOf(Badge(BadgeType.FREE, "grátis"), Badge(BadgeType.UBER, "uber")),
                    mapQuery = "Le Jardin Lavanda Park Gramado RS",
                    uberDestination = "Le Jardin Lavanda Park, RS-115, Gramado, RS"
                ),
                TravelActivity(
                    time = "11h",
                    emoji = "🏘️",
                    name = "Bairro Bavária",
                    detail = "Uber da Le Jardin direto ao Bairro Bavária (~10 min). Arquitetura típica alemã, casas enxaimel e decoração muito fotogênica. Passeio a pé pelo bairro.",
                    badges = listOf(Badge(BadgeType.FREE, "grátis"), Badge(BadgeType.UBER, "uber")),
                    mapQuery = "Bairro Bavaria Gramado RS",
                    uberDestination = "Bairro Bavária, Gramado, RS"
                ),
                TravelActivity(
                    time = "11h30",
                    emoji = "🏘️",
                    name = "Mini Mundo",
                    detail = "⭐ 4.8 · Av. das Hortênsias, 4747 — a poucos minutos a pé do Bairro Bavária. Maquetes detalhadas de monumentos mundiais em escala. ~1h30 de visita. 🎁 Incluso: 2 vouchers de vinho quente 180ml — retirar no Café do Mini Mundo.",
                    badges = listOf(Badge(BadgeType.BOOKED, "comprado"), Badge(BadgeType.WALKING, "a pé")),
                    mapQuery = "Mini Mundo Gramado RS"
                ),
                TravelActivity(
                    time = "13h",
                    emoji = "🍽️",
                    name = "Almoço — Forno dos Colonos",
                    detail = "Culinária colonial italiana típica da Serra Gaúcha. Uber do Mini Mundo ao restaurante (~5 min).",
                    badges = listOf(Badge(BadgeType.PAID, "pago"), Badge(BadgeType.UBER, "uber")),
                    mapQuery = "Forno dos Colonos Gramado RS",
                    uberDestination = "Forno dos Colonos, Gramado, RS"
                ),
                TravelActivity(
                    time = "14h30",
                    emoji = "🌿",
                    name = "Praça das Etnias",
                    detail = "Logo ao lado do Forno dos Colonos. Casas que representam as três etnias de Gramado: italiana, alemã e portuguesa.",
                    badges = listOf(Badge(BadgeType.FREE, "grátis"), Badge(BadgeType.WALKING, "a pé")),
                    mapQuery = "Praca das Etnias Gramado RS"
                ),
                TravelActivity(
                    time = "15h",
                    emoji = "↩️",
                    name = "Rua Torta",
                    detail = "⭐ 4.6 · ~5 min a pé da Praça das Etnias. A famosa rua curva de Gramado — uma das mais fotografadas da cidade.",
                    badges = listOf(Badge(BadgeType.FREE, "grátis"), Badge(BadgeType.WALKING, "a pé")),
                    mapQuery = "Rua Torta Gramado RS"
                ),
                TravelActivity(
                    time = "15h30",
                    emoji = "🌄",
                    name = "Belvedere Vale do Quilombo",
                    detail = "⭐ 4.6 · Av. Borges de Medeiros, 2536. Mirante com vista para o vale. Gratuito, lindo no frio de junho com névoa.",
                    badges = listOf(Badge(BadgeType.FREE, "grátis"), Badge(BadgeType.WALKING, "a pé")),
                    mapQuery = "Belvedere Vale do Quilombo Gramado RS"
                ),
                TravelActivity(
                    time = "16h",
                    emoji = "🏨",
                    name = "Retorno ao hotel — descanso",
                    detail = "Uber do Belvedere ao Hotel San Lucas. ~2h de descanso antes de sair para a noite.",
                    badges = listOf(Badge(BadgeType.UBER, "uber"))
                ),
                TravelActivity(
                    time = "18h30",
                    emoji = "🧀",
                    name = "Empório — queijos, embutidos & vinho",
                    detail = "Uber até o centro. Parada na Casa do Queijo Gramado ou empório colonial — queijos coloniais, embutidos da Serra e um vinho gaúcho. ~R\$80–100 para dois.",
                    badges = listOf(Badge(BadgeType.PAID, "pago"), Badge(BadgeType.UBER, "uber")),
                    mapQuery = "Casa do Queijo Gramado RS",
                    uberDestination = "Casa do Queijo Gramado, Gramado, RS"
                ),
                TravelActivity(
                    time = "19h",
                    emoji = "🌃",
                    name = "Passeio noturno — centro de Gramado",
                    detail = "Gramado à noite no inverno tem um clima especial. Passeio pela Av. Borges de Medeiros, Rua Coberta e Praça Major Nicoletti — lojas iluminadas e charme europeu.",
                    badges = listOf(Badge(BadgeType.FREE, "grátis"), Badge(BadgeType.WALKING, "a pé")),
                    mapQuery = "Rua Coberta Gramado RS"
                ),
                TravelActivity(
                    time = "20h",
                    emoji = "☕",
                    name = "Chocolate quente — Florybal ou Caracol",
                    detail = "Encerrar o passeio com um chocolate quente artesanal — imperdível no frio de junho. Florybal (Av. das Hortênsias, 5386) ou Caracol Chocolates no centro. ~R\$25–30 para dois.",
                    badges = listOf(Badge(BadgeType.PAID, "pago"), Badge(BadgeType.WALKING, "a pé")),
                    mapQuery = "Florybal Thematic Shop Gramado RS"
                ),
                TravelActivity(
                    time = "20h45",
                    emoji = "🍷",
                    name = "Tábua no hotel — fim de noite",
                    detail = "Uber de volta ao hotel. Montar a tábua de frios com o que foi comprado no empório — queijos, embutidos e vinho gaúcho. Noite tranquila antes do dia cheio do Bustour.",
                    badges = listOf(Badge(BadgeType.UBER, "uber"))
                )
            )
        ),

        // ═══ DIA 3 — 11 Jun (Quinta) ══════════════════════════════════════
        TravelDay(
            id = 3,
            date = LocalDate.of(2026, 6, 11),
            dayOfWeek = "Quinta-feira",
            title = "Bus Tour — Canela & Noite Gaúcha",
            weather = WeatherInfo(5, 16, "Sol com nuvens, manhã bem fria — monitorar condições para os Bondinhos", "🌤"),
            dayAlert = "Bus Tour funciona qui–ter, 9h–17h30. Hop on/hop off. ⚠️ Fique atento ao SMS da Brocker com o horário do Tour Trem & Vinho de amanhã — chega hoje. Noite Gaúcha: bebidas e taxa de serviço não estão inclusas — levar dinheiro extra.",
            vouchers = listOf(
                DayVoucher("📱", "Bustour — TRL 4704707 / 4704708"),
                DayVoucher("📱", "Caracol — COW0793 / CDZ5147"),
                DayVoucher("📱", "Bondinhos — 958052 / 958054")
            ),
            activities = listOf(
                TravelActivity(
                    time = "9h40",
                    emoji = "🚌",
                    name = "Embarque — parada Exceed Games Park",
                    detail = "Parada 16 da Linha Vermelha, em frente ao Exceed Games Park (Av. das Hortênsias, 4510) — a poucos minutos do hotel a pé. Seguir direto para Canela.",
                    badges = listOf(Badge(BadgeType.BOOKED, "comprado"), Badge(BadgeType.WALKING, "a pé")),
                    mapQuery = "Exceed Games Park Gramado RS"
                ),
                TravelActivity(
                    time = "10h10",
                    emoji = "💧",
                    name = "Parque do Caracol",
                    detail = "Trocar para a Linha Amarela no centro de Canela. Cachoeira de 131m, trilhas e vista espetacular. ~1h de visita. ⚠️ Bilhete agendado para 11/06 com 0 dias de flexibilidade — obrigatório visitar exatamente nessa data. Guardar o bilhete até a saída do parque.",
                    badges = listOf(Badge(BadgeType.BOOKED, "linha amarela · parada 4"), Badge(BadgeType.BOOKED, "comprado")),
                    mapQuery = "Parque do Caracol Canela RS"
                ),
                TravelActivity(
                    time = "11h10",
                    emoji = "🚡",
                    name = "Bondinhos Aéreos",
                    detail = "Vista aérea do canyon do Caracol — único no sul do Brasil. ~45 min. ⚠️ Voucher agendado para 11/06 — não comparecimento invalida o ingresso. Sujeito a fechamento por condições climáticas.",
                    badges = listOf(Badge(BadgeType.BOOKED, "linha amarela · parada 5"), Badge(BadgeType.BOOKED, "comprado")),
                    mapQuery = "Bondinhos Aereos Canela RS"
                ),
                TravelActivity(
                    time = "12h",
                    emoji = "⛪",
                    name = "Catedral de Pedra — Centro de Canela",
                    detail = "Voltar ao centro de Canela pela Linha Amarela. Visita à Catedral neogótica — interior gratuito e imperdível.",
                    badges = listOf(Badge(BadgeType.BOOKED, "linha amarela · parada 1"), Badge(BadgeType.FREE, "grátis")),
                    mapQuery = "Catedral de Pedra Canela RS"
                ),
                TravelActivity(
                    time = "12h30",
                    emoji = "🥖",
                    name = "Almoço — Fornos de Canela",
                    detail = "⭐ 4.7 · A poucos metros da Catedral, na praça central. Pão com linguiça feito no forno de pedra. Opções: frango, carne e queijo com alho.",
                    badges = listOf(Badge(BadgeType.PAID, "pago")),
                    mapQuery = "Fornos de Canela RS"
                ),
                TravelActivity(
                    time = "13h30",
                    emoji = "🚶",
                    name = "Passeio livre — Centro de Canela",
                    detail = "Tempo livre na praça central de Canela — lojas de chocolate, cafés, artesanato. Clima mais tranquilo e interiorano que Gramado. Aproveitar sem pressa.",
                    badges = listOf(Badge(BadgeType.FREE, "grátis")),
                    mapQuery = "Praca da Matriz Canela RS"
                ),
                TravelActivity(
                    time = "15h",
                    emoji = "🚌",
                    name = "Bus de volta — descer na parada 16",
                    detail = "Embarcar no centro de Canela e seguir de volta à parada 16 (Exceed Games Park).",
                    badges = listOf(Badge(BadgeType.BOOKED, "linha amarela → vermelha · parada 16"))
                ),
                TravelActivity(
                    time = "15h30",
                    emoji = "🏨",
                    name = "Retorno ao hotel — descanso",
                    detail = "A poucos minutos a pé do Exceed Games Park até o Hotel San Lucas. Descanse bem — a noite ainda tem Noite Gaúcha.",
                    badges = listOf(Badge(BadgeType.WALKING, "a pé"))
                ),
                TravelActivity(
                    time = "~20h",
                    emoji = "🤠",
                    name = "Garfo e Bombacha — Noite Gaúcha",
                    detail = "Jantar + show de danças gaúchas na Churrascaria Garfo e Bombacha. Transfer incluso — busca no hotel. Horário exato informado por SMS no próprio dia até as 16h. Duração: ~3h. ⚠️ Bebidas e taxa de serviço não estão inclusas — levar dinheiro extra.",
                    badges = listOf(Badge(BadgeType.BOOKED, "comprado")),
                    mapQuery = "Churrascaria Garfo e Bombacha Canela RS"
                )
            )
        ),

        // ═══ DIA 4 — 12 Jun (Sexta) ════════════════════════════════════════
        TravelDay(
            id = 4,
            date = LocalDate.of(2026, 6, 12),
            dayOfWeek = "Sexta-feira",
            title = "Tour Trem & Vinho — Epopeia Italiana",
            weather = WeatherInfo(9, 11, "Chuvoso de manhã e à noite, aberturas de sol à tarde (11mm) — o dia mais frio da viagem", "🌧"),
            dayAlert = "O horário de saída será informado por SMS um dia antes. Transfer incluso — o carro da agência busca no Hotel San Lucas. Mínimo de 8 participantes para confirmação da saída.",
            vouchers = listOf(
                DayVoucher("📱", "Tour Trem & Vinho — Brocker (loc. 1891643-1369366)"),
                DayVoucher("🪪", "Documento com foto")
            ),
            activities = listOf(
                TravelActivity(
                    time = "manhã",
                    emoji = "🚌",
                    name = "Embarque no hotel",
                    detail = "Transfer da Brocker Turismo busca no hotel. Horário confirmado por SMS na véspera. Duração total: ~12 horas.",
                    badges = listOf(Badge(BadgeType.BOOKED, "comprado"))
                ),
                TravelActivity(
                    time = "manhã",
                    emoji = "🎭",
                    name = "Parque Cultural Epopeia Italiana",
                    detail = "Cenários imersivos que contam a trajetória dos primeiros imigrantes italianos no Brasil.",
                    badges = listOf(Badge(BadgeType.INCLUDED, "incluso")),
                    mapQuery = "Parque Cultural Epopeia Italiana Gramado RS"
                ),
                TravelActivity(
                    time = "manhã",
                    emoji = "🚂",
                    name = "Trem Maria Fumaça",
                    detail = "Passeio a bordo do trem histórico com show de músicas e danças italianas nos vagões. Degustação de vinhos, espumantes e sucos durante o trajeto.",
                    badges = listOf(Badge(BadgeType.INCLUDED, "incluso"))
                ),
                TravelActivity(
                    time = "almoço",
                    emoji = "🍽️",
                    name = "Almoço",
                    detail = "Almoço incluso no pacote durante o passeio em Bento Gonçalves e arredores.",
                    badges = listOf(Badge(BadgeType.INCLUDED, "incluso"))
                ),
                TravelActivity(
                    time = "tarde",
                    emoji = "🍷",
                    name = "Vinícola tradicional",
                    detail = "Visita com explicação sobre o processo de fabricação e degustação de vinhos e espumantes da Serra Gaúcha.",
                    badges = listOf(Badge(BadgeType.INCLUDED, "incluso"))
                ),
                TravelActivity(
                    time = "tarde",
                    emoji = "🧀",
                    name = "Fetina de Formaio + Tramontina + Malharia Gdom",
                    detail = "Degustação de queijos artesanais · Showroom Tramontina com descontos especiais · Peças em tricô direto da fábrica Gdom.",
                    badges = listOf(Badge(BadgeType.INCLUDED, "incluso"))
                ),
                TravelActivity(
                    time = "~20h",
                    emoji = "🏨",
                    name = "Retorno ao hotel",
                    detail = "Transfer de volta ao Hotel San Lucas. Ao chegar, verificar o SMS da Brocker com o horário do transfer de saída para o aeroporto (confirmado hoje para o Dia 5). Para o jantar: lanche leve ou delivery é a opção mais prática após um dia de 12h.",
                    badges = listOf(Badge(BadgeType.INCLUDED, "incluso"))
                )
            )
        ),

        // ═══ DIA 5 — 13 Jun (Sábado) ══════════════════════════════════════
        TravelDay(
            id = 5,
            date = LocalDate.of(2026, 6, 13),
            dayOfWeek = "Sábado",
            title = "Manhã no bairro — Partida",
            weather = WeatherInfo(7, 16, "Sol com névoa ao amanhecer, tarde mais aberta", "🌤"),
            dayAlert = "Tudo a pé do hotel! ~1,1 km total pela Av. das Hortênsias. Sem nenhum Uber até a partida.",
            vouchers = listOf(
                DayVoucher("📱", "Dreamland — TRL 4704707 / 4704708"),
                DayVoucher("📱", "Transfer OUT — aguardar SMS"),
                DayVoucher("🧳", "Malas prontas")
            ),
            activities = listOf(
                TravelActivity(
                    time = "9h",
                    emoji = "🎭",
                    name = "Dreamland — Museu de Cera",
                    detail = "⭐ 4.3 · Av. das Hortênsias, 5507 — ~500m do hotel. Estátuas de cera de celebridades mundiais, ótimo para fotos. ~1h30 de visita. Abre às 8h. Ingresso válido por 1 ano — apresentar no celular ou impresso na entrada.",
                    badges = listOf(Badge(BadgeType.BOOKED, "comprado"), Badge(BadgeType.WALKING, "a pé")),
                    mapQuery = "Dreamland Wax Museum Gramado RS"
                ),
                TravelActivity(
                    time = "10h30",
                    emoji = "🍫",
                    name = "Chocolataria Gramado",
                    detail = "⭐ 4.9 · Av. das Hortênsias, 5500 — ~150m do Dreamland. Tour pela fábrica e degustação. Boa pedida para comprar lembranças de última hora.",
                    badges = listOf(Badge(BadgeType.WALKING, "a pé")),
                    mapQuery = "Chocolataria Gramado Hortensias 5500"
                ),
                TravelActivity(
                    time = "11h15",
                    emoji = "🍽️",
                    name = "Almoço leve — Coelho Café Colonial",
                    detail = "⭐ 4.5 · Av. das Hortênsias, 5433 — ~300m da Chocolataria. Abre todos os dias às 11h. Opção tranquila e próxima antes do transfer.",
                    badges = listOf(Badge(BadgeType.PAID, "pago"), Badge(BadgeType.WALKING, "a pé")),
                    mapQuery = "Coelho Cafe Colonial Gramado RS"
                ),
                TravelActivity(
                    time = "12h30",
                    emoji = "🏨",
                    name = "Retorno ao hotel — malas e partida",
                    detail = "~350m de volta ao Hotel San Lucas para pegar as malas e aguardar o transfer.",
                    badges = listOf(Badge(BadgeType.WALKING, "a pé"))
                ),
                TravelActivity(
                    time = "14h~",
                    emoji = "✈️",
                    name = "Partida para o aeroporto",
                    detail = "Transfer contratado do hotel ao aeroporto. Horário exato confirmado por SMS no dia anterior. Voo de POA parte às 20h30 — conexão VCP → REC chega 14 Jun às 2h15. Boa viagem!",
                    badges = listOf(Badge(BadgeType.BOOKED, "transfer"))
                )
            )
        )
    )

    // ── CONTATOS ──────────────────────────────────────────────────────────

    val contacts: List<Contact> = listOf(
        Contact(
            name = "Brocker Turismo",
            role = "Passeios & Transfers · opção 7 (Serra Gaúcha)",
            phone = "5432825400",
            type = ContactType.AGENCY,
            hasWhatsApp = true
        ),
        Contact(
            name = "Brocker — Plantão emergência",
            role = "Somente ligações",
            phone = "5498123-8056",
            type = ContactType.AGENCY
        ),
        Contact(
            name = "Patricia Souza — Alfândega Turismo",
            role = "Agência de viagens",
            phone = "81992672588",
            type = ContactType.AGENCY
        ),
        Contact(
            name = "EHTL — Plantão 24h",
            role = "Operadora · fora do horário comercial",
            phone = "1147089326",
            type = ContactType.AGENCY
        ),
        Contact(
            name = "Hotel San Lucas",
            role = "Rua João Carniel, 73 — Bairro Carniel, Gramado",
            phone = null,
            type = ContactType.HOTEL
        ),
        Contact(
            name = "Parque do Caracol",
            role = "Dúvidas · sac@cnct.com.br",
            phone = null,
            type = ContactType.ATTRACTION
        ),
        Contact(
            name = "Laçador de Ofertas",
            role = "Bondinhos Aéreos · reagendamento e dúvidas",
            phone = "5130626070",
            type = ContactType.ATTRACTION
        ),
        Contact(
            name = "SAMU",
            role = "Emergência médica",
            phone = "192",
            type = ContactType.EMERGENCY,
            isEmergency = true
        ),
        Contact(
            name = "Bombeiros",
            role = "Emergência",
            phone = "193",
            type = ContactType.EMERGENCY,
            isEmergency = true
        ),
        Contact(
            name = "Polícia Militar",
            role = "Emergência",
            phone = "190",
            type = ContactType.EMERGENCY,
            isEmergency = true
        )
    )

    // ── CARTÕES DE EMBARQUE ───────────────────────────────────────────────

    val boardingPasses: List<BoardingPass> = listOf(

        // ── IDA: REC → GRU (09 Jun 02h30) ──────────────────────────────
        BoardingPass(
            origin          = "REC",
            originCity      = "Recife",
            destination     = "GRU",
            destinationCity = "São Paulo",
            flightNumber    = "AD 4153",
            date            = "09 Jun 2026",
            boardingTime    = "02h30",
            passenger       = "Rodrigo Augusto",
            walletUrl       = "https://pay.google.com/gp/v/save/eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJlbnRlcnByaXNlLXdhbGxldC1wcmRAZW50ZXJwcmlzZS13YWxsZXQtcHJkLmlhbS5nc2VydmljZWFjY291bnQuY29tIiwiYXVkIjoiZ29vZ2xlIiwidHlwIjoic2F2ZXRvd2FsbGV0IiwicGF5bG9hZCI6eyJmbGlnaHRPYmplY3RzIjpbeyJpZCI6IjMzODgwMDAwMDAwMjI5MTcwNjAucHJkMjAyNjA2MDlBRDQxNTNSRUNHUlVXTU04TkdNakFoUVVSVSJ9XX19.JO4fIQtVSKkM0P0_FZo6OAOybX_Q2XpVJl5nUB5-y3y6SoHIYJhNFqE5h6cqh2L43U5I8q1SaLQ97q5WDsnRoxGmsY1hci3wouEhDpi3UkflhxHBnbRpzfoEacNmkb_JOR2p8duWp0TrUL51MD1Rwa3-RPI4eCLWbuAkO6QaT-FvTAOhZzDaB2o0RhySDTF9X4gJ_CZ4ff-8ijilqgeNRMU61S0ndUG5H3TTIK6TeK3Wr_GbQFo5o5nmKMgnX_5Yj3JPBFIWkCrcBQ-vHvfNNX_HETMH2lGqn-WkZko6Q6qQGZASvHTiedjqCvw3Auc1F03RY0VhJ0y5NfqLywh-bQ"
        ),
        BoardingPass(
            origin          = "REC",
            originCity      = "Recife",
            destination     = "GRU",
            destinationCity = "São Paulo",
            flightNumber    = "AD 4153",
            date            = "09 Jun 2026",
            boardingTime    = "02h30",
            passenger       = "Gerlayne Regina",
            walletUrl       = "https://pay.google.com/gp/v/save/eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJlbnRlcnByaXNlLXdhbGxldC1wcmRAZW50ZXJwcmlzZS13YWxsZXQtcHJkLmlhbS5nc2VydmljZWFjY291bnQuY29tIiwiYXVkIjoiZ29vZ2xlIiwidHlwIjoic2F2ZXRvd2FsbGV0IiwicGF5bG9hZCI6eyJmbGlnaHRPYmplY3RzIjpbeyJpZCI6IjMzODgwMDAwMDAwMjI5MTcwNjAucHJkMjAyNjA2MDlBRDQxNTNSRUNHUlVXTU04TkdNakVoUVVSVSJ9XX19.AfVQ9mGr3wQUvZFQgJYgP_ml8qK9wlBvvrbaS6OKraRgVhhTZO0zOKDCEoneB-V-10OOkyhhKPhT61s_8mbry-V04qA4c0hj9jo7Lax6OHJQvrIGKkDboJcHC4J-on4VDIAeVEEKaDbhjNm6Rsil0NSbgq5lmG7ZzwSyP-Ma2yuv8RRTa-5qjmyu_JYk_xJ0DNVkwlKGDf7p0s_uu40ailI_K4N2OxWAH6RSzjylgvaDobMAXTTfqSaUsk_3eMxDFrstabuGQVWVtKYFq-nFZG7vIuomXe4MzXDwUV-4vmqIYEImNyL1NUkgfkUSP3SXyHELyUakNyQ5wfc0hSVUUA"
        ),

        // ── IDA: GRU → POA (09 Jun 06h30) ──────────────────────────────
        BoardingPass(
            origin          = "GRU",
            originCity      = "São Paulo",
            destination     = "POA",
            destinationCity = "Porto Alegre",
            flightNumber    = "AD 2842",
            date            = "09 Jun 2026",
            boardingTime    = "06h30",
            passenger       = "Rodrigo Augusto",
            walletUrl       = "https://pay.google.com/gp/v/save/eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJlbnRlcnByaXNlLXdhbGxldC1wcmRAZW50ZXJwcmlzZS13YWxsZXQtcHJkLmlhbS5nc2VydmljZWFjY291bnQuY29tIiwiYXVkIjoiZ29vZ2xlIiwidHlwIjoic2F2ZXRvd2FsbGV0IiwicGF5bG9hZCI6eyJmbGlnaHRPYmplY3RzIjpbeyJpZCI6IjMzODgwMDAwMDAwMjI5MTcwNjAucHJkMjAyNjA2MDlBRDI4NDJHUlVQT0FXTU04TkdNakFoUVVSVSJ9XX19.iN4Fj3HV99UbfZz0zfivrLVY8gQgbgQn7BYqYXKdJdqKxh0vguUbJPQ9taRr4v6132t5enSq0EHIR7zCuQA7yu2CCoL9UsI0cV-Bfz0LdOPksYDmySZ6rw2lWGGYN8qisUgvxOPQ6jIwyqYxJUImo6481yG5Yb4nxLr2wRa0AEYsgTE12C9VaLvZL725Xudh90KmvgHfrGqwStpa9VBgzTpY-f_5rXileu0QrMggEIlBtTbXrg1JWc6PiSsvZmHfpaImZ7oy8p6CduGoAaG9DzVNPo-1aygEKrsPc9Ae1oufVQU5NKwpA7LWip2_CX-aAZZPOZWI3kck6RE9JH2LEg"
        ),
        BoardingPass(
            origin          = "GRU",
            originCity      = "São Paulo",
            destination     = "POA",
            destinationCity = "Porto Alegre",
            flightNumber    = "AD 2842",
            date            = "09 Jun 2026",
            boardingTime    = "06h30",
            passenger       = "Gerlayne Regina",
            walletUrl       = "https://pay.google.com/gp/v/save/eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJlbnRlcnByaXNlLXdhbGxldC1wcmRAZW50ZXJwcmlzZS13YWxsZXQtcHJkLmlhbS5nc2VydmljZWFjY291bnQuY29tIiwiYXVkIjoiZ29vZ2xlIiwidHlwIjoic2F2ZXRvd2FsbGV0IiwicGF5bG9hZCI6eyJmbGlnaHRPYmplY3RzIjpbeyJpZCI6IjMzODgwMDAwMDAwMjI5MTcwNjAucHJkMjAyNjA2MDlBRDI4NDJHUlVQT0FXTU04TkdNakVoUVVSVSJ9XX19.UeM2JwXdSjPtzxW_X4lIXNC6lh0rgqIlVm_kqMGOGqfJAoW1OAfMuPsN3772TebB2260YI1Vag2qnUH0rqYtLB_G-s44fOFqpthExahKxFSYL1VcBsgew46jV8svwtkhjCKJCMXO9TwAXKK08BtKB_-BwuduTJvS1-mdcThgzLL_cbIiJetmB42mnqsOEk6L_d6X8ubYvrREyv_D2X4KUi3vf6WExSAdS96WMIUKEjrN8zscEd-j49d75zCpgDds8EMwSMxTEe8RapuYc3PbNDTsRp5FqXMH4ESb-AymMG1yRVYBpCEHxWhAsegX2s9VJKEuq9O6OHr_OiTVpjVdSg"
        ),

        // ── VOLTA: POA → VCP (13 Jun 20h30) — check-in pendente ────────
        BoardingPass(
            origin          = "POA",
            originCity      = "Porto Alegre",
            destination     = "VCP",
            destinationCity = "Campinas",
            flightNumber    = "AD 2699",
            date            = "13 Jun 2026",
            boardingTime    = "20h30",
            passenger       = "Rodrigo Augusto",
            walletUrl       = null
        ),
        BoardingPass(
            origin          = "POA",
            originCity      = "Porto Alegre",
            destination     = "VCP",
            destinationCity = "Campinas",
            flightNumber    = "AD 2699",
            date            = "13 Jun 2026",
            boardingTime    = "20h30",
            passenger       = "Gerlayne Regina",
            walletUrl       = null
        ),

        // ── VOLTA: VCP → REC (13 Jun 23h10) — check-in pendente ────────
        BoardingPass(
            origin          = "VCP",
            originCity      = "Campinas",
            destination     = "REC",
            destinationCity = "Recife",
            flightNumber    = "AD ----",  // confirmar número no check-in
            date            = "13 Jun 2026",
            boardingTime    = "22h40",    // ~30 min antes da partida 23h10
            passenger       = "Rodrigo Augusto",
            walletUrl       = null
        ),
        BoardingPass(
            origin          = "VCP",
            originCity      = "Campinas",
            destination     = "REC",
            destinationCity = "Recife",
            flightNumber    = "AD ----",
            date            = "13 Jun 2026",
            boardingTime    = "22h40",
            passenger       = "Gerlayne Regina",
            walletUrl       = null
        )
    )

    // ── VOUCHERS ──────────────────────────────────────────────────────────

    val vouchers: List<Voucher> = listOf(
        Voucher(
            emoji = "✈️",
            groupName = "Voo",
            name = "Voo — Azul (09 e 13 Jun)",
            assetPath = "vouchers/voo.jpeg"
        ),
        Voucher(
            emoji = "📋",
            groupName = "Transfers & Passeios",
            name = "Transfer + Bustour + Noite Gaúcha + Trem",
            assetPath = "vouchers/VOUCHER ATUALIZADO .pdf"
        ),
        Voucher(
            emoji = "🏘️",
            groupName = "Mini Mundo · 10 Jun",
            name = "Ingressos + Vinho Quente (2x)",
            assetPath = "vouchers/minimundo/vouchers - Mini Mundo.pdf",
            dayId = 2
        ),
        Voucher(
            emoji = "🌿",
            groupName = "Parque do Caracol · 11 Jun",
            name = "Bilhete 1",
            person = "Rodrigo",
            assetPath = "vouchers/caracol/v1.pdf",
            dayId = 3
        ),
        Voucher(
            emoji = "🌿",
            groupName = "Parque do Caracol · 11 Jun",
            name = "Bilhete 2",
            person = "Gerlayne",
            assetPath = "vouchers/caracol/v2.pdf",
            dayId = 3
        ),
        Voucher(
            emoji = "🚡",
            groupName = "Bondinhos Aéreos · 11 Jun",
            name = "Ingresso 1",
            person = "Rodrigo",
            assetPath = "vouchers/bondinhos/v1.pdf",
            dayId = 3
        ),
        Voucher(
            emoji = "🚡",
            groupName = "Bondinhos Aéreos · 11 Jun",
            name = "Ingresso 2",
            person = "Gerlayne",
            assetPath = "vouchers/bondinhos/v2.pdf",
            dayId = 3
        ),
        Voucher(
            emoji = "🎭",
            groupName = "Dreamland · 13 Jun",
            name = "Ingresso 1",
            person = "Rodrigo",
            assetPath = "vouchers/dreamland/8630511.pdf",
            dayId = 5
        ),
        Voucher(
            emoji = "🎭",
            groupName = "Dreamland · 13 Jun",
            name = "Ingresso 2",
            person = "Gerlayne",
            assetPath = "vouchers/dreamland/8630512.pdf",
            dayId = 5
        )
    )

    // ── QUERIES ───────────────────────────────────────────────────────────

    fun getDayById(id: Int): TravelDay? = days.find { it.id == id }

    fun getTodayDay(): TravelDay? = days.find { it.isToday }

    fun getVouchersByDay(dayId: Int): List<Voucher> = vouchers.filter { it.dayId == dayId }

    fun getContactsByType(type: ContactType): List<Contact> = contacts.filter { it.type == type }
}
