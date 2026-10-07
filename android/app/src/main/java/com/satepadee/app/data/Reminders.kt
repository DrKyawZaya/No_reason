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
 * (and tomorrow's vegetarian-day note) and schedules the next day. Uses an exact alarm when
 * the phone allows it (always below Android 12), otherwise an inexact one that may run late.
 */
object Reminders {
    private const val CHANNEL = "daily"
    private const val NOTIFICATION_ID = 1

    fun prefs(context: Context) = context.getSharedPreferences("satepadee", Context.MODE_PRIVATE)

    private const val EXTRA_RECITATION = "recitation"
    private const val K_SCHEDULED = "scheduledRecitationAlarms"

    /** Sets (or cancels) the ကိုးနဝင်း alarm and one alarm per ပုတီး that has a reminder. */
    fun reschedule(context: Context, state: AppState) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val main = alarmIntent(context, null)
        alarms.cancel(main)
        if (state.reminderOn) setAlarm(alarms, nextTrigger(state.reminderMinutes), main)

        // Cancel alarms of ပုတီး that were deleted or switched off, then set the current ones.
        val prefs = prefs(context)
        val before = prefs.getString(K_SCHEDULED, "").orEmpty().split(',').filter { it.isNotEmpty() }
        before.forEach { alarms.cancel(alarmIntent(context, it)) }
        val now = state.recitations.filter { it.reminderOn }
        now.forEach { setAlarm(alarms, nextTrigger(it.reminderMinutes), alarmIntent(context, it.id)) }
        prefs.edit().putString(K_SCHEDULED, now.joinToString(",") { it.id }).apply()
    }

    private fun setAlarm(alarms: AlarmManager, at: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun nextTrigger(minutes: Int): Long {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60); set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= System.currentTimeMillis() + 1000) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    /** Each ပုတီး gets its own alarm (distinct action and request code); null is the ကိုးနဝင်း alarm. */
    private fun alarmIntent(context: Context, recitationId: String?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        if (recitationId != null) intent.setAction("reminder:$recitationId").putExtra(EXTRA_RECITATION, recitationId)
        return PendingIntent.getBroadcast(
            context, recitationId?.hashCode() ?: 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun recitationIdOf(intent: Intent): String? = intent.getStringExtra(EXTRA_RECITATION)

    /** Message for one ပုတီး; null when today's rounds are already done. */
    fun message(model: AppModel, recitationId: String): Pair<String, String>? {
        val r = model.recitation(recitationId) ?: return null
        val p = model.recitationProgress(r)
        if (p.rounds >= r.dailyRounds) return null
        val text = "ယနေ့ ${Mm.n(p.rounds)} / ${Mm.n(r.dailyRounds)} ပတ်" + if (r.text.isNotBlank()) " · ${r.text}" else ""
        return r.name to text
    }

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

    fun post(context: Context, recitationId: String? = null) {
        val model = AppModel(prefs(context), Content.load(context))
        val (title, text) = (if (recitationId != null) message(model, recitationId) else message(model)) ?: return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(CHANNEL, "နေ့စဉ် သတိပေးချက်", NotificationManager.IMPORTANCE_DEFAULT))
        // A ပုတီး reminder opens its counter; the ကိုးနဝင်း reminder opens Home (so a new stage still asks for the wish).
        val launch = Intent(context, MainActivity::class.java)
        recitationId?.let { launch.putExtra(MainActivity.EXTRA_TARGET, Target.Custom(it).key) }
        val open = PendingIntent.getActivity(
            context, recitationId?.hashCode() ?: 0,
            launch
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_bead)
            .setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open).setAutoCancel(true)
            // Nothing private in a reminder, so show the full text on the lock screen.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(context).notify(recitationId?.hashCode() ?: NOTIFICATION_ID, n)
    }
}

/**
 * Xiaomi (MIUI/HyperOS) and similar phones stop alarms for apps without "Autostart". Opens the
 * Autostart list on Xiaomi, otherwise this app's system settings page.
 */
fun openReminderHelpSettings(context: Context) {
    val autostart = Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
    val appDetails = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null))
    for (intent in listOf(autostart, appDetails)) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return
        } catch (_: Exception) { }
    }
}

val isXiaomiFamily: Boolean get() = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.post(context, Reminders.recitationIdOf(intent))
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
