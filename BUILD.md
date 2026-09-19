# Building the scaffold

This is the UI scaffold for the app described in [README.md](README.md): a real Kotlin/Compose
Android project with every screen the README lists, rendered against synthetic data. There is no
Room, no Hilt and no repository layer yet — those land under the screens without moving them.

## Open it

Android Studio (Ladybug or newer) → **Open** → this folder. It bundles a JDK 17+ and will sync
Gradle on its own.

From a terminal you need JDK 17 or newer on `JAVA_HOME` (the system JDK on this machine is 15,
which AGP 8.7 rejects):

```bash
./gradlew :app:assembleDebug
```

`local.properties` points at your Android SDK and is machine-specific, so it is gitignored. After
cloning, Android Studio writes it for you on first sync; from a terminal, create it with a single
line: `sdk.dir=/path/to/Android/Sdk`.

## What is here

| Path | What it holds |
|---|---|
| `app/src/main/java/com/moneymanager/MainActivity.kt` | Entry point, and the direction contract the whole design answers to |
| `app/src/main/java/com/moneymanager/ui/Theme.kt` | The depth ladder, both colour schemes, the Archivo type scale, shapes |
| `app/src/main/java/com/moneymanager/ui/Water.kt` | `WaterField`, `WaterColumn`, `WaterBar`, the glass plate modifier |
| `app/src/main/java/com/moneymanager/ui/Charts.kt` | Ring, paired columns, sounding line, calendar grid, vessel, depth gauge — all drawn on Canvas |
| `app/src/main/java/com/moneymanager/ui/Components.kt` | `MoneyText`, `Plate`, `LedgerRow`, `Pill`, `Chip`, `NavRow`, empty states |
| `app/src/main/java/com/moneymanager/ui/Nav.kt` | Routes, navigation bar and rail, FAB, transitions |
| `app/src/main/java/com/moneymanager/ui/screens/` | Every screen |
| `app/src/main/java/com/moneymanager/data/Model.kt` | The domain shapes every screen reads, the clock, money formatting |
| `app/src/main/java/com/moneymanager/data/db/` | Room entities, DAOs, the database and its first-run seed |
| `app/src/main/java/com/moneymanager/data/MoneyRepository.kt` | `LedgerState`: every total, derived from the ledger at read time |
| `app/src/main/java/com/moneymanager/data/StatementImport.kt` | CSV parsing, amount formats, duplicate matching |
| `app/src/main/java/com/moneymanager/data/Rates.kt` | Exchange rates and integer currency conversion |
| `app/src/main/java/com/moneymanager/data/ReceiptParser.kt` | Reading a total off OCR text. Pure, and tested |
| `app/src/main/java/com/moneymanager/data/SecurityStore.kt` | PIN hashing and the lock settings |
| `app/src/main/java/com/moneymanager/data/Reminders.kt` | The daily bill check and its notification |
| `app/src/main/java/com/moneymanager/data/DemoData.kt` | A month of invented activity. Settings only, never on first run. |
| `app/src/main/java/com/moneymanager/MoneyApp.kt` | The whole dependency graph, plus the single view model |
| `app/src/test/java/com/moneymanager/data/` | The 37 unit tests |

## Screens

Tabs: **Home**, **Ledger**, **Budgets**, **Reports**, **More**.

Under them: transaction editor and detail, budget detail, accounts and account detail, bills and
subscriptions, goals, debt payoff, capture (receipt / email / statement), progress and badges,
currencies, lock and privacy, backup and sync, settings, and the PIN lock screen.

## What is built

Everything in the README's MVP v1, plus most of v2:

- **Transactions** — add, edit, delete, split across categories, tag, in any currency.
- **Accounts** — create, transfer between them. A balance is the opening figure plus every
  transaction since, never a stored total.
- **Budgets** — a monthly limit per category, with rollover, and three ways of reading the same
  envelopes: envelope, 50/30/20, zero-based.
- **Bills & subscriptions** — recurring, with local reminders (WorkManager, no server), and
  "mark paid" that logs the money and rolls the date forward.
- **Goals and debts** — create, contribute, pay down. Snowball and avalanche ordering.
- **Reports** — category ring, income against spend, net worth, cash-flow calendar, top
  merchants, all derived from the ledger at read time.
- **App lock** — PIN (PBKDF2-SHA256, salted, throttled) and biometrics.
- **Statement import** — CSV from any bank, with column inference and duplicate detection.
- **Receipt scanning** — on-device OCR that pre-fills a transaction and never posts by itself.
- **Multi-currency** — off by default; rates from Frankfurter, cached for offline, overridable.

## What is deliberately not wired

- **No Hilt.** One database, one repository, one view model: `MoneyApp` is the whole graph in
  twenty lines. Swapping to Hilt later changes nothing above it.
- **No Vico, no MPAndroidChart.** Every chart is a `Canvas` composable, because the design needs
  a ring with labels on the arc and a mirrored net-worth sounding, and because a chart library
  brings its own house style into a committed visual world.
- **No Material You.** The depth palette is what makes the waterline legible; a wallpaper-derived
  scheme reorders it. It is offered as an explicit opt-in in Settings → Appearance.
- **No date picker when logging.** Everything logs as now; bills and goals do have one.
- **Gmail import, Drive backup and Firebase sync are not started.** Each needs a Google Cloud or
  Firebase project owned by whoever ships this, with OAuth clients tied to the release signing
  certificate. There is nothing useful to write until those exist.

## Permissions, and why there are only two

- `POST_NOTIFICATIONS` — bill reminders. Asked for at the toggle, never at startup.
- `INTERNET` — the exchange-rate lookup, and nothing else. The request carries a currency code:
  no account, no amount, no identifier. It never runs unless multi-currency is switched on.

No camera permission: receipt capture is delegated to the system camera app. No storage
permission: receipt photos live in the app's own private directory, not in shared media.

## Verified, and not

**Verified by tests.** 37 unit tests, all passing, over the three places where a silent mistake
would look like a transaction rather than a bug:

- `StatementImportTest` (14) — every amount format a bank writes, CSV quoting, duplicate matching.
- `RatesTest` (11) — currency conversion, including rounding of negatives and round trips.
- `ReceiptParserTest` (12) — finding the total among the other numbers on a receipt.

Run them with `./gradlew :app:testDebugUnitTest`.

**Verified by the compiler.** AGP 8.7.3, Kotlin 2.0.21, compileSdk 35, minSdk 26. Colour contrast
was checked numerically: every foreground/background token pair in both schemes clears 4.5:1.

**Not verified at all.** Everything that needs a screen, a database round trip, a camera or a
network. Specifically: the Room layer has no instrumented tests, so nothing automated would catch
a migration or query fault; notification delivery, the biometric prompt and OCR accuracy have
never been exercised. Those are the highest-value things to cover next, in that order, because a
fault in the data layer corrupts a financial record rather than merely looking wrong.

Before trusting this with real money, use it for a week with data you would not mind losing.
