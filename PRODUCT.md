# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

Kotlin + Jetpack Compose (Material 3), confirmed by the user via the README's suggested stack.

What the scaffold actually depends on today: Compose BOM, Material 3, Material Icons Extended,
Navigation Compose, Activity Compose, Lifecycle. Nothing else. Charts are drawn on `Canvas`
rather than pulled from Vico or MPAndroidChart, because the design calls for a ring with labels
on the arc and a mirrored net-worth sounding that neither library draws, and because a chart
library brings its own house style into a committed visual world.

Planned but deliberately not present yet, because this is a UI scaffold and each would only be
boilerplate to rewrite once real data exists: Room, Hilt, WorkManager, the Google Drive backup
path, and Firebase. The README remains the authority on where they land.

Toolchain note: Android SDK 35/36 is installed at `%LOCALAPPDATA%\Android\Sdk`, but the system
JDK is 15, which AGP 8.7 rejects, and no AVD or system image is installed. Android Studio (which
bundles its own JDK) is the expected build and run environment.

## Users

One person tracking their own money day to day, plus (later) the family or household they
share a wallet with. The core moment is the 10-second one: they just spent money, they are
standing somewhere, and they need the transaction logged before they forget it. The second
moment is reflective: end of week or month, sitting down to see where the money went and
whether the budget held.

Distribution intent is a real Google Play release (the $25 registration is the only
budgeted cost), so the app must satisfy Play policy, not just personal use.

## Product Purpose

Track expenses, income, bills, subscriptions, budgets, and net worth in one place, entirely
on-device by default. Success is that the user keeps logging: the record stays complete
enough that the reports are worth trusting, and the budget numbers reflect reality.

## Positioning

Everything a paid finance app does, at permanently zero running cost, with no bank
aggregation. Competitors buy completeness with per-account aggregator fees; this one earns
it with fast manual entry plus free capture paths — on-device receipt OCR, Gmail receipt
parsing, and bank statement import. Local-first is the mechanism, not a limitation: the app
is fully functional with the network off, and sync is an opt-in layer on the free Firebase
Spark tier.

## Operating Context

- Android phone, one-handed, frequently in public and in a hurry; thumb-reach matters.
- Entry is manual first. Capture assists (OCR, email, statement import) always pre-fill for
  confirmation and never post silently.
- Offline is a normal state, not an error state.
- Money is the user's own real money: wrong numbers are the worst possible failure.

## Capabilities and Constraints

Confirmed feature areas (all eleven from README.md are in scope for this scaffold):
transactions, accounts/wallets, budgets, bills & subscriptions, reports & insights, goals &
savings, multi-currency, security & sync, receipt scanning & auto-import, customization,
gamification.

MVP v1 (must be real, not placeholder): manual transactions with categories, multiple
accounts + transfers, monthly category budgets with progress, category pie + income/expense
trend, recurring bill reminders, PIN/biometric app lock, local storage only.

Deferred to v2+ but designed now: multi-currency, Firebase sync/shared wallets, cloud
backup, debt payoff, widgets, receipt OCR, email/statement import, gamification.

Hard constraints:
- $0 to build and run, forever, except the one-time $25 Play registration. Any feature
  needing a paid service is cut, not deferred.
- Bank-account aggregation (Plaid/Yodlee) is permanently excluded.
- SMS auto-read is permanently excluded: Play policy restricts READ_SMS/RECEIVE_SMS to apps
  whose core function is SMS handling; a finance app requesting it gets rejected.
- Gamification is a layer over real data. Every badge, streak, and XP event maps to an
  actual transaction, budget, or goal action. No standalone gamification state that can
  drift from the user's real finances.
- Leaderboards are opt-in, off by default, and never expose raw balances.

Terminology: "accounts" (also called wallets) hold money; transfers between them are
excluded from income and expense totals.

## Brand Commitments

Working name: Money Manager. No logo, wordmark, brand palette, or voice guide exists yet —
none may be fabricated as pre-existing.

Binding visual constraint from the user: the visual world follows `UI sample/UI 5.jpg` —
deep blue, layered, glassy, with a hero ring chart and translucent stacked cards.

## Evidence on Hand

- `README.md` — the full feature spec, cost analysis, and MVP scope. The authority on what
  the product is.
- `UI sample/UI 1–5.jpg` — collected finance-app UI references. UI 5 is binding; the other
  four are taste evidence only.
- No users, no reviews, no screenshots of a shipped build, no benchmarks, no press. None of
  these may be invented in any surface.

## Product Principles

1. **The number must be right before it is beautiful.** Money data is never rounded,
   estimated, or animated into ambiguity.
2. **Logging must survive a hurry.** The add-transaction path is the most optimized path in
   the app; everything else can afford a tap.
3. **Free is a design constraint, not a footnote.** No feature may assume a paid service.
4. **Offline is the default state.** Nothing critical degrades without a network.
5. **Nothing posts to the ledger without the user confirming it.** Automation pre-fills;
   the user commits.

## Accessibility & Inclusion

Android platform expectations apply: dynamic type must not clip money values, touch targets
respect the 48dp minimum, colour is never the only carrier of the income/expense
distinction, and the app must remain usable one-handed. Dark and light themes both ship.
