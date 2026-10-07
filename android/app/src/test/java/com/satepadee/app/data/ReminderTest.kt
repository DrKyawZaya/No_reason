package com.satepadee.app.data

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The daily alarm posts today's ကိုးနဝင်း reminder and schedules the next one. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReminderTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val nm get() = context.getSystemService(NotificationManager::class.java)
    private val alarms get() = context.getSystemService(AlarmManager::class.java)

    @Before fun setUp() {
        // Day 5 of ကိုးနဝင်း (လောကဝိဒူ, 5 rounds, vegetarian day); reminder on at 5:30.
        Reminders.prefs(context).edit().clear()
            .putLong("start", Days.today() - 4).putBoolean("reminderOn", true).putInt("reminderMinutes", 330)
            .commit()
    }

    @Test fun alarmPostsNotificationAndSchedulesNextDay() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ReminderReceiver().onReceive(context, Intent(context, ReminderReceiver::class.java))

        val posted = shadowOf(nm).allNotifications
        assertEquals(1, posted.size)
        val n = posted.single()
        assertEquals("ကိုးနဝင်း · ပထမ အဆင့် · ရက် ၅", n.extras.getString(NotificationCompat.EXTRA_TITLE))
        assertTrue(n.extras.getCharSequence(NotificationCompat.EXTRA_TEXT).toString().startsWith("ယနေ့ လောကဝိဒူ · ၅ ပတ် · သက်သက်လွတ်နေ့"))
        assertEquals(android.app.Notification.VISIBILITY_PUBLIC, n.visibility)

        val next = shadowOf(alarms).nextScheduledAlarm
        assertNotNull(next)
        assertTrue(next!!.triggerAtTime > System.currentTimeMillis())
        assertTrue(next.triggerAtTime - System.currentTimeMillis() <= 24 * 3600_000L)
    }

    @Test fun dayBeforeVegetarianDayMentionsIt() {
        Reminders.prefs(context).edit().putLong("start", Days.today() - 3).commit() // day 4; day 5 is vegetarian
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        Reminders.post(context)
        val text = shadowOf(nm).allNotifications.single().extras.getCharSequence(NotificationCompat.EXTRA_TEXT).toString()
        assertTrue(text, text.contains("မနက်ဖြန် သက်သက်လွတ်နေ့"))
    }

    @Test fun noNotificationWithoutPermission() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        Reminders.post(context)
        assertEquals(0, shadowOf(nm).allNotifications.size)
    }

    @Test fun turningOffCancelsAlarm() {
        val model = AppModel(Reminders.prefs(context), Content.load(context))
        Reminders.reschedule(context, model.state)
        assertNotNull(shadowOf(alarms).nextScheduledAlarm)
        Reminders.reschedule(context, model.state.copy(reminderOn = false))
        assertNull(shadowOf(alarms).nextScheduledAlarm)
    }

    @Test fun rebootReschedules() {
        RescheduleReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertNotNull(shadowOf(alarms).nextScheduledAlarm)
    }
}

/** Each ပုတီး with a reminder gets its own alarm and notification. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecitationReminderTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val nm get() = context.getSystemService(NotificationManager::class.java)
    private val alarms get() = context.getSystemService(AlarmManager::class.java)

    private fun model(): AppModel {
        Reminders.prefs(context).edit().clear().putBoolean("setupDone", true).commit()
        return AppModel(Reminders.prefs(context), Content.load(context)).also { m -> m.onRemindersChanged = { Reminders.reschedule(context, it) } }
    }

    @Test fun ownPutteeReminderSchedulesAndPosts() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val m = model()
        val id = m.addRecitation("မေတ္တာပို့", "သဗ္ဗေ သတ္တာ", 108, 3, reminderOn = true, reminderMinutes = 20 * 60)
        assertEquals(1, shadowOf(alarms).scheduledAlarms.size)

        val fire = shadowOf(alarms).scheduledAlarms.single().operation
        val intent = shadowOf(fire).savedIntent
        ReminderReceiver().onReceive(context, intent)
        val n = shadowOf(nm).allNotifications.single()
        assertEquals("မေတ္တာပို့", n.extras.getString(NotificationCompat.EXTRA_TITLE))
        assertTrue(n.extras.getCharSequence(NotificationCompat.EXTRA_TEXT).toString().startsWith("ယနေ့ ၀ / ၃ ပတ်"))

        // Turning the main ကိုးနဝင်း reminder on adds a second, separate alarm.
        m.setReminder(true)
        assertEquals(2, shadowOf(alarms).scheduledAlarms.size)
        // Deleting the ပုတီး cancels its alarm.
        m.deleteRecitation(id)
        assertEquals(1, shadowOf(alarms).scheduledAlarms.size)
    }

    @Test fun noReminderWhenTodayIsDone() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val m = model()
        val id = m.addRecitation("ဂုဏ်တော်", "", 2, 1, reminderOn = true)
        m.count(com.satepadee.app.data.Target.Custom(id)); m.count(com.satepadee.app.data.Target.Custom(id))
        Reminders.post(context, id)
        assertEquals(0, shadowOf(nm).allNotifications.size)
    }
}
