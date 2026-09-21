package com.moneymanager.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek

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

    /** Warn before a logging streak lapses. Separate from bills: a different kind of nag. */
    var streakWarnings: Boolean
        get() = prefs.getBoolean(KEY_STREAK_WARN, false)
        set(value) = prefs.edit().putBoolean(KEY_STREAK_WARN, value).apply()

    // --- Appearance ----------------------------------------------------------------------------

    /** One of [ThemeMode]. Stored by name so a new mode does not renumber the old ones. */
    var themeMode: ThemeMode
        get() = ThemeMode.of(prefs.getString(KEY_THEME, null))
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    /**
     * Material You for the Material surfaces. Off by default, and deliberately so: the depth
     * ladder is what makes the waterline legible, and a wallpaper-derived scheme reorders it.
     * The authored water palette is kept even when this is on -- see `MoneyManagerTheme`.
     */
    var materialYou: Boolean
        get() = prefs.getBoolean(KEY_MATERIAL_YOU, false)
        set(value) = prefs.edit().putBoolean(KEY_MATERIAL_YOU, value).apply()

    /** The growing bed on the Progress screen. Skippable, as the README asks. */
    var showGrowth: Boolean
        get() = prefs.getBoolean(KEY_GROWTH, true)
        set(value) = prefs.edit().putBoolean(KEY_GROWTH, value).apply()

    // --- Round-ups ------------------------------------------------------------------------------

    var roundUpEnabled: Boolean
        get() = prefs.getBoolean(KEY_ROUNDUP, false)
        set(value) = prefs.edit().putBoolean(KEY_ROUNDUP, value).apply()

    /** Minor units to round each spend up to. 100 is the nearest whole unit. */
    var roundUpToMinor: Int
        get() = prefs.getInt(KEY_ROUNDUP_TO, 100)
        set(value) = prefs.edit().putInt(KEY_ROUNDUP_TO, value).apply()

    /** Which goal a sweep feeds. Empty until the user picks one. */
    var roundUpGoalId: String
        get() = prefs.getString(KEY_ROUNDUP_GOAL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_ROUNDUP_GOAL, value).apply()

    /**
     * The day of the last sweep, or null if there has never been one.
     *
     * Stored because it is a fact about something that happened, not a derived total. Without it
     * the same change would be offered again every time the screen opened.
     */
    var roundUpSweptOn: java.time.LocalDate?
        get() = prefs.getLong(KEY_ROUNDUP_SWEPT, -1L).takeIf { it >= 0 }
            ?.let(java.time.LocalDate::ofEpochDay)
        set(value) = prefs.edit().putLong(KEY_ROUNDUP_SWEPT, value?.toEpochDay() ?: -1L).apply()

    // --- Periods -------------------------------------------------------------------------------

    /**
     * Day of the calendar month a budget cycle opens on, or [Periods.PAYDAY] to take it from the
     * ledger's own income rows. Anything else is clamped to 1..28 by [Periods].
     */
    var monthStartDay: Int
        get() = prefs.getInt(KEY_MONTH_START, 1)
        set(value) = prefs.edit().putInt(KEY_MONTH_START, value).apply()

    /** Which column the calendars start on, and where a week is cut for weekly figures. */
    var weekStart: DayOfWeek
        get() = DayOfWeek.of(prefs.getInt(KEY_WEEK_START, DayOfWeek.MONDAY.value))
        set(value) = prefs.edit().putInt(KEY_WEEK_START, value.value).apply()

    private val _periods = MutableStateFlow(PeriodSettings(monthStartDay, weekStart))

    /**
     * The period settings as a stream.
     *
     * The repository folds this into the ledger the same way it folds the base currency in, and
     * for the same reason: without it, moving the start of the month would change a stored
     * preference and nothing the user can see until the next cold start.
     */
    val periods: StateFlow<PeriodSettings> = _periods.asStateFlow()

    /** Write both period settings, refresh the snapshot, and wake everything reading it. */
    fun setPeriods(monthStartDay: Int, weekStart: DayOfWeek) {
        this.monthStartDay = monthStartDay
        this.weekStart = weekStart
        publishPeriods()
    }

    /** Push both period settings into the snapshot the screens and the repository read. */
    fun publishPeriods() {
        Periods.monthStartDay = monthStartDay
        Periods.weekStart = weekStart
        _periods.value = PeriodSettings(monthStartDay, weekStart)
    }

    private companion object {
        const val KEY_REMINDERS = "reminders_enabled"
        const val KEY_LEAD = "remind_days_before"
        const val KEY_HOUR = "remind_hour"
        const val KEY_STREAK_WARN = "streak_warnings"
        const val KEY_THEME = "theme_mode"
        const val KEY_MATERIAL_YOU = "material_you"
        const val KEY_MONTH_START = "month_start_day"
        const val KEY_WEEK_START = "week_start"
        const val KEY_GROWTH = "show_growth"
        const val KEY_ROUNDUP = "round_up_enabled"
        const val KEY_ROUNDUP_TO = "round_up_to_minor"
        const val KEY_ROUNDUP_GOAL = "round_up_goal"
        const val KEY_ROUNDUP_SWEPT = "round_up_swept_epoch_day"
    }
}

enum class ThemeMode(val label: String) {
    System("Follow the system"), Dark("Always dark"), Light("Always light");

    companion object {
        fun of(name: String?) = entries.firstOrNull { it.name == name } ?: System
    }
}
