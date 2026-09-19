package com.moneymanager.data

import android.content.Context

/**
 * Small settings that are not security and not ledger data.
 *
 * Plain [android.content.SharedPreferences]: these are a handful of primitives read at startup
 * and on a settings screen. A DataStore would buy asynchronous reads for values that are already
 * in memory before the first frame.
 */
class AppPrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("prefs", Context.MODE_PRIVATE)

    /** Off until the user turns it on, because it needs a permission and an opinion. */
    var remindersEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDERS, false)
        set(value) = prefs.edit().putBoolean(KEY_REMINDERS, value).apply()

    /** How many days before a bill is due to say something. */
    var remindDaysBefore: Int
        get() = prefs.getInt(KEY_LEAD, 2)
        set(value) = prefs.edit().putInt(KEY_LEAD, value.coerceIn(0, 14)).apply()

    /** Hour of the day to check, 24-hour. Morning by default: a bill you learn about at 11pm. */
    var remindHour: Int
        get() = prefs.getInt(KEY_HOUR, 9)
        set(value) = prefs.edit().putInt(KEY_HOUR, value.coerceIn(0, 23)).apply()

    private companion object {
        const val KEY_REMINDERS = "reminders_enabled"
        const val KEY_LEAD = "remind_days_before"
        const val KEY_HOUR = "remind_hour"
    }
}
