package com.moneymanager.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.moneymanager.MainActivity
import com.moneymanager.R
import com.moneymanager.data.db.MoneyDatabase
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/*
 * Bill reminders, entirely on the device.
 *
 * The README lists Firebase Cloud Messaging against this row, but a reminder about a bill the app
 * already knows about needs no server to originate it: WorkManager wakes up once a day, reads the
 * local database, and posts a notification. That works with the network off, costs nothing, and
 * sends nothing about your bills anywhere. FCM would be the right tool only for something the
 * phone could not have known by itself.
 */

private const val CHANNEL_ID = "bill_reminders"
private const val WORK_NAME = "bill-reminders"

/** Created once at startup. Posting to a channel that does not exist silently drops the message. */
fun createReminderChannel(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "Bill reminders",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "A few days before a bill or subscription is due."
        setShowBadge(true)
    }
    NotificationManagerCompat.from(context).createNotificationChannel(channel)
}

/** Whether the app may actually post. False on Android 13+ until the user says yes. */
fun canNotify(context: Context): Boolean =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

/**
 * Schedules the daily check, or cancels it.
 *
 * The first run is aimed at the user's chosen hour. WorkManager will not hit it to the second --
 * it batches work to save battery, which is the trade that makes background work acceptable at
 * all -- so this is "that morning", not "09:00:00".
 */
fun scheduleReminders(context: Context, prefs: AppPrefs) {
    val work = WorkManager.getInstance(context)
    if (!prefs.remindersEnabled) {
        work.cancelUniqueWork(WORK_NAME)
        return
    }
    work.enqueueUniquePeriodicWork(
        WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        PeriodicWorkRequestBuilder<BillReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilHour(prefs.remindHour), TimeUnit.MILLISECONDS)
            .build(),
    )
}

private fun millisUntilHour(hour: Int): Long {
    val now = LocalDateTime.now()
    var next = now.toLocalDate().atTime(LocalTime.of(hour, 0))
    if (!next.isAfter(now)) next = next.plusDays(1)
    return Duration.between(now, next).toMillis().coerceAtLeast(0)
}

/**
 * Reads the bills due inside the reminder window and says so, once per bill.
 *
 * The notification id is derived from the bill id, so tomorrow's run replaces today's notification
 * for the same bill instead of stacking a new one every morning until it is paid.
 */
class BillReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = AppPrefs(applicationContext)
        if (!prefs.remindersEnabled || !canNotify(applicationContext)) return Result.success()

        val db = MoneyDatabase.get(applicationContext)
        val today = LocalDate.now()
        val horizon = today.plusDays(prefs.remindDaysBefore.toLong())
        val due = db.bills().dueBetween(today.minusDays(30).toEpochDay(), horizon.toEpochDay())

        val manager = NotificationManagerCompat.from(applicationContext)
        due.forEach { bill ->
            val when_ = LocalDate.ofEpochDay(bill.dueEpochDay)
            val overdue = when_.isBefore(today)
            // Something a month overdue has been seen and ignored; nagging daily forever is how a
            // user turns every notification from this app off.
            if (overdue && java.time.temporal.ChronoUnit.DAYS.between(when_, today) > 7) return@forEach

            manager.notifySafely(
                applicationContext,
                id = bill.id.hashCode(),
                title = if (overdue) "${bill.name} is overdue" else "${bill.name} is due ${dueLabel(when_).lowercase()}",
                body = "${money(bill.amountMinor)} from ${Accounts[bill.accountId].name.ifEmpty { "your account" }}",
            )
        }
        return Result.success()
    }
}

private fun NotificationManagerCompat.notifySafely(
    context: Context,
    id: Int,
    title: String,
    body: String,
) {
    if (!canNotify(context)) return
    val open = android.app.PendingIntent.getActivity(
        context,
        id,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(body)
        .setContentIntent(open)
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .build()
    try {
        notify(id, notification)
    } catch (_: SecurityException) {
        // The permission can be revoked between the check above and here. Losing a reminder is
        // the correct outcome; crashing in a background worker is not.
    }
}
