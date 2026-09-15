# Money Manager — Android Expense & Finance Tracker

A comprehensive personal finance app for Android: track expenses, income, bills, subscriptions, and net worth in one place. Feature set below is synthesized from research on Wallet by BudgetBakers, Spendee, YNAB, PocketGuard, Monefy, and Goodbudget (2026).

> **Hard constraint: $0 to build and run, forever — the only real-money cost anywhere in this project is the one-time Google Play $25 registration fee.** Every tool/API choice below was picked to satisfy this, and any feature that inherently requires a paid service (see "Cost to Build" below) is cut, not just deferred.

## Core Features

### 1. Transactions
- Quick-add expense/income (amount, category, account, date, note, photo of receipt)
- Manual entry as the primary path; bank-aggregator sync (Plaid/Yodlee) **excluded permanently** — those charge per linked account, which breaks the zero-cost constraint. Receipt OCR + email + statement import (§9) cover the same "catch every transaction" goal for free.
- Split transactions across multiple categories
- Custom categories & subcategories with icons/colors
- Tags for cross-category grouping (e.g. "Trip to Bali")
- Search & filter by date, category, account, amount range

### 2. Accounts ("Appliances" / Wallets)
- Multiple accounts: cash, bank, credit card, e-wallet, savings
- Track physical assets/appliances as items with purchase price, warranty date, depreciation (optional)
- Transfer money between accounts (excluded from income/expense totals)
- Per-account balance and running history

### 3. Budgets
- Monthly/weekly budgets per category
- Envelope budgeting method (Goodbudget-style)
- 50/30/20 rule template
- Zero-based budgeting ("give every dollar a job", YNAB-style)
- Rollover unused budget to next period (optional)
- Real-time "safe to spend" indicator (PocketGuard-style)

### 4. Bills & Subscriptions
- Recurring bill tracker (rent, utilities, loans)
- Subscription manager with renewal reminders
- Calendar view of upcoming due dates
- Flag unused/forgotten subscriptions
- Push notifications before due date

### 5. Reports & Insights
- Spending by category (pie/donut chart)
- Income vs. expense trend (line/bar chart, monthly/yearly)
- Net worth over time (assets − liabilities)
- Cash flow calendar heatmap
- Top merchants / biggest expenses
- Exportable reports (CSV/PDF)

### 6. Goals & Savings
- Savings goals with target amount & date (e.g. "Emergency Fund")
- Debt payoff tracker (snowball/avalanche method)
- Progress bar per goal, linked to a dedicated account

### 7. Multi-Currency
- Per-transaction currency with auto-converted base currency
- Live exchange rates via a free API (Frankfurter — no key, no limit — or exchangerate-api.com free tier); manual override always available offline

### 8. Security & Sync
- App lock (PIN/biometric)
- Local-first storage (Room DB) — the app fully works with zero network access
- Backup/restore via Google Drive App Data folder (free, tied to the user's own Drive quota)
- Multi-device sync & shared wallets: Firebase **Spark plan (free, no credit card, no time limit)** — Firestore + free Auth cover a personal/family-scale wallet well within the free quota (1 GiB storage, 50k reads/20k writes per day). No paid tier needed at this app's scale.

### 9. Receipt Scanning & Auto-Import
- **Receipt OCR:** snap/upload a photo → on-device OCR extracts merchant, date, total, line items → pre-fills a transaction for user confirmation (never auto-posts silently)
- **Email receipt import:** connect Gmail via Gmail API (read-only, user-consented OAuth) → detect order/invoice emails → extract amount/merchant automatically
- **Bank/credit card statement import:** upload CSV/PDF/XLSX export from the bank → parse rows → map to transactions, dedupe against existing entries
- **Merchant-based auto-categorization:** learns category from merchant name over time (e.g. "Starbucks" → Food & Drink)
- **Duplicate/match detection:** if a receipt scan matches an already-imported statement line, merge instead of double-counting

> ⚠️ **SMS auto-read is not viable for a Play Store release.** Google Play's permissions policy restricts `READ_SMS`/`RECEIVE_SMS` to apps whose *core function* is SMS handling (default SMS/dialer/Assistant apps); a finance app requesting it is routinely rejected. Skip it — cover the same "auto-detect income/outcome" need with email parsing + statement import above, which are policy-safe and don't need a sensitive-permission declaration.

### 10. Customization
- Light/dark theme
- Widget: today's spend, balance, upcoming bills
- Recurring transaction templates (auto-log rent, salary)
- Custom start-of-month/week

### 11. Gamification
- **Streaks:** consecutive days of logging a transaction or staying under budget; streak-break warnings ("don't lose your 12-day streak")
- **Achievement badges:** milestones like "First 50 Transactions Logged", "3 Months Under Budget", "Debt-Free", "Savings Goal Hit" — tied to real behaviors, not vanity taps
- **Levels / XP:** small XP for logging expenses, hitting budgets, clearing bills on time; level-up unlocks cosmetic themes/icons (no pay-to-win)
- **Savings challenges:** built-in templates like "No-Spend Weekend", "52-Week Challenge", round-up-and-save rules (Qapital-style: "save $2 every time I log a coffee expense")
- **Visual progress:** goal progress bars, a simple avatar/city/plant that grows as savings goals or budget-adherence streaks improve (Fortune City-style, optional and skippable)
- **Friendly leaderboard (opt-in only):** compare savings-rate or streak length with friends/family who share a wallet — never raw balances, and off by default for privacy

> Keep it a layer on top of real data, not a separate system: every badge/streak/XP event must map to an actual transaction, budget, or goal action already in the model — no gamification state that can drift from the user's real finances.

## Feature Comparison Reference

| App | Standout Feature |
|---|---|
| YNAB | Zero-based "give every dollar a job" budgeting |
| Spendee | Clean visual charts, shared wallets |
| Wallet (BudgetBakers) | Affordable multi-account tracking |
| PocketGuard | Auto bank sync, subscription/bill negotiation, "safe to spend" |
| Monefy | Fast manual entry, simple visual breakdown |
| Goodbudget | Digital envelope budgeting |

## Suggested Tech Stack (Android)

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Local DB:** Room (SQLite)
- **Charts:** Vico or MPAndroidChart
- **Architecture:** MVVM + Repository pattern
- **DI:** Hilt
- **Backup:** Google Drive API (optional, v2)
- **Notifications:** WorkManager for recurring bill reminders
- **Receipt OCR:** ML Kit Text Recognition v2 (on-device, free, no API cost/latency)
- **Email import:** Gmail API (read-only OAuth scope) + a rules/regex or lightweight ML parser for receipt emails
- **Statement import:** CSV via OpenCSV/Kotlin stdlib split; PDF via PdfBox-Android if needed later

## Cost to Build ($0, except the one-time Play Store $25 registration)

Every single feature in this doc is covered by a free option — nothing needs a credit card:

| Piece | Free option used |
|---|---|
| App framework/DB/DI/notifications | Kotlin, Compose, Room, Hilt, WorkManager — free official tooling |
| Charts | Vico / MPAndroidChart — open source |
| Receipt OCR | ML Kit Text Recognition — on-device, no API key, no per-call fee |
| Email receipt import | Gmail API — free at personal-scale quota |
| Statement import | CSV/XLSX parsed on-device — no service at all |
| Multi-currency FX rates | Frankfurter API (free, unlimited, no key) |
| Backup | Google Drive App Data folder — free, uses the user's own quota |
| Multi-device sync & shared wallets | Firebase **Spark plan** — permanently free tier, not a trial |
| Bill reminders | Firebase Cloud Messaging — free, unlimited |
| Gamification (streaks/badges/XP) | Pure client-side logic over local data — no service |

**Permanently excluded because it cannot be free at any scale:** bank-account aggregation (Plaid/Yodlee bill per linked account, no free tier that suits a shipped consumer app). Everything else that touches money data stays on-device or on free-tier Google/Firebase services.

**Watch-out, not a cost today:** Firebase Spark and Gmail/Drive free quotas are generous for one person or one family, but they are *quota-capped, not contractually free forever regardless of usage* — if the app ever gets thousands of users sharing your Firebase project, it could cross into Firebase's paid Blaze tier. Not a concern for personal/self-use, worth knowing before ever distributing it beyond that.

## MVP Scope (v1)

Ship the smallest useful version first, expand after:

1. Manual transactions (add/edit/delete) with categories
2. Multiple accounts + transfers
3. Monthly budget per category with progress bars
4. Category spending pie chart + income/expense trend
5. Recurring bill reminders (local notifications)
6. PIN/biometric app lock
7. Local storage only (no cloud sync yet)

**Deferred to v2+ (still free, just not in v1):** multi-currency, Firebase-based multi-device sync/shared wallets, cloud backup, debt payoff tracker, widgets, receipt OCR, email/statement import, gamification (streaks/badges/challenges).

**Permanently cut, not deferred:** bank-account aggregation sync and bill negotiation — both require a paid third-party service and can't fit the $0 constraint at any version.

---
Sources:
- [SparkReceipt – AI Receipt Scanner](https://sparkreceipt.com/features/receipt-scanner/)
- [13 best receipt scanner apps in 2026 - Bill.com](https://www.bill.com/blog/best-receipt-scanning-app)
- [7 Expense Automation Tools That Match Receipts to Transactions - Navan](https://navan.com/blog/expense-automation-tools-match-receipts-transactions)
- [Best Apps to Track Receipts Automatically in 2026 | Finny Blog](https://getfinny.app/blog/best-apps-track-receipts-automatically-2026)
- [What is gamification for fintech apps and top examples | Plotline](https://www.plotline.so/blog/fintech-app-gamification-examples)
- [How to Gamify a Savings App: Mechanics, Examples - Trophy](https://trophy.so/blog/gamify-a-savings-app)
- [Gamification for Personal-Finance Apps - Trophy](https://trophy.so/blog/gamification-for-personal-finance-apps)
- [Top 10 Gamified Finance Apps in 2026 - Yu-kai Chou](https://yukaichou.com/gamification-examples/top-10-finance-apps-for-2017-from-an-octalysis-gamification-perspective/)
- [Best Personal Expense Tracker Apps in 2026 | Expensify](https://use.expensify.com/blog/personal-expense-tracker-apps)
- [Best Money Manager Apps in 2026 | Finny Blog](https://getfinny.app/blog/best-money-manager-apps-2026)
- [5 Best Expense Tracker Apps for Android in 2026 | Money Vault](https://aimoneyvault.app/resources/articles/best-expense-tracker-apps-android)
- [Best Budgeting Apps of 2026 – Forbes Advisor](https://www.forbes.com/financial-services/best-budgeting-apps-2/)
- [Best budgeting apps of 2026 - NerdWallet](https://www.nerdwallet.com/finance/learn/best-budget-apps)
- [Goodbudget: Budget & Finance - Google Play](https://play.google.com/store/apps/details?id=com.dayspringtech.envelopes&hl=en_US)
- [Best Expense Tracker Apps of 2026 - WalletHub](https://wallethub.com/edu/b/best-expense-tracker/155193)
- [PocketGuard – Budgeting Tool for Personal Finance](https://pocketguard.com/budgeting/)
- [Monefy - Best Free Budgeting Tools for 2025](https://www.monefy.com/article/best-free-budgeting-tools-2025)
