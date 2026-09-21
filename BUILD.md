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
- **Assets** — a motorbike, a laptop, a fridge: what it cost, when it was bought, how long it
  stays worth anything, and an optional warranty date. Straight-line depreciation by whole
  months, counted into net worth at today's value. Nothing about the value is stored.
- **Budgets** — a monthly limit per category, with rollover, and three ways of reading the same
  envelopes: envelope, 50/30/20, zero-based.
- **Bills & subscriptions** — recurring, with local reminders (WorkManager, no server), and
  "mark paid" that logs the money and rolls the date forward.
- **Goals and debts** — create, contribute, pay down. Snowball and avalanche ordering.
- **Reports** — category ring, income against spend, net worth, cash-flow calendar, top
  merchants, all derived from the ledger at read time.
- **App lock** — PIN (PBKDF2-SHA256, salted, throttled) and biometrics.
- **Statement import** — CSV or XLSX from any bank, with column inference and duplicate
  detection. The workbook reader is hand-written against the zip-of-XML that .xlsx is, for the
  same reason the CSV reader is: a spreadsheet library would weigh more than the whole app.
- **Ledger search** — text, direction, how far back, account, and an amount band. The rule is
  `LedgerQuery` in the data layer rather than in the screen, so it can be tested without a device.
- **Report export** — the whole ledger as CSV, or the month as a PDF for printing or sending
  on. `android.graphics.pdf` has shipped since API 19, so neither needs a dependency.
- **Receipt scanning** — on-device OCR that pre-fills a transaction and never posts by itself.
- **Multi-currency** — off by default; rates from Frankfurter, cached for offline, overridable.
- **Recurring templates** — a saved transaction shape that fills the editor in one tap. It
  never posts by itself; see the note under the product rules.
- **Categories** — rename, re-icon, re-parent, archive. Archived rather than deleted, so a
  past transaction is never relabelled by a present setting.
- **Notifications** — bill reminders and an evening streak warning, each with its own
  switch, each asking for the permission at the toggle.
- **Periods** — the month can open on the 1st, 15th, 25th or on payday, read as the median
  day income arrived over the last six months. Every derived figure follows it.
- **Appearance** — dark, light or follow the system, and Material You as an opt-in.
- **Round-ups** — the change from every expense, derived from the ledger and swept into a goal
  only on a tap. Nothing moves on a schedule; the only thing stored is the date of the last sweep.
- **52-week challenge** — the classic ladder, pinned to the calendar year because this engine
  stores no start date, and measured against money that actually reached a savings account.
- **A growing bed** — the README's "avatar/city/plant", grown as kelp so it lives in the design
  system's own water rather than beside it. Skippable, as the README asks.
- **Export and erase** — the whole ledger to CSV through the system picker, and a
  two-tap erase that re-seeds the day-one categories and cash account.

## What is deliberately not wired

- **No Hilt.** One database, one repository, one view model: `MoneyApp` is the whole graph in
  twenty lines. Swapping to Hilt later changes nothing above it.
- **No Vico, no MPAndroidChart.** Every chart is a `Canvas` composable, because the design needs
  a ring with labels on the arc and a mirrored net-worth sounding, and because a chart library
  brings its own house style into a committed visual world.
- **Material You reaches the Material surfaces and stops there.** Opt-in in Settings →
  Appearance, off by default. Plates, chips and sheets follow the wallpaper; the water column
  keeps the authored depth ladder, because those five steps in that order are what make the
  waterline a reading rather than a decoration.
- **No date picker when logging.** Everything logs as now; bills and goals do have one.
- **No leaderboard switch.** Comparing savings rate needs a shared wallet, a shared wallet is
  Firebase, and Firebase is not started. A toggle storing a preference no server will read is a
  dead control with a feature's name on it, so the Progress screen says so instead.
- **No PDF statement import.** CSV and XLSX carry the same rows in a form that can be read
  exactly; a PDF carries a picture of them. Extracting text needs a library several times the
  size of this app, and what comes out is a layout to be guessed at rather than columns — which
  is how an import puts the wrong number in the ledger without anything looking wrong. The
  import screen says so when a PDF is chosen, and names the two formats that do work.
- **Gmail import, Drive backup and Firebase sync are not started.** Each needs a Google Cloud or
  Firebase project owned by whoever ships this, with OAuth clients tied to the release signing
  certificate. There is nothing useful to write until those exist. `SERVICES.md` is the runbook:
  what to create, what to hand over, and what gets built when it arrives.

## Permissions, and why there are only two

- `POST_NOTIFICATIONS` — bill reminders. Asked for at the toggle, never at startup.
- `INTERNET` — the exchange-rate lookup, and nothing else. The request carries a currency code:
  no account, no amount, no identifier. It never runs unless multi-currency is switched on.

No camera permission: receipt capture is delegated to the system camera app. No storage
permission: receipt photos live in the app's own private directory, not in shared media.

## Verified, and not

**Verified by tests.** 176 unit tests, all passing, over the places where a silent mistake would
look like a transaction rather than a bug:

- `RepositoryWriteTest` (12) — the write paths, against a real database: transfers as a linked
  pair, cross-currency conversion on both sides, refusal when no rate exists, deleting a
  transfer's twin, paying a bill, splits charged to their own envelopes.
- `LedgerStateTest` (30) — every headline figure, including conversion into the base currency.
- `StatementImportTest` (14) — every amount format a bank writes, CSV quoting, duplicate matching.
- `ChallengesTest` (20) — derived runs, and what they must refuse to claim.
- `ReceiptParserTest` (12) — finding the total among the other numbers on a receipt.
- `RatesTest` (11) — currency conversion, including rounding of negatives and round trips.
- `NextDueTest` (7) — advancing a bill, including month-end drift and paying early.
- `LedgerCsvTest` (4) — export quoting and signs, read back through the importer.

- `RoundUpTest` (10) — the change from one spend and across the ledger: that an amount already
  on the boundary yields nothing, that income is never rounded up, that the sweep watermark is
  exclusive so the same change is not offered twice, and that an unconvertible row is skipped.
- `XlsxImportTest` (17) — reading a bank's workbook: shared and inline strings, rich-text
  runs, sparse cells that must not shift a column, and above all the dates, which are plain
  numbers that only a style says are dates. Workbooks are built in the test rather than checked
  in, so what is being asserted is readable.
- `LedgerSearchTest` (11) — the search rule, and especially that an amount band is compared on
  size: money out is held negative, so a signed comparison would let every expense through an
  "at least" filter.
- `AssetTest` (10) — what a possession is worth and when: the day it was bought, a straight
  line down, nothing after its life is up and never less than nothing, dates before it was
  owned, a life of zero rather than a division by it, and a house-sized price that must not
  overflow on the way through.

Two of those suites came with the Settings work:

- `PeriodsTest` (9) — where a cycle opens: the day before the opening, wrapping into the
  previous year, a derived payday clamped off the 31st, a leap February, and the calendar
  header rotating rather than being relabelled.
- `SettingsWriteTest` (9) — templates round-tripping, a rename that does not touch the
  transactions filed under it, an archive that leaves history alone, and the two erase cases:
  that it empties the ledger, and that what comes back is usable rather than a database with
  no categories and no account.

Run them with `./gradlew :app:testDebugUnitTest`. `RepositoryWriteTest` runs Room on the JVM
through Robolectric, so no device or emulator is needed; its first run downloads an Android
runtime jar and takes several minutes.

**SQL has a floor.** `minSdk` is 26, and the SQLite that ships with a device is the one that
shipped with its Android version. `INSERT ... ON CONFLICT DO UPDATE` needs SQLite 3.24, which
did not arrive until API 30 — it was in `MerchantMemoryDao.remember`, which runs on every
manually logged transaction, and it threw on Android 8, 9 and 10. Prefer SQL that works at the
floor; UPDATE-then-INSERT is the portable upsert.

**Verified by the compiler.** AGP 8.7.3, Kotlin 2.0.21, compileSdk 35, minSdk 26. Colour contrast
was checked numerically: every foreground/background token pair in both schemes clears 4.5:1.

**Not verified at all.** Everything that needs a screen, a camera or a network: no UI test has
ever run, and notification delivery, the biometric prompt and OCR accuracy have never been
exercised. Room migrations are still uncovered — the write paths are tested, the upgrade path
between schema versions is not.

Before trusting this with real money, use it for a week with data you would not mind losing.
