# Publicação na Google Play — material de apoio

Material pronto para transcrever no Play Console na primeira publicação do **Pipa**
(`com.rodrigoleao.pipa`). Conta **individual recente** → caminho: **teste fechado**
(≥20 testadores por ≥14 dias) antes de liberar produção.

---

## 1. Ficha da loja (Store listing)

### Nome do app (máx. 30)
`Pipa: Roteiros de Viagem`

### Descrição curta (máx. 80)

| Idioma | Texto |
|---|---|
| pt | Organize roteiros de viagem: dias, vouchers, embarque, contatos e notas. |
| en | Plan travel itineraries: days, vouchers, boarding passes, contacts & notes. |
| es | Organiza itinerarios de viaje: días, comprobantes, embarque, contactos y notas. |

### Descrição completa (máx. 4000)

**pt**
```
O Pipa organiza suas viagens do começo ao fim — tudo em um só lugar e funcionando offline.

Monte o roteiro dia a dia, guarde vouchers e reservas, cartões de embarque, contatos úteis e anotações. Precisa dividir a viagem com quem vai junto? Compartilhe tudo em um único arquivo, sem precisar de conta.

Recursos:
• Roteiro por dias, com atividades e horários
• Vouchers e reservas organizados por categoria, pessoa ou dia
• Cartões de embarque sempre à mão
• Contatos da viagem (hotel, guias, emergência)
• Notas por dia ou gerais
• Previsão do tempo do destino
• Assistente de IA para criar ou completar seu roteiro
• Compartilhe e importe viagens por arquivo
• Funciona offline — seus dados ficam no seu aparelho
• Disponível em português, inglês e espanhol

Sem cadastro. Sem anuncios. Simples e bonito.
```

**en**
```
Pipa organizes your trips from start to finish — all in one place and working offline.

Build a day-by-day itinerary, keep vouchers and bookings, boarding passes, useful contacts and notes. Traveling with others? Share the whole trip in a single file, no account required.

Features:
• Day-by-day itinerary with activities and times
• Vouchers and bookings grouped by category, person or day
• Boarding passes always at hand
• Trip contacts (hotel, guides, emergency)
• Per-day or general notes
• Destination weather forecast
• AI assistant to create or complete your itinerary
• Share and import trips via a file
• Works offline — your data stays on your device
• Available in Portuguese, English and Spanish

No sign-up. No ads. Simple and beautiful.
```

**es**
```
Pipa organiza tus viajes de principio a fin, todo en un solo lugar y funcionando sin conexión.

Arma el itinerario día a día, guarda comprobantes y reservas, tarjetas de embarque, contactos útiles y notas. ¿Viajas acompañado? Comparte todo el viaje en un único archivo, sin necesidad de cuenta.

Funciones:
• Itinerario día a día con actividades y horarios
• Comprobantes y reservas por categoría, persona o día
• Tarjetas de embarque siempre a mano
• Contactos del viaje (hotel, guías, emergencia)
• Notas por día o generales
• Pronóstico del tiempo del destino
• Asistente de IA para crear o completar tu itinerario
• Comparte e importa viajes con un archivo
• Funciona sin conexión: tus datos quedan en tu dispositivo
• Disponible en portugués, inglés y español

Sin registro. Sin anuncios. Simple y bonito.
```

### Categoria / contato
- **Categoria:** Viagens e local (Travel & Local)
- **E-mail de contato:** rodrigoleao1995@gmail.com
- **Política de privacidade:** https://rodrigoleao111.github.io/rumo-app/privacy/

---

## 2. Recursos gráficos (assets)

| Asset | Requisito | Status |
|---|---|---|
| Ícone hi-res | 512×512 PNG (32-bit, com alfa) | derivar do `ic_launcher` |
| **Feature graphic** | **1024×500 PNG/JPEG** | ⚠️ **provavelmente falta criar** |
| Screenshots de telefone | mín. 2, 16:9 ou 9:16, 320–3840 px | ✅ há capturas do build debug em `docs/screenshots/` |
| Screenshots de tablet | opcional | — |

> A **feature graphic** é obrigatória e costuma ser o único asset que ainda não existe. É o banner do topo da ficha.

---

## 3. Segurança dos dados (Data Safety) — respostas recomendadas

Baseado no mapeamento real do código. Princípios do app: **sem login, sem anúncios, sem localização do dispositivo (GPS)**. Dados de viagem (viagens, vouchers, contatos com nome/telefone, notas, passageiros dos cartões) ficam **só no aparelho** → pela definição do Play **não são "coletados"** (nunca saem do dispositivo), então **não se declaram**.

### Perguntas gerais
- **O app coleta ou compartilha dados exigidos?** → **Sim** (telemetria Analytics/Crashlytics sai do dispositivo).
- **Todos os dados são criptografados em trânsito?** → **Sim** (HTTPS para Firebase, Gemini e Open-Meteo).
- **Usuários podem pedir exclusão de dados?** → O app **não tem servidor próprio**; os dados ficam no aparelho e somem ao desinstalar (e podem ser apagados dentro do app). Marcar a opção e informar o e-mail de contato da política.

### Tipos de dados a declarar

| Tipo de dado | Coletado | Compart. | Finalidade | Ligado à identidade | Origem |
|---|---|---|---|---|---|
| **Interações no app** (App activity → App interactions) | Sim | Não | Análise | Não | Firebase Analytics |
| **Registros de falhas** (App info & perf → Crash logs) | Sim | Não | Análise + Funcionalidade | Não | Crashlytics |
| **Diagnósticos** (App info & perf → Diagnostics) | Sim | Não | Análise + Funcionalidade | Não | Analytics/Crashlytics |
| **IDs de dispositivo ou outros** (Device or other IDs) | Sim | Não | Análise + Funcionalidade | Não | App Instance ID do Firebase (⚠️ **não** é o Advertising ID — desativado) |

Marcar todos como **coletados automaticamente** (não fornecidos pelo usuário) e **não obrigatórios**.

### ⚠️ Ponto que exige decisão — entrada da IA (Gemini)

Quando o usuário usa o assistente de IA, o texto que ele digita (destino, datas, hotel e o chat livre) **é enviado à API do Gemini (Google)**. Isso sai do dispositivo, então deve ser considerado:

- **Tipo:** *App activity → Other user-generated content* (o chat é texto livre).
- **Coletado:** Sim · **Finalidade:** Funcionalidade do app · **Ligado à identidade:** Não.
- **Compartilhado?** Depende do **tier da API do Gemini**: no **tier gratuito** (chave do AI Studio), os termos permitem que o Google **use os prompts para melhorar os produtos** → nesse caso marque também como **compartilhado**. No tier pago, não. **Confirmar qual tier a chave usa** antes de responder.
- A política de privacidade já orienta "evite digitar informações sensíveis" no chat.

### O que NÃO declarar (e por quê)
- **Localização:** o app **não** acessa GPS/localização do dispositivo. As coordenadas do **destino** enviadas ao Open-Meteo são *conteúdo* que o usuário digitou, não a localização do usuário → não é o tipo "Localização" do Play.
- **Dados de viagem locais** (nomes de contatos/passageiros, telefones, notas): nunca saem do aparelho → não são "coletados".

---

## 4. SHA-1 das chaves (para a Parte B pós-upload)

Restringir a chave do Firebase por app Android exige o **SHA-1 da app signing key** (a do Google), que só aparece em **Play Console → Setup → App signing** depois do 1º upload.

- **Debug key** (`~/.android/debug.keystore`, senha pública `android`):
  `SHA1: AF:46:18:16:BA:2C:8D:51:34:79:F2:D3:21:87:D8:2D:F3:09:65:EF`
- **Upload key** (`keystore/gramado2026.jks`) — rodar você mesmo (pede a senha da store):
  ```powershell
  & "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore "keystore\gramado2026.jks" -alias gramado
  ```
- **App signing key** (Google) → só no Console após o upload.

---

## 5. Ordem de execução no Console
1. Criar app → nome, pt-BR, App, Gratuito.
2. Painel "Configurar seu app": política de privacidade, acesso, anúncios (não), classificação IARC, público-alvo, Data Safety (§3).
3. Ficha da loja (§1) + gráficos (§2).
4. Criar release em **Teste fechado** → aceitar Play App Signing → subir `app-release.aab` → notas da versão → revisar e lançar.
5. Recrutar ≥20 testadores e manter o teste ≥14 dias.
6. Solicitar acesso à produção → publicar.
7. Pós-upload: Parte B (restrição da chave Firebase com o SHA da app signing key).
