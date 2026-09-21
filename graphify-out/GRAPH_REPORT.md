# Graph Report - money tracker yet to build  (2026-09-21)

## Corpus Check
- 69 files · ~94,427 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 987 nodes · 2783 edges · 51 communities (39 shown, 12 thin omitted)
- Extraction: 85% EXTRACTED · 15% INFERRED · 0% AMBIGUOUS · INFERRED: 420 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `0fb96c3d`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Money.kt
- LedgerState
- MoneyViewModel
- StatementImportTest
- MoneyRepository
- RatesStore
- XlsxImportTest
- parseReceipt
- Flow
- Water.kt
- Money Manager Design System
- nextDue
- Receipt Scanning and Auto-Import (feature area 9)
- Build Guide (UI scaffold)
- Transactions (feature area 1)
- MoneyApp.kt
- A Cross-Currency Write Converts Or Is Refused
- Money Manager Feature Spec (README)
- MoneyRepository.kt
- MoneyDatabase
- The One Authored Motion Rule
- BillDao
- BudgetDao
- OffsetMapping
- AccountEntity
- CategoryEntity
- DebtDao
- GoalDao
- LedgerQuery
- Multi-Currency Conversion Policy
- TabColumn
- Emergency Fund Goal Cover Photo (City Skyline)
- MerchantMemoryDao
- LedgerFilter
- Goal Bali Cover Image
- goal_laptop.jpg (moody beach and mountain landscape)
- Method
- Reports.kt
- gradlew
- PeriodsTest
- rememberBiometricPrompt
- LedgerRange
- TemplateDao
- Txn
- Connecting the three services that are not started
- AssetTest
- Kelp.kt
- AssetDao

## God Nodes (most connected - your core abstractions)
1. `LedgerState` - 78 edges
2. `MoneyRepository` - 49 edges
3. `Plate()` - 46 edges
4. `Pill()` - 46 edges
5. `MoneyNavHost()` - 44 edges
6. `MoneyViewModel` - 38 edges
7. `LedgerStateTest` - 37 edges
8. `SectionHeading()` - 36 edges
9. `Chip()` - 33 edges
10. `Flow` - 32 edges

## Surprising Connections (you probably didn't know these)
- `Gamification Is A Layer Over Real Data` --semantically_similar_to--> `Totals Derived At Read Time`  [INFERRED] [semantically similar]
  PRODUCT.md → BUILD.md
- `No Hilt` --semantically_similar_to--> `Hilt Deliberately Absent`  [INFERRED] [semantically similar]
  BUILD.md → PRODUCT.md
- `Merchant Monogram` --semantically_similar_to--> `Merchant-Based Auto-Categorization`  [INFERRED] [semantically similar]
  DESIGN.md → README.md
- `No Control Wired To Nothing` --semantically_similar_to--> `A Cross-Currency Write Converts Or Is Refused`  [INFERRED] [semantically similar]
  DESIGN.md → PRODUCT.md
- `Goal Photo Placeholders` --conceptually_related_to--> `The Photography Rule`  [INFERRED]
  .impeccable/assets/provenance.md → DESIGN.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Depth Replaces Shadow As The Elevation System** — design_lit_column_of_water, design_depth_ladder, design_glass_plate, design_elevation_is_depth_rule, design_transmission_rule, build_no_material_you, design_dynamic_color_opt_in [EXTRACTED 1.00]
- **Where A Silent Mistake Would Look Like A Transaction** — build_test_suite, build_repositorywritetest, build_ledgerstatetest, build_statementimporttest, build_challengestest, build_receiptparsertest, build_ratestest, build_nextduetest, build_ledgercsvtest, build_unverified_surfaces, product_number_right_before_beautiful [EXTRACTED 1.00]
- **Free Capture Paths Standing In For Paid Aggregation** — product_zero_cost_constraint, readme_cost_to_build, product_no_bank_aggregation, readme_plaid_yodlee_exclusion, readme_sms_policy_exclusion, readme_receipt_scanning_and_auto_import, readme_ml_kit_text_recognition, readme_gmail_api, product_nothing_posts_without_confirmation [EXTRACTED 1.00]
- **Goal Cover Asset Pattern** — app_src_main_res_drawable_nodpi_goal_bali_goal_bali, app_src_main_res_drawable_nodpi_goal_bali_savings_goal_cover_art, app_src_main_res_drawable_nodpi_goal_bali_nodpi_density_bucket, app_src_main_res_drawable_nodpi_goal_bali_muted_photographic_backdrop [INFERRED 0.75]
- **Goal Cover Art Asset Convention (naming, density bucket, crop, palette)** — app_src_main_res_drawable_nodpi_goal_emergency, app_src_main_res_drawable_nodpi_goal_emergency_goal_cover_art_convention, app_src_main_res_drawable_nodpi_goal_emergency_nodpi_density_bucket, app_src_main_res_drawable_nodpi_goal_emergency_portrait_hero_crop, app_src_main_res_drawable_nodpi_goal_emergency_muted_overlay_safe_palette [INFERRED 0.85]

## Communities (51 total, 12 thin omitted)

### Community 0 - "Money.kt"
Cohesion: 0.07
Nodes (124): android, Accounts, cycleDayOf(), dayLabel(), dueLabel(), short(), symbolOf(), canNotify() (+116 more)

### Community 1 - "LedgerState"
Cohesion: 0.07
Nodes (20): DemoData, Account, AccountKind, Bank, Card, Cash, Savings, Wallet (+12 more)

### Community 2 - "MoneyViewModel"
Cohesion: 0.06
Nodes (8): Template, Factory, T, MoneyViewModel, SettingsWriteTest, com, MoneyFlow, ViewModelProvider

### Community 3 - "StatementImportTest"
Cohesion: 0.10
Nodes (16): csvCell(), ledgerCsv(), minorToDecimal(), ColumnMap, detectDelimiter(), ImportCandidate, inferColumns(), matchDuplicates() (+8 more)

### Community 4 - "MoneyRepository"
Cohesion: 0.09
Nodes (7): Debt, Goal, MoneyRepository, Plans, toDomain(), RepositoryWriteTest, kotlinx

### Community 5 - "RatesStore"
Cohesion: 0.09
Nodes (11): Conversion, convertMinor(), divRound(), StateFlow, parseRatesJson(), Rates, RatesStore, changeFrom() (+3 more)

### Community 6 - "XlsxImportTest"
Cohesion: 0.12
Nodes (22): cellText(), columnIndexOf(), dateStyleIndices(), firstOrNull(), forEach(), joinToString(), ByteArray, T (+14 more)

### Community 7 - "parseReceipt"
Cohesion: 0.13
Nodes (12): findDate(), findMerchant(), findTotal(), lastAmountOn(), parseReceipt(), ReceiptGuess, separatorFor(), Context (+4 more)

### Community 8 - "Flow"
Cohesion: 0.15
Nodes (8): AccountSum, CategorySum, TxnDao, TxnWithDetails, Flow, In, Out, Transfer

### Community 9 - "Water.kt"
Cohesion: 0.27
Nodes (18): caustic(), drawSwell(), glassPlate(), Color, Dp, Modifier, Shape, State (+10 more)

### Community 10 - "Money Manager Design System"
Cohesion: 0.16
Nodes (19): Goal Photo Placeholders, Launcher Icon Vector Drawables, Lorem Picsum (Unsplash source), Raster Provenance Register, Modifier.bleed(), Five-Step Depth Ladder, The Elevation Is Depth Rule, Modifier.glassPlate(shape, depth) (+11 more)

### Community 11 - "nextDue"
Cohesion: 0.19
Nodes (7): nextDue(), Recurrence, Monthly, Quarterly, Weekly, Yearly, NextDueTest

### Community 12 - "Receipt Scanning and Auto-Import (feature area 9)"
Cohesion: 0.17
Nodes (16): Gmail / Drive / Firebase Blocked, Not Deferred, StatementImportTest (bank CSV formats), Offline Is The Default State, Bank Aggregation Permanently Excluded, SMS Auto-Read Permanently Excluded, Zero Running Cost Forever, Cost to Build ($0 table), Firebase Spark Plan (+8 more)

### Community 13 - "Build Guide (UI scaffold)"
Cohesion: 0.19
Nodes (16): ChallengesTest (derived runs), Totals Derived At Read Time, LedgerStateTest (headline figures), No Chart Library (Canvas charts), No Hilt, ReceiptParserTest (finding the total), Build Guide (UI scaffold), 100 Unit Tests (+8 more)

### Community 14 - "Transactions (feature area 1)"
Cohesion: 0.13
Nodes (16): LedgerCsvTest (export round trip), No Date Picker When Logging, SQL Has A Floor (minSdk 26 SQLite), categoryScale() (eight-step cool scale), Signature: Hand-drawn Charts, Merchant Monogram, Logging Must Survive A Hurry, The 10-Second Logging Moment (+8 more)

### Community 15 - "MoneyApp.kt"
Cohesion: 0.06
Nodes (36): AppPrefs, StateFlow, ThemeMode, Dark, Light, System, PeriodSettings, ForeignAmount (+28 more)

### Community 16 - "A Cross-Currency Write Converts Or Is Refused"
Cohesion: 0.14
Nodes (14): No Material You by default, RepositoryWriteTest (write paths), Archivo Variable Type Scale, The Cents Are Shown Rule, Dynamic Color Off By Default, moneyParts (Long minor units), MoneyText, No Control Wired To Nothing (+6 more)

### Community 17 - "Money Manager Feature Spec (README)"
Cohesion: 0.19
Nodes (14): The One Warm Hue Rule, MVP v1 Scope (must be real), Product Brief, Budgets (feature area 3), Envelope Budgeting, 50/30/20 Rule Template, Gamification (feature area 11), Goals and Savings (feature area 6) (+6 more)

### Community 18 - "MoneyRepository.kt"
Cohesion: 0.24
Nodes (5): MerchantMemoryEntity, SplitEntity, TagEntity, TxnEntity, YearMonth

### Community 19 - "MoneyDatabase"
Cohesion: 0.24
Nodes (5): Context, MoneyDatabase, Seed, RoomDatabase, SupportSQLiteDatabase

### Community 20 - "The One Authored Motion Rule"
Cohesion: 0.20
Nodes (10): The Arriving Curve Rule, The Expenses Are Not Red Rule, The Feedback Is Not A Gesture Rule, MoneyMotion (duration and easing tokens), motionEnabled(), The Never-Colour-Alone Rule, The One Authored Motion Rule, rememberFill (+2 more)

### Community 23 - "OffsetMapping"
Cohesion: 0.29
Nodes (5): AnnotatedString, MoneyDigits, OffsetMapping, TransformedText, VisualTransformation

### Community 29 - "Multi-Currency Conversion Policy"
Cohesion: 0.29
Nodes (8): NextDueTest (bill advancement), RatesTest (currency conversion), Only Two Permissions, Bills, Goals and Debts Read Currency From Their Account, Multi-Currency Conversion Policy, Bills and Subscriptions (feature area 4), Frankfurter API (FX rates), Multi-Currency (feature area 7)

### Community 30 - "TabColumn"
Cohesion: 0.52
Nodes (6): androidx, bleed(), Dp, Modifier, Stack(), TabColumn()

### Community 31 - "Emergency Fund Goal Cover Photo (City Skyline)"
Cohesion: 0.60
Nodes (6): Emergency Fund Goal Cover Photo (City Skyline), Emergency Fund Savings Goal, goal_<name> Cover Art Naming Convention, Muted Desaturated Palette for Text Overlay Legibility, drawable-nodpi Photographic Asset Bucket, Portrait 4:5 Hero Crop for Goal Cards

### Community 33 - "LedgerFilter"
Cohesion: 0.40
Nodes (5): LedgerFilter, All, In, Out, Transfers

### Community 34 - "Goal Bali Cover Image"
Cohesion: 0.60
Nodes (5): Bali Travel Savings Goal, Goal Bali Cover Image, Muted Photographic Backdrop Style, nodpi Density Bucket Packaging, Savings Goal Cover Art

### Community 35 - "goal_laptop.jpg (moody beach and mountain landscape)"
Cohesion: 0.50
Nodes (5): Aspirational Imagery Motivates Saving, goal_laptop.jpg (moody beach and mountain landscape), Laptop Purchase Goal (filename-implied subject), drawable-nodpi Density Bucket (no per-density scaling), Savings Goal Cover Art

### Community 36 - "Method"
Cohesion: 0.50
Nodes (4): Method, Envelopes, Fifty, Zero

### Community 37 - "Reports.kt"
Cohesion: 0.16
Nodes (27): MerchantTotal, weekColumnOf(), weekdayInitials(), YearMonth, monthTitle(), paint(), writeReportPdf(), BalanceTrace() (+19 more)

### Community 38 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 43 - "rememberBiometricPrompt"
Cohesion: 0.43
Nodes (5): biometricAvailable(), findFragmentActivity(), rememberBiometricPrompt(), BiometricPrompt, FragmentActivity

### Community 44 - "LedgerRange"
Cohesion: 0.33
Nodes (5): LedgerRange, Cycle, Everything, Quarter, Year

### Community 46 - "Txn"
Cohesion: 0.16
Nodes (10): challengesFor(), fiftyTwoWeekChallenge(), leanRunChallenge(), medianOf(), savedByIsoWeek(), spendByDay(), weekendChallenge(), Challenge (+2 more)

### Community 47 - "Connecting the three services that are not started"
Cohesion: 0.18
Nodes (10): 0. The one thing all three need first, 1. Gmail receipt import, 2. Google Drive backup, 3. Firebase sync and shared wallets, Connecting the three services that are not started, Steps, Steps, Steps (+2 more)

### Community 49 - "Kelp.kt"
Cohesion: 0.60
Nodes (5): KelpBed(), Dp, Modifier, State, rememberSway()

## Ambiguous Edges - Review These
- `Emergency Fund Savings Goal` → `Portrait 4:5 Hero Crop for Goal Cards`  [AMBIGUOUS]
  app/src/main/res/drawable-nodpi/goal_emergency.jpg · relation: conceptually_related_to
- `Bali Travel Savings Goal` → `Goal Bali Cover Image`  [AMBIGUOUS]
  app/src/main/res/drawable-nodpi/goal_bali.jpg · relation: references
- `Laptop Purchase Goal (filename-implied subject)` → `goal_laptop.jpg (moody beach and mountain landscape)`  [AMBIGUOUS]
  app/src/main/res/drawable-nodpi/goal_laptop.jpg · relation: references

## Knowledge Gaps
- **50 isolated node(s):** `System`, `Dark`, `Light`, `In`, `Out` (+45 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **12 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Emergency Fund Savings Goal` and `Portrait 4:5 Hero Crop for Goal Cards`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Bali Travel Savings Goal` and `Goal Bali Cover Image`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `Laptop Purchase Goal (filename-implied subject)` and `goal_laptop.jpg (moody beach and mountain landscape)`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **Why does `LedgerState` connect `LedgerState` to `Money.kt`, `MoneyViewModel`, `MoneyRepository`, `Reports.kt`, `Txn`, `MoneyApp.kt`, `MoneyRepository.kt`?**
  _High betweenness centrality (0.135) - this node is a cross-community bridge._
- **Why does `Flow` connect `Flow` to `Money.kt`, `LedgerState`, `MoneyViewModel`, `TemplateDao`, `MoneyApp.kt`, `AssetDao`, `MoneyRepository.kt`, `BillDao`, `BudgetDao`, `AccountEntity`, `CategoryEntity`, `DebtDao`, `GoalDao`, `LedgerQuery`?**
  _High betweenness centrality (0.078) - this node is a cross-community bridge._
- **Why does `Txn` connect `Txn` to `Money.kt`, `LedgerState`, `StatementImportTest`, `MoneyRepository`, `RatesStore`, `LedgerQuery`?**
  _High betweenness centrality (0.061) - this node is a cross-community bridge._
- **Are the 28 inferred relationships involving `LedgerState` (e.g. with `.`a bill already paid this month is not still committed`()` and `.`a bill in a currency with no rate is named rather than dropped in silence`()`) actually correct?**
  _`LedgerState` has 28 INFERRED edges - model-reasoned connections that need verification._