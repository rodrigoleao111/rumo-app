# Módulo 12 — Notificações e lembrete de check-in

**Arquivos:**
- `notifications/NotificationHelper.kt` — fachada de agendamento (WorkManager + canal)
- `notifications/CheckInReminderWorker.kt` — `Worker` que dispara a notificação
- `ui/boarding/BoardingPassScreen.kt` — `CheckInReminderCard` (UI do toggle)

**Entry point:** aba **Embarque** (`BoardingPassScreen`), card exibido apenas quando há passagem do tipo `FLIGHT`.

---

## Visão geral

O app agenda **uma notificação local** para lembrar o usuário de fazer o check-in do voo de volta, **72h antes** do horário do voo. O agendamento usa **WorkManager** (`OneTimeWorkRequest` com delay inicial) e a notificação é postada por um `Worker`. O canal de notificação é criado no `onCreate` da `MainActivity`. Não há notificações remotas (FCM) — tudo é 100% local e offline.

> **Dívida técnica conhecida — dados hardcoded.** Tanto o horário do voo (`13 Jun 2026 20:30`) quanto o texto da notificação são fixos no código, herdados do contexto original da viagem única (Gramado 2026). O recurso **não é dirigido pelos dados da `BoardingPass`** da viagem aberta. Generalizar exigiria derivar `FLIGHT_DATETIME` da passagem de volta real e tornar o texto dinâmico. Ver *Checklist* ao final.

---

## Fluxo geral

```
MainActivity.onCreate
  └─ NotificationHelper.createChannel(context)   ← cria o canal (Android 8+)

BoardingPassScreen  (aba Embarque, se há voo)
  └─ CheckInReminderCard(isActive, onActivate, onCancel)
       ├─ onActivate:
       │    Android 13+ → pede permissão POST_NOTIFICATIONS (permLauncher)
       │                    → se concedida: NotificationHelper.schedule(context)
       │    Android <13 → NotificationHelper.schedule(context) direto
       │    persiste "checkin_active" em SharedPreferences("reminders")
       └─ onCancel:
            NotificationHelper.cancel(context)
            grava "checkin_active" = false

WorkManager  (após o delay agendado)
  └─ CheckInReminderWorker.doWork()
       ├─ verifica POST_NOTIFICATIONS → sem permissão: Result.failure()
       └─ posta a notificação (BigTextStyle) → Result.success()
```

---

## `NotificationHelper`

`object` singleton (sem estado, apenas funções utilitárias sobre WorkManager).

| Constante | Valor | Uso |
|---|---|---|
| `CHANNEL_ID` | `"gramado_reminders"` | ID do canal de notificação |
| `CHANNEL_NAME` | `"Lembretes da Viagem"` | Nome exibido nas configurações do sistema |
| `NOTIFICATION_ID` | `1001` | ID da notificação postada |
| `WORK_TAG` | `"checkin_reminder"` | Tag do trabalho único no WorkManager |

**Datas (hardcoded):**
```kotlin
private val FLIGHT_DATETIME   = LocalDateTime.of(2026, 6, 13, 20, 30)   // voo de volta
private val REMINDER_DATETIME = FLIGHT_DATETIME.minusHours(72)          // lembrete
val reminderDisplay: String get() = "10 Jun · 20h30"                    // rótulo para a UI
```

### Métodos

| Método | Retorno | Responsabilidade |
|---|---|---|
| `createChannel(context)` | — | Cria o `NotificationChannel` com `IMPORTANCE_HIGH` (só Android 8+ / API 26; abaixo disso é no-op). Chamado no `onCreate` da `MainActivity`. |
| `schedule(context)` | `Boolean` | Calcula o delay `now → REMINDER_DATETIME`; se já passou (`<= 0`), retorna `false` sem agendar. Senão enfileira um `OneTimeWorkRequest` com `enqueueUniqueWork(WORK_TAG, REPLACE, …)` e retorna `true`. |
| `cancel(context)` | — | `cancelUniqueWork(WORK_TAG)`. |
| `isScheduled(context)` | `Boolean` | Consulta **síncrona** (`.get()`) do estado do trabalho — `true` se `ENQUEUED` ou `RUNNING`. Chamar **fora da thread principal**. |

**`ExistingWorkPolicy.REPLACE`:** reagendar substitui o agendamento anterior — não acumula lembretes duplicados.

**Constraint:** `NetworkType.NOT_REQUIRED` — o lembrete dispara mesmo offline (não depende de rede).

---

## `CheckInReminderWorker`

`Worker` clássico (`doWork()` síncrono, roda na thread do WorkManager).

```kotlin
override fun doWork(): Result {
    // Android 13+: sem POST_NOTIFICATIONS a notificação não aparece
    if (checkSelfPermission(POST_NOTIFICATIONS) != GRANTED) return Result.failure()

    val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("✈️ Hora do check-in da volta!")
        .setContentText("Seu voo POA → VCP → REC é daqui a 3 dias (13 Jun às 20h30).")
        .setStyle(NotificationCompat.BigTextStyle().bigText("…"))
        .setPriority(PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(NotificationHelper.NOTIFICATION_ID, notification)
    return Result.success()
}
```

- **Ícone:** `R.drawable.ic_launcher_foreground` (não há ícone dedicado de notificação).
- **Conteúdo:** título/texto/`bigText` fixos, mencionando o app da Azul e o Google Wallet — específicos da viagem original.
- **`setAutoCancel(true)`:** a notificação some ao ser tocada.
- Sem permissão retorna `Result.failure()` (não reagenda; o `Worker` não faz retry por padrão aqui).

---

## `CheckInReminderCard` (UI)

Renderizado em `BoardingPassScreen` **apenas se** `passes.any { it.transportType == "FLIGHT" }`.

**Estados:** `reminderActive` é hidratado de `SharedPreferences("reminders")` chave `checkin_active` (default `false`) e mantido em `remember`.

**Ativação (`onActivate`):**
```kotlin
if (Build.VERSION.SDK_INT >= TIRAMISU) {         // Android 13+
    permLauncher.launch(POST_NOTIFICATIONS)       // pede permissão; agenda no callback se concedida
} else {
    val ok = NotificationHelper.schedule(context) // <13 não precisa de permissão
    reminderActive = ok
    reminderPrefs.edit().putBoolean("checkin_active", ok).apply()
}
```

**Cancelamento (`onCancel`):** `NotificationHelper.cancel` + grava `checkin_active = false`.

**Texto do card:** usa `NotificationHelper.reminderDisplay` — quando ativo, *"✅ Notificação agendada para 10 Jun · 20h30"*; quando inativo, *"Notificar 72h antes do voo de volta (10 Jun · 20h30)"*.

---

## Permissão `POST_NOTIFICATIONS`

- Declarada no `AndroidManifest.xml`; obrigatória em runtime a partir do **Android 13 (TIRAMISU)**.
- Pedida via `rememberLauncherForActivityResult(RequestPermission())` no momento da ativação — nunca no início do app.
- Se o usuário negar, `schedule` não é chamado e o toggle permanece inativo. O `Worker` também revalida a permissão em `doWork` (o usuário pode revogá-la depois do agendamento).

---

## Persistência

| Chave | Store | Conteúdo |
|---|---|---|
| `checkin_active` | `SharedPreferences("reminders")` | Estado do toggle na UI (o WorkManager mantém o agendamento real; essa flag só reflete o estado visual) |

O agendamento em si vive no **WorkManager** (banco interno próprio), sobrevive a reinícios do app e do dispositivo. A flag em `SharedPreferences` é apenas o espelho para a UI — coerente com a regra do projeto de usar `SharedPreferences` para estados voláteis simples que não merecem migration Room.

---

## Checklist para futuras modificações

- **Tornar o lembrete dirigido por dados (remover hardcode):** derivar `FLIGHT_DATETIME` da última `BoardingPass` do tipo `FLIGHT` da viagem (data + horário) em vez da constante fixa; passar data/rota como `inputData` do `Worker` para montar o texto dinamicamente. Isso alinharia o recurso ao modelo multi-viagem (mesma direção da remoção do mapa do Bustour — ver `docs/modulo-03-day-detail.md`).
- **Ícone dedicado de notificação:** substituir `ic_launcher_foreground` por um `drawable` monocromático próprio (o ícone de notificação deve ser branco sobre transparente).
- **Lembretes por atividade do dia:** reusar `NotificationHelper.schedule` com múltiplos `WORK_TAG` distintos (um por atividade) — hoje só existe um trabalho único.
- **Reagendar após boot:** se for necessário garantir o disparo após reinício do dispositivo, o WorkManager já persiste o trabalho; nenhuma ação extra é necessária (não usar `AlarmManager` a menos que se precise de exatidão ao minuto).
