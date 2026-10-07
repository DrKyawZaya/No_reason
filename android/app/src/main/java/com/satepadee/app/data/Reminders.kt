package com.satepadee.app.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.satepadee.app.MainActivity
import com.satepadee.app.R
import java.util.Calendar

/**
 * One daily alarm at the user's chosen time. Each time it fires it posts today's reminder
 * (and tomorrow's vegetarian-day note) and schedules the next day. Inexact alarms need no
 * special permission and are accurate enough for a morning reminder.
 */
object Reminders {
    private const val CHANNEL = "daily"
    private const val NOTIFICATION_ID = 1

    fun prefs(context: Context) = context.getSharedPreferences("satepadee", Context.MODE_PRIVATE)

    fun reschedule(context: Context, state: AppState) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = alarmIntent(context)
        alarms.cancel(pi)
        if (!state.reminderOn) return
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger(state.reminderMinutes), pi)
    }

    private fun nextTrigger(minutes: Int): Long {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60); set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= System.currentTimeMillis() + 1000) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Builds the message from today's state; null when there is nothing to remind about. */
    fun message(model: AppModel): Pair<String, String>? {
        val day = model.todayDay()
        val index = model.dayIndex
        val tomorrowVegetarian = day != null && day.index + 1 < Kozawin.TOTAL_DAYS && Kozawin.day(day.index + 1).vegetarian
        val veg = if (model.state.vegetarianReminder && tomorrowVegetarian) "မနက်ဖြန် သက်သက်လွတ်နေ့ ဖြစ်ပါသည်။" else null
        return when {
            day != null && day.index !in model.state.done -> {
                val guna = model.content.guna(day.guna).pali
                "ကိုးနဝင်း · ${Mm.STAGES[day.stage]} အဆင့် · ရက် ${Mm.n(day.index + 1)}" to
                    listOfNotNull("ယနေ့ $guna · ${Mm.n(day.rounds)} ပတ်" + if (day.vegetarian) " · သက်သက်လွတ်နေ့" else "", veg).joinToString("\n")
            }
            index == -1 -> "ကိုးနဝင်း" to "မနက်ဖြန် တနင်္လာနေ့တွင် ကိုးနဝင်း စတင်ပါမည်။"
            veg != null -> "ကိုးနဝင်း" to veg
            else -> "စိပ်ပုတီး" to "ယနေ့ ပုတီးစိပ်ရန် အချိန်ရောက်ပါပြီ။"
        }
    }

    fun post(context: Context) {
        val model = AppModel(prefs(context), Content.load(context))
        val (title, text) = message(model) ?: return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(CHANNEL, "နေ့စဉ် သတိပေးချက်", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_bead)
            .setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open).setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.post(context)
        val model = AppModel(Reminders.prefs(context), Content.load(context))
        Reminders.reschedule(context, model.state)
    }
}

/** Alarms are cleared on reboot and on app update; set the next one again. */
class RescheduleReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val model = AppModel(Reminders.prefs(context), Content.load(context))
        Reminders.reschedule(context, model.state)
    }
}
