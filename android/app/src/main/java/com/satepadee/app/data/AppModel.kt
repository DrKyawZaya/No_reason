package com.satepadee.app.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

data class Progress(val beads: Int = 0, val rounds: Int = 0)

/** A recitation the user made: one text per bead, with a daily target. */
data class Recitation(
    val id: String,
    val name: String,
    val text: String,
    val perRound: Int,
    val dailyRounds: Int,
    /** Progress belongs to this epoch day; on any other day it starts from zero. */
    val progressDay: Long = -1,
    val progress: Progress = Progress(),
)

data class AppState(
    /** Epoch day of ကိုးနဝင်း day 1 (always a Monday), or null before the user starts. */
    val start: Long? = null,
    val done: Set<Int> = emptySet(),
    /** Progress belongs to [progressDay]; any other day starts from zero. */
    val progressDay: Int = -1,
    val progress: Progress = Progress(),
    val wishes: Map<Int, String> = emptyMap(),
    val mode: CountMode = CountMode.Tap,
    val woodId: String? = null,
    val birthDay: String? = null,
    val free: Progress = Progress(),
    val recitations: List<Recitation> = emptyList(),
    /** Beads counted per epoch day, across all recitations. */
    val history: Map<Long, Int> = emptyMap(),
    val reminderOn: Boolean = false,
    /** Minutes after midnight. */
    val reminderMinutes: Int = 5 * 60 + 30,
    val vegetarianReminder: Boolean = true,
)

enum class CountMode { Tap, Swipe }

/** What the counter is counting. [key] survives process death in saved UI state. */
sealed interface Target {
    val key: String
    data object Kozawin : Target { override val key = "kozawin" }
    data object Free : Target { override val key = "free" }
    data class Custom(val id: String) : Target { override val key = "custom:$id" }

    companion object {
        fun fromKey(key: String): Target = when {
            key == Kozawin.key -> Kozawin
            key.startsWith("custom:") -> Custom(key.removePrefix("custom:"))
            else -> Free
        }
    }
}

enum class CountResult { Bead, Round, DayDone, StageDone, ProgramDone, GoalDone }

/** Consecutive days with at least one bead, ending today (or yesterday, if today has none yet). */
fun streak(history: Map<Long, Int>, today: Long): Int {
    var day = if ((history[today] ?: 0) > 0) today else today - 1
    var n = 0
    while ((history[day] ?: 0) > 0) { n++; day-- }
    return n
}

/** All app state, saved to SharedPreferences on every change so a count is never lost. */
class AppModel(private val prefs: SharedPreferences, val content: Content) {
    var state by mutableStateOf(load())
        private set
    var today by mutableLongStateOf(Days.today())
        private set

    /** Called after any change that affects reminders. */
    var onRemindersChanged: (AppState) -> Unit = {}

    fun refreshToday() { today = Days.today() }

    /** Index of today in the program; negative before the start date, ≥ 81 after the end. */
    val dayIndex: Int? get() = state.start?.let { (today - it).toInt() }

    fun todayDay(): Kozawin.Day? = dayIndex?.takeIf { it in 0 until Kozawin.TOTAL_DAYS }?.let(Kozawin::day)

    fun missedDays(): List<Int> {
        val i = dayIndex ?: return emptyList()
        return (0 until minOf(i, Kozawin.TOTAL_DAYS)).filter { it !in state.done }
    }

    fun todayProgress(): Progress = if (state.progressDay == dayIndex) state.progress else Progress()

    fun recitation(id: String): Recitation? = state.recitations.firstOrNull { it.id == id }

    fun recitationProgress(r: Recitation): Progress = if (r.progressDay == today) r.progress else Progress()

    fun progressFor(target: Target): Progress = when (target) {
        Target.Kozawin -> todayProgress()
        Target.Free -> state.free
        is Target.Custom -> recitation(target.id)?.let(::recitationProgress) ?: Progress()
    }

    fun beadsPerRound(target: Target): Int =
        (target as? Target.Custom)?.let { recitation(it.id)?.perRound } ?: Kozawin.BEADS_PER_ROUND

    fun targetRounds(target: Target): Int = when (target) {
        Target.Kozawin -> todayDay()?.rounds ?: 0
        Target.Free -> 0
        is Target.Custom -> recitation(target.id)?.dailyRounds ?: 0
    }

    fun wood(): Wood = content.woods.firstOrNull { it.id == state.woodId }
        ?: content.woods.firstOrNull { it.day == state.birthDay }
        ?: content.woods.first { it.id == "padauk" }

    fun streakDays(): Int = streak(state.history, today)

    fun count(target: Target): CountResult {
        val per = beadsPerRound(target)
        val p = progressFor(target)
        var beads = p.beads + 1
        var rounds = p.rounds
        var result = CountResult.Bead
        if (beads >= per) { beads = 0; rounds++; result = CountResult.Round }
        val next = Progress(beads, rounds)
        val history = state.history + (today to (state.history[today] ?: 0) + 1)

        when (target) {
            Target.Free -> update(state.copy(free = next, history = history))
            is Target.Custom -> {
                val r = recitation(target.id) ?: return result
                if (result == CountResult.Round && rounds == r.dailyRounds) result = CountResult.GoalDone
                val updated = r.copy(progressDay = today, progress = next)
                update(state.copy(recitations = state.recitations.map { if (it.id == r.id) updated else it }, history = history))
            }
            Target.Kozawin -> {
                val day = todayDay() ?: return result
                var done = state.done
                if (rounds >= day.rounds && day.index !in done) {
                    done = done + day.index
                    result = when {
                        day.index == Kozawin.TOTAL_DAYS - 1 -> CountResult.ProgramDone
                        day.lastOfStage -> CountResult.StageDone
                        else -> CountResult.DayDone
                    }
                }
                update(state.copy(progressDay = day.index, progress = next, done = done, history = history))
            }
        }
        return result
    }

    fun start() {
        update(state.copy(start = Days.nextMondayOrToday(today), done = emptySet(), wishes = emptyMap(), progressDay = -1, progress = Progress()))
    }

    /** "I recited without the app" — count every missed day as done. */
    fun markMissedDone() = update(state.copy(done = state.done + missedDays()))

    /** Start the stage of the first missed day again, from today. */
    fun restartStage() {
        val first = missedDays().firstOrNull() ?: return
        val stageStart = first / Kozawin.DAYS_PER_STAGE * Kozawin.DAYS_PER_STAGE
        update(state.copy(start = today - stageStart, done = state.done.filter { it < stageStart }.toSet(), progressDay = -1, progress = Progress()))
    }

    fun restartProgram() = start()

    fun setWish(stage: Int, wish: String) = update(state.copy(wishes = state.wishes + (stage to wish.trim())))
    fun setMode(mode: CountMode) = update(state.copy(mode = mode))
    fun setWood(id: String) = update(state.copy(woodId = id))
    fun setBirthDay(id: String) {
        val woodId = content.woods.firstOrNull { it.day == id }?.id ?: state.woodId
        update(state.copy(birthDay = id, woodId = woodId))
    }

    fun addRecitation(name: String, text: String, perRound: Int, dailyRounds: Int) {
        val r = Recitation("r${System.currentTimeMillis()}", name.trim(), text.trim(), perRound.coerceIn(1, 1000), dailyRounds.coerceIn(1, 999))
        update(state.copy(recitations = state.recitations + r))
    }

    fun deleteRecitation(id: String) = update(state.copy(recitations = state.recitations.filterNot { it.id == id }))

    fun setReminder(on: Boolean, minutes: Int = state.reminderMinutes, vegetarian: Boolean = state.vegetarianReminder) {
        update(state.copy(reminderOn = on, reminderMinutes = minutes, vegetarianReminder = vegetarian))
        onRemindersChanged(state)
    }

    private fun update(new: AppState) { state = new; save(new) }

    private fun load(): AppState = AppState(
        start = if (prefs.contains(K_START)) prefs.getLong(K_START, 0) else null,
        done = prefs.getString(K_DONE, "").orEmpty().split(',').mapNotNull { it.toIntOrNull() }.toSet(),
        progressDay = prefs.getInt(K_PDAY, -1),
        progress = Progress(prefs.getInt(K_PBEADS, 0), prefs.getInt(K_PROUNDS, 0)),
        wishes = prefs.getString(K_WISHES, null)?.let { s ->
            val o = JSONObject(s); o.keys().asSequence().associate { it.toInt() to o.getString(it) }
        }.orEmpty(),
        mode = runCatching { CountMode.valueOf(prefs.getString(K_MODE, "Tap")!!) }.getOrDefault(CountMode.Tap),
        woodId = prefs.getString(K_WOOD, null),
        birthDay = prefs.getString(K_BIRTH, null),
        free = Progress(prefs.getInt(K_FBEADS, 0), prefs.getInt(K_FROUNDS, 0)),
        recitations = prefs.getString(K_RECITATIONS, null)?.let { s ->
            val a = JSONArray(s)
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                Recitation(
                    o.getString("id"), o.getString("name"), o.optString("text"), o.getInt("perRound"), o.getInt("dailyRounds"),
                    o.optLong("progressDay", -1), Progress(o.optInt("beads"), o.optInt("rounds")),
                )
            }
        }.orEmpty(),
        history = prefs.getString(K_HISTORY, null)?.let { s ->
            val o = JSONObject(s); o.keys().asSequence().associate { it.toLong() to o.getInt(it) }
        }.orEmpty(),
        reminderOn = prefs.getBoolean(K_REM_ON, false),
        reminderMinutes = prefs.getInt(K_REM_MIN, 5 * 60 + 30),
        vegetarianReminder = prefs.getBoolean(K_REM_VEG, true),
    )

    private fun save(s: AppState) {
        prefs.edit().apply {
            if (s.start == null) remove(K_START) else putLong(K_START, s.start)
            putString(K_DONE, s.done.sorted().joinToString(","))
            putInt(K_PDAY, s.progressDay)
            putInt(K_PBEADS, s.progress.beads)
            putInt(K_PROUNDS, s.progress.rounds)
            putString(K_WISHES, JSONObject(s.wishes.mapKeys { it.key.toString() }).toString())
            putString(K_MODE, s.mode.name)
            putString(K_WOOD, s.woodId)
            putString(K_BIRTH, s.birthDay)
            putInt(K_FBEADS, s.free.beads)
            putInt(K_FROUNDS, s.free.rounds)
            putString(K_RECITATIONS, JSONArray(s.recitations.map {
                JSONObject().put("id", it.id).put("name", it.name).put("text", it.text).put("perRound", it.perRound)
                    .put("dailyRounds", it.dailyRounds).put("progressDay", it.progressDay)
                    .put("beads", it.progress.beads).put("rounds", it.progress.rounds)
            }).toString())
            putString(K_HISTORY, JSONObject(s.history.mapKeys { it.key.toString() }).toString())
            putBoolean(K_REM_ON, s.reminderOn)
            putInt(K_REM_MIN, s.reminderMinutes)
            putBoolean(K_REM_VEG, s.vegetarianReminder)
        }.apply()
    }

    private companion object {
        const val K_START = "start"; const val K_DONE = "done"; const val K_PDAY = "progressDay"
        const val K_PBEADS = "progressBeads"; const val K_PROUNDS = "progressRounds"; const val K_WISHES = "wishes"
        const val K_MODE = "mode"; const val K_WOOD = "wood"; const val K_BIRTH = "birthDay"
        const val K_FBEADS = "freeBeads"; const val K_FROUNDS = "freeRounds"; const val K_RECITATIONS = "recitations"
        const val K_HISTORY = "history"; const val K_REM_ON = "reminderOn"; const val K_REM_MIN = "reminderMinutes"
        const val K_REM_VEG = "vegetarianReminder"
    }
}
