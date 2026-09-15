package com.moneymanager.data

import java.time.LocalTime

/*
 * A month of invented activity, for looking at the design with realistic density behind it.
 *
 * This is not what a new install gets. A finance app that opens full of spending that never
 * happened teaches the user to distrust every number on it, so a fresh database seeds categories
 * and one cash account and nothing else. This runs only when someone asks for it from Settings,
 * and everything it writes is ordinary data the user can delete.
 *
 * Every name and figure below is invented. Dates are relative to today so the demo always looks
 * like the current month.
 */
object DemoData {

    suspend fun install(repo: MoneyRepository) {
        // Accounts beyond the seeded cash one.
        repo.upsertAccount(
            Account("acc_everyday", "Everyday", AccountKind.Bank, 0, "USD"), 1_284_12,
        )
        repo.upsertAccount(
            Account("acc_card", "Visa ending 4417", AccountKind.Card, 0, "USD", limitMinor = 3_000_00),
            0,
        )
        repo.upsertAccount(
            Account("acc_wallet", "GoPay", AccountKind.Wallet, 0, "USD"), 41_05,
        )
        repo.upsertAccount(
            Account("acc_emergency", "Emergency fund", AccountKind.Savings, 0, "USD"), 4_120_00,
        )

        suspend fun log(
            merchant: String,
            category: String,
            account: String,
            amount: Long,
            daysAgo: Long,
            hour: Int,
            minute: Int,
            flow: Flow = Flow.Out,
            note: String? = null,
            tags: List<String> = emptyList(),
            splits: List<Split> = emptyList(),
        ) = repo.saveTransaction(
            merchant = merchant,
            categoryId = category,
            accountId = account,
            amountMinor = amount,
            flow = flow,
            date = today.minusDays(daysAgo),
            time = LocalTime.of(hour, minute),
            note = note,
            tags = tags,
            splits = splits,
        )

        log("Kopi Kenangan", "food", "acc_wallet", 3_20, 0, 8, 41)
        log("Transjakarta", "transport", "acc_wallet", 1_40, 0, 7, 55)
        log(
            "Superindo", "groceries", "acc_everyday", 48_16, 0, 19, 12,
            note = "Week's shop plus birthday candles",
            tags = listOf("household"),
            splits = listOf(Split("groceries", -41_16), Split("fun", -7_00)),
        )
        log("Spotify", "subs", "acc_card", 5_99, 1, 3, 0)
        log("Warung Bu Ida", "food", "acc_cash", 2_75, 1, 12, 30)
        log("Shell", "transport", "acc_card", 34_00, 1, 17, 48)
        log("Apotek K-24", "health", "acc_everyday", 12_90, 2, 10, 5, tags = listOf("reimbursable"))
        log("Tavi Studio", "income", "acc_everyday", 640_00, 2, 9, 0, Flow.In, "Freelance invoice")
        log("Gramedia", "fun", "acc_card", 18_40, 3, 16, 22)
        log("Bolt", "transport", "acc_card", 9_12, 3, 21, 9, tags = listOf("trip:portugal"))
        log("PLN listrik", "utilities", "acc_everyday", 31_25, 4, 11, 0)
        log("Petshop Ceria", "pets", "acc_cash", 14_60, 4, 18, 40)
        log("Norwood Ltd", "income", "acc_everyday", 2_450_00, 5, 6, 0, Flow.In, "Monthly salary")
        log("Indomaret", "groceries", "acc_cash", 6_35, 6, 20, 15)
        log("Netflix", "subs", "acc_card", 15_49, 7, 3, 0)
        log("Kopi Kenangan", "food", "acc_wallet", 3_20, 7, 8, 38)
        log("Sewa kontrakan", "home", "acc_everyday", 620_00, 8, 9, 0)

        // Envelopes for the current month.
        repo.setBudget("home", 760_00)
        repo.setBudget("food", 360_00)
        repo.setBudget("groceries", 260_00)
        repo.setBudget("transport", 220_00)
        repo.setBudget("utilities", 120_00)
        repo.setBudget("fun", 140_00, rollsOver = true)
        repo.setBudget("health", 90_00)
        repo.setBudget("subs", 60_00)
        repo.setBudget("pets", 50_00)

        // Bills and subscriptions.
        repo.upsertBill(Bill("b1", "Sewa kontrakan", 620_00, today.plusDays(22), Recurrence.Monthly, "acc_everyday"))
        repo.upsertBill(Bill("b2", "Internet IndiHome", 34_90, today.plusDays(6), Recurrence.Monthly, "acc_everyday"))
        repo.upsertBill(Bill("b3", "Netflix", 15_49, today.plusDays(9), Recurrence.Monthly, "acc_card", subscription = true))
        repo.upsertBill(Bill("b4", "iCloud 2 TB", 9_99, today.plusDays(11), Recurrence.Monthly, "acc_card", subscription = true))
        repo.upsertBill(Bill("b5", "Gym Fit Hub", 29_00, today.plusDays(14), Recurrence.Monthly, "acc_card", subscription = true))
        repo.upsertBill(Bill("b6", "Adobe Photography", 11_99, today.plusDays(18), Recurrence.Monthly, "acc_card", subscription = true))
        repo.upsertBill(Bill("b7", "Motor insurance", 184_00, today.plusDays(24), Recurrence.Quarterly, "acc_everyday"))
        repo.upsertBill(Bill("b8", "Spotify", 5_99, today.minusDays(1), Recurrence.Monthly, "acc_card", subscription = true))

        // Goals.
        repo.upsertGoal(Goal("g1", "Emergency fund", 6_000_00, 4_120_00, today.plusMonths(6), "acc_emergency"))
        repo.upsertGoal(Goal("g2", "New laptop", 1_400_00, 385_00, today.plusMonths(3), "acc_emergency"))
        repo.upsertGoal(Goal("g3", "Trip to Bali", 900_00, 210_00, today.plusMonths(9), "acc_emergency"))
    }
}
